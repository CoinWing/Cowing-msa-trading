package cowing.project.cowingmsatrading.trade.service;

import cowing.project.cowingmsatrading.trade.domain.entity.order.Status;
import cowing.project.cowingmsatrading.trade.domain.repository.OrderRepository;
import cowing.project.cowingmsatrading.trade.domain.repository.TradeRepository;
import cowing.project.cowingmsatrading.trade.dto.PendingOrderData;
import cowing.project.cowingmsatrading.trade.dto.TradeHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.AuthenticationException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final TradeRepository tradeRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<TradeHistoryResponse> getTradeHistories(String username) throws AuthenticationException {
            validateUsername(username);
            return tradeRepository.findAllByUsername(username).stream()
                    .map(TradeHistoryResponse::of)
                    .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingOrderData> getPendingOrders(String username) throws AuthenticationException {
            validateUsername(username);
            return orderRepository.findAllByUsernameAndStatus(username, Status.PENDING).stream()
                    .map(PendingOrderData::of)
                    .toList();
    }

    private void validateUsername(String username) throws AuthenticationException {
        if (username == null || username.isBlank()) {
            log.warn("사용자 아이디는 null이거나 빈 문자열일 수 없습니다.");
            throw new AuthenticationException("사용자 인증 실패");
        }
    }
}
