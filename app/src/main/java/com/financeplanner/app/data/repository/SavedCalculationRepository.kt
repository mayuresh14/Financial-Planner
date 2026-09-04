package com.financeplanner.app.data.repository

import com.financeplanner.app.data.local.db.SavedCalculationDao
import com.financeplanner.app.data.local.db.SavedCalculationEntity
import com.financeplanner.app.domain.model.SavedCalculation
import com.financeplanner.app.domain.model.SavedCalculationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SavedCalculationRepository @Inject constructor(
    private val dao: SavedCalculationDao
) {
    fun observeAll(): Flow<List<SavedCalculation>> =
        dao.observeAll().map { entities -> entities.mapNotNull { it.toDomain() } }

    suspend fun getById(id: Long): SavedCalculation? = dao.getById(id)?.toDomain()

    suspend fun save(item: SavedCalculation): Long = dao.upsert(item.toEntity())

    suspend fun delete(item: SavedCalculation) = dao.delete(item.toEntity())

    private fun SavedCalculationEntity.toDomain(): SavedCalculation? {
        val parsedType = runCatching { SavedCalculationType.valueOf(type) }.getOrNull() ?: return null
        return SavedCalculation(
            id = id,
            type = parsedType,
            customName = customName,
            notes = notes,
            detailsJson = detailsJson,
            lastComputedValue = lastComputedValue,
            createdAt = createdAt
        )
    }

    private fun SavedCalculation.toEntity(): SavedCalculationEntity = SavedCalculationEntity(
        id = id,
        type = type.name,
        customName = customName,
        notes = notes,
        detailsJson = detailsJson,
        lastComputedValue = lastComputedValue,
        createdAt = createdAt
    )
}
