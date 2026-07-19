package com.financeplanner.app.domain.model

data class SipVsLumpsumInput(
    val sipInput: SipInput,
    val lumpsumInput: LumpsumInput
)

data class SipVsLumpsumResult(
    val sipResult: SipResult,
    val lumpsumResult: LumpsumResult
)
