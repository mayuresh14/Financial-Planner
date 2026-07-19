package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.EmiInput
import com.financeplanner.app.domain.model.PrepaymentStrategy
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateEmiUseCaseTest {

    private val useCase = CalculateEmiUseCase()

    @Test
    fun `no prepayment leaves prepayment fields null`() {
        val result = useCase(EmiInput(loanAmount = 3_000_000.0, interestRatePercent = 9.0, tenureMonths = 240))
        assertTrue(result.emi > 0)
        assertNull(result.totalInterestWithPrepayment)
        assertNull(result.interestSaved)
    }

    @Test
    fun `reduce tenure prepayment shortens the loan and saves interest`() {
        val result = useCase(
            EmiInput(
                loanAmount = 3_000_000.0, interestRatePercent = 9.0, tenureMonths = 240,
                prepaymentAmount = 500_000.0, prepaymentAfterMonth = 24,
                prepaymentStrategy = PrepaymentStrategy.REDUCE_TENURE
            )
        )
        assertTrue(result.interestSaved!! > 0)
        assertTrue(result.monthsSaved!! > 0)
    }

    @Test
    fun `reduce EMI prepayment lowers the EMI without reporting months saved`() {
        val result = useCase(
            EmiInput(
                loanAmount = 3_000_000.0, interestRatePercent = 9.0, tenureMonths = 240,
                prepaymentAmount = 500_000.0, prepaymentAfterMonth = 24,
                prepaymentStrategy = PrepaymentStrategy.REDUCE_EMI
            )
        )
        assertTrue(result.newEmiAfterPrepayment!! < result.emi)
        assertNull(result.monthsSaved)
    }
}
