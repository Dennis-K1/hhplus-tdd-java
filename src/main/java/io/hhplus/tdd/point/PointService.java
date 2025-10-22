package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint getUserPoint(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than 0");
        }
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getUserPointHistory(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than 0");
        }
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint chargePoint(long userId, long amount) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than 0");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        // Get current point
        UserPoint currentPoint = userPointTable.selectById(userId);
        long newPoint = currentPoint.point() + amount;

        // Update point
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);

        // Record history
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return updatedPoint;
    }

    public UserPoint usePoint(long userId, long amount) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than 0");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        // Get current point
        UserPoint currentPoint = userPointTable.selectById(userId);

        // Check sufficient balance
        if (currentPoint.point() < amount) {
            throw new IllegalArgumentException("Insufficient point balance");
        }

        long newPoint = currentPoint.point() - amount;

        // Update point
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);

        // Record history
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

        return updatedPoint;
    }
}
