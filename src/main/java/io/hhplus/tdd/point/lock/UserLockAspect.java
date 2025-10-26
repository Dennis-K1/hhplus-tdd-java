package io.hhplus.tdd.point.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @UserLock 어노테이션이 붙은 메서드에 사용자별 락을 적용하는 Aspect
 *
 * <p>이 Aspect는 메서드의 첫 번째 파라미터를 userId로 간주하여
 * 해당 사용자에 대해 ReentrantLock을 적용합니다.
 *
 * <p>동작 방식:
 * <ul>
 *   <li>각 사용자별로 별도의 Lock 인스턴스를 관리</li>
 *   <li>같은 userId에 대한 동시 호출은 순차적으로 처리</li>
 *   <li>다른 userId에 대한 호출은 병렬로 처리</li>
 * </ul>
 */
@Aspect
@Component
public class UserLockAspect {

    private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

    /**
     * @UserLock 어노테이션이 붙은 메서드를 intercept하여 락을 적용합니다.
     *
     * @param joinPoint 메서드 실행 정보
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생한 예외
     */
    @Around("@annotation(io.hhplus.tdd.point.lock.UserLock)")
    public Object applyUserLock(ProceedingJoinPoint joinPoint) throws Throwable {
        // 첫 번째 파라미터를 userId로 간주
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof Long)) {
            throw new IllegalStateException(
                "@UserLock annotation requires first parameter to be Long userId"
            );
        }

        long userId = (Long) args[0];
        Lock lock = getUserLock(userId);

        lock.lock();
        try {
            return joinPoint.proceed();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 사용자별 락을 조회하거나 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 Lock 인스턴스
     */
    private Lock getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
    }
}
