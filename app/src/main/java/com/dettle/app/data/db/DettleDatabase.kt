package com.dettle.app.data.db

import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.content.Context
import com.dettle.app.data.db.dao.DeploymentDao
import com.dettle.app.data.db.dao.MemoryDao
import com.dettle.app.data.db.dao.TaskLogDao
import com.dettle.app.data.db.dao.GoalDao
import com.dettle.app.data.db.dao.ProjectDao
import com.dettle.app.data.db.dao.ConversationDao
import com.dettle.app.data.db.dao.UserProfileDao
import com.dettle.app.data.db.dao.KnowledgeGraphDao
import com.dettle.app.data.db.entity.DeploymentEntity
import com.dettle.app.data.db.entity.MemoryEntity
import com.dettle.app.data.db.entity.TaskLogEntity
import com.dettle.app.data.db.entity.GoalEntity
import com.dettle.app.data.db.entity.ProjectEntity
import com.dettle.app.data.db.entity.ProjectMemoryEntity
import com.dettle.app.data.db.entity.ConversationEntity
import com.dettle.app.data.db.entity.ConversationMessageEntity
import com.dettle.app.data.db.entity.UserProfileEntity
import com.dettle.app.data.db.entity.ConceptNodeEntity
import com.dettle.app.data.db.entity.ConceptEdgeEntity

@Database(
    entities = [
        MemoryEntity::class,
        TaskLogEntity::class,
        DeploymentEntity::class,
        GoalEntity::class,
        ProjectEntity::class,
        ProjectMemoryEntity::class,
        ConversationEntity::class,
        ConversationMessageEntity::class,
        UserProfileEntity::class,
        ConceptNodeEntity::class,
        ConceptEdgeEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(DettleConverters::class)
abstract class DettleDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
    abstract fun taskLogDao(): TaskLogDao
    abstract fun deploymentDao(): DeploymentDao
    abstract fun goalDao(): GoalDao
    abstract fun projectDao(): ProjectDao
    abstract fun conversationDao(): ConversationDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun knowledgeGraphDao(): KnowledgeGraphDao

    companion object {
        @Volatile private var INSTANCE: DettleDatabase? = null

        fun getInstance(context: Context): DettleDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DettleDatabase::class.java,
                    "dettle.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

class DettleConverters {
    @TypeConverter fun listToString(list: List<String>): String = list.joinToString("|||")
    @TypeConverter fun stringToList(s: String): List<String> =
        if (s.isBlank()) emptyList() else s.split("|||")
        
    @TypeConverter 
    fun floatArrayToByteArray(floats: FloatArray?): ByteArray? {
        if (floats == null) return null
        val buffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.nativeOrder())
        for (f in floats) {
            buffer.putFloat(f)
        }
        return buffer.array()
    }
    
    @TypeConverter 
    fun byteArrayToFloatArray(bytes: ByteArray?): FloatArray? {
        if (bytes == null) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) {
            floats[i] = buffer.getFloat()
        }
        return floats
    }
}
