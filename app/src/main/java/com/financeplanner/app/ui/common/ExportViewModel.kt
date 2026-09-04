package com.financeplanner.app.ui.common

import androidx.lifecycle.ViewModel
import com.financeplanner.app.data.export.InvestmentExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val investmentExporter: InvestmentExporter
) : ViewModel() {
    /** Suspends while writing the export files — not wrapped in viewModelScope since the
     * caller (a Composable's own coroutineScope) needs the built Intent back to launch it. */
    suspend fun buildExportShareIntent() = investmentExporter.buildShareIntent()
}
