package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.theartofdev.edmodo.cropper.CropImage
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.NumberFormatException
import java.util.*
import kotlin.collections.ArrayList

class ReceiptActivity : AppCompatActivity() {

    private var receiptMode: String? = null

    // for receipt image processing
    private lateinit var recognizer: TextRecognizer
    private lateinit var receiptImage: InputImage

    // for UI
    private lateinit var addPayerButton: Button
    private lateinit var submitButton: Button
    private lateinit var receiptListView: ListView
    private var receiptList = ArrayList<Pair<String, String>>()
    private lateinit var adapterEntry: ReceiptEntryListAdapter
    private var payers = mutableSetOf<String>()  // payers in popup

    // for firebase
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mCurrUser: FirebaseUser

    // for saving receipt
    private lateinit var receiptEntry: ReceiptEntry
    private var title = "No Title"
    private var date = ""
    private var payer = ""
    private var priceList = arrayListOf<String>()
    private var itemList = arrayListOf<String>()
    private var payerList = arrayListOf<String>()  // index = receipt item row, value = payer

    companion object {
        const val RECEIPT_SUBMITTED_TOAST = "Receipt Submitted"
        val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "July", "Aug", "Sept", "Oct", "Nov", "Dec")
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

        setContentView(R.layout.activity_receipt)
        receiptListView = findViewById<ListView>(R.id.receiptList)

        // creating checklist to format receipt
        adapterEntry = ReceiptEntryListAdapter(this, receiptList)
        receiptListView.adapter = adapterEntry

        receiptMode = intent.getStringExtra(Globals.RECEIPT_MODE_KEY)
        if (receiptMode == null) {
            receiptMode = Globals.RECEIPT_NEW_MODE
        }

        if (receiptMode == Globals.RECEIPT_NEW_MODE) {

            // observe changes in popup button display
            adapterEntry.payersMap.observe(this, Observer { it ->
                // reload list with new popup displays
                adapterEntry.notifyDataSetChanged()
                println("debug: payersMap updated")
            })

            val pref = getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
            val email = pref.getString(SignUpActivity.EMAIL_KEY, "")!!
            val password = pref.getString(SignUpActivity.PASSWORD_KEY, "")!!

            // firebase and load the view
            mFirebaseAuth = FirebaseAuth.getInstance()
            mFirebaseFirestore = FirebaseFirestore.getInstance()
            mFirebaseAuth.signInWithEmailAndPassword(email, password)

            if (mFirebaseAuth.currentUser != null) {
                mCurrUser = mFirebaseAuth.currentUser!!
                mUserId = mCurrUser.uid
            }

            // get username
            mFirebaseFirestore.collection("users").document(mUserId).get()
                .addOnSuccessListener {
                    // if logged in
                    payer = it.data!!["username"].toString()
                    payers.add(payer)
                    adapterEntry.payers = payers
                    adapterEntry.notifyDataSetChanged()
                }

            // display button for payer addition
            addPayerButton = findViewById<Button>(R.id.add_payer_button)
            addPayerButton.setOnClickListener {
                val intent = Intent(this, SearchBarActivity::class.java)
                startActivity(intent)
            }

            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            getReceipt()

        } else if (receiptMode == Globals.RECEIPT_HISTORY_MODE) {

            // hide popups
            adapterEntry.displayMode = Globals.HIDE_POPUP

            // hide buttons
            addPayerButton = findViewById<Button>(R.id.add_payer_button)
            submitButton = findViewById<Button>(R.id.submit_receipt_button)
            addPayerButton.visibility = View.GONE
            submitButton.visibility = View.GONE

            // get receipt object fields
            title = intent.getStringExtra(Globals.RECEIPT_TITLE_KEY)!!
            priceList = intent.getStringArrayListExtra(Globals.RECEIPT_PRICELIST_KEY)!!
            itemList = intent.getStringArrayListExtra(Globals.RECEIPT_ITEMLIST_KEY)!!
            payerList = intent.getStringArrayListExtra(Globals.RECEIPT_PAYERLIST_KEY)!!
            // creating payer map, adding prices and items to adapter
            for (i in 0 until payerList.size) {
                val payer = payerList[i]
                val item = itemList[i]
                val price = priceList[i]
                adapterEntry.payersMapStore[i] = payer
                receiptList.add(Pair(item, price))
            }
            // notify adapter
            adapterEntry.notifyDataSetChanged()

            // set title
            val titleEditText = findViewById<EditText>(R.id.receipt_title)
            titleEditText.setText(title)
        }
    }

    override fun onResume() {

        super.onResume()
        val pref = getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
        val selectedUsersSet = pref.getStringSet(Globals.SELECTED_USERS_KEY, null)
        if (selectedUsersSet != null) {
            println("debug: onResume() called, storing new users")
            for (user in selectedUsersSet) {
                payers.add(user)
                adapterEntry.payers = payers
                adapterEntry.notifyDataSetChanged()
            }
            val editor = pref.edit()
            editor.remove(Globals.SELECTED_USERS_KEY)
            editor.apply()
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
                    var price = line.text
                    if (price[0] == '$') {
                        price = price.slice(IntRange(1, line.text.length - 1))
                    }
                    // display receipt line item
                    val receiptItem = Pair(item, price)
                    receiptList.add(receiptItem)
                    // store to arrays for eventual database storage
                    priceList.add(price)
                    itemList.add(item)

                }
            }
            println("debug: priceList = $priceList")
        }
        adapterEntry.notifyDataSetChanged()
    }

    // saves receipt entry to database
    fun saveReceiptEntry() {

        // set up database and view model
        val database = ReceiptEntryDatabase.getInstance(this)
        val databaseDao = database.receiptEntryDatabaseDao
        val viewModelFactory = ReceiptEntryViewModelFactory(databaseDao)
        val receiptEntryViewModel = ViewModelProvider(this, viewModelFactory).get(ReceiptEntryViewModel::class.java)

        // create entry object
        receiptEntry = ReceiptEntry()

        // get user-inputted title
        val titleEditText = findViewById<EditText>(R.id.receipt_title)
        title = titleEditText.text.toString()

        // get current date
        val calendar = Calendar.getInstance()
        val month = MONTHS[calendar.get(Calendar.MONTH)]
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)
        date = "$month $day $year"

        // getting payerList based on map stored in adapter
        val payersMap = adapterEntry.payersMapStore
        // create list where index = receipt row and value is payer username for that item
        for (i in 0 until priceList.size) {
            println("debug: index = i")
            // if row has not been assigned payer: current user is payer
            if (payersMap.containsKey(i)) {
                payerList.add(payersMap[i]!!)
            } else {
                println("debug: key not found, $i")
                payerList.add(payer)
            }
        }

        // setting entry column values
        receiptEntry.title = title
        receiptEntry.date = date
        receiptEntry.payer = payer
        receiptEntry.priceList = Globals.ArrayList2Byte(priceList)
        receiptEntry.itemList = Globals.ArrayList2Byte(itemList)
        receiptEntry.payerList = Globals.ArrayList2Byte(payerList)

        // insert into database
        receiptEntryViewModel.insert(receiptEntry)
    }

    // for when user submits receipt
    fun onSubmitReceipt(view: View) {
        if (receiptMode == Globals.RECEIPT_NEW_MODE) {
            // save receipt
            saveReceiptEntry()

            // display toast and exit activity
            Toast.makeText(this, RECEIPT_SUBMITTED_TOAST, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}