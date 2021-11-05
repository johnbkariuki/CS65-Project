package com.example.checkmate

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.w3c.dom.Comment

// for view model interaction with database
@Dao
interface ReceiptEntryDatabaseDao {

    @Insert
    suspend fun insertReceiptEntry(exerciseEntry: ReceiptEntry): Long

    @Query("SELECT * FROM receipt_entry_table")
    fun getAllReceiptEntries(): Flow<List<ReceiptEntry>>

    @Query("DELETE FROM receipt_entry_table")
    suspend fun deleteAll()

    @Query("DELETE FROM receipt_entry_table WHERE id = :key") //":" indicates that it is a Bind variable
    suspend fun deleteReceiptEntry(key: Long)

//    @Query("SELECT * FROM receipt_entry_table WHERE payer_column = :username")
//    fun getReceiptEntry(username: String): Flow<List<ReceiptEntry>>

}