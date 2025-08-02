package cowing.project.cowingmsatrading.trade.controller;

import cowing.project.cowingmsatrading.trade.dto.PendingOrderData;
import cowing.project.cowingmsatrading.trade.service.processor.PendingOrderManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "주문 API", description = "매수/매도 주문 및 조회/취소 관련 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final PendingOrderManager pendingOrderManager;

    @Operation(summary = "미체결 주문 취소", description = "대기열에 있는 특정 미체결 주문들을 취소합니다.")
    @ApiResponse(responseCode = "200", description = "취소가 완료되었습니다.")
    @PostMapping("/pending")
    public ResponseEntity<String> cancelOrders(@RequestBody List<PendingOrderData> ordersToCancel){
        pendingOrderManager.cancelPendingOrders(ordersToCancel);
        return ResponseEntity.ok("취소가 완료되었습니다.");
    }
}