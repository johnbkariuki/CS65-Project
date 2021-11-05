package com.example.checkmate

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.io.*


// for UI interaction with database repository
class ReceiptEntryViewModel(private val receiptEntryDatabaseDao: ReceiptEntryDatabaseDao) : ViewModel() {

    var receiptList = receiptEntryDatabaseDao.getAllReceiptEntries().asLiveData()

    // inserting entry
    fun insert(receiptEntry: ReceiptEntry) {
        CoroutineScope(Dispatchers.IO).launch{
            receiptEntryDatabaseDao.insertReceiptEntry(receiptEntry)
        }
    }

    // deleting specific entry
    fun delete(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            receiptEntryDatabaseDao.deleteReceiptEntry(id)
        }
    }

    // deleting all entries
    fun deleteAll(){
        CoroutineScope(Dispatchers.IO).launch {
            receiptEntryDatabaseDao.deleteAll()
        }
    }

}

class ReceiptEntryViewModelFactory (private val databaseDao: ReceiptEntryDatabaseDao) : ViewModelProvider.Factory {
    override fun<T: ViewModel> create(modelClass: Class<T>) : T{ //create() creates a new instance of the modelClass, which is CommentViewModel in this case.
        if(modelClass.isAssignableFrom(ReceiptEntryViewModel::class.java))
            return ReceiptEntryViewModel(databaseDao) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}