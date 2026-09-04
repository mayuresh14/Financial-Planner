package com.financeplanner.app.domain.model

/**
 * For calculators where the result is genuinely ambiguous — a SIP could be
 * a real recurring investment you hold, or just a "what-if" scenario —
 * this is which the user chose when saving. Investments-target saves go to
 * [SavedInvestmentType]; Calculation-target saves go to [SavedCalculationType].
 */
enum class SaveTarget { CALCULATION, INVESTMENT }
