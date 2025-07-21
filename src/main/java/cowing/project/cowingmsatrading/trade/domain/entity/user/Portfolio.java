package cowing.project.cowingmsatrading.trade.domain.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "market_code", nullable = false)
    private String marketCode;

    @Setter
    @Getter
    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Setter
    @Getter
    @Column(name = "total_cost", nullable = false)
    private Long totalCost;

    @Setter
    @Getter
    @Column(name = "average_cost", nullable = false)
    private Long averageCost;

    @Setter
    @Getter
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Portfolio(String username, String marketCode, BigDecimal quantity, Long averageCost, Long totalCost) {
        this.username = username;
        this.marketCode = marketCode;
        this.quantity = quantity;
        this.averageCost = averageCost;
        this.totalCost = totalCost;
        this.createdAt = LocalDateTime.now();
    }

    public void initializeValues() {
        this.averageCost = 0L;
        this.totalCost = 0L;
        this.quantity = BigDecimal.ZERO;
    }
}
