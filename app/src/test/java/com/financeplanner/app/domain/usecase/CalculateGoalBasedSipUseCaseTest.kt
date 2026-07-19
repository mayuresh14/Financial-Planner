package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.GoalBasedSipInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.abs

class CalculateGoalBasedSipUseCaseTest {

    private val goalUseCase = CalculateGoalBasedSipUseCase()
    private val sipUseCase = CalculateSipUseCase()

    @Test
    fun `required SIP reproduces the target when fed back into SIP calculation`() {
        val input = GoalBasedSipInput(targetAmount = 2_323_000.0, durationYears = 10, expectedReturnPercent = 12.0)
        val result = goalUseCase(input)
        assertNull(result.inflationAdjustedTarget)

        val sipResult = sipUseCase(
            com.financeplanner.app.domain.model.SipInput(
                monthlyAmount = result.requiredMonthlySip,
                expectedReturnPercent = 12.0,
                durationYears = 10
            )
        )
        assertEquals(2_323_000.0, sipResult.maturityValue, 500.0)
    }

    @Test
    fun `inflation inflates the target before solving for required SIP`() {
        val withoutInflation = goalUseCase(
            GoalBasedSipInput(targetAmount = 1_000_000.0, durationYears = 10, expectedReturnPercent = 12.0)
        )
        val withInflation = goalUseCase(
            GoalBasedSipInput(targetAmount = 1_000_000.0, durationYears = 10, expectedReturnPercent = 12.0, inflationPercent = 6.0)
        )
        assert(withInflation.requiredMonthlySip > withoutInflation.requiredMonthlySip)
        assert(abs(withInflation.inflationAdjustedTarget!! - 1_790_847.0) < 5_000.0)
    }
}
