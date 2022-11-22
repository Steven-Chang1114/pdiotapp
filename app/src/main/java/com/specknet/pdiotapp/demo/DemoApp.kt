package com.specknet.pdiotapp.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.*
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.onError
import com.github.kittinunf.result.success
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.specknet.pdiotapp.HomePage
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.ml.RespeckModel
import com.specknet.pdiotapp.ml.ThingyModel
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.FloatBuffer
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class DemoApp : AppCompatActivity() {

    // global graph variables
    lateinit var respeckData : MutableList<List<Float>>
    lateinit var respeckBuffer: FloatBuffer
    lateinit var thingyData : MutableList<List<Float>>

    lateinit var respeckModel: RespeckModel
    lateinit var thingyModel: ThingyModel

    lateinit var lastMovement: ActionEnum
    lateinit var tflite : Interpreter

    lateinit var userId : String
    lateinit var db : FirebaseFirestore

    lateinit var respeckActiveBtn: Button
    lateinit var thingyActiveBtn: Button
    lateinit var cloudActiveBtn: Button
    lateinit var localActiveBtn: Button
    lateinit var backBtn: ImageView
    lateinit var actionImage: ImageView
    lateinit var respeckStatus: TextView
    lateinit var thingyStatus: TextView
    lateinit var title: TextView

    var isRespeckActive = false
    var isThingyActive = false
    var isCloudActive = true

    private lateinit var classifiedMovementField : TextView

    var respeckCloudCounter = 0
    var thingyCounter = 0

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    lateinit var looperThingy: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        val policy = ThreadPolicy.Builder().permitAll().build()

        StrictMode.setThreadPolicy(policy)

        respeckBuffer = FloatBuffer.allocate(500)

        setupPage()

        setupClickListeners()

        onrespeckReceive()

        onThingyReceive()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePage(action: ActionEnum) {
        runOnUiThread {
            // Stuff that updates the UI
            classifiedMovementField.text = action.movement
            actionImage.setBackgroundResource(getImageFile(action))
        }
    }

    private fun getImageFile(action: ActionEnum): Int {
        return when (action) {
            ActionEnum.DESK_WORK -> R.drawable.desk_work
            ActionEnum.WALKING_AT_NORMAL_SPEED -> R.drawable.walk
            ActionEnum.STANDING -> R.drawable.standing
            ActionEnum.SITTING_BENT_FORWARD -> R.drawable.sit_forward
            ActionEnum.SITTING_STRAIGHT -> R.drawable.sit_straight
            ActionEnum.SITTING_BENT_BACKWARD -> R.drawable.sit_backward
            ActionEnum.LYING_DOWN_ON_THE_RIGHT_SIDE -> R.drawable.lying_down_on_right
            ActionEnum.LYING_DOWN_ON_THE_LEFT_SIDE -> R.drawable.lying_down_on_left
            ActionEnum.LYING_DOWN_ON_THE_BACK -> R.drawable.lying_down_on_back
            ActionEnum.LYING_DOWN_ON_STOMACH -> R.drawable.lying_down_on_stomach
            ActionEnum.GENERAL_MOVEMENT -> R.drawable.general_movement
            ActionEnum.RUNNING -> R.drawable.running
            ActionEnum.ASCENDING_STAIRS -> R.drawable.climbing_stairs
            ActionEnum.DESCENDING_STAIRS -> R.drawable.descending_stairs
            else -> R.drawable.general_movement
        }
    }

    private fun onThingyReceive() {
        // set up the broadcast receiver
        thingyLiveUpdateReceiver = object : BroadcastReceiver() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("Thingy_demo_thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Thingy_demo_Live", "onReceive: liveData = " + liveData)

                    if (isThingyActive) {

                        runOnUiThread {
                            if (thingyCounter == 0) {
                                thingyStatus.text = "Thingy Status:\nStreaming"
                                thingyStatus.setTextColor(Color.parseColor("#379237"))
                            }

                            // get all relevant intent contents
                            val timestamp = liveData.phoneTimestamp.toFloat()

                            val accelX = liveData.accelX
                            val accelY = liveData.accelY
                            val accelZ = liveData.accelZ

                            val gyroX = liveData.gyro.x
                            val gyroY = liveData.gyro.y
                            val gyroZ = liveData.gyro.z

                            val magX = liveData.mag.x
                            val magY = liveData.mag.y
                            val magZ = liveData.mag.z

                            val thingyArr = listOf(timestamp, accelX, accelY, accelZ, gyroX, gyroY, gyroZ, magX, magY, magZ)

                            thingyData.add(thingyArr)
                            while (thingyData.size > 50) {
                                thingyData.removeAt(0)
                            }

                            if (thingyData.size >= 50) {
                                if (isCloudActive) {
                                    classifiedMovementOnCloud()
                                } else {
//                                classifiedMovementLocal()
                                }
                            }

                            thingyCounter += 1
                        }
                    }

                }
            }
        }

        // register receiver on another thread
        val handlerThreadThingy = HandlerThread("bgThreadThingyLiveDemo")
        handlerThreadThingy.start()
        looperThingy = handlerThreadThingy.looper
        val handlerThingy = Handler(looperThingy)
        this.registerReceiver(thingyLiveUpdateReceiver, filterTestThingy, null, handlerThingy)
    }

    private fun onrespeckReceive() {
        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("respeck_demo_thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Respeck_demo_Live", "onReceive: liveData = " + liveData)

                    if (isRespeckActive) {

                        runOnUiThread {
                            if (respeckCloudCounter == 0) {
                                respeckStatus.text = "Respeck Status:\nStreaming"
                                respeckStatus.setTextColor(Color.parseColor("#379237"))
                            }

                            // get all relevant intent contents
                            val timestamp = liveData.phoneTimestamp.toFloat()

                            val accelX = liveData.accelX
                            val accelY = liveData.accelY
                            val accelZ = liveData.accelZ

                            val gyroX = liveData.gyro.x
                            val gyroY = liveData.gyro.y
                            val gyroZ = liveData.gyro.z

                            val resultArr = listOf(timestamp, accelX, accelY, accelZ, gyroX, gyroY, gyroZ)
//                        respeckBuffer.put(floatArrayOf(accelX, accelY, accelZ, gyroX, gyroY, gyroZ))

                            respeckData.add(resultArr)
                            while (respeckData.size > 50) {
                                respeckData.removeAt(0)
                            }

                            if (respeckData.size >= 50) {
                                if (isCloudActive) {
                                    classifiedMovementOnCloud()
                                } else {
//                                classifiedMovementLocal(respeckBuffer)
                                }
                            }

                            respeckCloudCounter += 1
                        }
                    }

                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckDemo")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun classifiedMovementLocal(respeckBuffer: FloatBuffer) {
        if (isRespeckActive) {
            val inputArray = respeckBuffer.array().sliceArray(IntRange(0, 299))
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 50, 6), DataType.FLOAT32)
            inputFeature0.loadArray(inputArray)

            respeckBuffer.clear()

            val output = respeckModel.process(inputFeature0).outputFeature0AsTensorBuffer.floatArray
