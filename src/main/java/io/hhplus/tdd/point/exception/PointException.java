package io.hhplus.tdd.point.exception;

/**
 * 포인트 도메인에서 발생하는 모든 예외의 최상위 클래스
 */
public class PointException extends RuntimeException {

    public PointException(String message) {
        super(message);
    }

    public PointException(String message, Throwable cause) {
        super(message, cause);
    }
}
