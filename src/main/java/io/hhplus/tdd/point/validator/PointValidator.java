package io.hhplus.tdd.point.validator;

import io.hhplus.tdd.point.exception.InvalidAmountException;
import io.hhplus.tdd.point.exception.InvalidUserIdException;
import io.hhplus.tdd.point.exception.PointLimitExceededException;
import org.springframework.stereotype.Component;

/**
 * 포인트 관련 검증을 담당하는 Validator
 *
 * <p>이 클래스는 포인트 도메인의 검증 로직을 중앙화하여 관리합니다.
 * 검증 규칙이 복잡해지거나 여러 도메인 간 협력이 필요한 경우
 * 이 클래스를 확장하여 사용할 수 있습니다.
 *
 * <p>예시:
 * <ul>
 *   <li>사용자 등급(일반/VIP)에 따른 충전 한도 검증</li>
 *   <li>휴면 계정의 포인트 사용 제한</li>
 *   <li>특정 시간대의 거래 제한</li>
 * </ul>
 */
@Component
public class PointValidator {

    private static final long MIN_TRANSACTION_AMOUNT = 100L;
    private static final long MAX_CHARGE_AMOUNT = 100_000L;
    private static final long MAX_BALANCE = 1_000_000L;

    /**
     * 사용자 ID 유효성 검증
     *
     * @param userId 검증할 사용자 ID
     * @throws InvalidUserIdException 유효하지 않은 사용자 ID인 경우
     */
    public void validateUserId(long userId) {
        if (userId <= 0) {
            throw new InvalidUserIdException(userId);
        }
    }

    /**
     * 거래 금액 유효성 검증 (충전/사용 공통)
     *
     * @param amount 검증할 금액
     * @throws InvalidAmountException 유효하지 않은 금액인 경우
     */
    public void validateTransactionAmount(long amount) {
        if (amount <= 0) {
            throw InvalidAmountException.zeroOrNegative(amount);
        }
        if (amount < MIN_TRANSACTION_AMOUNT) {
            throw InvalidAmountException.belowMinimum(amount, MIN_TRANSACTION_AMOUNT);
        }
    }

    /**
     * 충전 금액이 1회 충전 한도를 초과하는지 검증
     *
     * @param amount 충전 금액
     * @throws PointLimitExceededException 1회 충전 한도 초과 시
     */
    public void validateChargeAmount(long amount) {
        if (amount > MAX_CHARGE_AMOUNT) {
            throw PointLimitExceededException.chargeLimit(amount, MAX_CHARGE_AMOUNT);
        }
    }

    /**
     * 충전 후 잔액이 최대 보유 한도를 초과하는지 검증
     *
     * @param currentBalance 현재 잔액
     * @param chargeAmount 충전할 금액
     * @throws PointLimitExceededException 최대 보유 한도 초과 시
     */
    public void validateBalanceLimit(long currentBalance, long chargeAmount) {
        long newBalance = currentBalance + chargeAmount;
        if (newBalance > MAX_BALANCE) {
            throw PointLimitExceededException.balanceLimit(newBalance, MAX_BALANCE);
        }
    }

    /**
     * 최소 거래 금액 조회 (테스트 또는 외부 참조용)
     */
    public long getMinTransactionAmount() {
        return MIN_TRANSACTION_AMOUNT;
    }

    /**
     * 최대 충전 금액 조회 (테스트 또는 외부 참조용)
     */
    public long getMaxChargeAmount() {
        return MAX_CHARGE_AMOUNT;
    }

    /**
     * 최대 잔액 조회 (테스트 또는 외부 참조용)
     */
    public long getMaxBalance() {
        return MAX_BALANCE;
    }
}
