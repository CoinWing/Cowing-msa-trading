//package cowing.project.cowingmsatrading.trade;
//
//import cowing.project.cowingmsatrading.orderbook.RealTimeOrderbook;
//import cowing.project.cowingmsatrading.orderbook.vo.OrderbookUnitVo;
//import cowing.project.cowingmsatrading.trade.domain.entity.order.Order;
//import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderPosition;
//import cowing.project.cowingmsatrading.trade.domain.entity.order.OrderType;
//import cowing.project.cowingmsatrading.trade.service.OrderService;
//import cowing.project.cowingmsatrading.trade.service.processor.PendingOrderSetManager;
//import cowing.project.cowingmsatrading.trade.service.processor.TradeProcessor;
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//import java.math.BigDecimal;
//import java.util.Arrays;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Transactional
//@ActiveProfiles("local")
//class TradeTest {
//
//    @Autowired
//    private TradeProcessor tradeProcessor;
//
//    @MockitoBean
//    private RealTimeOrderbook realTimeOrderbook;
//
//    @MockitoBean
//    private OrderService orderService;
//
//    @MockitoBean
//    private PendingOrderSetManager pendingOrderSetManager;
//
//    @Test
//    @DisplayName("시장가 매수 주문을 성공적으로 체결한다.")
//    void executeMarketBuyOrder_Success() {
//        // given
//        Order marketBuyOrder = Order.builder()
//                .marketCode("KRW-BTC")
//                .orderType(OrderType.MARKET)
//                .orderPosition(OrderPosition.BUY)
//                .totalPrice(100000L) // 10만원
//                .username("testUser")
//                .build();
//
//        List<OrderbookUnitVo> askOrderbook = Arrays.asList(
//                new OrderbookUnitVo(50000.0, 1.0), // 5만원에 1개
//                new OrderbookUnitVo(50001.0, 1.0)  // 5만1원에 1개
//        );
//
//        given(realTimeOrderbook.getAskOrderbook(anyString())).willReturn(askOrderbook);
//
//        // when
//        tradeProcessor.startTradeExecution(marketBuyOrder);
//
//        // then
//        verify(orderService).processTradeRecordsAndSettlement(any(Order.class), any(List.class), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager, never()).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("시장가 매도 주문을 성공적으로 체결한다.")
//    void executeMarketSellOrder_Success() {
//        // given
//        Order marketSellOrder = Order.builder()
//                .marketCode("KRW-BTC")
//                .orderType(OrderType.MARKET)
//                .orderPosition(OrderPosition.SELL)
//                .totalQuantity(BigDecimal.valueOf(0.5)) // 0.5개
//                .username("testUser")
//                .build();
//
//        List<OrderbookUnitVo> bidOrderbook = Arrays.asList(
//                new OrderbookUnitVo(49999.0, 1.0),
//                new OrderbookUnitVo(49998.0, 1.0)
//        );
//
//        given(realTimeOrderbook.getBidOrderbook(anyString())).willReturn(bidOrderbook);
//
//        // when
//        tradeProcessor.startTradeExecution(marketSellOrder);
//
//        // then
//        verify(orderService).processTradeRecordsAndSettlement(any(Order.class), any(List.class), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager, never()).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("지정가 매수 주문이 즉시 체결된다.")
//    void executeLimitBuyOrder_ImmediateExecution() {
//        // given
//        Order limitBuyOrder = Order.builder()
//                .marketCode("KRW-BTC")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.BUY)
//                .orderPrice(50000L) // 5만원 지정가
//                .totalQuantity(BigDecimal.valueOf(1.0)) // 1개
//                .username("testUser")
//                .build();
//
//        List<OrderbookUnitVo> askOrderbook = List.of(
//                new OrderbookUnitVo(49999.0, 2.0) // 지정가보다 낮으므로 체결 가능
//        );
//
//        given(realTimeOrderbook.getAskOrderbook(anyString())).willReturn(askOrderbook);
//
//        // when
//        tradeProcessor.startTradeExecution(limitBuyOrder);
//
//        // then
//        verify(orderService).processTradeRecordsAndSettlement(any(Order.class), any(List.class), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager, never()).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("지정가 매수 주문이 체결되지 않고 대기열에 추가된다.")
//    void executeLimitBuyOrder_Pending() {
//        // given
//        Order limitBuyOrder = Order.builder()
//                .uuid("test-uuid-for-pending")
//                .marketCode("KRW-BTC")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.BUY)
//                .orderPrice(50000L) // 5만원 지정가
//                .totalQuantity(BigDecimal.valueOf(1.0))
//                .username("testUser")
//                .build();
//
//        // 호가 가격이 지정가보다 높아서 체결 불가
//        List<OrderbookUnitVo> askOrderbook = List.of(
//                new OrderbookUnitVo(50001.0, 2.0)
//        );
//
//        given(realTimeOrderbook.getAskOrderbook(anyString())).willReturn(askOrderbook);
//
//        // when
//        tradeProcessor.startTradeExecution(limitBuyOrder);
//
//        // then
//        verify(pendingOrderSetManager).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//        verify(orderService, never()).processTradeRecordsAndSettlement(any(Order.class), any(List.class), any(BigDecimal.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("지정가 매도 주문이 체결되지 않고 대기열에 추가된다.")
//    void executeLimitSellOrder_Pending() {
//        // given
//        Order limitSellOrder = Order.builder()
//                .uuid("test-uuid-for-pending-sell")
//                .marketCode("KRW-BTC")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.SELL)
//                .orderPrice(50000L) // 5만원 지정가
//                .totalQuantity(BigDecimal.valueOf(1.0))
//                .username("testUser")
//                .build();
//
//        // 호가 가격이 지정가보다 낮아서 체결 불가
//        List<OrderbookUnitVo> bidOrderbook = List.of(
//                new OrderbookUnitVo(49999.0, 2.0)
//        );
//
//        given(realTimeOrderbook.getBidOrderbook(anyString())).willReturn(bidOrderbook);
//
//        // when
//        tradeProcessor.startTradeExecution(limitSellOrder);
//
//        // then
//        verify(pendingOrderSetManager).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//        verify(orderService, never()).processTradeRecordsAndSettlement(any(Order.class), any(List.class), any(BigDecimal.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("지정가 매도 주문이 여러 호가에 걸쳐 모두 체결된다.")
//    void executeLimitSellOrder_Success_AcrossMultipleOrderbookLevels() {
//        // given
//        Order limitSellOrder = Order.builder()
//                .uuid("test-uuid-for-partial-sell")
//                .marketCode("KRW-BTC")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.SELL)
//                .orderPrice(50000L) // 5만원 지정가
//                .totalQuantity(BigDecimal.valueOf(2.0)) // 총 2개 주문
//                .username("testUser")
//                .build();
//
//        // 매수 호가 수량이 총 2개이므로, 매도 주문 2개가 모두 체결 가능
//        List<OrderbookUnitVo> bidOrderbook = Arrays.asList(
//                new OrderbookUnitVo(50001.0, 1.0), // 1개 체결
//                new OrderbookUnitVo(50000.0, 1.0)  // 1개 체결
//        );
//
//        given(realTimeOrderbook.getBidOrderbook(anyString())).willReturn(bidOrderbook);
//
//        // when
//        tradeProcessor.startTradeExecution(limitSellOrder);
//
//        // then
//        // 1. 전체 체결에 대한 정산 처리 호출
//        verify(orderService).processTradeRecordsAndSettlement(any(Order.class), any(List.class), any(BigDecimal.class), any(BigDecimal.class));
//
//        // 2. 대기열에 추가되지 않아야 함
//        verify(pendingOrderSetManager, never()).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//    }
//}
