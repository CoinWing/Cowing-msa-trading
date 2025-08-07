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
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("TradeProcessor 로직 테스트")
//class TradeProcessorLogicTest {
//
//    @InjectMocks
//    private TradeProcessor tradeProcessor;
//
//    @Mock
//    private RealTimeOrderbook realTimeOrderbook;
//
//    @Mock
//    private OrderService orderService;
//
//    @Mock
//    private PendingOrderSetManager pendingOrderSetManager;
//
//    @Test
//    @DisplayName("시장가 매수 주문이 성공적으로 체결된다")
//    void marketBuyOrder_Success() {
//        // Given
//        Order marketBuyOrder = Order.builder()
//                .uuid("test-uuid")
//                .marketCode("KRW-BTC")
//                .username("testUser")
//                .orderType(OrderType.MARKET)
//                .orderPosition(OrderPosition.BUY)
//                .totalPrice(150_000L)
//                .build();
//
//        List<OrderbookUnitVo> askOrderbook = Arrays.asList(
//                new OrderbookUnitVo(50000.0, 1.0),
//                new OrderbookUnitVo(50001.0, 2.0)
//        );
//        given(realTimeOrderbook.getAskOrderbook("KRW-BTC")).willReturn(askOrderbook);
//
//        // When
//        tradeProcessor.startTradeExecution(marketBuyOrder);
//
//        // Then
//        verify(orderService).processTradeRecordsAndSettlement(any(Order.class), anyList(), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager, never()).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("시장가 매도 주문이 성공적으로 체결된다")
//    void marketSellOrder_Success() {
//        // Given
//        Order marketSellOrder = Order.builder()
//                .uuid("test-uuid")
//                .marketCode("KRW-BTC")
//                .username("testUser")
//                .orderType(OrderType.MARKET)
//                .orderPosition(OrderPosition.SELL)
//                .totalQuantity(BigDecimal.valueOf(1.5))
//                .build();
//
//        List<OrderbookUnitVo> bidOrderbook = Arrays.asList(
//                new OrderbookUnitVo(50000.0, 1.0),
//                new OrderbookUnitVo(49999.0, 1.0)
//        );
//        given(realTimeOrderbook.getBidOrderbook("KRW-BTC")).willReturn(bidOrderbook);
//
//        // When
//        tradeProcessor.startTradeExecution(marketSellOrder);
//
//        // Then
//        verify(orderService).processTradeRecordsAndSettlement(any(Order.class), anyList(), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager, never()).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("지정가 매수 주문 - 즉시 전체 체결된다")
//    void limitBuyOrder_ImmediateFullExecution() {
//        // Given
//        Order limitBuyOrder = Order.builder()
//                .uuid("test-uuid")
//                .marketCode("KRW-BTC")
//                .username("testUser")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.BUY)
//                .orderPrice(50000L)
//                .totalQuantity(BigDecimal.valueOf(1.0))
//                .build();
//
//        List<OrderbookUnitVo> askOrderbook = Collections.singletonList(
//                new OrderbookUnitVo(49999.0, 2.0)
//        );
//        given(realTimeOrderbook.getAskOrderbook("KRW-BTC")).willReturn(askOrderbook);
//
//        // When
//        tradeProcessor.startTradeExecution(limitBuyOrder);
//
//        // Then
//        verify(orderService).processTradeRecordsAndSettlement(any(Order.class), anyList(), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager, never()).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("지정가 매수 주문 - 체결되지 않고 대기열에 추가된다")
//    void limitBuyOrder_Pending() {
//        // Given
//        Order limitBuyOrder = Order.builder()
//                .uuid("test-uuid")
//                .marketCode("KRW-BTC")
//                .username("testUser")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.BUY)
//                .orderPrice(50000L)
//                .totalQuantity(BigDecimal.valueOf(1.0))
//                .build();
//
//        List<OrderbookUnitVo> askOrderbook = Collections.singletonList(
//                new OrderbookUnitVo(50001.0, 2.0)
//        );
//        given(realTimeOrderbook.getAskOrderbook("KRW-BTC")).willReturn(askOrderbook);
//
//        // When
//        tradeProcessor.startTradeExecution(limitBuyOrder);
//
//        // Then
//        verify(pendingOrderSetManager).addOrderToPendingQueue(any(Order.class), eq(new BigDecimal("1.0")));
//        verify(orderService, never()).processTradeRecordsAndSettlement(any(Order.class), anyList(), any(BigDecimal.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("지정가 매수 주문 - 부분 체결되고 나머지는 대기열에 추가된다")
//    void limitBuyOrder_PartialExecution() {
//        // Given
//        Order limitBuyOrder = Order.builder()
//                .uuid("test-uuid")
//                .marketCode("KRW-BTC")
//                .username("testUser")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.BUY)
//                .orderPrice(50000L)
//                .totalQuantity(BigDecimal.valueOf(2.0))
//                .build();
//
//        // First call returns volume, subsequent calls return empty list
//        when(realTimeOrderbook.getAskOrderbook("KRW-BTC"))
//                .thenReturn(Collections.singletonList(new OrderbookUnitVo(49999.0, 1.5)))
//                .thenReturn(Collections.emptyList());
//
//        // When
//        tradeProcessor.startTradeExecution(limitBuyOrder);
//
//        // Then
//        verify(orderService, never()).processTradeRecordsAndSettlement(any(Order.class), anyList(), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager).addOrderToPendingQueue(any(Order.class), eq(new BigDecimal("0.5")));
//    }
//
//
//    @Test
//    @DisplayName("지정가 매도 주문 - 즉시 전체 체결된다")
//    void limitSellOrder_ImmediateFullExecution() {
//        // Given
//        Order limitSellOrder = Order.builder()
//                .uuid("test-uuid")
//                .marketCode("KRW-BTC")
//                .username("testUser")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.SELL)
//                .orderPrice(50000L)
//                .totalQuantity(BigDecimal.valueOf(1.0))
//                .build();
//
//        List<OrderbookUnitVo> bidOrderbook = Collections.singletonList(
//                new OrderbookUnitVo(50001.0, 2.0)
//        );
//        given(realTimeOrderbook.getBidOrderbook("KRW-BTC")).willReturn(bidOrderbook);
//
//        // When
//        tradeProcessor.startTradeExecution(limitSellOrder);
//
//        // Then
//        verify(orderService).processTradeRecordsAndSettlement(any(Order.class), anyList(), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager, never()).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("지정가 매도 주문 - 체결되지 않고 대기열에 추가된다")
//    void limitSellOrder_Pending() {
//        // Given
//        Order limitSellOrder = Order.builder()
//                .uuid("test-uuid")
//                .marketCode("KRW-BTC")
//                .username("testUser")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.SELL)
//                .orderPrice(50000L)
//                .totalQuantity(BigDecimal.valueOf(1.0))
//                .build();
//
//        List<OrderbookUnitVo> bidOrderbook = Collections.singletonList(
//                new OrderbookUnitVo(49999.0, 2.0)
//        );
//        given(realTimeOrderbook.getBidOrderbook("KRW-BTC")).willReturn(bidOrderbook);
//
//        // When
//        tradeProcessor.startTradeExecution(limitSellOrder);
//
//        // Then
//        verify(pendingOrderSetManager).addOrderToPendingQueue(any(Order.class), eq(new BigDecimal("1.0")));
//        verify(orderService, never()).processTradeRecordsAndSettlement(any(Order.class), anyList(), any(BigDecimal.class), any(BigDecimal.class));
//    }
//
//    @Test
//    @DisplayName("지정가 매도 주문 - 부분 체결되고 나머지는 대기열에 추가된다")
//    void limitSellOrder_PartialExecution() {
//        // Given
//        Order limitSellOrder = Order.builder()
//                .uuid("test-uuid")
//                .marketCode("KRW-BTC")
//                .username("testUser")
//                .orderType(OrderType.LIMIT)
//                .orderPosition(OrderPosition.SELL)
//                .orderPrice(50000L)
//                .totalQuantity(BigDecimal.valueOf(2.0))
//                .build();
//
//        when(realTimeOrderbook.getBidOrderbook("KRW-BTC"))
//                .thenReturn(Collections.singletonList(new OrderbookUnitVo(50000.0, 1.5)))
//                .thenReturn(Collections.emptyList());
//
//        // When
//        tradeProcessor.startTradeExecution(limitSellOrder);
//
//        // Then
//        verify(orderService, never()).processTradeRecordsAndSettlement(any(Order.class), anyList(), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager).addOrderToPendingQueue(any(Order.class), eq(new BigDecimal("0.5")));
//    }
//
//    @Test
//    @DisplayName("거래 처리 중 예외 발생 시 후속 처리를 하지 않는다")
//    void tradeExecution_ThrowsException() {
//        // Given
//        Order marketBuyOrder = Order.builder()
//                .uuid("test-uuid")
//                .marketCode("KRW-BTC")
//                .username("testUser")
//                .orderType(OrderType.MARKET)
//                .orderPosition(OrderPosition.BUY)
//                .totalPrice(100000L)
//                .build();
//
//        given(realTimeOrderbook.getAskOrderbook(anyString())).willThrow(new RuntimeException("Orderbook service is down"));
//
//        // When & Then
//        Assertions.assertThrows(RuntimeException.class, () -> {
//            tradeProcessor.startTradeExecution(marketBuyOrder);
//        });
//
//        verify(orderService, never()).processTradeRecordsAndSettlement(any(Order.class), anyList(), any(BigDecimal.class), any(BigDecimal.class));
//        verify(pendingOrderSetManager, never()).addOrderToPendingQueue(any(Order.class), any(BigDecimal.class));
//    }
//}
