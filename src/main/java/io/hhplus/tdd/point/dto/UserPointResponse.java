package io.hhplus.tdd.point.dto;

import io.hhplus.tdd.point.UserPoint;

/**
 * 사용자 포인트 조회 API 응답 DTO
 *
 * <p>UserPoint 엔티티를 API 응답 형태로 변환합니다.
 * 엔티티와 API 응답을 분리하여 내부 구조 변경이 외부 API에 영향을 주지 않도록 합니다.
 */
public record UserPointResponse(
    long id,
    long point,
    long updateMillis
) {
    /**
     * UserPoint 엔티티로부터 응답 DTO 생성
     */
    public static UserPointResponse from(UserPoint userPoint) {
        return new UserPointResponse(
            userPoint.id(),
            userPoint.point(),
            userPoint.updateMillis()
        );
    }
}
