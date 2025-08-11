package cowing.project.cowingmsatrading.trade.service.processor;

import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import cowing.project.cowingmsatrading.trade.dto.PendingOrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class PendingOrderStreamManager {

    private final StreamOperations<String, Object, Object> streamOperations;
    private final String CONSUMER_NAME;

    private String generateConsumerName() {
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "processor_" + random + "_" + timestamp;
    }

    public PendingOrderStreamManager(
            @Qualifier("pendingRedisTemplate") RedisTemplate<String, Object> pendingRedis) {
        this.streamOperations = pendingRedis.opsForStream();
        this.CONSUMER_NAME = generateConsumerName();
        initializeStreamAndGroup();
    }

    private void initializeStreamAndGroup() {
        try {
            streamOperations.createGroup(PendingOrderConstants.STREAM_KEY.getValue(), ReadOffset.from("0"), PendingOrderConstants.GROUP_NAME.getValue());
            log.info("Consumer Group '{}' 생성 완료", PendingOrderConstants.GROUP_NAME.getValue());
        } catch (Exception e) {
            log.debug("Consumer Group '{}' 이미 존재합니다.", PendingOrderConstants.GROUP_NAME.getValue());
        }
    }

    public void addPendingOrder(Order order, BigDecimal remaining) {
        PendingOrderDto pendingOrderDto = new PendingOrderDto(order, remaining);
        MapRecord<String, Object, Object> record = MapRecord.create(PendingOrderConstants.STREAM_KEY.getValue(), Map.of(PendingOrderConstants.FIELD_KEY.getValue(), pendingOrderDto));
        streamOperations.add(record);
        log.info("미체결 주문이 삽입되었습니다. UUID:{}", order.getUuid());
    }

    @Scheduled(fixedRate = 600000) // 10분마다 기록
    private void checkPendingOrders() {
        Long size = streamOperations.size(PendingOrderConstants.STREAM_KEY.getValue());
        if (size == null || size == 0) {
            return;
        }
        log.info("대기열에 미체결 주문이 {}건 존재합니다.", size -1);
    }

    public String getConsumerName() {
        return CONSUMER_NAME;
    }
}
