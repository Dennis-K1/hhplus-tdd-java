package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PointController 통합 테스트")
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /point/{id} - 유저 포인트 조회 성공")
    void getUserPoint_Success() throws Exception {
        // given
        long userId = 100L;

        // when & then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(0))
                .andExpect(jsonPath("$.updateMillis").isNumber());
    }

    @Test
    @DisplayName("GET /point/{id} - 잘못된 userId (0)")
    void getUserPoint_InvalidUserId() throws Exception {
        // given
        long invalidUserId = 0L;

        // when & then
        mockMvc.perform(get("/point/{id}", invalidUserId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("GET /point/{id}/histories - 포인트 히스토리 조회 (빈 리스트)")
    void getUserPointHistory_Empty() throws Exception {
        // given
        long userId = 101L;

        // when & then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /point/{id}/histories - 포인트 히스토리 조회 (충전 후)")
    void getUserPointHistory_AfterCharge() throws Exception {
        // given
        long userId = 102L;
        long chargeAmount = 1000L;

        // 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(chargeAmount))
                .andExpect(jsonPath("$[0].type").value("CHARGE"));
    }

    @Test
    @DisplayName("PATCH /point/{id}/charge - 포인트 충전 성공")
    void chargePoint_Success() throws Exception {
        // given
        long userId = 103L;
        long chargeAmount = 5000L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount))
                .andExpect(jsonPath("$.updateMillis").isNumber());
    }

    @Test
    @DisplayName("PATCH /point/{id}/charge - 여러 번 충전하면 누적됨")
    void chargePoint_Multiple() throws Exception {
        // given
        long userId = 104L;
        long firstCharge = 1000L;
        long secondCharge = 2000L;

        // when
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(firstCharge)))
                .andExpect(status().isOk());

        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondCharge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(firstCharge + secondCharge));
    }

    @Test
    @DisplayName("PATCH /point/{id}/charge - 잘못된 금액 (0)")
    void chargePoint_InvalidAmount_Zero() throws Exception {
        // given
        long userId = 105L;
        long invalidAmount = 0L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("PATCH /point/{id}/charge - 최소 금액 미만 (100 미만)")
    void chargePoint_BelowMinimum() throws Exception {
        // given
        long userId = 106L;
        long belowMinimum = 99L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(belowMinimum)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("PATCH /point/{id}/charge - 1회 충전 한도 초과")
    void chargePoint_ExceedsMaxChargeAmount() throws Exception {
        // given
        long userId = 107L;
        long exceedAmount = 100_001L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(exceedAmount)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("PATCH /point/{id}/charge - 최대 잔고 한도 초과")
    void chargePoint_ExceedsMaxBalance() throws Exception {
        // given
        long userId = 108L;
        long chargeAmount = 100_000L;

        // 9번 충전 (900,000)
        for (int i = 0; i < 9; i++) {
            mockMvc.perform(patch("/point/{id}/charge", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.valueOf(chargeAmount)))
                    .andExpect(status().isOk());
        }

        // when & then - 10번째 충전 시도 시 최대 잔고 초과
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount + 1)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("PATCH /point/{id}/use - 포인트 사용 성공")
    void usePoint_Success() throws Exception {
        // given
        long userId = 109L;
        long chargeAmount = 5000L;
        long useAmount = 2000L;

        // 먼저 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount - useAmount));
    }

    @Test
    @DisplayName("PATCH /point/{id}/use - 잔액 부족")
    void usePoint_InsufficientBalance() throws Exception {
        // given
        long userId = 110L;
        long chargeAmount = 1000L;
        long useAmount = 2000L;

        // 먼저 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("PATCH /point/{id}/use - 최소 금액 미만")
    void usePoint_BelowMinimum() throws Exception {
        // given
        long userId = 111L;
        long chargeAmount = 5000L;
        long belowMinimum = 99L;

        // 먼저 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(belowMinimum)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("에러가 발생했습니다."));
    }

    @Test
    @DisplayName("통합 시나리오 - 충전 후 사용하고 히스토리 확인")
    void integrationScenario_ChargeUseAndCheckHistory() throws Exception {
        // given
        long userId = 112L;
        long firstCharge = 10000L;
        long secondCharge = 5000L;
        long firstUse = 3000L;
        long secondUse = 2000L;

        // when - 충전 2회
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(firstCharge)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondCharge)))
                .andExpect(status().isOk());

        // when - 사용 2회
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(firstUse)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondUse)))
                .andExpect(status().isOk());

        // then - 최종 잔액 확인
        long expectedBalance = firstCharge + secondCharge - firstUse - secondUse;
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(expectedBalance));

        // then - 히스토리 확인 (4개의 트랜잭션)
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].amount").value(firstCharge))
                .andExpect(jsonPath("$[1].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].amount").value(secondCharge))
                .andExpect(jsonPath("$[2].type").value("USE"))
                .andExpect(jsonPath("$[2].amount").value(firstUse))
                .andExpect(jsonPath("$[3].type").value("USE"))
                .andExpect(jsonPath("$[3].amount").value(secondUse));
    }
}
