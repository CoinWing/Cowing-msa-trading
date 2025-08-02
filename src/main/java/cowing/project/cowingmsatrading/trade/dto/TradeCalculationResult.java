package cowing.project.cowingmsatrading.trade.dto;

import java.math.BigDecimal;

public record TradeCalculationResult(BigDecimal tradeQuantity, BigDecimal tradePrice, BigDecimal remainingAfterTrade) {
}
