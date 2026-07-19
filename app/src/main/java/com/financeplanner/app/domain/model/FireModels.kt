package com.financeplanner.app.domain.model

enum class FireVariant { TRADITIONAL, LEAN, FAT, COAST }

data class FireInput(
    val variant: FireVariant,
    val currentAge: Int,
    val retirementAge: Int,
    val currentAnnualExpenses: Double,
    val inflationPercent: Double,
    val preRetirementReturnPercent: Double,
    val existingCorpus: Double = 0.0
) {
    init {
        require(currentAge in 1..100) { "Current age must be realistic" }
        require(retirementAge > currentAge) { "Retirement age must be after current age" }
        require(currentAnnualExpenses > 0) { "Current annual expenses must be positive" }
        require(inflationPercent >= 0) { "Inflation cannot be negative" }
        require(preRetirementReturnPercent >= 0) { "Expected return cannot be negative" }
        require(existingCorpus >= 0) { "Existing corpus cannot be negative" }
    }
}

data class FireResult(
    val requiredCorpus: Double,
    val requiredMonthlySip: Double,
    val isCoastFireAchieved: Boolean? = null // only meaningful for the COAST variant
)
