package io.github.mobdev.chat

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MessageEntity::class,
        PendingMessageEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    abstract fun pendingMessageDao(): PendingMessageDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
    }
}