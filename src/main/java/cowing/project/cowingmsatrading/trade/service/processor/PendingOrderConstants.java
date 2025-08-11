package cowing.project.cowingmsatrading.trade.service.processor;

import lombok.Getter;

@Getter
public enum PendingOrderConstants {
    STREAM_KEY("trading:pending:orders"),
    GROUP_NAME("trading-group"),
    FIELD_KEY("pendingOrder"),
    PENDING_ORDERS_SET_KEY("pending_orders"),
    CANCELLED_ORDERS_KEY("trading:cancelled:orders");

    private final String value;

    PendingOrderConstants(String value) {
        this.value = value;
    }

}

