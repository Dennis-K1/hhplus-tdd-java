package io.hhplus.tdd.point.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 사용자별 락을 적용하는 어노테이션
 *
 * <p>이 어노테이션이 붙은 메서드는 AOP를 통해 자동으로 사용자별 락이 적용됩니다.
 * 같은 사용자에 대한 연산은 순차적으로 처리되며, 서로 다른 사용자의 연산은 병렬로 처리됩니다.
 *
 * <p>사용 예시:
 * <pre>
 * {@code
 * @UserLock
 * public UserPoint chargePoint(long userId, long amount) {
 *     // 이 메서드는 userId에 대해 자동으로 락이 적용됨
 * }
 * }
 * </pre>
 *
 * @see UserLockAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserLock {
}
