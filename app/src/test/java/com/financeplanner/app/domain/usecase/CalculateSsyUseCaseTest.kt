package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.SsyInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateSsyUseCaseTest {

    private val useCase = CalculateSsyUseCase()

    @Test
    fun `maturity value exceeds total deposited`() {
        val result = useCase(SsyInput(girlAgeAtOpening = 5, yearlyDeposit = 50_000.0, interestRatePercent = 8.2))
        assertEquals(750_000.0, result.totalDeposited, 0.01) // 50k * 15 years
        assertTrue(result.maturityValue > result.totalDeposited)
        assertEquals(26, result.girlAgeAtMaturity) // 5 + 21
    }

    @Test(expected = IllegalArgumentException::class)
    fun `age 10 or above throws`() {
        SsyInput(girlAgeAtOpening = 10, yearlyDeposit = 50_000.0)
    }
}
