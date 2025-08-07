package cowing.project.cowingmsatrading.trade.dto;

import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import java.math.BigDecimal;

public record PendingOrderDto(
        Order order,
        BigDecimal remaining
) {
}
