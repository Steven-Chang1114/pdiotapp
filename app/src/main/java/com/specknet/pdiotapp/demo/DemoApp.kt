package com.specknet.pdiotapp.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.ml.Model
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.FloatBuffer

class DemoApp : AppCompatActivity() {

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet
    lateinit var data : MutableList<FloatArray>
    lateinit var model: Model
    lateinit var floatArrayBuffer: FloatBuffer
    lateinit var lastMovement: ActionEnum
    lateinit var tflite : Interpreter

    lateinit var userId : String

    lateinit var respackActiveBtn: Button
    lateinit var thingyActiveBtn: Button
    lateinit var cloudActiveBtn: Button
    lateinit var localActiveBtn: Button
    lateinit var actionImage: ImageView
    lateinit var title: TextView

    var isRespackActive = true
    var isThingyActive = false
    var isCloudActive = true

    lateinit var classifiedMovement: ActionEnum
    private lateinit var classifiedMovementField : TextView

    var respackCounter = 0
    var thingyCounter = 0
    lateinit var allRespeckData: LineData

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

        floatArrayBuffer = FloatBuffer.allocate(500)

        setupPage()

        setupClickListeners()

        onRespackReceive()

        onThingyReceive()

    }

    private fun onThingyReceive() {
        // set up the broadcast receiver
        thingyLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("Thingy_demo_thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Thingy_demo_Live", "onReceive: liveData = " + liveData)

                    if (thingyCounter == 0) {
                        Toast.makeText(baseContext, "Thingy is running",
                            Toast.LENGTH_SHORT).show()
                    }

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    thingyCounter += 1

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

    private fun onRespackReceive() {
        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("Respack_demo_thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Respack_demo_Live", "onReceive: liveData = " + liveData)

                    if (respackCounter == 0) {
                        Toast.makeText(baseContext, "Respack is running",
                            Toast.LENGTH_SHORT).show()
                    }

                    // get all relevant intent contents
                    val accelX = liveData.accelX
                    val accelY = liveData.accelY
                    val accelZ = liveData.accelZ

                    val x = liveData.gyro.x
                    val y = liveData.gyro.y
                    val z = liveData.gyro.z

//                    data.add(floatArrayOf(accelX, accelY, accelZ, x, y, z))
//                    floatArrayBuffer.put(floatArrayOf(accelX, accelY, accelZ, x, y, z))

                    // Send data to the ML model and run for update
//                    classifyMovement(floatArrayBuffer)

                    respackCounter += 1
                    // updateGraph("respeck", x, y, z)

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

    private fun setupClickListeners() {
        respackActiveBtn.setOnClickListener {
            if (isRespackActive) {
                isRespackActive = false
                respackActiveBtn.setBackgroundResource(R.drawable.hardware_button_inactive)
            } else {
                isRespackActive = true
                respackActiveBtn.setBackgroundResource(R.drawable.hardware_button_active)
            }
        }

        thingyActiveBtn.setOnClickListener {

            if (isThingyActive) {
                isThingyActive = false
                thingyActiveBtn.setBackgroundResource(R.drawable.hardware_button_inactive)
            } else {
                isThingyActive = true
                thingyActiveBtn.setBackgroundResource(R.drawable.hardware_button_active)
            }

        }

        cloudActiveBtn.setOnClickListener {
            if (!isCloudActive)  {
                isCloudActive = true

                actionImage.setBackgroundResource(R.drawable.lying_down_on_back)

                classifiedMovementField.text = ActionEnum.LYING_DOWN_ON_THE_BACK.movement
                cloudActiveBtn.setBackgroundResource(R.drawable.hardware_button_active)
                localActiveBtn.setBackgroundResource(R.drawable.hardware_button_inactive)
            }
        }

        localActiveBtn.setOnClickListener {
            if (isCloudActive) {
                isCloudActive = false

                actionImage.setBackgroundResource(R.drawable.standing)

                classifiedMovementField.text = ActionEnum.STANDING.movement

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
            1 -> ActionEnum.WALKING_AT_NORMAL_SPEED
            2 -> ActionEnum.STANDING
            3 -> ActionEnum.SITTING_BENT_BACKWARD
            4 -> ActionEnum.SITTING_STRAIGHT
            5 -> ActionEnum.SITTING_BENT_BACKWARD
            6 -> ActionEnum.LYING_DOWN_ON_THE_RIGHT_SIDE
            7 -> ActionEnum.LYING_DOWN_ON_THE_LEFT_SIDE
            8 -> ActionEnum.LYING_DOWN_ON_THE_BACK
            9 -> ActionEnum.LYING_DOWN_ON_STOMACH
            10 -> ActionEnum.GENERAL_MOVEMENT
            11 -> ActionEnum.RUNNING
            12 -> ActionEnum.ASCENDING_STAIRS
            13 -> ActionEnum.DESCENDING_STAIRS
            else -> ActionEnum.LOADING
        }
    }

    private fun setupPage() {
        lastMovement = ActionEnum.GENERAL_MOVEMENT
        model = Model.newInstance(this)

        respackActiveBtn = findViewById(R.id.respack_button)
        thingyActiveBtn = findViewById(R.id.thingy_button)
        cloudActiveBtn = findViewById(R.id.azure_button)
        localActiveBtn = findViewById(R.id.local_button)
        classifiedMovementField = findViewById(R.id.movement)
        actionImage = findViewById(R.id.movement_img)
        title = findViewById(R.id.user)

        getUserId()

        val username = intent.getStringExtra("name")
        title.text = String.format("%s's\ncurrent action:", username)

        classifiedMovementField.text = ActionEnum.LYING_DOWN_ON_THE_RIGHT_SIDE.movement
        actionImage.setBackgroundResource(R.drawable.general_movement)
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

    private fun updatePage(action: ActionEnum) {
        runOnUiThread {
            // Stuff that updates the UI
            classifiedMovementField.text = action.movement
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        unregisterReceiver(thingyLiveUpdateReceiver)
        model.close()
        looperRespeck.quit()
        looperThingy.quit()
    }

}