package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.FireInput
import com.financeplanner.app.domain.model.FireVariant
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateFireUseCaseTest {

    private val useCase = CalculateFireUseCase()

    @Test
    fun `fat FIRE requires a larger corpus than lean FIRE`() {
        val base = FireInput(
            variant = FireVariant.TRADITIONAL, currentAge = 30, retirementAge = 50,
            currentAnnualExpenses = 1_000_000.0, inflationPercent = 6.0, preRetirementReturnPercent = 12.0
        )
        val lean = useCase(base.copy(variant = FireVariant.LEAN))
        val fat = useCase(base.copy(variant = FireVariant.FAT))
        assertTrue(fat.requiredCorpus > lean.requiredCorpus)
        assertNull(lean.isCoastFireAchieved)
    }

    @Test
    fun `coast FIRE reports achievement status`() {
        val result = useCase(
            FireInput(
                variant = FireVariant.COAST, currentAge = 30, retirementAge = 60,
                currentAnnualExpenses = 500_000.0, inflationPercent = 6.0,
                preRetirementReturnPercent = 12.0, existingCorpus = 5_000_000.0
            )
        )
        assertTrue(result.isCoastFireAchieved != null)
    }
}
