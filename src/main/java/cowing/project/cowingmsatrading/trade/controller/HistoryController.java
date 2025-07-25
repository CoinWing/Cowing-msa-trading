package cowing.project.cowingmsatrading.trade.controller;

import cowing.project.cowingmsatrading.trade.dto.PendingOrderData;
import cowing.project.cowingmsatrading.trade.dto.TradeHistoryResponse;
import cowing.project.cowingmsatrading.trade.service.HistoryService;
import cowing.project.cowingmsatrading.trade.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.AuthenticationException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/histories")
@Tag(name = "User Trade History", description = "사용자 매매 내역 관련 API")
@SecurityRequirement(name = "bearerAuth") // 클래스 레벨에 보안 요구사항 적용
public class HistoryController {

    private final HistoryService historyService;
    private final OrderService orderService;

    @Operation(summary = "체결 내역 조회", description = "체결 내역을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TradeHistoryResponse.class)
                    )
            }),
            @ApiResponse(responseCode = "400", description = "조회 실패(인증된 사용자가 아니거나 잘못된 요청 형식)")
    })
    @Parameter(name = "Authorization", description = "Bearer 토큰 형식의 인증 헤더", required = true, example = "Bearer your_jwt_token")
    @GetMapping("/trades")
    public ResponseEntity<List<TradeHistoryResponse>> getTradeHistories(@RequestHeader("Authorization") String authorizationHeader) throws AuthenticationException {
        return ResponseEntity.ok(historyService.getTradeHistories(orderService.extractUsernameFromToken(authorizationHeader)));
    }

    @Operation(summary = "미체결 주문 내역 조회", description = "미체결 주문 내역을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PendingOrderData.class)
                    )
            }),
            @ApiResponse(responseCode = "400", description = "조회 실패(인증된 사용자가 아니거나 잘못된 요청 형식)")
    })
    @Parameter(name = "Authorization", description = "Bearer 토큰 형식의 인증 헤더", required = true, example = "Bearer your_jwt_token")
    @GetMapping("/orders/pending")
    public ResponseEntity<List<PendingOrderData>> getPendingOrders(@RequestHeader("Authorization") String authorizationHeader) throws AuthenticationException {
        return ResponseEntity.ok(historyService.getPendingOrders(orderService.extractUsernameFromToken(authorizationHeader)));
    }
}

