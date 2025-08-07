package cowing.project.cowingmsatrading.trade.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderPosition;
import cowing.project.cowingmsatrading.trade.dto.PendingOrderData;
import cowing.project.cowingmsatrading.trade.dto.PendingOrderDto;
import cowing.project.cowingmsatrading.trade.dto.TradeExecutionResult;
import cowing.project.cowingmsatrading.trade.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class PendingOrderProcessor {

    private final TradeProcessor tradeProcessor;
    private final OrderService orderService;
    private final PendingOrderSetManager pendingOrderSetManager;
    private final PendingOrderStreamManager pendingOrderStreamManager;
    private final ObjectMapper objectMapper;
    private final StreamOperations<String, Object, Object> streamOperations;

    private static final long TIMEOUT_HOURS = 5;

    public PendingOrderProcessor(
            TradeProcessor tradeProcessor,
            OrderService orderService,
            @Qualifier("pendingRedisTemplate") RedisTemplate<String, Object> pendingRedis,
            PendingOrderSetManager pendingOrderSetManager,
            PendingOrderStreamManager pendingOrderStreamManager,
            ObjectMapper objectMapper) {
        this.tradeProcessor = tradeProcessor;
        this.orderService = orderService;
        this.pendingOrderSetManager = pendingOrderSetManager;
        this.pendingOrderStreamManager = pendingOrderStreamManager;
        this.objectMapper = objectMapper;
        this.streamOperations = pendingRedis.opsForStream();
    }

    @Scheduled(fixedRate = 5000) // 5초마다 미체결 주문 확인
    public void processPendingOrders() {
        List<MapRecord<String, Object, Object>> messages = streamOperations.read(
                Consumer.from(PendingOrderConstants.GROUP_NAME.getValue(), pendingOrderStreamManager.getConsumerName()),
                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(1)),
                StreamOffset.create(PendingOrderConstants.STREAM_KEY.getValue(), ReadOffset.lastConsumed())
        );

        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> message : messages) {
            Object rawData = message.getValue().get(PendingOrderConstants.FIELD_KEY.getValue());
            PendingOrderDto pending = objectMapper.convertValue(rawData, PendingOrderDto.class);
            try {
                boolean success = this.processOrder(pending.order(), pending.remaining());
                if (success) {
                    pendingOrderSetManager.removeFromPendingSet(pending.order().getUuid());
                    streamOperations.acknowledge(PendingOrderConstants.GROUP_NAME.getValue(), message);
                    streamOperations.delete(PendingOrderConstants.STREAM_KEY.getValue(), message.getId());
                    log.info("미체결 주문이 완전히 체결되어 대기열에서 제거되었습니다. UUID: {}", pending.order().getUuid());
                } else {
                    // 체결 실패: 메시지를 삭제하고 스트림의 끝에 다시 추가
                    streamOperations.acknowledge(PendingOrderConstants.GROUP_NAME.getValue(), message);
                    streamOperations.delete(PendingOrderConstants.STREAM_KEY.getValue(), message.getId());
                    pendingOrderStreamManager.addPendingOrder(pending.order(), pending.remaining());
                }
            } catch (Exception e) {
                log.error("미체결 요청 처리 중 오류가 발생했습니다. Message : {}", e.getMessage());
                // 오류 발생 시 메시지를 삭제하고 스트림의 끝에 다시 추가
                streamOperations.acknowledge(PendingOrderConstants.GROUP_NAME.getValue(), message);
                streamOperations.delete(PendingOrderConstants.STREAM_KEY.getValue(), message.getId());
                pendingOrderStreamManager.addPendingOrder(pending.order(), pending.remaining());
                log.info("오류 발생: 메시지 {} 를 재처리하기 위해 스트림에 다시 추가합니다. (UUID: {})", message.getId(), pending.order().getUuid());
            }
        }
    }

    public boolean processOrder(Order order, BigDecimal remaining) {
        //취소 요청
        if (pendingOrderSetManager.isRequestedToCancel(order.getUuid())) {
            pendingOrderSetManager.removeCancelledOrderFlag(order.getUuid());
            log.info("취소 요청을 받은 주문이 대기열에서 제거되었습니다. UUID: {}", order.getUuid());
            return false;
        }

        // 주문이 5시간 이상 지났다면 취소 처리
        if (order.getOrderRequestedAt().isBefore(LocalDateTime.now().minusHours(TIMEOUT_HOURS))) {
            orderService.cancelOrder(order);
            log.info("주문 시간(5시간) 초과로 취소 처리되었습니다. UUID: {}", order.getUuid());
            return false;
        }
        return retryTrade(order, remaining);
    }

    private boolean retryTrade(Order order, BigDecimal remaining) {
        boolean isBuyOrder = order.getOrderPosition() == OrderPosition.BUY;
        BigDecimal limitPrice = BigDecimal.valueOf(order.getOrderPrice());

        TradeExecutionResult result;
        try {
            result = tradeProcessor.executeTradeWithCondition(order, remaining, isBuyOrder, limitPrice);
        } catch (Exception e) {
            return false;
        }

        if (result.remainingAfterTrade().compareTo(BigDecimal.ZERO) <= 0) {
            orderService.processTradeRecordsAndSettlement(order, result.tradeRecords(), result.totalQuantity(), result.totalPrice());
            return true;
        }
        return false;
    }

    public void cancelPendingOrders(List<PendingOrderData> ordersToCancel) {
        if (ordersToCancel == null || ordersToCancel.isEmpty()) return;

        for (PendingOrderData orderData : ordersToCancel) {
            if (pendingOrderSetManager.isInvalidCancelRequest(orderData)) continue;

            String uuid = orderData.uuid();
            try {
                if (!orderService.isOrderExists(uuid)) {
                    log.error("존재하지 않는 주문입니다. UUID: {}", uuid);
                    throw new IllegalArgumentException();
                }
                pendingOrderSetManager.setCancelledOrderFlag(uuid);
                orderService.cancelOrderByUuid(uuid);
                log.info("미체결 주문 취소 요청을 수신하였습니다. UUID: {}", uuid);
            } catch (Exception e) {
                log.error("주문 취소 처리 오류. UUID: {}", uuid, e);
                pendingOrderSetManager.removeCancelledOrderFlag(uuid);
            }
        }
    }
}