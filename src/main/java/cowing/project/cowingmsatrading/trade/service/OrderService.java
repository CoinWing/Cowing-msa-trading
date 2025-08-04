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

    public void processTradeRecordsAndSettlement(Order order, List<Trade> tradeRecords, BigDecimal totalQuantity, BigDecimal totalPrice) {
        transactionTemplate.execute(status -> {
            // 체결 내역 저장
            tradeRepository.saveAll(tradeRecords);

            // 포트폴리오 업데이트
            portfolioRepository.findByUsernameAndMarketCode(order.getUsername(), order.getMarketCode())
                    .ifPresentOrElse(
                            portfolio -> {
                                // 포트폴리오가 존재할 경우, 해당 포트폴리오를 업데이트한다.
                                if ( order.getOrderPosition() == OrderPosition.BUY ) {
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

                                    // 만약 매도 후 수량이 0 이하가 되면 해당 포트폴리오의 내용(quantity, totalCost, averageCost)를 초기화한다.
                                    if (portfolio.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                                        portfolio.initializeValues();
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
                    user.decreaseHoldings((long) (totalPrice.longValue() + (totalPrice.longValue() * 0.0005)));
                } else {
                    user.increaseHoldings((long) (totalPrice.longValue() - (totalPrice.longValue() * 0.0005)));
                }
                userRepository.save(user);
            });

            return null;
        });
    }

    public void validateOrderPreconditions(String username, Order order) throws IllegalArgumentException {
        transactionTemplate.execute(status -> {
            userRepository.findByUsername(username)
                    .ifPresentOrElse(
                            user -> {
                                if (order.getOrderPosition() == OrderPosition.BUY) {
                                    if (user.getUHoldings() < order.getTotalPrice()) {
                                        throw new IllegalArgumentException("사용자의 재산이 부족합니다.");
                                    }
                                } else {
                                    portfolioRepository.findByUsernameAndMarketCode(username, order.getMarketCode())
                                            .ifPresentOrElse(
                                                    portfolio -> {
                                                        if (portfolio.getQuantity().compareTo(order.getTotalQuantity()) < 0) {
                                                            throw new IllegalArgumentException("매도할 수량이 부족합니다.");
                                                        }
                                                    },
                                                    () -> {
                                                        throw new IllegalArgumentException("보유한 코인이 없습니다.");
                                                    }
                                            );
                                }
                            },
                            () -> {
                                throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                            }
                    );
            orderRepository.save(order);
            return null;
        });
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
