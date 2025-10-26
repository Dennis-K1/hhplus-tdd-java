package io.hhplus.tdd.point.exception;

/**
 * 포인트 한도를 초과한 경우 발생하는 예외
 */
public class PointLimitExceededException extends PointException {

    public PointLimitExceededException(String message) {
        super(message);
    }

    public static PointLimitExceededException chargeLimit(long amount, long maxCharge) {
        return new PointLimitExceededException(
            "Charge amount cannot exceed " + maxCharge + ", but was: " + amount
        );
    }

    public static PointLimitExceededException balanceLimit(long newBalance, long maxBalance) {
        return new PointLimitExceededException(
            "Point balance cannot exceed " + maxBalance + ", but would be: " + newBalance
        );
    }
}
