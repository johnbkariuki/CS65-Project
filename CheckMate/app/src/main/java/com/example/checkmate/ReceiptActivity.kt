package com.example.checkmate

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.SmsManager
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
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
import com.example.checkmate.console.PaymentActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.NumberFormatException
import java.util.*
import kotlin.collections.ArrayList

class ReceiptActivity : AppCompatActivity() {

    private var receiptMode: String? = null

    // for receipt image processing
    private lateinit var recognizer: TextRecognizer
    private lateinit var receiptImage: InputImage

    // for UI
    private lateinit var addPayerButton: FloatingActionButton
    private lateinit var submitButton: FloatingActionButton
    private lateinit var sendVenmoButton: FloatingActionButton
    private lateinit var sendTextButton: FloatingActionButton
    private lateinit var receiptListView: ListView
    private var receiptList = ArrayList<Pair<String, String>>()
    private lateinit var adapterEntry: ReceiptEntryListAdapter
    private var payers = arrayListOf<String>()  // payers in popup

    // for firebase
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mCurrUser: FirebaseUser

    // for venmo
    private lateinit var mUsername: String

    // for saving receipt
    private lateinit var receiptEntry: ReceiptEntry
    private var title = "No Title"
    private var date = ""
    private var payer = ""
    private var priceList = arrayListOf<String>()
    private var itemList = arrayListOf<String>()
    private var quantityList = arrayListOf<Float>()
    private var payerList = arrayListOf<String>()  // index = receipt item row, value = payer

    private lateinit var backPressedCallback: OnBackPressedCallback

    private var clicked = false

