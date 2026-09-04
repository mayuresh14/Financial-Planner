package com.financeplanner.app.domain.model

/**
 * What a SIP or Lumpsum investment is actually invested in — these two
 * calculators are the "could be anything market-linked" ones, unlike FD/RD/
 * PPF/EPF/SSY/NPS which are each tied to one instrument by definition.
 */
enum class AssetCategory {
    MUTUAL_FUND, STOCKS, CRYPTO, GOLD, OTHER
}
