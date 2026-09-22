package com.dettle.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concept_nodes")
data class ConceptNodeEntity(
    @PrimaryKey val name: String, // e.g. "Retrofit", "Cloudflare", "API_34"
    val category: String,         // e.g. "LIBRARY", "PLATFORM", "ERROR", "PROJECT"
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
