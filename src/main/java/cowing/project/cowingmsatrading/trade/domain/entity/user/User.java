package cowing.project.cowingmsatrading.trade.domain.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwd;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Authority authority;

    @Column(nullable = false)
    private Long uHoldings;

    @Column
    private LocalDateTime bankruptAt;

    @Column
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        this.authority = Authority.ROLE_USER;
    }

    public void increaseHoldings(Long totalPrice) {
        this.uHoldings = uHoldings + totalPrice;
    }
    
    public void decreaseHoldings(Long totalPrice) {
        this.uHoldings = uHoldings - totalPrice;
    }

}