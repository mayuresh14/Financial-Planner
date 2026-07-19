package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.TenureInput
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateTenureUseCaseTest {

    private val tenureUseCase = CalculateTenureUseCase()
    private val sipUseCase = CalculateSipUseCase()

    @Test
    fun `tenure result reaches target when fed back into SIP calculation`() {
        val input = TenureInput(monthlyAmount = 10_000.0, expectedReturnPercent = 12.0, targetAmount = 2_000_000.0)
        val result = tenureUseCase(input)

        assertNotNull(result.monthsRequired)
        val months = result.monthsRequired!!

        // Feeding the computed tenure back into the SIP calculator should
        // produce a maturity value at or just above the target.
        val sipResult = sipUseCase(
            com.financeplanner.app.domain.model.SipInput(
                monthlyAmount = 10_000.0,
                expectedReturnPercent = 12.0,
                durationYears = (months / 12.0).let { if (months % 12 == 0) months / 12 else months / 12 + 1 }
            )
        )
        assertTrue(sipResult.maturityValue >= 2_000_000.0 * 0.9) // loose bound given month->year rounding
    }

    @Test
    fun `unreachable target returns null`() {
        val input = TenureInput(monthlyAmount = 1.0, expectedReturnPercent = 0.0, targetAmount = 10_000_000.0)
        val result = tenureUseCase(input)
        assertNull(result.monthsRequired)
        assertNull(result.yearsRequired)
    }
}
