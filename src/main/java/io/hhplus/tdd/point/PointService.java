package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.exception.*;
import io.hhplus.tdd.point.validator.PointValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 포인트 관리 서비스
 *
 * <p>비즈니스 정책:
 * <ul>
 *   <li>최소 거래 금액: 100 포인트 (충전/사용 모두 적용)</li>
 *   <li>1회 최대 충전 한도: 100,000 포인트</li>
 *   <li>최대 보유 한도: 1,000,000 포인트</li>
 * </ul>
 *
 * <p>동시성 제어:
 * <ul>
 *   <li>유저별 락을 사용하여 동시 접근 제어</li>
 *   <li>ReentrantLock을 통해 같은 유저의 포인트 연산을 순차적으로 처리</li>
 *   <li>서로 다른 유저의 연산은 병렬로 처리 가능</li>
 * </ul>
 */
@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final PointValidator pointValidator;
    private final ConcurrentHashMap<Long, Lock> userLocks;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable, PointValidator pointValidator) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
        this.pointValidator = pointValidator;
        this.userLocks = new ConcurrentHashMap<>();
    }

    /**
     * 유저별 락 획득
     */
    private Lock getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
    }

    /**
     * 유저별 락을 사용하여 작업을 안전하게 실행
     *
     * @param userId 작업 대상 유저 ID
     * @param operation 실행할 작업
     * @param <T> 작업 결과 타입
     * @return 작업 실행 결과
     */
    private <T> T executeWithLock(long userId, Supplier<T> operation) {
        Lock lock = getUserLock(userId);
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    public UserPoint getUserPoint(long userId) {
        pointValidator.validateUserId(userId);
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getUserPointHistory(long userId) {
        pointValidator.validateUserId(userId);
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전
     *
     * @param userId 사용자 ID
     * @param amount 충전 금액
     * @return 충전 후 사용자 포인트 정보
     * @throws InvalidUserIdException 유효하지 않은 사용자 ID
     * @throws InvalidAmountException 유효하지 않은 금액
     * @throws PointLimitExceededException 충전/잔액 한도 초과
     */
    public UserPoint chargePoint(long userId, long amount) {
        pointValidator.validateUserId(userId);
        pointValidator.validateTransactionAmount(amount);
        pointValidator.validateChargeAmount(amount);

        return executeWithLock(userId, () -> {
            UserPoint currentPoint = userPointTable.selectById(userId);
            pointValidator.validateBalanceLimit(currentPoint.point(), amount);

            long newPoint = currentPoint.point() + amount;
            UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return updatedPoint;
        });
    }

    /**
     * 포인트 사용
     *
     * @param userId 사용자 ID
     * @param amount 사용 금액
     * @return 사용 후 사용자 포인트 정보
     * @throws InvalidUserIdException 유효하지 않은 사용자 ID
     * @throws InvalidAmountException 유효하지 않은 금액
     * @throws InsufficientPointException 잔액 부족
     */
    public UserPoint usePoint(long userId, long amount) {
        pointValidator.validateUserId(userId);
        pointValidator.validateTransactionAmount(amount);

        return executeWithLock(userId, () -> {
            UserPoint currentPoint = userPointTable.selectById(userId);

            if (currentPoint.point() < amount) {
                throw new InsufficientPointException(currentPoint.point(), amount);
            }

            long newPoint = currentPoint.point() - amount;
            UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);
            pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            return updatedPoint;
        });
    }
}
