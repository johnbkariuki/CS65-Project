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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

class ReceiptActivity : AppCompatActivity() {
    private lateinit var recognizer: TextRecognizer
    private lateinit var receiptImage: InputImage
    private lateinit var linearLayout: LinearLayout

    companion object {
        // toast messages
        const val RECEIPT_SUBMITTED_TOAST = "Receipt Submitted"
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

        linearLayout = findViewById<LinearLayout>(R.id.receiptInfo)

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        getReceipt()
    }

    //    what to do with photo after taken
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


    fun runTextRecognition(){
        if (this::receiptImage.isInitialized){
            recognizer.process(receiptImage).addOnSuccessListener {
                println("debug: text recognition success")
                processTextRecognition(it)
            }.addOnFailureListener{
                it.printStackTrace()
                println("debug: text recognition failure")
            }
        }
    }

    fun processTextRecognition(text: Text){
        val blocks = text.textBlocks
        println("size:${blocks.size}")
        for (block in blocks){
            println(block.text)
            println("---------------------")
            for (line in block.lines){
                val info = TextView(this)
                info.setText(line.text)
                info.setTextSize(16F)
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(15, 15, 15, 15)
                info.layoutParams = params
                linearLayout.addView(info)
            }
        }
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