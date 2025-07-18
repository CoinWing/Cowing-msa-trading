package cowing.project.cowingmsatrading.trade.service;

import cowing.project.cowingmsatrading.global.config.TokenProvider;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderPosition;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Status;
import cowing.project.cowingmsatrading.trade.domain.entity.order.Trade;
import cowing.project.cowingmsatrading.trade.domain.entity.user.Portfolio;
import cowing.project.cowingmsatrading.trade.domain.repository.OrderRepository;
import cowing.project.cowingmsatrading.trade.domain.repository.PortfolioRepository;
import cowing.project.cowingmsatrading.trade.domain.repository.TradeRepository;
import cowing.project.cowingmsatrading.trade.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public void insertToOrderHistory(Order order) {
        orderRepository.save(order);
    }

    protected void processTradeRecordsAndSettlement(Order order, List<Trade> tradeRecords, BigDecimal totalQuantity, BigDecimal totalPrice) {
        transactionTemplate.execute(status -> {
            // 체결 내역 저장
            tradeRepository.saveAll(tradeRecords);

            // 포트폴리오 업데이트
            portfolioRepository.findByUsernameAndMarketCode(order.getUsername(), order.getMarketCode())
                    .ifPresentOrElse(
                            portfolio -> {
                                // 포트폴리오가 존재할 경우, 해당 포트폴리오를 업데이트한다.
                                if (order.getOrderPosition() == OrderPosition.BUY) {
                                    portfolio.setQuantity(portfolio.getQuantity().add(totalQuantity));
                                    portfolio.setTotalCost(portfolio.getTotalCost() + totalPrice.longValue());
                                    portfolio.setAverageCost(
                                            BigDecimal.valueOf(portfolio.getTotalCost())
                                                    .divide(portfolio.getQuantity(), 8, RoundingMode.HALF_UP)
                                                    .longValue()
                                    ); //BigDecimal을 기준으로 평단가 계산
                                }
                                if( order.getOrderPosition() == OrderPosition.SELL ) {
                                    // 매도일 경우
                                    portfolio.setQuantity(portfolio.getQuantity().subtract(totalQuantity));
                                    portfolio.setTotalCost(portfolio.getTotalCost() - totalPrice.longValue());

                                    // 만약 매도 후 수량이 0 이하가 되면 해당 포트폴리오를 삭제한다.
                                    if (portfolio.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                                        portfolioRepository.delete(portfolio);
                                        return;
                                    }
                                }
                                portfolioRepository.save(portfolio);
                            },
                            () ->
                                    portfolioRepository.save(
                                            Portfolio.builder()
                                                    .username(order.getUsername())
                                                    .marketCode(order.getMarketCode())
                                                    .quantity(totalQuantity)
                                                    .totalCost(totalPrice.longValue())
                                                    .averageCost(totalPrice.divide(totalQuantity, 8, RoundingMode.HALF_UP).longValue())
                                                    .build()
                                    )
                    );

            // 주문 상태를 완료로 변경
            order.setStatus(Status.COMPLETED);
            orderRepository.save(order);

            // 자산 업데이트
            userRepository.findByUsername(order.getUsername()).ifPresent(user -> {
                if (order.getOrderPosition() == OrderPosition.BUY) {
                    user.decreaseHoldings(totalPrice.longValue());
                } else {
                    user.increaseHoldings(totalPrice.longValue());
                }
                userRepository.save(user);
            });

            return null;
        });
    }

    @Transactional(readOnly = true)
    public boolean checkUserAssets(String username, Long totalPrice) {
        return userRepository.findByUsername(username)
                .map(user -> user.getUHoldings() >= totalPrice)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean checkPortfolio(String username, String marketCode, BigDecimal totalQuantity) {
        return portfolioRepository.findByUsernameAndMarketCode(username, marketCode)
                .map(portfolio -> portfolio.getQuantity().compareTo(totalQuantity) >= 0)
                .orElse(false);
    }

    public String extractUsernameFromToken(String token) {
        return tokenProvider.getUsername(token.replace("Bearer ", ""));
    }

    @Transactional
    public void cancelOrder(Order order) {
        order.setStatus(Status.CANCELLED);
        orderRepository.save(order);
    }

}
