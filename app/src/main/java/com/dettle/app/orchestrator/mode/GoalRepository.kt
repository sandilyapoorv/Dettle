package com.dettle.app.orchestrator.mode

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// ─── Entity ───────────────────────────────────────────────────────────────────

/**
 * A persistent GOAL mode objective.
 *
 * Created when the user sends a message in GOAL mode.
 * Survives app restarts — the loop can resume from [progress] after a kill.
 * Cleared by the user explicitly or when marked [isCompleted].
 *
 * Note: Goals do NOT survive [ChatViewModel.clearChat] —
 * clearChat() resets conversation UI but leaves goals intact in the DB.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "gates_json") val gatesJson: String = "[]",      // JSON array of gate IDs
    @ColumnInfo(name = "progress_json") val progressJson: String = "{}", // JSON map gate→status
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "started_at") val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "last_step") val lastStep: Int = 0,
    @ColumnInfo(name = "summary") val summary: String? = null
)

// ─── Domain model ─────────────────────────────────────────────────────────────

data class Goal(
    val id: String,
    val description: String,
    val gates: List<String>,
    val progress: Map<String, GateStatus>,
    val isCompleted: Boolean,
    val startedAt: Long,
    val completedAt: Long?,
    val lastStep: Int,
    val summary: String?
) {
    val passedGates: Int get() = progress.values.count { it == GateStatus.PASSED }
    val totalGates: Int get() = gates.size
    val progressFraction: Float get() = if (totalGates == 0) 0f else passedGates.toFloat() / totalGates
}

enum class GateStatus { PENDING, PASSED, FAILED }

// ─── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE is_completed = 0 ORDER BY started_at DESC LIMIT 1")
    suspend fun getActiveGoal(): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: String): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY started_at DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("UPDATE goals SET is_completed = 1, completed_at = :completedAt, summary = :summary WHERE id = :id")
    suspend fun markCompleted(id: String, completedAt: Long, summary: String)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM goals WHERE is_completed = 1")
    suspend fun clearCompleted()
}

// ─── Repository ───────────────────────────────────────────────────────────────

/**
 * Repository for GOAL mode persistence.
 *
 * Wraps [GoalDao] with domain model conversion and JSON serialization of
 * the gates list and progress map.
 */
@Singleton
class GoalRepository @Inject constructor(
    private val dao: GoalDao,
    private val json: Json
) {
    /** Get the current active (incomplete) goal, or null */
    suspend fun getActiveGoal(): Goal? = dao.getActiveGoal()?.toDomain(json)

    /** Create a new goal, replacing any previous active goal */
    suspend fun createGoal(description: String, gates: List<String>): Goal {
        val entity = GoalEntity(
            description = description,
            gatesJson = json.encodeToString(gates),
            progressJson = json.encodeToString(gates.associateWith { GateStatus.PENDING.name })
        )
        dao.insert(entity)
        return entity.toDomain(json)
    }

    /** Update progress for a specific gate */
    suspend fun updateGate(goalId: String, gateId: String, status: GateStatus) {
        val entity = dao.getById(goalId) ?: return
        val progress = json.decodeFromString<Map<String, String>>(entity.progressJson)
            .toMutableMap()
        progress[gateId] = status.name
        dao.update(entity.copy(progressJson = json.encodeToString(progress)))
    }

    /** Increment the last completed step number */
    suspend fun updateStep(goalId: String, step: Int) {
        val entity = dao.getById(goalId) ?: return
        dao.update(entity.copy(lastStep = step))
    }

    /** Mark a goal as done with a summary */
    suspend fun completeGoal(goalId: String, summary: String) {
        dao.markCompleted(goalId, System.currentTimeMillis(), summary)
    }

    /** Observe all goals (for the Goals screen in Phase 3) */
    fun observeAll(): Flow<List<Goal>> = dao.observeAll().map { list ->
        list.map { it.toDomain(json) }
    }

    suspend fun getGoal(goalId: String): Goal? = dao.getById(goalId)?.toDomain(json)

    suspend fun deleteGoal(goalId: String) = dao.delete(goalId)
    suspend fun clearCompleted() = dao.clearCompleted()
}

// ─── Mapping ──────────────────────────────────────────────────────────────────

@Suppress("UNCHECKED_CAST")
private fun GoalEntity.toDomain(json: Json): Goal {
    val gates = try {
        json.decodeFromString<List<String>>(gatesJson)
    } catch (_: Exception) { emptyList() }

    val progress = try {
        json.decodeFromString<Map<String, String>>(progressJson)
            .mapValues { (_, v) -> runCatching { GateStatus.valueOf(v) }.getOrDefault(GateStatus.PENDING) }
    } catch (_: Exception) { emptyMap() }

    return Goal(
        id = id,
        description = description,
        gates = gates,
        progress = progress,
        isCompleted = isCompleted,
        startedAt = startedAt,
        completedAt = completedAt,
        lastStep = lastStep,
        summary = summary
    )
}
