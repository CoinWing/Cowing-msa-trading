package cowing.project.cowingmsatrading.trade.service.processor;

import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderPosition;
import cowing.project.cowingmsatrading.trade.dto.PendingOrderData;
import cowing.project.cowingmsatrading.trade.dto.PendingOrderDto;
import cowing.project.cowingmsatrading.trade.dto.TradeExecutionResult;
import cowing.project.cowingmsatrading.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOrderManager {

    private final ConcurrentMap<String, PendingOrderDto> pendingOrders;
    private final TradeProcessor tradeProcessor;
    private final OrderService orderService;

    private static final long TIMEOUT_HOURS = 5;

    @Scheduled(fixedRate = 5000) // 5초마다 미체결 주문 확인
    private void processPendingOrders() {
        if (pendingOrders.isEmpty()) {
            return;
        }

        for (PendingOrderDto pending : pendingOrders.values()) {
            Order order = pending.order();
            if (order.getOrderRequestedAt().isBefore(LocalDateTime.now().minusHours(TIMEOUT_HOURS))) {
                orderService.cancelOrder(order);
                pendingOrders.remove(order.getUuid());
                log.info("주문 시간(5시간) 초과로 취소 처리되었습니다. UUID: {}", order.getUuid());
                continue;
            }

            retryTrade(order, pending.remaining());
        }
    }

    @Scheduled(fixedRate = 600000) // 10분마다 기록
    private void checkPendingOrders() {
        if (pendingOrders.isEmpty()) {
            return;
        }
        log.info("대기열에 미체결 주문이 {}건 존재합니다.", pendingOrders.size());
    }

    private void retryTrade(Order order, BigDecimal remaining) {
        boolean isBuyOrder = order.getOrderPosition() == OrderPosition.BUY;
        BigDecimal limitPrice = BigDecimal.valueOf(order.getOrderPrice());

        // TradeProcessor의 공통 로직을 재사용하여 체결 시도
        TradeExecutionResult result;
        try {
            result = tradeProcessor.executeTradeWithCondition(order, remaining, isBuyOrder, limitPrice);
        } catch (Exception e) {
            return;
        }

        if (result.remainingAfterTrade().compareTo(BigDecimal.ZERO) <= 0) {
            // 체결 후 정산 및 수량 업데이트
            orderService.processTradeRecordsAndSettlement(order, result.tradeRecords(), result.totalQuantity(), result.totalPrice());
            pendingOrders.remove(order.getUuid());
            log.info("미체결 주문이 완전히 체결되어 대기열에서 제거되었습니다. UUID: {}, 체결 수량: {}", order.getUuid(), result.totalQuantity());
        }
    }

    public void cancelPendingOrders(List<PendingOrderData> ordersToCancel) {
        if (ordersToCancel == null || ordersToCancel.isEmpty()) return;

        for (PendingOrderData orderData : ordersToCancel) {
            if (isInvalidCancelRequest(orderData)) continue;

            String uuid = orderData.uuid();
            PendingOrderDto removedOrder = pendingOrders.remove(uuid);

            if (removedOrder != null) {
                orderService.cancelOrder(removedOrder.order());
                log.info("미체결 주문이 성공적으로 취소되어 대기열에서 제거되었습니다. UUID: {}", uuid);
            } else {
                log.warn("취소하려는 주문이 대기열에 없습니다. 이미 처리되었을 수 있습니다. UUID: {}", uuid);
            }
        }
    }

    private boolean isInvalidCancelRequest(PendingOrderData orderData) {
        String uuid = orderData.uuid();
        if (uuid == null || uuid.isBlank()) {
            log.warn("유효하지 않은 요청데이터입니다.");
            return true;
        }
        return false;
    }

}
