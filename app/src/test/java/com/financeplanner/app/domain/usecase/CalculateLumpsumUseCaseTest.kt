package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.LumpsumInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateLumpsumUseCaseTest {

    private val useCase = CalculateLumpsumUseCase()

    @Test
    fun `lumpsum matches compound interest formula`() {
        // ₹1,00,000 at 12% for 10 years => 100000 * 1.12^10 ≈ 3,10,585
        val input = LumpsumInput(
            principal = 100_000.0,
            expectedReturnPercent = 12.0,
            durationYears = 10
        )

        val result = useCase(input)

        assertEquals(100_000.0, result.principal, 0.01)
        assertTrue(kotlin.math.abs(result.maturityValue - 310_585.0) < 1_000.0)
        assertNull(result.inflationAdjustedValue)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero principal throws`() {
        LumpsumInput(principal = 0.0, expectedReturnPercent = 12.0, durationYears = 5)
    }
}
