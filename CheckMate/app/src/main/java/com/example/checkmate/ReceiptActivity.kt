package com.example.checkmate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class ReceiptActivity : AppCompatActivity() {

    companion object {
        // toast messages
        const val RECEIPT_SUBMITTED_TOAST = "Receipt Submitted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        getReceipt()
    }

    fun getReceipt() {
        intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUri)
        cameraResult.launch(intent)
    }

    val cameraResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    )
    { result: ActivityResult ->  // nothing right now
//        if(result.resultCode == Activity.RESULT_OK){
//            val bitmap = Util.getBitmap(this, tempPhotoUri)
//            myViewModel.userImage.value = bitmap
//        }
    }

    override fun onResume() {
        super.onResume()
    }

    // for when user submits receipt
    fun onSubmitReceipt(view: View) {

        // display toast and exit activity
        Toast.makeText(this, RECEIPT_SUBMITTED_TOAST, Toast.LENGTH_SHORT).show()
        finish()
    }
}