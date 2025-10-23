package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 포인트 관리 서비스
 *
 * <p>비즈니스 정책:
 * <ul>
 *   <li>최소 거래 금액: 100 포인트 (충전/사용 모두 적용)</li>
 *   <li>1회 최대 충전 한도: 100,000 포인트</li>
 *   <li>최대 보유 한도: 1,000,000 포인트</li>
 * </ul>
 */
@Service
public class PointService {

    private static final long MIN_TRANSACTION_AMOUNT = 100L;
    private static final long MAX_CHARGE_AMOUNT = 100_000L;
    private static final long MAX_BALANCE = 1_000_000L;

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final ConcurrentHashMap<Long, Lock> userLocks;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
        this.userLocks = new ConcurrentHashMap<>();
    }

    /**
     * 유저별 락 획득
     */
    private Lock getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
    }

    /**
     * 유저 ID 유효성 검사
     */
    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than 0");
        }
    }

    /**
     * 거래 금액 유효성 검사 (충전/사용 공통)
     */
    private void validateTransactionAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (amount < MIN_TRANSACTION_AMOUNT) {
            throw new IllegalArgumentException("Transaction amount must be at least " + MIN_TRANSACTION_AMOUNT);
        }
    }

    public UserPoint getUserPoint(long userId) {
        validateUserId(userId);
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getUserPointHistory(long userId) {
        validateUserId(userId);
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전
     *
     * @param userId 사용자 ID
     * @param amount 충전 금액
     * @return 충전 후 사용자 포인트 정보
     * @throws IllegalArgumentException 유효하지 않은 입력값 또는 정책 위반 시
     */
    public UserPoint chargePoint(long userId, long amount) {
        validateUserId(userId);
        validateTransactionAmount(amount);

        if (amount > MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("Charge amount cannot exceed " + MAX_CHARGE_AMOUNT);
        }

        Lock lock = getUserLock(userId);
        lock.lock();
        try {
            UserPoint currentPoint = userPointTable.selectById(userId);
            long newPoint = currentPoint.point() + amount;

            if (newPoint > MAX_BALANCE) {
                throw new IllegalArgumentException("Point balance cannot exceed " + MAX_BALANCE);
            }

            UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return updatedPoint;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 포인트 사용
     *
     * @param userId 사용자 ID
     * @param amount 사용 금액
     * @return 사용 후 사용자 포인트 정보
     * @throws IllegalArgumentException 유효하지 않은 입력값, 잔액 부족 또는 정책 위반 시
     */
    public UserPoint usePoint(long userId, long amount) {
        validateUserId(userId);
        validateTransactionAmount(amount);

        Lock lock = getUserLock(userId);
        lock.lock();
        try {
            UserPoint currentPoint = userPointTable.selectById(userId);

            if (currentPoint.point() < amount) {
                throw new IllegalArgumentException("Insufficient point balance");
            }

            long newPoint = currentPoint.point() - amount;

            UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);
            pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            return updatedPoint;
        } finally {
            lock.unlock();
        }
    }
}
