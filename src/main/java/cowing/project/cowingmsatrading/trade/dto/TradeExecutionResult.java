package cowing.project.cowingmsatrading.trade.dto;

import cowing.project.cowingmsatrading.trade.domain.entity.order.Trade;

import java.math.BigDecimal;
import java.util.List;

public record TradeExecutionResult(List<Trade> tradeRecords, BigDecimal totalQuantity, BigDecimal totalPrice, BigDecimal remainingAfterTrade) {
}
