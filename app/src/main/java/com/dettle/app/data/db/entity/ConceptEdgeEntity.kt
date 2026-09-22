package com.dettle.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concept_edges")
data class ConceptEdgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceNode: String,
    val targetNode: String,
    val relationship: String, // e.g. "DEPENDS_ON", "CAUSES_ERROR", "WORKS_WELL_WITH"
    val weight: Float = 1.0f,
    val context: String = "", // e.g. "Encountered during deployment task"
    val createdAt: Long = System.currentTimeMillis()
)
