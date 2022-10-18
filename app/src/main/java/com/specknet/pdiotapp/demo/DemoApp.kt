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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.ml.Model
import com.specknet.pdiotapp.onboarding.OnBoardingActivity
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData

class DemoApp : AppCompatActivity() {

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet
    lateinit var model: Model

    lateinit var classifiedMovement: ActionEnum
    private lateinit var classifiedMovementField : TextView

    var time = 0f
    lateinit var allRespeckData: LineData

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        setupPage()

        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val accelX = liveData.accelX
                    val accelY = liveData.accelY
                    val accelZ = liveData.accelZ

                    val x = liveData.gyro.x
                    val y = liveData.gyro.y
                    val z = liveData.gyro.z

                    // Send data to the ML model and run for update
                    classifyMovement(
                        accelX,
                        accelY,
                        accelZ,
                        x,
                        y,
                        z
                    )

                    time += 1
                    // updateGraph("respeck", x, y, z)

                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)

    }

    private fun classifyMovement(
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        x: Float,
        y: Float,
        z: Float
    ) {

//        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 50, 6), DataType.FLOAT32)
//        inputFeature0.loadBuffer(byteBuffer)

//        val output = model.process()

//        Log.i("OUTPUT", "Our model predict it is ${output}")

        updatePage(ActionEnum.LYING_DOWN_ON_THE_BACK.movement)
    }

    private fun setupPage() {
        model = Model.newInstance(this)
        classifiedMovementField = findViewById(R.id.movement)
        classifiedMovementField.text = ActionEnum.LYING_DOWN_ON_THE_BACK.movement
    }

    private fun updatePage(movement: String) {

        classifiedMovementField.text = movement
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        model.close()
        looperRespeck.quit()
    }

}