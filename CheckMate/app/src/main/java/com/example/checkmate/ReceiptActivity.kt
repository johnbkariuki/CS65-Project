package com.example.checkmate

import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.theartofdev.edmodo.cropper.CropImage
import java.io.File
import android.widget.CheckedTextView




class ReceiptActivity : AppCompatActivity() {
    private lateinit var recognizer: TextRecognizer
    private lateinit var receiptImage: InputImage

    private lateinit var receiptListView: ListView
    private var receiptList = ArrayList<Pair<String, Float>>()
    private lateinit var adapter: ReceiptListAdapter

    companion object {
        // toast messages
        const val RECEIPT_SUBMITTED_TOAST = "Receipt Submitted"

        // receipt display parameters
        const val RECEIPT_MARGIN = 15
        const val RECEIPT_TEXT_SIZE = 16F
    }

    private val cropActivityResultContract = object: ActivityResultContract<Any,Uri?>(){
        override fun createIntent(context: Context, input: Any?): Intent {
            return CropImage.activity()
                .getIntent(this@ReceiptActivity)

        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent)?.uri
        }
    }

    private lateinit var cropActivityResultLauncher: ActivityResultLauncher<Any?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)
        receiptListView = findViewById<ListView>(R.id.receiptList)

        // creating checklist to format receipt
        adapter = ReceiptListAdapter(this, receiptList)
        receiptListView.adapter = adapter
        // when item on receipt list is clicked
        receiptListView.setOnItemClickListener { adapterView, view, i, l ->
            // check item
            val checkedTextView = view as CheckedTextView
            checkedTextView.isChecked = !checkedTextView.isChecked
        }

        // launching camera
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        getReceipt()
    }

    fun showPopupMenu(view: View) {
        PopupMenu(view.context, view).apply {
            menuInflater.inflate(R.menu.popup_menu, menu)
            setOnMenuItemClickListener { item ->
                Toast.makeText(view.context, "You Clicked : " + item.title, Toast.LENGTH_SHORT).show()
                true
            }
        }.show()
    }

    // taking photo
    fun getReceipt() {
        cropActivityResultLauncher = registerForActivityResult(cropActivityResultContract){
            it?.let{ uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
                receiptImage = InputImage.fromBitmap(bitmap, 0)
                runTextRecognition()
            }
        }
        cropActivityResultLauncher.launch(null)
    }

    // processing photo
    fun runTextRecognition(){
        if (this::receiptImage.isInitialized){
            recognizer.process(receiptImage).addOnSuccessListener {
                println("debug: text recognition success")
                displayReceipt(it)
            }.addOnFailureListener{
                it.printStackTrace()
                println("debug: text recognition failure")
            }
        }
    }

    // displaying photo
    fun displayReceipt(text: Text){

        val blocks = text.textBlocks
        // loop through text blocks
        for (i in 0 until blocks.size) {
            val block = blocks[i]
            for (line in block.lines) {
                // if on receipt line item
                if (line.text[0] == '$') {
                    // get item and price
                    val item = blocks[i - 1].lines[0].text
                    val price = line.text.slice(IntRange(1, line.text.length - 1)).toFloat()
                    // display receipt line item
                    val receiptItem = Pair(item, price)
                    receiptList.add(receiptItem)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
    }

    // for when user submits receipt
    fun onSubmitReceipt(view: View) {

        // display toast and exit activity
        Toast.makeText(this, RECEIPT_SUBMITTED_TOAST, Toast.LENGTH_SHORT).show()

        // parse the receipt and apply text recognition
        // itemize the receipt into a receipt object
        // launch the user select activity

        finish()
    }
}