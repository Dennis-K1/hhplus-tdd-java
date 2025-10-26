package io.hhplus.tdd.point.exception;

/**
 * 포인트 잔액이 부족한 경우 발생하는 예외
 */
public class InsufficientPointException extends PointException {

    public InsufficientPointException(String message) {
        super(message);
    }

    public InsufficientPointException(long currentBalance, long requestedAmount) {
        super("Insufficient point balance. Current: " + currentBalance + ", Requested: " + requestedAmount);
    }
}
