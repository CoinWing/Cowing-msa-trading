package cowing.project.cowingmsatrading.trade.service;

import cowing.project.cowingmsatrading.global.exception.OrderPendingException;
import cowing.project.cowingmsatrading.orderbook.RealTimeOrderbook;
import cowing.project.cowingmsatrading.orderbook.vo.OrderbookUnitVo;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderPosition;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Trade;
import cowing.project.cowingmsatrading.trade.dto.PendingOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeProcessor {

    private final OrderService orderService;
    private final RealTimeOrderbook orderbookProvider;
    private final ConcurrentHashMap<String, PendingOrderDto> pendingOrders;

    private static final int MAX_ATTEMPTS = 5;


    public void startTradeExecution(Order orderForExecution) {

        // 주문이 유효한지 확인한다. 매수 주문일 때, 사용자의 보유 금액이 충분한지 확인하고, 매도 주문일 때, 사용자의 보유 수량이 충분한지 확인한다.
        validateCurrentOrder(orderForExecution);

        //요청 타입에 따라 각 메서드로 분기한다.
        switch (orderForExecution.getOrderType()) {
            case MARKET -> executeMarketOrder(orderForExecution);
            case LIMIT -> executeLimitOrder(orderForExecution);
        }
    }

    private void validateCurrentOrder(Order orderForExecution) {
        switch (orderForExecution.getOrderPosition()) {
            case BUY -> {
                if (!orderService.checkUserAssets(orderForExecution.getUsername(), orderForExecution.getTotalPrice())) {
                    log.error("사용자의 자산이 부족합니다. 주문을 처리할 수 없습니다.");
                    throw new IllegalStateException("사용자의 자산이 부족합니다.");
                }
            }
            case SELL -> {
                if (!orderService.checkPortfolio(orderForExecution.getUsername(), orderForExecution.getMarketCode(), orderForExecution.getTotalQuantity())) {
                    log.error("사용자의 포트폴리오에 충분한 수량이 없습니다. 주문을 처리할 수 없습니다.");
                    throw new IllegalStateException("사용자의 포트폴리오에 충분한 수량이 없습니다.");
                }
            }
        }
    }

    // 시장가 매매 주문 처리
    private void executeMarketOrder(Order order) {
        boolean isBuyOrder = confirmBuyOrder(order.getOrderPosition());
        BigDecimal remaining = initializeRemaining(isBuyOrder, order.getTotalPrice(), order.getTotalQuantity());

        TradeExecutionResult result = executeTradeWithCondition(order, remaining, isBuyOrder, null);

        orderService.processTradeRecordsAndSettlement(order, result.tradeRecords(), result.totalQuantity(), result.totalPrice());
    }

     // 지정가 매매 주문 처리
    private void executeLimitOrder(Order order) {
        boolean isBuyOrder = confirmBuyOrder(order.getOrderPosition());
        BigDecimal limitPrice = BigDecimal.valueOf(order.getOrderPrice());
        BigDecimal remainingQuantity = order.getTotalQuantity();

        TradeExecutionResult result;
        try {
            result = executeTradeWithCondition(order, remainingQuantity, isBuyOrder, limitPrice);
        } catch (OrderPendingException e) {
            log.info("지정가 주문이 " + MAX_ATTEMPTS + "번의 호가 조회 시도 후에도 체결되지 않았습니다. 대기열로 넘어갑니다. UUID: {}", e.getMessage());
            return;
        }
        orderService.processTradeRecordsAndSettlement(order, result.tradeRecords(), result.totalQuantity(), result.totalPrice());
    }

    // 조건에 따른 거래 실행 공통 로직
    protected TradeExecutionResult executeTradeWithCondition(Order order, BigDecimal remaining, boolean isBuyOrder, BigDecimal limitPrice) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<Trade> tradeRecords = new ArrayList<>();
        int attemptCount = 0;

        do {

            if (limitPrice != null && ++attemptCount >= MAX_ATTEMPTS) {
                pendingOrders.putIfAbsent(order.getUuid(), new PendingOrderDto(order, remaining));
                throw new OrderPendingException(order.getUuid());
            }

            List<OrderbookUnitVo> orderbook = getOrderbook(order.getOrderPosition(), order.getMarketCode());

            for (OrderbookUnitVo unit : orderbook) {
                BigDecimal currentPrice = BigDecimal.valueOf(unit.price());
                BigDecimal currentSize = BigDecimal.valueOf(unit.size());

                // 지정가 조건 확인 (지정가 주문인 경우에만)
                if (limitPrice != null && !isPriceConditionMet(isBuyOrder, limitPrice, currentPrice)) break;

                TradeCalculationResult calcResult = calculateTradeAmount(remaining, currentPrice, currentSize, isBuyOrder, limitPrice != null);

                if (calcResult.tradeQuantity().compareTo(BigDecimal.ZERO) == 0) break;

                remaining = calcResult.remainingAfterTrade();

                // 체결 기록 생성
                tradeRecords.add(createTradeRecord(order, calcResult.tradeQuantity(), calcResult.tradePrice()));

                totalQuantity = totalQuantity.add(calcResult.tradeQuantity());
                totalPrice = totalPrice.add(calcResult.tradePrice());

                if (checkConstraints(remaining)) {
                    remaining = BigDecimal.ZERO;
                    break;
                }
            }

        } while (!isRemainingZero(remaining));

        return new TradeExecutionResult(tradeRecords, totalQuantity, totalPrice, remaining);
    }

     // 거래 수량과 금액 계산
    private TradeCalculationResult calculateTradeAmount(BigDecimal remaining, BigDecimal currentPrice, BigDecimal currentSize, boolean isBuyOrder, boolean isLimitOrder) {
        BigDecimal tradeQuantity;
        BigDecimal tradePrice;
        BigDecimal remainingAfterTrade;

        if (isLimitOrder) {
            // 지정가 주문: 남은 수량과 호가 수량 중 작은 값
            tradeQuantity = remaining.min(currentSize);
            tradePrice = tradeQuantity.multiply(currentPrice).setScale(0, RoundingMode.HALF_UP);
            remainingAfterTrade = remaining.subtract(tradeQuantity);
        } else {
            // 시장가 주문
            BigDecimal quantityForTrade = getQuantityForTradeFromIsBuyOrder(isBuyOrder, remaining, currentPrice);
            tradeQuantity = calculateTradeQuantity(isBuyOrder, quantityForTrade, remaining, currentSize);
            tradePrice = tradeQuantity.multiply(currentPrice).setScale(0, RoundingMode.HALF_UP);

            remainingAfterTrade = isBuyOrder
                    ? remaining.subtract(tradePrice).setScale(0, RoundingMode.HALF_UP)
                    : remaining.subtract(tradeQuantity).setScale(2, RoundingMode.HALF_UP);
        }

        return new TradeCalculationResult(tradeQuantity, tradePrice, remainingAfterTrade);
    }

    // 거래 기록 생성
    private Trade createTradeRecord(Order order, BigDecimal tradeQuantity, BigDecimal tradePrice) {
        return Trade.builder()
                .orderUuid(order.getUuid())
                .marketCode(order.getMarketCode())
                .username(order.getUsername())
                .orderType(order.getOrderType())
                .orderPosition(order.getOrderPosition())
                .tradePrice(tradePrice.longValue())
                .tradeQuantity(tradeQuantity)
                .build();
    }

    // 제약 조건 확인
    private static boolean checkConstraints(BigDecimal remaining) {
        return remaining.compareTo(BigDecimal.ZERO) < 0 || remaining.compareTo(BigDecimal.ZERO) == 0 || remaining.scale() == 3 || remaining.scale() > 3;
    }

    // 매수/매도 주문에 따라 남은 금액 또는 수량 초기화
    private BigDecimal initializeRemaining(boolean isBuyOrder, Long totalPrice, BigDecimal totalQuantity) {
        return isBuyOrder
                ? BigDecimal.valueOf(totalPrice)
                : totalQuantity;
    }

    private boolean confirmBuyOrder(OrderPosition position) {
        return position == OrderPosition.BUY;
    }

    // 매수/매도 주문에 따라 체결 수량 결정
    private BigDecimal getQuantityForTradeFromIsBuyOrder(boolean isBuy, BigDecimal remaining, BigDecimal currentPrice) {
        return isBuy ? remaining.divide(currentPrice, 8, RoundingMode.HALF_UP) : remaining;
    }

    // 지정가 주문의 가격 조건 확인
    private static boolean isPriceConditionMet(boolean isBuyOrder, BigDecimal limitPrice, BigDecimal currentPrice) {
        if (isBuyOrder) {
            return limitPrice.compareTo(currentPrice) >= 0; // 매수: 지정가 >= 매도호가
        } else {
            return limitPrice.compareTo(currentPrice) <= 0; // 매도: 지정가 <= 매수호가
        }
    }

    // 체결 수량을 결정하는 로직을 담당하는 메서드
    private BigDecimal calculateTradeQuantity(boolean isBuy, BigDecimal quantityForTrade, BigDecimal remaining, BigDecimal currentSize) {
        if (isBuy) {
            // 매수일 경우: 내가 살 수 있는 수량(quantityForTrade)과 시장에 나와있는 매도 물량(currentSize) 중 더 적은 쪽으로 체결
            return quantityForTrade.min(currentSize);
        } else { // SELL
            // 매도일 경우: 내가 팔려는 남은 수량(remaining)과 시장의 매수 물량(currentSize) 중 더 적은 쪽으로 체결
            return remaining.min(currentSize);
        }
    }

    // 주문 포지션에 따라 호가 정보 조회
    private List<OrderbookUnitVo> getOrderbook(OrderPosition orderPosition, String marketCode) {
        return orderPosition == OrderPosition.BUY
                ? orderbookProvider.getAskOrderbook(marketCode)
                : orderbookProvider.getBidOrderbook(marketCode);
    }

    private boolean isRemainingZero(BigDecimal remaining) {
        return remaining.compareTo(BigDecimal.ZERO) == 0;
    }

    // 거래 계산 결과를 담는 레코드
    public record TradeCalculationResult(BigDecimal tradeQuantity, BigDecimal tradePrice, BigDecimal remainingAfterTrade) {
    }

    // 거래 실행 결과
    public record TradeExecutionResult(List<Trade> tradeRecords, BigDecimal totalQuantity, BigDecimal totalPrice, BigDecimal remainingAfterTrade) {
    }
}
