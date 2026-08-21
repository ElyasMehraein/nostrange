package com.nostrange.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nostrange.app.data.local.dao.BlockedPubkeyDao
import com.nostrange.app.data.local.dao.CandidateDao
import com.nostrange.app.data.local.dao.MessageDao
import com.nostrange.app.data.local.dao.ProfileDao
import com.nostrange.app.data.local.dao.RelayDao
import com.nostrange.app.data.local.entity.BlockedPubkeyEntity
import com.nostrange.app.data.local.entity.CandidateEntity
import com.nostrange.app.data.local.entity.MessageEntity
import com.nostrange.app.data.local.entity.RelayEntity
import com.nostrange.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        CandidateEntity::class,
        MessageEntity::class,
        BlockedPubkeyEntity::class,
        RelayEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun candidateDao(): CandidateDao
    abstract fun messageDao(): MessageDao
    abstract fun blockedPubkeyDao(): BlockedPubkeyDao
    abstract fun relayDao(): RelayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val DEFAULT_RELAYS = listOf(
            RelayEntity("wss://relay.damus.io", read = true, write = true, isDefault = true),
            RelayEntity("wss://nos.lol", read = true, write = true, isDefault = true),
            RelayEntity("wss://relay.primal.net", read = true, write = true, isDefault = true)
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nostrange_local_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.relayDao()?.insertRelays(DEFAULT_RELAYS)
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
