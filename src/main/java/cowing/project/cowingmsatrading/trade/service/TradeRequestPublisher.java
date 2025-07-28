package cowing.project.cowingmsatrading.trade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import cowing.project.cowingmsatrading.trade.dto.OrderDto;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;


@Service
@RequiredArgsConstructor
public class TradeRequestPublisher {

    private final SqsTemplate sqsTemplate;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    private final String q = "sample-queue.fifo";

    public void enqueue(OrderDto orderDto, String token) throws IllegalArgumentException {
        String username = orderService.extractUsernameFromToken(token);
        Order orderToSQS = orderDto.toOrder(username);
        orderService.validateOrderPreconditions(username, orderToSQS);
        try {
            String orderDtoJson = objectMapper.writeValueAsString(orderToSQS);
            sqsTemplate.send(to -> to
                    .queue(q)
                    .messageGroupId(username)
                    .messageDeduplicationId(generateDeduplicationId(username, orderToSQS))
                    .payload(orderDtoJson)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("OrderDto를 JSON으로 변환할 수 없습니다.", e);
        }
    }

    private static String generateDeduplicationId(String username, Order order) {
        long timestamp = Instant.now().getEpochSecond();
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);

        return String.format("%s-%s-%d-%d",
                username,
                order.getMarketCode(),
                timestamp,
                random
        );
    }
}
