package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.SipVsLumpsumInput
import com.financeplanner.app.domain.model.SipVsLumpsumResult
import javax.inject.Inject

/** Thin composition over the two existing use cases — no new math. */
class CalculateSipVsLumpsumUseCase @Inject constructor(
    private val calculateSip: CalculateSipUseCase,
    private val calculateLumpsum: CalculateLumpsumUseCase
) {
    operator fun invoke(input: SipVsLumpsumInput): SipVsLumpsumResult {
        return SipVsLumpsumResult(
            sipResult = calculateSip(input.sipInput),
            lumpsumResult = calculateLumpsum(input.lumpsumInput)
        )
    }
}
