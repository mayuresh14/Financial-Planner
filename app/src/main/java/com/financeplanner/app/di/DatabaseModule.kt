package com.financeplanner.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financeplanner.app.data.local.db.AppDatabase
import com.financeplanner.app.data.local.db.SavedCalculationDao
import com.financeplanner.app.data.local.db.SavedInvestmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Adds the saved_calculations table — SIP/Lumpsum/Goal-based SIP saves moved
 * out of saved_investments (which now only holds real FD/RD/PPF/EPF/SSY/NPS
 * accounts) into their own table for "what-if" scenario snapshots. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_calculations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                customName TEXT NOT NULL,
                notes TEXT,
                detailsJson TEXT NOT NULL,
                lastComputedValue REAL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // Existing SIP/LUMPSUM/GOAL_BASED_SIP rows in saved_investments no
        // longer match that table's (now narrower) SavedInvestmentType enum —
        // drop them rather than leave orphaned rows the app can't parse back.
        db.execSQL("DELETE FROM saved_investments WHERE type IN ('SIP', 'LUMPSUM', 'GOAL_BASED_SIP')")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "moneymint.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideSavedInvestmentDao(database: AppDatabase): SavedInvestmentDao {
        return database.savedInvestmentDao()
    }

    @Provides
    @Singleton
    fun provideSavedCalculationDao(database: AppDatabase): SavedCalculationDao {
        return database.savedCalculationDao()
    }
}
