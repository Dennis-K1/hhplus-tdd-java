package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.exception.*;
import io.hhplus.tdd.point.validator.PointValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PointService 단위 테스트
 *
 * <p>이 테스트는 Mockito를 사용하여 PointService의 비즈니스 로직만을 순수하게 검증합니다.
 * 외부 의존성(Table, Validator)은 Mock 객체로 대체하여 테스트의 독립성을 보장합니다.
 *
 * <p>통합 테스트와 동시성 테스트는 PointServiceIntegrationTest를 참조하세요.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PointService 단위 테스트 (Mock)")
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private PointValidator pointValidator;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("유저 포인트 조회 - 성공")
    void getUserPoint_Success() {
        // given
        long userId = 1L;
        UserPoint expected = new UserPoint(userId, 5000L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(expected);

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertNotNull(result);
        assertEquals(expected, result);
        verify(pointValidator).validateUserId(userId);
        verify(userPointTable).selectById(userId);
    }

    @Test
    @DisplayName("유저 포인트 조회 - 잘못된 userId")
    void getUserPoint_InvalidUserId() {
        // given
        long invalidUserId = 0L;
        doThrow(new InvalidUserIdException(invalidUserId))
            .when(pointValidator).validateUserId(invalidUserId);

        // when & then
        assertThrows(InvalidUserIdException.class, () -> {
            pointService.getUserPoint(invalidUserId);
        });
        verify(pointValidator).validateUserId(invalidUserId);
        verify(userPointTable, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("유저 포인트 히스토리 조회 - 성공")
    void getUserPointHistory_Success() {
        // given
        long userId = 1L;
        List<PointHistory> expected = List.of(
            new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
            new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expected);

        // when
        List<PointHistory> result = pointService.getUserPointHistory(userId);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expected, result);
        verify(pointValidator).validateUserId(userId);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("포인트 충전 - 성공")
    void chargePoint_Success() {
        // given
        long userId = 1L;
        long amount = 1000L;
        UserPoint currentPoint = new UserPoint(userId, 5000L, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, 6000L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(currentPoint);
        when(userPointTable.insertOrUpdate(eq(userId), eq(6000L))).thenReturn(updatedPoint);
        when(pointHistoryTable.insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong()))
            .thenReturn(new PointHistory(1L, userId, amount, TransactionType.CHARGE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.chargePoint(userId, amount);

        // then
        assertNotNull(result);
        assertEquals(6000L, result.point());
        verify(pointValidator).validateUserId(userId);
        verify(pointValidator).validateTransactionAmount(amount);
        verify(pointValidator).validateChargeAmount(amount);
        verify(pointValidator).validateBalanceLimit(currentPoint.point(), amount);
        verify(userPointTable).selectById(userId);
        verify(userPointTable).insertOrUpdate(userId, 6000L);
        verify(pointHistoryTable).insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("포인트 충전 - 잘못된 금액")
    void chargePoint_InvalidAmount() {
        // given
        long userId = 1L;
        long invalidAmount = -100L;
        doThrow(InvalidAmountException.zeroOrNegative(invalidAmount))
            .when(pointValidator).validateTransactionAmount(invalidAmount);

        // when & then
        assertThrows(InvalidAmountException.class, () -> {
            pointService.chargePoint(userId, invalidAmount);
        });
        verify(pointValidator).validateUserId(userId);
        verify(pointValidator).validateTransactionAmount(invalidAmount);
        verify(userPointTable, never()).selectById(anyLong());
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
    }

    @Test
    @DisplayName("포인트 충전 - 충전 한도 초과")
    void chargePoint_ExceedsChargeLimit() {
        // given
        long userId = 1L;
        long exceedAmount = 100_001L;
        doThrow(PointLimitExceededException.chargeLimit(exceedAmount, 100_000L))
            .when(pointValidator).validateChargeAmount(exceedAmount);

        // when & then
        assertThrows(PointLimitExceededException.class, () -> {
            pointService.chargePoint(userId, exceedAmount);
        });
        verify(pointValidator).validateUserId(userId);
        verify(pointValidator).validateTransactionAmount(exceedAmount);
        verify(pointValidator).validateChargeAmount(exceedAmount);
        verify(userPointTable, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("포인트 충전 - 최대 잔액 한도 초과")
    void chargePoint_ExceedsBalanceLimit() {
        // given
        long userId = 1L;
        long amount = 100_000L;
        UserPoint currentPoint = new UserPoint(userId, 950_000L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(currentPoint);
        doThrow(PointLimitExceededException.balanceLimit(1_050_000L, 1_000_000L))
            .when(pointValidator).validateBalanceLimit(currentPoint.point(), amount);

        // when & then
        assertThrows(PointLimitExceededException.class, () -> {
            pointService.chargePoint(userId, amount);
        });
        verify(pointValidator).validateUserId(userId);
        verify(pointValidator).validateTransactionAmount(amount);
        verify(pointValidator).validateChargeAmount(amount);
        verify(userPointTable).selectById(userId);
        verify(pointValidator).validateBalanceLimit(currentPoint.point(), amount);
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
    }

    @Test
    @DisplayName("포인트 사용 - 성공")
    void usePoint_Success() {
        // given
        long userId = 1L;
        long amount = 1000L;
        UserPoint currentPoint = new UserPoint(userId, 5000L, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, 4000L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(currentPoint);
        when(userPointTable.insertOrUpdate(eq(userId), eq(4000L))).thenReturn(updatedPoint);
        when(pointHistoryTable.insert(eq(userId), eq(amount), eq(TransactionType.USE), anyLong()))
            .thenReturn(new PointHistory(1L, userId, amount, TransactionType.USE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.usePoint(userId, amount);

        // then
        assertNotNull(result);
        assertEquals(4000L, result.point());
        verify(pointValidator).validateUserId(userId);
        verify(pointValidator).validateTransactionAmount(amount);
        verify(userPointTable).selectById(userId);
        verify(userPointTable).insertOrUpdate(userId, 4000L);
        verify(pointHistoryTable).insert(eq(userId), eq(amount), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("포인트 사용 - 잔액 부족")
    void usePoint_InsufficientBalance() {
        // given
        long userId = 1L;
        long amount = 10000L;
        UserPoint currentPoint = new UserPoint(userId, 5000L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(currentPoint);

        // when & then
        assertThrows(InsufficientPointException.class, () -> {
            pointService.usePoint(userId, amount);
        });
        verify(pointValidator).validateUserId(userId);
        verify(pointValidator).validateTransactionAmount(amount);
        verify(userPointTable).selectById(userId);
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("포인트 사용 - 잘못된 금액")
    void usePoint_InvalidAmount() {
        // given
        long userId = 1L;
        long invalidAmount = 0L;
        doThrow(InvalidAmountException.zeroOrNegative(invalidAmount))
            .when(pointValidator).validateTransactionAmount(invalidAmount);

        // when & then
        assertThrows(InvalidAmountException.class, () -> {
            pointService.usePoint(userId, invalidAmount);
        });
        verify(pointValidator).validateUserId(userId);
        verify(pointValidator).validateTransactionAmount(invalidAmount);
        verify(userPointTable, never()).selectById(anyLong());
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
    }
}
