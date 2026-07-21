package com.financeplanner.app.domain.model

/** Goal categories a user can plan for — see technical design doc §2 (Goal). */
enum class GoalType(val labelKey: String) {
    RETIREMENT("goal_type_retirement"),
    HOUSE("goal_type_house"),
    EDUCATION("goal_type_education"),
    CAR("goal_type_car"),
    CUSTOM("goal_type_custom")
}
