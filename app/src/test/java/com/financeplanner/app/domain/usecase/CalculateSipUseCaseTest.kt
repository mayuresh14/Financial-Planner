package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.SipInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.abs

class CalculateSipUseCaseTest {

    private val useCase = CalculateSipUseCase()

    @Test
    fun `regular SIP matches known formula result`() {
        // ₹10,000/month, 12% annual return, 10 years — a commonly cited
        // reference figure for this exact input is ~₹23.23 lakh maturity.
        val input = SipInput(
            monthlyAmount = 10_000.0,
            expectedReturnPercent = 12.0,
            durationYears = 10
        )

        val result = useCase(input)

        assertEquals(1_200_000.0, result.totalInvested, 0.01)
        assertTrue(
            "Expected maturity value near ₹23.2L, got ${result.maturityValue}",
            abs(result.maturityValue - 2_323_000.0) < 20_000.0
        )
        assertNull(result.inflationAdjustedValue)
    }

    @Test
    fun `step-up SIP produces higher maturity than regular SIP`() {
        val regular = useCase(
            SipInput(
                monthlyAmount = 10_000.0,
                expectedReturnPercent = 12.0,
                durationYears = 10
            )
        )
        val steppedUp = useCase(
            SipInput(
                monthlyAmount = 10_000.0,
                expectedReturnPercent = 12.0,
                durationYears = 10,
                stepUpPercent = 10.0
            )
        )

        assertTrue(steppedUp.maturityValue > regular.maturityValue)
        assertTrue(steppedUp.totalInvested > regular.totalInvested)
    }

    @Test
    fun `inflation adjustment is applied when inflation percent supplied`() {
        val result = useCase(
            SipInput(
                monthlyAmount = 10_000.0,
                expectedReturnPercent = 12.0,
                durationYears = 10,
                inflationPercent = 6.0
            )
        )

        assertNotNull(result.inflationAdjustedValue)
        assertTrue(result.inflationAdjustedValue!! < result.maturityValue)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative monthly amount throws`() {
        SipInput(monthlyAmount = -100.0, expectedReturnPercent = 12.0, durationYears = 5)
    }

    private fun assertTrue(message: String, condition: Boolean) {
        org.junit.Assert.assertTrue(message, condition)
    }
}
