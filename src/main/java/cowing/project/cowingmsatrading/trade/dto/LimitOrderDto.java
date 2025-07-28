package cowing.project.cowingmsatrading.trade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderPosition;
import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderType;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "지정가 주문 요청 DTO")
public record LimitOrderDto(
        @Schema(description = "마켓 코드", example = "KRW-BTC")
        @JsonProperty("market_code") @NotNull @NotBlank String marketCode,

        @Schema(description = "주문 가격", example = "100000000")
        @JsonProperty("order_price") @NotNull @Positive BigDecimal orderPrice,

        @Schema(description = "매매 포지션 (buy/sell)", example = "but")
        @JsonProperty("position") @NotNull @NotBlank String position,

        @Schema(description = "주문 수량", example = "1")
        @JsonProperty("order_quantity") @NotNull @Positive BigDecimal orderQuantity,

        @Schema(description = "총 주문 금액", example = "100000000")
        @JsonProperty("total_order_price") @NotNull @Positive BigDecimal totalOrderPrice,

        @Schema(description = "코인 티커", example = "BTC")
        @JsonProperty("coin_ticker") String coinTicker,

        @Schema(hidden = true)
        LocalDateTime orderRequestedAt
) implements OrderDto {
    public LimitOrderDto {
        orderRequestedAt = LocalDateTime.now();
    }

    @Override
    public Order toOrder(String username) {
        return Order.builder()
                .uuid(UUID.randomUUID().toString())
                .marketCode(marketCode)
                .orderType(OrderType.LIMIT)
                .orderPosition(OrderPosition.valueOf(position.toUpperCase()))
                .totalPrice(totalOrderPrice.longValue())
                .orderPrice(orderPrice.longValue())
                .totalQuantity(orderQuantity)
                .orderRequestedAt(orderRequestedAt)
                .status(Status.PENDING)
                .username(username)
                .build();
    }
}
