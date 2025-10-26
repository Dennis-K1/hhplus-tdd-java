package io.hhplus.tdd.point.exception;

/**
 * 유효하지 않은 사용자 ID로 요청한 경우 발생하는 예외
 */
public class InvalidUserIdException extends PointException {

    public InvalidUserIdException(String message) {
        super(message);
    }

    public InvalidUserIdException(long userId) {
        super("Invalid user ID: " + userId + ". User ID must be greater than 0");
    }
}