//            output.indexOf(output.max()!!)

            val outputList = output.toList()
            val movementIdx = outputList.indexOf(outputList.maxOrNull() ?: 0)
            val currentMovement = selectMovements(movementIdx)

            Log.i("PDIOT_DEMO_RESULT_RES_Local", currentMovement.movement)

            lastMovement = currentMovement
            updatePage(lastMovement)
        } else if (isThingyActive) {

        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun classifiedMovementOnCloud() {
        val respeckList = respeckData.toList()
        val thingyList = thingyData.toList()

        if (isRespeckActive && isThingyActive) {
            if (respeckData.size >= 50 && thingyData.size >= 50) {
                sendDataToAzure("both", thingyList, respeckList, "PDIOT_DEMO_RESULT_BOTH_CLOUD", "PDIOT_DEMO_RESULT_BOTH_ERR")
                for (i in 1..25) {
                    if (respeckData.size > 25) {
                        respeckData.removeAt(0)
                    }

                    if (thingyData.size > 25) {
                        thingyData.removeAt(0)
                    }
                }
            }
        } else if (isRespeckActive) {
            if (respeckData.size >= 50) {
                sendDataToAzure("respeck", thingyList, respeckList, "PDIOT_DEMO_RESULT_RES_CLOUD", "PDIOT_DEMO_RESULT_RES_ERROR")
                for (i in 1..25) {
                    if (respeckData.size > 25) {
                        respeckData.removeAt(0)
                    }
                }
            }
        } else if (isThingyActive) {
            if (thingyData.size >= 50) {
                sendDataToAzure("thingy", thingyList, respeckList, "PDIOT_DEMO_RESULT_THINGY_CLOUD", "PDIOT_DEMO_RESULT_THINGY_ERROR")
                for (i in 1..25) {
                    if (thingyData.size > 25) {
                        thingyData.removeAt(0)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendDataToAzure(type: String, thingyData: List<List<Float>>, respeckData : List<List<Float>>, successTag: String, errorTag: String) {
        val values = mapOf("type" to type, "thingy_json" to thingyData.toString(), "respeck_json" to respeckData.toString())

//        Log.d("PDIOT_DEMO_CURRENT_RES_SIZE", respeckData.size.toString())
//        Log.d("PDIOT_DEMO_CURRENT_THI_SIZE", thingyData.size.toString())
        Log.d("PDIOT_DEMO_INPUT", Gson().toJson(values).toString())

        val (request, response, result) = "https://iot-inference.azurewebsites.net/api/inference"
            .httpPost()
            .jsonBody(Gson().toJson(values).toString())
            .responseString()

        var movementId : Int

        result.success { f -> movementId = f.toInt()
            Log.d(successTag, selectMovements(movementId).movement)
            lastMovement = selectMovements(movementId)
            saveData(type, lastMovement.movement, values.toString())
            updatePage(lastMovement)
        }

        result.onError { e ->  Log.e(errorTag, e.toString())}
    }

    private fun setupClickListeners() {
        backBtn.setOnClickListener {
            this.finish()

            val intent = Intent(this, HomePage::class.java)

            val auth = Firebase.auth
            val firebaseAccount = auth.currentUser
            val googleAuthAccount = GoogleSignIn.getLastSignedInAccount(this)

            if (googleAuthAccount != null) {
                intent.putExtra("name" , googleAuthAccount.displayName)
            } else if (firebaseAccount != null) {
                intent.putExtra("name" , firebaseAccount.displayName)
            }

            startActivity(intent)
            overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
        }

        respeckActiveBtn.setOnClickListener {
            if (isRespeckActive) {
                isRespeckActive = false

                respeckStatus.text = "Respeck Status:\nDiabled"
                respeckStatus.setTextColor(Color.parseColor("#EB6440"))

                respeckActiveBtn.setBackgroundResource(R.drawable.hardware_button_inactive)
            } else {
                isRespeckActive = true

                respeckStatus.text = "Respeck Status:\nDisconnected"
                respeckStatus.setTextColor(Color.parseColor("#FD841F"))

                respeckActiveBtn.setBackgroundResource(R.drawable.hardware_button_active)
            }

            respeckCloudCounter = 0
            respeckData = mutableListOf()
        }

        thingyActiveBtn.setOnClickListener {
            if (isThingyActive) {
                isThingyActive = false

                thingyStatus.text = "Thingy Status:\nDiabled"
                thingyStatus.setTextColor(Color.parseColor("#EB6440"))

                thingyActiveBtn.setBackgroundResource(R.drawable.hardware_button_inactive)
            } else {
                isThingyActive = true

                thingyStatus.text = "Thingy Status:\nDisconnected"
                thingyStatus.setTextColor(Color.parseColor("#FD841F"))

                thingyActiveBtn.setBackgroundResource(R.drawable.hardware_button_active)
            }

            thingyCounter = 0
            thingyData = mutableListOf<List<Float>>()
        }

        cloudActiveBtn.setOnClickListener {
            if (!isCloudActive)  {
                isCloudActive = true

                cloudActiveBtn.setBackgroundResource(R.drawable.hardware_button_active)
                localActiveBtn.setBackgroundResource(R.drawable.hardware_button_inactive)
            }
        }

        localActiveBtn.setOnClickListener {
            if (isCloudActive) {
                isCloudActive = false

                cloudActiveBtn.setBackgroundResource(R.drawable.hardware_button_inactive)
                localActiveBtn.setBackgroundResource(R.drawable.hardware_button_active)
            }
        }
    }

    private fun classifyMovement(floatArrayBuffer: FloatBuffer) {
//        if (counter < 50) {
//            return updatePage(lastMovement)
//        }
//
//        val inputArray = floatArrayBuffer.array().sliceArray(IntRange(0, 299))
//        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 50, 6), DataType.FLOAT32)
//        inputFeature0.loadArray(inputArray)
//
//        counter = 0
//        floatArrayBuffer.clear()
//
//        val output = model.process(inputFeature0).outputFeature0AsTensorBuffer.floatArray
//
//        val movementIdx = output.indexOf(output.max()!!)
//
//        Log.i("OUTPUT", "Predicted movement is ${movementIdx}")
//        val currentMovement = selectMovements(movementIdx)
//
//        lastMovement = currentMovement
//        updatePage(currentMovement)
    }

    private fun selectMovements(idx: Int) : ActionEnum {
        return when (idx) {
            0 -> ActionEnum.DESK_WORK
            1 -> ActionEnum.WALKING_AT_NORMAL_SPEED // Thingy
            2 -> ActionEnum.ASCENDING_STAIRS // Thingy
            3 -> ActionEnum.DESCENDING_STAIRS // Thingy
            4 -> ActionEnum.SITTING_STRAIGHT
            5 -> ActionEnum.SITTING_BENT_FORWARD
            6 -> ActionEnum.SITTING_BENT_BACKWARD
            7 -> ActionEnum.STANDING
            8 -> ActionEnum.RUNNING // Thingy
            9 -> ActionEnum.LYING_DOWN_ON_THE_LEFT_SIDE
            10 -> ActionEnum.LYING_DOWN_ON_THE_RIGHT_SIDE
            11 -> ActionEnum.LYING_DOWN_ON_THE_BACK
            12 -> ActionEnum.LYING_DOWN_ON_STOMACH
            13 -> ActionEnum.GENERAL_MOVEMENT
            else -> ActionEnum.GENERAL_MOVEMENT
        }
    }

    private fun setupPage() {
        lastMovement = ActionEnum.GENERAL_MOVEMENT
        respeckModel = RespeckModel.newInstance(this)
        thingyModel = ThingyModel.newInstance(this)

        db = Firebase.firestore

        respeckData = mutableListOf()
        thingyData = mutableListOf()

        respeckStatus = findViewById(R.id.respeck_status)
        thingyStatus = findViewById(R.id.thingy_status)
        respeckActiveBtn = findViewById(R.id.respeck_button)
        thingyActiveBtn = findViewById(R.id.thingy_button)
        cloudActiveBtn = findViewById(R.id.azure_button)
        localActiveBtn = findViewById(R.id.local_button)
        classifiedMovementField = findViewById(R.id.movement)
        actionImage = findViewById(R.id.movement_img)
        backBtn = findViewById(R.id.back_btn)
        title = findViewById(R.id.user)

        getUserId()

        val username = intent.getStringExtra("name")
        title.text = String.format("%s's\ncurrent action:", username)

        classifiedMovementField.text = ""
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveData(type: String, movement: String, data: String) {
        val curTimestamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())

        // Add a new document with a generated ID
        db.collection(userId)
            .add(hashMapOf(
                "type" to type,
                "time" to curTimestamp,
                "movement" to movement,
                "data" to data
            ))
            .addOnSuccessListener { documentReference ->
                Log.d("PDIOT_DB", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("PDIOT_DB", "Error adding document", e)
            }
    }

    private fun getUserId() {
        val auth = Firebase.auth
        val firebaseAccount = auth.currentUser
        val googleAuthAccount = GoogleSignIn.getLastSignedInAccount(this)

        if (googleAuthAccount != null) {
            userId = googleAuthAccount.email.toString()
        } else if (firebaseAccount != null) {
            userId = firebaseAccount.uid
        }

        Log.d("PDIOT_FIREBASE_DEMO", userId)
    }

    override fun onDestroy() {
        unregisterReceiver(respeckLiveUpdateReceiver)
        unregisterReceiver(thingyLiveUpdateReceiver)

        respeckData = mutableListOf()
        thingyData = mutableListOf()

        respeckModel.close()
        thingyModel.close()

        looperRespeck.quit()
        looperThingy.quit()

        super.onDestroy()
    }

}