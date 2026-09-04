package com.financeplanner.app.data.repository

import com.financeplanner.app.data.local.db.SavedInvestmentDao
import com.financeplanner.app.data.local.db.SavedInvestmentEntity
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.domain.model.SavedInvestmentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Phase 2, local-only: saved investments live entirely in the on-device Room
 * database, no account or backend involved. This is the seam Phase 3 can
 * later swap for a synced implementation without touching ViewModels.
 */
class SavedInvestmentRepository @Inject constructor(
    private val dao: SavedInvestmentDao
) {
    fun observeAll(): Flow<List<SavedInvestment>> =
        dao.observeAll().map { entities -> entities.mapNotNull { it.toDomain() } }

    suspend fun getById(id: Long): SavedInvestment? = dao.getById(id)?.toDomain()

    suspend fun save(item: SavedInvestment): Long = dao.upsert(item.toEntity())

    suspend fun delete(item: SavedInvestment) = dao.delete(item.toEntity())

    private fun SavedInvestmentEntity.toDomain(): SavedInvestment? {
        val parsedType = runCatching { SavedInvestmentType.valueOf(type) }.getOrNull() ?: return null
        return SavedInvestment(
            id = id,
            type = parsedType,
            customName = customName,
            institutionName = institutionName,
            notes = notes,
            detailsJson = detailsJson,
            lastComputedValue = lastComputedValue,
            createdAt = createdAt
        )
    }

    private fun SavedInvestment.toEntity(): SavedInvestmentEntity = SavedInvestmentEntity(
        id = id,
        type = type.name,
        customName = customName,
        institutionName = institutionName,
        notes = notes,
        detailsJson = detailsJson,
        lastComputedValue = lastComputedValue,
        createdAt = createdAt
    )
}
