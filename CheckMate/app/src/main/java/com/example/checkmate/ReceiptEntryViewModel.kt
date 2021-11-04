package com.example.checkmate

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.io.*


// for UI interaction with database repository
class ReceiptEntryViewModel(private val receiptEntryDatabaseDao: ReceiptEntryDatabaseDao) : ViewModel() {

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

    // convert arraylist to byte array for database storage
    public fun ArrayList2Byte(list: ArrayList<String>): ByteArray {

        val bos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(bos)
        oos.writeObject(list)

        return bos.toByteArray()
    }
    
    // convert byte array from database storage back into array list of strings
    public fun Byte2ArrayList(byteArray: ByteArray): ArrayList<String> {

        val bis = ByteArrayInputStream(byteArray)
        val ois = ObjectInputStream(bis)

        return ois.readObject() as ArrayList<String>
    }

}

class ReceiptEntryViewModelFactory (private val databaseDao: ReceiptEntryDatabaseDao) : ViewModelProvider.Factory {
    override fun<T: ViewModel> create(modelClass: Class<T>) : T{ //create() creates a new instance of the modelClass, which is CommentViewModel in this case.
        if(modelClass.isAssignableFrom(ReceiptEntryViewModel::class.java))
            return ReceiptEntryViewModel(databaseDao) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}