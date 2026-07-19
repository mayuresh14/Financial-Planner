package com.financeplanner.app.domain.model

/** Goal categories a user can plan for — see technical design doc §2 (Goal). */
enum class GoalType(val labelKey: String) {
    RETIREMENT("goal_type_retirement"),
    HOUSE("goal_type_house"),
    EDUCATION("goal_type_education"),
    CAR("goal_type_car"),
    CUSTOM("goal_type_custom")
}

/**
 * A calculated (not yet saved) goal plan. Phase 1 supports calculating a
 * plan for any goal without login; saving/tracking it requires an account,
 * gated behind ComingSoonSheet until Phase 2's backend lands.
 */
data class GoalPlanInput(
    val name: String,
    val type: GoalType,
    val targetAmount: Double,
    val durationYears: Int,
    val expectedReturnPercent: Double,
    val inflationPercent: Double? = null
)

data class GoalPlanResult(
    val requiredMonthlySip: Double,
    val inflationAdjustedTarget: Double?
)
