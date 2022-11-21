package com.specknet.pdiotapp.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
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


class HistoricalData : AppCompatActivity() {
    lateinit var db : FirebaseFirestore

    lateinit var backBtn : ImageView
    lateinit var title: TextView
    lateinit var respeckBtn: Button
    lateinit var thingyBtn: Button
    lateinit var bothBtn: Button
    lateinit var chart: PieChart
    lateinit var dateSelector: Spinner

    lateinit var curType: String
    lateinit var userId : String
    lateinit var adapterArray : ArrayAdapter<String>

    var isThingySelected = false
    var isRespeckSelected = false
    var isBothSelected = false
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
        }

        dateSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val text = datesSelectable[position]

                if (parent != null) {
                    Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
                }
            }

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
    }

    private fun setUpPage() {
        db = Firebase.firestore
        getUserId()

        backBtn = findViewById(R.id.back_btn)
        title = findViewById(R.id.user)
        respeckBtn = findViewById(R.id.respeck_btn)
        thingyBtn = findViewById(R.id.thingy_btn)
        bothBtn = findViewById(R.id.both_btn)
        dateSelector = findViewById(R.id.spinner1)

        adapterArray = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, datesSelectable)
        dateSelector.adapter = adapterArray

        val username = intent.getStringExtra("name")
        title.text = String.format("%s's\nHistorical Data:", username)
    }

    private fun readData() {
        db.collection(userId)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d("PDIOT_DB", "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("PDIOT_DB", "Error getting documents.", exception)
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
