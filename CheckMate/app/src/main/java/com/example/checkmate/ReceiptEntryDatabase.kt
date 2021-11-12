package com.example.checkmate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.w3c.dom.Comment

@Database(entities = [ReceiptEntry::class], version = 1)
abstract class ReceiptEntryDatabase : RoomDatabase() {
    abstract val receiptEntryDatabaseDao: ReceiptEntryDatabaseDao

    companion object {
        //The Volatile keyword guarantees visibility of changes to the INSTANCE variable across threads
        @Volatile
        private var INSTANCE: ReceiptEntryDatabase? = null

        fun getInstance(context: Context): ReceiptEntryDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        ReceiptEntryDatabase::class.java, "receipt_entry_table"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}