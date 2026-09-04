package com.financeplanner.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.financeplanner.app.R
import com.financeplanner.app.domain.model.AssetCategory

@Composable
fun assetCategoryLabel(category: AssetCategory): String = when (category) {
    AssetCategory.MUTUAL_FUND -> stringResource(R.string.asset_category_mutual_fund)
    AssetCategory.STOCKS -> stringResource(R.string.asset_category_stocks)
    AssetCategory.CRYPTO -> stringResource(R.string.asset_category_crypto)
    AssetCategory.GOLD -> stringResource(R.string.asset_category_gold)
    AssetCategory.OTHER -> stringResource(R.string.asset_category_other)
}

/** Distinct from [investmentTypeChartColor]'s palette so the two charts never look like the same data. */
fun assetCategoryChartColor(category: AssetCategory): Color = when (category) {
    AssetCategory.MUTUAL_FUND -> Color(0xFF6A4C93)
    AssetCategory.STOCKS -> Color(0xFF1982C4)
    AssetCategory.CRYPTO -> Color(0xFFFF595E)
    AssetCategory.GOLD -> Color(0xFFFFB800)
    AssetCategory.OTHER -> Color(0xFF8AC926)
}
