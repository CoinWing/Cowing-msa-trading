package cowing.project.cowingmsatrading.global.exception;

// 이것은 OrderPendingException입니다
public class OrderPendingException extends RuntimeException {
    public OrderPendingException(String message) {
        super(message);
    }
}
