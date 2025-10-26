package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PointService AOP 기반 동시성 제어 검증 테스트
 *
 * <p>@UserLock 어노테이션과 AOP가 올바르게 작동하는지 검증합니다.
 */
@SpringBootTest
@DisplayName("PointService 동시성 제어 테스트")
class PointServiceConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Test
    @DisplayName("동시 충전 - AOP 락이 적용되어 모든 충전이 정확히 반영됨")
    void concurrentCharges_withAOPLock() throws InterruptedException {
        // given
        long userId = 10000L;  // 고유한 userId 사용
        int threadCount = 10;
        long chargeAmount = 1000L;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint result = pointService.getUserPoint(userId);
        assertEquals(threadCount * chargeAmount, result.point(),
            "@UserLock AOP가 정상 작동하여 동시 충전이 정확히 반영되어야 함");
    }

    @Test
    @DisplayName("동시 사용 - AOP 락이 적용되어 모든 사용이 정확히 반영됨")
    void concurrentUses_withAOPLock() throws InterruptedException {
        // given
        long userId = 20000L;  // 고유한 userId 사용
        int threadCount = 10;
        long useAmount = 1000L;
        long initialAmount = threadCount * useAmount;

        // 초기 잔액 충전
        pointService.chargePoint(userId, initialAmount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint result = pointService.getUserPoint(userId);
        assertEquals(0L, result.point(),
            "@UserLock AOP가 정상 작동하여 동시 사용이 정확히 차감되어야 함");
    }

    @Test
    @DisplayName("서로 다른 사용자의 동시 작업 - 병렬 처리됨")
    void differentUsers_processedInParallel() throws InterruptedException {
        // given
        long user1 = 30000L;
        long user2 = 30001L;
        long amount = 5000L;

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        long startTime = System.currentTimeMillis();

        // when - 서로 다른 사용자에 대해 동시 충전
        executorService.submit(() -> {
            try {
                pointService.chargePoint(user1, amount);
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                pointService.chargePoint(user2, amount);
            } finally {
                latch.countDown();
            }
        });

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        long duration = System.currentTimeMillis() - startTime;

        // then
        UserPoint result1 = pointService.getUserPoint(user1);
        UserPoint result2 = pointService.getUserPoint(user2);

        assertEquals(amount, result1.point());
        assertEquals(amount, result2.point());

        // 병렬로 처리되었다면 순차 처리(~1000ms)보다 훨씬 빨라야 함
        assertTrue(duration < 800,
            "서로 다른 사용자는 병렬로 처리되어야 함 (actual: " + duration + "ms)");
    }
}
