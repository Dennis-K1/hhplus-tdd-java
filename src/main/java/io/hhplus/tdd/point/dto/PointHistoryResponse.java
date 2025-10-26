package io.hhplus.tdd.point.dto;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;

/**
 * 포인트 거래 내역 API 응답 DTO
 *
 * <p>PointHistory 엔티티를 API 응답 형태로 변환합니다.
 */
public record PointHistoryResponse(
    long id,
    long userId,
    long amount,
    TransactionType type,
    long updateMillis
) {
    /**
     * PointHistory 엔티티로부터 응답 DTO 생성
     */
    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
            history.id(),
            history.userId(),
            history.amount(),
            history.type(),
            history.updateMillis()
        );
    }
}
