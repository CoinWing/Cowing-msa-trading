package cowing.project.cowingmsatrading.domain.entity;

import lombok.Getter;

@Getter
public enum OrderType {
    MARKET("market"),
    LIMIT("market");

    private final String orderType;

    OrderType(String orderType) {
        this.orderType = orderType;
    }
}