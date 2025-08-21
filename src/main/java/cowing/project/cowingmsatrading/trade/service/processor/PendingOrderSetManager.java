package cowing.project.cowingmsatrading.trade.service.processor;

import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import cowing.project.cowingmsatrading.trade.dto.PendingOrderData;
import cowing.project.cowingmsatrading.trade.dto.PendingOrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class PendingOrderSetManager {

    private final RedisTemplate<String, Object> pendingRedis;
    private final String PENDING_ORDERS_SET_KEY; // 대기열 중복 방지 set
    private final String CANCELLED_ORDERS_KEY; // 취소 요청 set

    public PendingOrderSetManager(@Qualifier("pendingRedisTemplate") RedisTemplate<String, Object> pendingRedis) {
        this.pendingRedis = pendingRedis;
        this.PENDING_ORDERS_SET_KEY = PendingOrderConstants.PENDING_ORDERS_SET_KEY.getValue();
        this.CANCELLED_ORDERS_KEY = PendingOrderConstants.CANCELLED_ORDERS_KEY.getValue();
    }

    public void addOrderToPendingQueue(Order order, BigDecimal remaining) {
        String orderUuid = order.getUuid();

        // 이미 대기열에 있는지 확인
        if (!isRequestedToPend(orderUuid)) {
            PendingOrderDto pendingOrderDto = new PendingOrderDto(order, remaining);
            MapRecord<String, Object, Object> record = MapRecord.create(
                    PendingOrderConstants.STREAM_KEY.getValue(),
                    Map.of(PendingOrderConstants.FIELD_KEY.getValue(), pendingOrderDto)
            );
            pendingRedis.opsForStream().add(record);
            pendingRedis.opsForSet().add(PENDING_ORDERS_SET_KEY, orderUuid);

            log.info("주문 {}를 대기열에 추가했습니다.", orderUuid);
        } else {
            log.info("주문 {}는 이미 대기열에 있습니다.", orderUuid);
        }
    }

    // 대기열 중복 방지 메서드
    private boolean isRequestedToPend(String orderUuid) {
        return Boolean.TRUE.equals(pendingRedis.opsForSet().isMember(PENDING_ORDERS_SET_KEY, orderUuid));
    }

    // 대기열 중복 방지 set 데이터 삭제
    protected void removeFromPendingSet(String orderUuid) {
        pendingRedis.opsForSet().remove(PENDING_ORDERS_SET_KEY, orderUuid);
    }

    // 주문 취소 요청이 있는지 확인하는 메서드
    protected boolean isRequestedToCancel(String uuid) {
        return Boolean.TRUE.equals(pendingRedis.opsForSet().isMember(CANCELLED_ORDERS_KEY, uuid));
    }


    protected void setCancelledOrderFlag(String uuid) {
        pendingRedis.opsForSet().add(CANCELLED_ORDERS_KEY, uuid);
    }

    protected void removeCancelledOrderFlag(String uuid) {
        pendingRedis.opsForSet().remove(CANCELLED_ORDERS_KEY, uuid);
    }

    protected boolean isInvalidCancelRequest(PendingOrderData orderData) {
        String uuid = orderData.uuid();
        if (uuid == null || uuid.isBlank()) {
            log.warn("유효하지 않은 요청데이터입니다.");
            return true;
        }
        return false;
    }

}