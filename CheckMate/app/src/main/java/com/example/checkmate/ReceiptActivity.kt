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
import android.view.*
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
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.lang.NumberFormatException
import com.google.firebase.database.DatabaseError

class ReceiptActivity : AppCompatActivity() {

    // for receipt image processing
    private lateinit var recognizer: TextRecognizer
    private lateinit var receiptImage: InputImage

    // for UI
    private lateinit var selectPayerButton: Button
    private lateinit var receiptListView: ListView
    private var receiptList = ArrayList<Pair<String, Float>>()
    private lateinit var adapter: ReceiptListAdapter
    private var currPayer = ""

    // for firebase
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var mDatabase: DatabaseReference

    companion object {
        const val RECEIPT_SUBMITTED_TOAST = "Receipt Submitted"
        const val PAYER_STR = "Payer:"
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

        // accessing firebase
        mFirebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().reference
        mFirebaseUser = mFirebaseAuth.currentUser!!
        mUserId = mFirebaseUser.uid

        // display button for user-selection popup menu
        selectPayerButton = findViewById<Button>(R.id.select_payer_button)
        var payerString = "$PAYER_STR $currPayer"
        selectPayerButton.text = payerString
        selectPayerButton.setOnClickListener {
            val intent = Intent(this, SearchBarActivity::class.java)
            startActivity(intent)
        }

        // launching camera only if not coming back from a payer select activity
        if (intent.getStringArrayListExtra("users") == null) {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            getReceipt()
        }
        else {
            println("debug: ${intent.getStringArrayListExtra("users")}")
            Toast.makeText(this, intent.getStringArrayListExtra("users").toString(), Toast.LENGTH_LONG).show()
        }
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

    // helper function for parsing receipt
    fun isPrice(string: String): Boolean {
        return try {
            val float = string.toFloat()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    // Brandon: *** need to handle quantity for displayReceipt() *** //

    // displaying photo
    fun displayReceipt(text: Text){

        val blocks = text.textBlocks
        // loop through text blocks
        for (i in 0 until blocks.size) {
            println("debug: --------")
            val block = blocks[i]

            for (j in 0 until block.lines.size) {
                var line = block.lines[j]
                val debugLine = line.text
                println("debug: $debugLine")
                // if on receipt line item
                if (line.text[0] == '$' || isPrice(line.text)) {
                    val item = blocks[i - 1].lines[j].text
                    val price = line.text.slice(IntRange(1, line.text.length - 1)).toFloat()
                    // display receipt line item
                    val receiptItem = Pair(item, price)
                    receiptList.add(receiptItem)

                }
            }
        }
        adapter.notifyDataSetChanged()
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