    private val cropActivityResultContract = object: ActivityResultContract<Any,Uri?>(){
        override fun createIntent(context: Context, input: Any?): Intent {
            return CropImage.activity()
                .getIntent(this@ReceiptActivity)

        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (resultCode == RESULT_OK){
                CropImage.getActivityResult(intent)?.uri
            } else{
                finish()
                null
            }
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

                // Get username for venmo
                mFirebaseFirestore.collection("users").document(mUserId).get()
                    .addOnSuccessListener {
                        mUsername = it.data!!["username"].toString()
                    }
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
            addPayerButton = findViewById<FloatingActionButton>(R.id.add_payer_button)
            addPayerButton.setOnClickListener {
                val intent = Intent(this, SearchBarActivity::class.java)
                intent.putExtra(Globals.EXISTING_PAYERS_KEY, payers)
                startActivity(intent)
            }

            // launch camera/receipt scanner
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            getReceipt()

            // if displaying past receipt from history
        } else if (receiptMode == Globals.RECEIPT_HISTORY_MODE) {

            // hide popups
            adapterEntry.displayMode = Globals.HIDE_DROPDOWN

            // hide buttons
            addPayerButton = findViewById<FloatingActionButton>(R.id.add_payer_button)
            submitButton = findViewById<FloatingActionButton>(R.id.submit_receipt_button)
            sendTextButton = findViewById<FloatingActionButton>(R.id.send_text)
            sendVenmoButton = findViewById<FloatingActionButton>(R.id.send_venmo)
            addPayerButton.visibility = View.GONE
            submitButton.visibility = View.GONE
            sendTextButton.visibility = View.GONE
            sendVenmoButton.visibility = View.GONE

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
//             reset
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
                    quantityList.add(line.text.toFloat())
                }
            }
        }
        println("debug:$priceList")
        println("debug:$itemList")
        println("debug:$quantityList")

        if (priceList.size == itemList.size){
            // if quantities were retrieved
            if (priceList.size == quantityList.size){
                var newPriceList = arrayListOf<String>()
                var newItemList = arrayListOf<String>()
                // split up into multiple line items
                for (i in 0 until priceList.size) {
                    val price = priceList[i]
                    val item = itemList[i]
                    val quantity = quantityList[i]
                    for (j in 0 until quantity.toInt()) {
                        val dividedPrice = price.toFloat().div(quantity)
                        // round to 2 decimal places
                        val dividedPriceString = String.format("%.2f", dividedPrice)
                        newPriceList.add(dividedPriceString)
                        newItemList.add(item)
                        receiptList.add(Pair(item, dividedPriceString))
                    }
                }
                priceList = newPriceList
                itemList = newItemList
            } else { // if quantities not retrieved
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
        if (title == "") {
            title = "No title"
        }

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
        storeToFirebase(payers,payerList,title, date, mUserId, priceList, itemList)
    }

    fun sendVenmoRequests() {
        // Map for final split: key is venmo id, value is amount to be requested
        val map: MutableMap<String, Double> = mutableMapOf()
        for(i in 0 until payerList.size) {
            // Get venmo id by username for each payer
            mFirebaseFirestore.collection("users")
                .whereEqualTo("username", payerList[i]).get().addOnCompleteListener {
                    val user = it.result.documents
                    var venmoId: String = ""
                    for(x in user) {
                        venmoId = x.data!!["venmo"].toString()
                        println("debug: venmoid in loop $venmoId")
                    }
                    // Update amount to be paid in map
                    if(venmoId != "" && payerList[i] != mUsername) {
                        if(map.containsKey(venmoId)) {
                            println("debug: map contains key")
                            val currentAmount = map[venmoId]
                            if (currentAmount != null) {
                                map[venmoId] = currentAmount + priceList[i].toDouble()
                            }
                        } else {
                            println("debug: map does not contain key")
                            map[venmoId] = priceList[i].toDouble()
                        }
                    }
                    // Send map to function to be called after DB fetches
                    if(i >= payerList.size-1) {
                        tempVenmoFunction(map)
                    }
                }
        }
    }

    fun tempVenmoFunction(map: MutableMap<String, Double>) {
        // List of requests' amounts, notes, ids
        val amountsList = arrayListOf<Double>()
        val notesList = arrayListOf<String>()
        val idsList = arrayListOf<String>()
        println("debug: above map keys")
        println("debug: map keys ${map.keys}")
        for(key in map.keys) {
            idsList.add(key)
            notesList.add("CheckMate receipt")
            amountsList.add(map[key]!!)
            println("debug: receiptactivity $key ${map[key]!!}")
        }

        // Pass the lists to PaymentActivity
        val intent = Intent(this, PaymentActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable("amountsList", amountsList)
        bundle.putSerializable("notesList", notesList)
        bundle.putSerializable("idsList", idsList)
        println("debug: amountsList $amountsList")
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
    }

    // for when user submits receipt
    fun onSubmitReceipt(view: View) {
        change_button_icon(clicked)
        setVisibility(clicked)
        clicked = !clicked
    }

    fun setVisibility(clicked: Boolean){
        sendTextButton = findViewById<FloatingActionButton>(R.id.send_text)
        sendVenmoButton = findViewById<FloatingActionButton>(R.id.send_venmo)

        if(!clicked){
            sendVenmoButton.visibility = View.VISIBLE
            sendTextButton.visibility = View.VISIBLE
        } else{
            sendVenmoButton.visibility = View.GONE
            sendTextButton.visibility = View.GONE
        }
    }

    fun change_button_icon(clicked: Boolean){
        submitButton = findViewById<FloatingActionButton>(R.id.submit_receipt_button)
        if(!clicked){
            submitButton.setImageResource(R.drawable.ic_cancel_send)
        } else{
            submitButton.setImageResource(R.drawable.ic_submit)
        }
    }

    fun onSendVenmoPressed(view: View){
        if (receiptMode == Globals.RECEIPT_NEW_MODE) {
            // save receipt

            if(receiptList.isNotEmpty()){
                saveReceiptEntry()
                sendVenmoRequests()

                // display toast and exit activity
                Toast.makeText(this, Globals.RECEIPT_SUBMITTED_TOAST, Toast.LENGTH_SHORT).show()
                finish()
            }else{
                Toast.makeText(this, Globals.RECEIPT_SUBMISSION_FAILURE, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onSendTextPressed(view: View){
        if (receiptMode == Globals.RECEIPT_NEW_MODE) {
            // save receipt
            if(receiptList.isNotEmpty()){
                saveReceiptEntry()
                sendSMS(payer,payerList,priceList,itemList)

                // display toast and exit activity
                finish()
            }else{
                Toast.makeText(this, Globals.RECEIPT_SUBMISSION_FAILURE, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendSMS(requestor: String, payersList: List<String>, priceList: List<String>, itemList: List<String>){

        for (index in payersList.indices) {
            // grab the user from firestore and modify their receipt history w this new receipt
            mFirebaseFirestore.collection("users").whereEqualTo("username", payersList[index])
                .get()
                .addOnCompleteListener {
                    println("completed!")
                    val user = it.result.documents
                    for (value in user) {
                        println("added to: ${value.get("phone")}")
                        val sms_message = "$requestor is reminding you to send ${priceList[index]} for ${itemList[index]}. For more information please visit the CheckMate App"
                        val number = value.get("phone") as String
                        println("number:$number")
                        sendSMSRequest(number, sms_message)
                    }
                }
        }
    }

    // the below code was sourced from https://stackoverflow.com/questions/18828455/android-sms-manager-not-sending-sms
    // our own edits were made
    fun sendSMSRequest(phoneNumber: String, sms_message: String){
        val SENT = "SMS_SENT"
        val DELIVERED = "SMS_DELIVERED"

        val sentPI = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)
        val deliveredPI = PendingIntent.getBroadcast(this, 0, Intent(DELIVERED), 0)

        //---when the SMS has been sent---
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, arg1: Intent) {
                when (resultCode) {
                    RESULT_OK -> Toast.makeText(
                        baseContext, "SMS sent",
                        Toast.LENGTH_SHORT
                    ).show()
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Toast.makeText(
                        baseContext, "Generic failure",
                        Toast.LENGTH_SHORT
                    ).show()
                    SmsManager.RESULT_ERROR_NO_SERVICE -> Toast.makeText(
                        baseContext, "No service",
                        Toast.LENGTH_SHORT
                    ).show()
                    SmsManager.RESULT_ERROR_NULL_PDU -> Toast.makeText(
                        baseContext, "Null PDU",
                        Toast.LENGTH_SHORT
                    ).show()
                    SmsManager.RESULT_ERROR_RADIO_OFF -> Toast.makeText(
                        baseContext, "Radio off",
                        Toast.LENGTH_SHORT
                    ).show()
                    SmsManager.RESULT_INVALID_ARGUMENTS -> Toast.makeText(
                        baseContext, "Invalid Arguments",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }, IntentFilter(SENT))

        //---when the SMS has been delivered---
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, arg1: Intent) {
                when (resultCode) {
                    RESULT_OK -> Toast.makeText(
                        baseContext, "SMS delivered",
                        Toast.LENGTH_SHORT
                    ).show()
                    RESULT_CANCELED -> Toast.makeText(
                        baseContext, "SMS not delivered",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }, IntentFilter(DELIVERED))
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber,null,sms_message,sentPI,deliveredPI)
    }

    // helper function to save in firestore db for all payers
    fun storeToInfo(payers: List<String>, receipt: Receipt) {
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

    fun storeToFirebase(payers: List<String>, payersList: List<String>, title: String, date: String, payer_id :String, priceList: List<String>, itemList: List<String>){
        mFirebaseFirestore.collection("users").get().addOnSuccessListener {
            val payersList_by_id = Array<String>(payersList.size){""}
            val mutable_payersList_by_id = payersList_by_id.toMutableList()
            for (document in it.documents){
                for (index in 0 until payersList.size){
                    if (payersList[index] == document.data?.get("username")){
                        mutable_payersList_by_id[index] = document.id
                    }
                }
            }
            println("debug1 $mutable_payersList_by_id")

            val receipt = Receipt(title, date, payer_id, priceList, itemList, mutable_payersList_by_id)
            storeToInfo(payers,receipt)
            if (!payers.contains(mUserId)) mFirebaseFirestore.collection("users").document(mUserId)
                    .update("receipts", FieldValue.arrayUnion(receipt))
        }
    }
}