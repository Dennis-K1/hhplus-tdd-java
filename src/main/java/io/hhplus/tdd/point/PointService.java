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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<PointHistory> getUserPointHistory(long userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public UserPoint chargePoint(long userId, long amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public UserPoint usePoint(long userId, long amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
