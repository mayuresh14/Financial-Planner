package com.financeplanner.app.domain.usecase

import com.financeplanner.app.domain.model.AggressivenessLevel
import com.financeplanner.app.domain.model.AssetAllocationInput
import com.financeplanner.app.domain.model.AssetAllocationResult
import javax.inject.Inject

/**
 * Produces a generic asset-allocation split from a well-known rule of thumb
 * (equity% roughly "100 minus age", scaled by a risk appetite multiplier and
 * capped low near retirement), not a computed optimum. This is intentionally
 * a heuristic, not personalized advice — always shown with a disclaimer.
 */
class CalculateAssetAllocationUseCase @Inject constructor() {

    operator fun invoke(input: AssetAllocationInput): AssetAllocationResult {
        val yearsToRetirement = (input.retirementAge - input.currentAge).coerceAtLeast(0)

        val aggressivenessMultiplier = when (input.aggressiveness) {
            AggressivenessLevel.CONSERVATIVE -> 0.75
            AggressivenessLevel.MODERATE -> 1.0
            AggressivenessLevel.AGGRESSIVE -> 1.2
        }
        var equity = ((100 - input.currentAge) * aggressivenessMultiplier).coerceIn(10.0, 85.0)
        if (yearsToRetirement <= 5) equity = equity.coerceAtMost(35.0)

        val gold = when (input.aggressiveness) {
            AggressivenessLevel.CONSERVATIVE -> 12.0
            AggressivenessLevel.MODERATE -> 10.0
            AggressivenessLevel.AGGRESSIVE -> 8.0
        }
        val emergencyFund = 5.0

        var crypto = if (input.includeCrypto) {
            val base = when (input.aggressiveness) {
                AggressivenessLevel.CONSERVATIVE -> 2.0
                AggressivenessLevel.MODERATE -> 5.0
                AggressivenessLevel.AGGRESSIVE -> 8.0
            }
            if (yearsToRetirement <= 5) base / 2 else base
        } else {
            0.0
        }

        var debt = 100.0 - equity - gold - emergencyFund - crypto
        if (debt < 0) {
            val deficit = -debt
            equity = (equity - deficit).coerceAtLeast(5.0)
            debt = (100.0 - equity - gold - emergencyFund - crypto).coerceAtLeast(0.0)
        }

        // Normalize so the buckets sum to exactly 100 after all the coercions above.
        val rawTotal = equity + debt + gold + emergencyFund + crypto
        val scale = 100.0 / rawTotal

        return AssetAllocationResult(
            equityPercent = equity * scale,
            debtPercent = debt * scale,
            goldPercent = gold * scale,
            emergencyFundPercent = emergencyFund * scale,
            cryptoPercent = crypto * scale
        )
    }
}
