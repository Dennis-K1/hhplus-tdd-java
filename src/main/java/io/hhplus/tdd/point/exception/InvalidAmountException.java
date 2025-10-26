package io.hhplus.tdd.point.exception;

/**
 * 유효하지 않은 금액으로 요청한 경우 발생하는 예외
 */
public class InvalidAmountException extends PointException {

    public InvalidAmountException(String message) {
        super(message);
    }

    public static InvalidAmountException zeroOrNegative(long amount) {
        return new InvalidAmountException("Amount must be greater than 0, but was: " + amount);
    }

    public static InvalidAmountException belowMinimum(long amount, long minimum) {
        return new InvalidAmountException(
            "Transaction amount must be at least " + minimum + ", but was: " + amount
        );
    }
}
