package com.financeplanner.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_investments")
data class SavedInvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val customName: String,
    val institutionName: String?,
    val notes: String?,
    val detailsJson: String,
    val lastComputedValue: Double?,
    val createdAt: Long
)
