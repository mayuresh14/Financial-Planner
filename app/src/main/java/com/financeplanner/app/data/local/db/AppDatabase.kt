package com.financeplanner.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedInvestmentEntity::class, SavedCalculationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedInvestmentDao(): SavedInvestmentDao
    abstract fun savedCalculationDao(): SavedCalculationDao
}
