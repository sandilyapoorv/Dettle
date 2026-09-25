package com.dettle.app.data.db.dao

import androidx.room.*
import com.dettle.app.data.db.entity.ConceptNodeEntity
import com.dettle.app.data.db.entity.ConceptEdgeEntity

@Dao
interface KnowledgeGraphDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNode(node: ConceptNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: ConceptEdgeEntity)

    @Query("SELECT * FROM concept_nodes WHERE name = :name LIMIT 1")
    suspend fun getNode(name: String): ConceptNodeEntity?

    @Query("SELECT * FROM concept_edges WHERE sourceNode = :nodeName OR targetNode = :nodeName ORDER BY weight DESC")
    suspend fun getConnectedEdges(nodeName: String): List<ConceptEdgeEntity>
    
    @Query("""
        SELECT n.* FROM concept_nodes n
        INNER JOIN concept_edges e ON n.name = e.targetNode
        WHERE e.sourceNode = :sourceNodeName
        ORDER BY e.weight DESC
    """)
    suspend fun getOutgoingNeighbors(sourceNodeName: String): List<ConceptNodeEntity>
    
    @Transaction
    suspend fun upsertRelationship(source: String, target: String, rel: String, catSource: String = "CONCEPT", catTarget: String = "CONCEPT", context: String = "") {
        insertNode(ConceptNodeEntity(name = source, category = catSource))
        insertNode(ConceptNodeEntity(name = target, category = catTarget))
        insertEdge(ConceptEdgeEntity(
            sourceNode = source,
            targetNode = target,
            relationship = rel,
            context = context
        ))
    }

    @Query("SELECT * FROM concept_nodes")
    suspend fun getAllNodes(): List<ConceptNodeEntity>

    @Query("SELECT * FROM concept_edges")
    suspend fun getAllEdges(): List<ConceptEdgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<ConceptNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<ConceptEdgeEntity>)
}
