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
import androidx.appcompat.app.AlertDialog
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
import com.google.firebase.firestore.FieldValue
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
    private var quantityList = arrayListOf<String>()
    private var payerList = arrayListOf<String>()  // index = receipt item row, value = payer

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

        // attach adapter
        adapterEntry = ReceiptEntryListAdapter(this, receiptList)
        receiptListView.adapter = adapterEntry

        receiptMode = intent.getStringExtra(Globals.RECEIPT_MODE_KEY)

        if (receiptMode == null) {
            receiptMode = Globals.RECEIPT_NEW_MODE
        }

        // if creating new receipt
        if (receiptMode == Globals.RECEIPT_NEW_MODE) {

            // observe changes in popup button displays
            adapterEntry.payersMap.observe(this, Observer { it ->
                // reload list with new popup displays
                adapterEntry.notifyDataSetChanged()
                println("debug: payersMap updated")
            })

            val pref = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
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

            mFirebaseFirestore.collection("users").document(mUserId).get()
                .addOnSuccessListener {
                    // get username - add to popup buttons
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

            // launch camera/receipt scanner
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            getReceipt()

            // if displaying past receipt from history
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

        val pref = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        val addedPayersSet = pref.getStringSet(Globals.ADDED_PAYERS_KEY, null)

        // if resuming after user added new payers, add to set of payers for popups
        if (addedPayersSet != null) {
            for (user in addedPayersSet) {
                payers.add(user)
                adapterEntry.payers = payers
                adapterEntry.notifyDataSetChanged()
            }
            // reset
            val editor = pref.edit()
            editor.remove(Globals.ADDED_PAYERS_KEY)
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


    // what does text represent
    fun getTextType(text: String): String {
        // if has dollar sign, price
        if (text[0] == '$') {
            return Globals.PRICE_TYPE
        } else {
            return try {
                val textFloat = text.toFloat()
                // if has decimal, price
                if (text.contains('.')) {
                    Globals.PRICE_TYPE
                } else {  // otherwise, quantity
                    Globals.QUANTITY_TYPE
                }
                // if is not number, it's an item
            } catch (e: NumberFormatException) {
                Globals.ITEM_TYPE
            }
        }
    }

    // displaying receipt
    fun displayReceipt(text: Text){

        val blocks = text.textBlocks

        // loop through text blocks and fill out corresponding lists
        for (i in 0 until blocks.size) {
            println("debug: --------")
            val block = blocks[i]

            for (j in 0 until block.lines.size) {

                var line = block.lines[j]
                val debugLine = line.text
                println("debug: $debugLine")
                val textType = getTextType(line.text)

                // add text to pertinent list 
                if (textType == Globals.PRICE_TYPE) {

                    // remove dollar sign if needed
                    if (line.text[0] == '$') {
                        priceList.add(line.text.slice(IntRange(1, line.text.length - 1)))
                    } else {
                        priceList.add(line.text)
                    }
                } else if (textType == Globals.ITEM_TYPE) {
                    itemList.add(line.text)
                } else if (textType == Globals.QUANTITY_TYPE) {
                    quantityList.add(line.text)
                }
            }
        }
        println("debug:$priceList")
        println("debug:$itemList")
        println("debug:$quantityList")

        if (priceList.size == itemList.size){
            if (priceList.size == quantityList.size){
                for (i in 0 until priceList.size) {
                    val price = priceList[i]
                    val quantityAndItem = quantityList[i] +" "+ itemList[i]
                    receiptList.add(Pair(quantityAndItem, price))
                }
            } else{
                for (i in 0 until priceList.size) {
                    val price = priceList[i]
                    val item = itemList[i]
                    receiptList.add(Pair(item, price))
                    println("Debug:$receiptList")
                }
            }
            adapterEntry.notifyDataSetChanged()
        } else{
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.receipt_scanning_error)
            builder.setTitle(R.string.signup_error_title)
            builder.setPositiveButton(android.R.string.ok){ _, _ -> restartCropActivity()}
            val dialog = builder.create()
            dialog.show()
        }
    }

    fun restartCropActivity(){
       val restart_receipt_intent = Intent(this,ReceiptActivity::class.java)
        finish()
        startActivity(restart_receipt_intent)
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
        val month = Globals.MONTHS[calendar.get(Calendar.MONTH)]
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)
        date = "$month $day $year"

        // getting payerList based on map stored in adapter
        val payersMap = adapterEntry.payersMapStore

        // create list where index = receipt row and value is payer username for that item
        for (i in 0 until priceList.size) {
            // if row has not been assigned payer: current user is payer
            if (payersMap.containsKey(i)) {
                payerList.add(payersMap[i]!!)
            } else {
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

        // save the receipt in firebase
        val receipt = Receipt(title, date, payer, priceList, itemList, payerList)
        storeToFirestore(payers, receipt)
        if (!payers.contains(mUserId)) mFirebaseFirestore.collection("users").document(mUserId)
                    .update("receipts", FieldValue.arrayUnion(receipt))
    }

    // for when user submits receipt
    fun onSubmitReceipt(view: View) {
        if (receiptMode == Globals.RECEIPT_NEW_MODE) {
            // save receipt

            if(receiptList.isNotEmpty()){
                saveReceiptEntry()

                // display toast and exit activity
                Toast.makeText(this, Globals.RECEIPT_SUBMITTED_TOAST, Toast.LENGTH_SHORT).show()
                finish()
            }else{
                Toast.makeText(this, Globals.RECEIPT_SUBMISSION_FAILURE, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // helper function to save in firestore db for all payers
    fun storeToFirestore(payers: Set<String>, receipt: Receipt) {
        // do for each payer
        for (payer in payers) {
            // grab the user from firestore and modify their receipt history w this new receipt
            mFirebaseFirestore.collection("users").whereEqualTo("username", payer)
                .get()
                .addOnCompleteListener {
                    println("completed!")
                    val user = it.result.documents
                    for (value in user) {
                        val id = value.id
                        println("added to: ${value}")
                        mFirebaseFirestore.collection("users").document(id)
                            .update("receipts", FieldValue.arrayUnion(receipt))
                    }
                }
        }
    }
}