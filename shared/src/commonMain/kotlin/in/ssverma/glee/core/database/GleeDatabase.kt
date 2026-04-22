package `in`.ssverma.glee.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.TypeConverter
import androidx.room3.TypeConverters
import `in`.ssverma.glee.features.chat.domain.model.ChatRole

@Database(entities = [ConversationEntity::class, MessageEntity::class], version = 1)
@ConstructedBy(GleeDatabaseConstructor::class)
@TypeConverters(ChatConverters::class)
abstract class GleeDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}

expect object GleeDatabaseConstructor : RoomDatabaseConstructor<GleeDatabase>

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessages(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessages(conversationId: String)
}

class ChatConverters {
    @TypeConverter
    fun fromChatRole(role: ChatRole): String = role.name

    @TypeConverter
    fun toChatRole(value: String): ChatRole = ChatRole.valueOf(value)
}
