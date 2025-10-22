package io.hhplus.tdd.point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    /**
     * 특정 유저의 포인트를 조회합니다.
     */
    @GetMapping("{id}")
    public UserPoint point(@PathVariable long id) {
        log.info("Fetching point for user: {}", id);
        return pointService.getUserPoint(id);
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회합니다.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(@PathVariable long id) {
        log.info("Fetching point history for user: {}", id);
        return pointService.getUserPointHistory(id);
    }

    /**
     * 특정 유저의 포인트를 충전합니다.
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(@PathVariable long id, @RequestBody long amount) {
        log.info("Charging {} points for user: {}", amount, id);
        return pointService.chargePoint(id, amount);
    }

    /**
     * 특정 유저의 포인트를 사용합니다.
     */
    @PatchMapping("{id}/use")
    public UserPoint use(@PathVariable long id, @RequestBody long amount) {
        log.info("Using {} points for user: {}", amount, id);
        return pointService.usePoint(id, amount);
    }
}
