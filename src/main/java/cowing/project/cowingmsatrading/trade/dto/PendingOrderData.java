package cowing.project.cowingmsatrading.trade.dto;

import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderPosition;
import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderType;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;


@Schema(description = "미체결 주문 관련 DTO")
public record PendingOrderData(
        @Schema(description = "주문 UUID", example = "c0e2b0a0-8b7a-4b0e-8b0a-0e2b0a0e2b0a")
        String uuid,
        @Schema(description = "마켓 코드", example = "KRW-BTC")
        String marketCode,
        @Schema(description = "주문 타입", example = "LIMIT")
        OrderType orderType,
        @Schema(description = "주문 포지션", example = "sell")
        OrderPosition orderPosition,
        @Schema(description = "주문 가격", example = "50000000")
        Long orderPrice,
        @Schema(description = "총 주문 금액", example = "5000000")
        Long totalPrice,
        @Schema(description = "주문 수량", example = "0.1")
        BigDecimal totalQuantity,
        @Schema(description = "주문 요청 시간", example = "2023-10-01T12:00:00")
        LocalDateTime orderRequestedAt,
        @Schema(description = "주문 상태", example = "PENDING")
        Status status
) {
    public static PendingOrderData of(Order order) {
        return new PendingOrderData(
                order.getUuid(),
                order.getMarketCode(),
                order.getOrderType(),
                order.getOrderPosition(),
                order.getOrderPrice(),
                order.getTotalPrice(),
                order.getTotalQuantity().setScale(6, RoundingMode.HALF_UP),
                order.getOrderRequestedAt(),
                order.getStatus()
        );
    }
}
