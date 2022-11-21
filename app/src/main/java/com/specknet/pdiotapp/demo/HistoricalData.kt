package com.specknet.pdiotapp.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.specknet.pdiotapp.HomePage
import com.specknet.pdiotapp.R
import java.time.Instant
import java.util.Date


class HistoricalData : AppCompatActivity() {
    lateinit var db : FirebaseFirestore

    lateinit var backBtn : ImageView
    lateinit var title: TextView
    lateinit var respeckBtn: Button
    lateinit var thingyBtn: Button
    lateinit var bothBtn: Button
    lateinit var chart: PieChart
    lateinit var dateSelector: Spinner

    lateinit var userId : String
    lateinit var endDate: Instant
    lateinit var startDate: Instant
    lateinit var adapterArray : ArrayAdapter<String>

    var timePeriod = "Past 5 minutes"
    var isThingySelected = false
    var isRespeckSelected = false
    var isBothSelected = false
    var historicalData = mapOf<String, HistoricalDataModel>()
    val datesSelectable = listOf("Past 5 minutes", "Past 10 minutes", "Past 30 minutes",
        "Past 1 hour", "Past 3 hours", "Past 6 hours", "Past 12 hours", "Past 24 hours",
        "Past 7 days")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.historical_data)

        setUpPage()

        setupClickListeners()

        setUpPieChart()

    }

    private fun setUpPieChart() {
        chart = findViewById(R.id.chart1);
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);

        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.parseColor("#FEF5E6"));

        chart.setTransparentCircleColor(Color.parseColor("#FEF5E6"));
        chart.setTransparentCircleAlpha(110);

        chart.setHoleRadius(40f);
        chart.setTransparentCircleRadius(43f);

        chart.animateY(1400, Easing.EaseInOutQuad);

        chart.legend.isEnabled = false
        chart.setEntryLabelTextSize(8f)

        setData(7, 100);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setData(count: Int, range: Int) {
        val values: ArrayList<PieEntry> = ArrayList()
        for (i in 0 until count) {
//            val icon = resources.getDrawable(R.drawable.desk_work)
//            val bitmap = (icon as BitmapDrawable).bitmap
//            val d: Drawable =
//                BitmapDrawable(resources,
//                    Bitmap.createScaledBitmap(
//                        bitmap,
//                        20,
//                        20,
//                        true
//                    )
//                )

            values.add(
                PieEntry(
                    (Math.random() * range + range / 5).toFloat(),
                    "AHDWHDWH"
                )
            )
        }

        val dataSet = PieDataSet(values, "Historical Movements")
        dataSet.setDrawIcons(true);
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS);

        //dataSet.setSelectionShift(0f);
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(chart))
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.parseColor("#FEF5E6"))
        chart.data = data

        // undo all highlights
        chart.highlightValues(null);

        chart.invalidate();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupClickListeners() {
        respeckBtn.setOnClickListener {
            if (!isRespeckSelected) {
                isThingySelected = false
                thingyBtn.setBackgroundResource(R.drawable.hardware_button_inactive)

                isBothSelected = false
                bothBtn.setBackgroundResource(R.drawable.hardware_button_inactive)

                isRespeckSelected = true
                respeckBtn.setBackgroundResource(R.drawable.hardware_button_active)
            }

            historicalData = mapOf<String, HistoricalDataModel>()
            updateData()
        }

        thingyBtn.setOnClickListener {
            if (!isThingySelected) {
                isRespeckSelected = false
                respeckBtn.setBackgroundResource(R.drawable.hardware_button_inactive)

                isBothSelected = false
                bothBtn.setBackgroundResource(R.drawable.hardware_button_inactive)

                isThingySelected = true
                thingyBtn.setBackgroundResource(R.drawable.hardware_button_active)
            }

            historicalData = mapOf<String, HistoricalDataModel>()
            updateData()
        }

        bothBtn.setOnClickListener {
            if (!isBothSelected) {
                isRespeckSelected = false
                respeckBtn.setBackgroundResource(R.drawable.hardware_button_inactive)

                isThingySelected = false
                thingyBtn.setBackgroundResource(R.drawable.hardware_button_inactive)

                isBothSelected = true
                bothBtn.setBackgroundResource(R.drawable.hardware_button_active)
            }

            historicalData = mapOf<String, HistoricalDataModel>()
            updateData()
        }

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

        dateSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                historicalData = mapOf<String, HistoricalDataModel>()

                val text = datesSelectable[position]
                timePeriod = text
                endDate = Instant.now()
                startDate = getStartDate()

                updateData()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateData() {
        endDate = Instant.now()
        startDate = getStartDate()

        readData()
    }

    @JvmName("getStartDate1")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getStartDate() : Instant {
        return when(timePeriod) {
            "Past 5 minutes", -> endDate.minusSeconds(5*60)
            "Past 10 minutes" -> endDate.minusSeconds(10*60)
            "Past 30 minutes" -> endDate.minusSeconds(30*60)
            "Past 1 hour" -> endDate.minusSeconds(1*60*60)
            "Past 3 hours" -> endDate.minusSeconds(3*60*60)
            "Past 6 hours" -> endDate.minusSeconds(6*60*60)
            "Past 12 hours" -> endDate.minusSeconds(12*60*60)
            "Past 24 hours" -> endDate.minusSeconds(24*60*60)
            "Past 7 days" -> endDate.minusSeconds(7*24*60*60)
            else -> endDate
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setUpPage() {
        db = Firebase.firestore
        getUserId()

        backBtn = findViewById(R.id.back_btn)
        title = findViewById(R.id.user)
        respeckBtn = findViewById(R.id.respeck_btn)
        thingyBtn = findViewById(R.id.thingy_btn)
        bothBtn = findViewById(R.id.both_btn)
        dateSelector = findViewById(R.id.spinner1)

        adapterArray = ArrayAdapter<String>(this, R.layout.spinner_item, datesSelectable)
        dateSelector.adapter = adapterArray

        historicalData = mapOf<String, HistoricalDataModel>()
        timePeriod = "Past 5 minutes"
        updateData()

        val username = intent.getStringExtra("name")
        title.text = String.format("%s's\nHistorical Data:", username)
    }

    private fun readData() {
        db.collection(userId)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
//                    val data = HistoricalDataModel(
//                        document.id,
//                        document.data["type"] as String,
//                        document.data["movement"] as String,
//                        document.data["time"],
//                    )
                    val type = document.data["type"]
                    if (type == getCurType()) {
                        Log.d("PDIOT_DB", "${document.id} => ${document.data}")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("PDIOT_DB", "Error getting documents.", exception)
            }
    }

    @JvmName("getCurType1")
    private fun getCurType() : String {
        if (isThingySelected) {
            return "thingy"
        } else if (isRespeckSelected) {
            return "respeck"
        } else if (isBothSelected) {
            return "both"
        } else {
            return "none"
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
}
