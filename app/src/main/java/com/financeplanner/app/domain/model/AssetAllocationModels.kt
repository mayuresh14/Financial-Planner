package com.financeplanner.app.domain.model

enum class AggressivenessLevel { CONSERVATIVE, MODERATE, AGGRESSIVE }

data class AssetAllocationInput(
    val currentAge: Int,
    val retirementAge: Int,
    val aggressiveness: AggressivenessLevel,
    val includeCrypto: Boolean
) {
    init {
        require(currentAge in 1..100) { "Current age must be realistic" }
        require(retirementAge > currentAge) { "Retirement age must be after current age" }
    }
}

/**
 * A generic, rule-of-thumb allocation split — not a computed optimum, just a
 * commonly-used heuristic (age-based equity glide path + risk adjustment).
 * Always shown with a disclaimer that this is educational, not personalized
 * advice. Percentages sum to 100.
 */
data class AssetAllocationResult(
    val equityPercent: Double,
    val debtPercent: Double,
    val goldPercent: Double,
    val emergencyFundPercent: Double,
    val cryptoPercent: Double
)
