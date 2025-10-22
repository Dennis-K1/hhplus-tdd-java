package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PointService 단위 테스트")
class PointServiceTest {

    private PointService pointService;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("유저 포인트 조회 - 성공")
    void getUserPoint_Success() {
        // given
        long userId = 1L;

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(0, result.point());
    }

    @Test
    @DisplayName("유저 포인트 조회 - 잘못된 userId (0 이하)")
    void getUserPoint_InvalidUserId() {
        // given
        long invalidUserId = 0L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.getUserPoint(invalidUserId);
        });
    }

    @Test
    @DisplayName("포인트 충전 - 성공")
    void chargePoint_Success() {
        // given
        long userId = 1L;
        long amount = 1000L;

        // when
        UserPoint result = pointService.chargePoint(userId, amount);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(amount, result.point());
    }

    @Test
    @DisplayName("포인트 충전 - 여러 번 충전하면 누적됨")
    void chargePoint_Multiple() {
        // given
        long userId = 1L;

        // when
        pointService.chargePoint(userId, 1000L);
        pointService.chargePoint(userId, 500L);
        UserPoint result = pointService.chargePoint(userId, 300L);

        // then
        assertEquals(1800L, result.point());
    }

    @Test
    @DisplayName("포인트 충전 - 잘못된 금액 (0 이하)")
    void chargePoint_InvalidAmount() {
        // given
        long userId = 1L;
        long invalidAmount = -100L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, invalidAmount);
        });
    }

    @Test
    @DisplayName("포인트 사용 - 성공")
    void usePoint_Success() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L);

        // when
        UserPoint result = pointService.usePoint(userId, 300L);

        // then
        assertEquals(700L, result.point());
    }

    @Test
    @DisplayName("포인트 사용 - 잔액 부족")
    void usePoint_InsufficientBalance() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 500L);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, 1000L);
        });
    }

    @Test
    @DisplayName("포인트 사용 - 잘못된 금액 (0 이하)")
    void usePoint_InvalidAmount() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, 0L);
        });
    }

    @Test
    @DisplayName("포인트 히스토리 조회 - 빈 리스트")
    void getUserPointHistory_Empty() {
        // given
        long userId = 1L;

        // when
        List<PointHistory> result = pointService.getUserPointHistory(userId);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("포인트 히스토리 조회 - 충전 내역 기록됨")
    void getUserPointHistory_WithChargeHistory() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L);

        // when
        List<PointHistory> result = pointService.getUserPointHistory(userId);

        // then
        assertEquals(1, result.size());
        assertEquals(TransactionType.CHARGE, result.get(0).type());
        assertEquals(1000L, result.get(0).amount());
    }

    @Test
    @DisplayName("포인트 히스토리 조회 - 충전/사용 내역 모두 기록됨")
    void getUserPointHistory_WithMultipleHistory() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L);
        pointService.chargePoint(userId, 500L);
        pointService.usePoint(userId, 300L);

        // when
        List<PointHistory> result = pointService.getUserPointHistory(userId);

        // then
        assertEquals(3, result.size());
        assertEquals(TransactionType.CHARGE, result.get(0).type());
        assertEquals(TransactionType.CHARGE, result.get(1).type());
        assertEquals(TransactionType.USE, result.get(2).type());
    }

    @Test
    @DisplayName("포인트 충전 - 1회 충전 한도 초과 (100,000 초과)")
    void chargePoint_ExceedsMaxChargeLimit() {
        // given
        long userId = 1L;
        long exceedAmount = 100_001L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, exceedAmount);
        });
    }

    @Test
    @DisplayName("포인트 충전 - 최소 충전 금액 미만 (100 미만)")
    void chargePoint_BelowMinimumAmount() {
        // given
        long userId = 1L;
        long belowMinimum = 99L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, belowMinimum);
        });
    }

    @Test
    @DisplayName("포인트 충전 - 최대 보유 한도 초과 (1,000,000 초과)")
    void chargePoint_ExceedsMaxBalance() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 100_000L);
        pointService.chargePoint(userId, 100_000L);
        pointService.chargePoint(userId, 100_000L);
        pointService.chargePoint(userId, 100_000L);
        pointService.chargePoint(userId, 100_000L);
        pointService.chargePoint(userId, 100_000L);
        pointService.chargePoint(userId, 100_000L);
        pointService.chargePoint(userId, 100_000L);
        pointService.chargePoint(userId, 100_000L);
        // Total: 900,000

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, 100_001L); // Total would be 1,000,001
        });
    }

    @Test
    @DisplayName("포인트 사용 - 최소 사용 금액 미만 (100 미만)")
    void usePoint_BelowMinimumAmount() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L);
        long belowMinimum = 99L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, belowMinimum);
        });
    }
}
