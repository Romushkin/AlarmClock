package com.example.alarmclock

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var toolbarMain: Toolbar
    private lateinit var setAlarmBTN: Button
    private lateinit var alarmListLV: ListView

    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val alarms: MutableList<String> = mutableListOf()
    private val alarmTimes: MutableList<Long> = mutableListOf()
    private var calendar: Calendar? = null
    private var materialTimePicker: MaterialTimePicker? = null

    private val ALARMS_PREFS = "alarms_prefs"
    private val ALARMS_LIST_KEY = "alarms_list"
    private val ALARMS_TIMES_KEY = "alarms_times_list"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        toolbarMain = findViewById(R.id.toolbarMain)
        setSupportActionBar(toolbarMain)
        title = "Мой будильник"
        toolbarMain.setTitleTextColor(ContextCompat.getColor(this, R.color.white))

        loadAlarms()

        alarmListLV = findViewById(R.id.alarmListLV)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, alarms)
        alarmListLV.adapter = adapter

        setAlarmBTN = findViewById(R.id.setAlarmBTN)
        setAlarmBTN.setOnClickListener {
            materialTimePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Выберите время будильника")
                .build()
            materialTimePicker!!.addOnPositiveButtonClickListener {
                calendar = Calendar.getInstance()
                calendar?.set(Calendar.SECOND, 0)
                calendar?.set(Calendar.MILLISECOND, 0)
                calendar?.set(Calendar.MINUTE, materialTimePicker!!.minute)
                calendar?.set(Calendar.HOUR_OF_DAY, materialTimePicker!!.hour)

                val alarmTime = calendar!!.timeInMillis
                val alarmTimeFormatted = dateFormat.format(calendar!!.time)

                alarms.add(alarmTimeFormatted)
                alarmTimes.add(alarmTime)

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                val pendingIntent = getAlarmPendingIntent(alarmTime)

                alarmManager.setExact(RTC_WAKEUP, alarmTime, pendingIntent)

                adapter.notifyDataSetChanged()

                saveAlarms()

                Toast.makeText(this, "Будильник установлен на $alarmTimeFormatted", Toast.LENGTH_SHORT).show()
            }
            materialTimePicker!!.show(supportFragmentManager, "tag_picker")
        }
    }

    private fun getAlarmPendingIntent(alarmTime: Long): PendingIntent {
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("alarm_time", alarmTime)
        return PendingIntent.getBroadcast(
            this,
            alarmTime.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun saveAlarms() {
        val sharedPreferences = getSharedPreferences(ALARMS_PREFS, MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putStringSet(ALARMS_LIST_KEY, alarms.toSet())  // Сохраняем строковые значения
        editor.putString(ALARMS_TIMES_KEY, alarmTimes.joinToString(","))  // Сохраняем время как строку

        editor.apply()
    }

    private fun loadAlarms() {
        val sharedPreferences = getSharedPreferences(ALARMS_PREFS, MODE_PRIVATE)

        val alarmSet = sharedPreferences.getStringSet(ALARMS_LIST_KEY, setOf())
        val alarmTimesString = sharedPreferences.getString(ALARMS_TIMES_KEY, "")

        alarms.clear()
        alarms.addAll(alarmSet!!.toList())

        alarmTimes.clear()
        if (alarmTimesString != null && alarmTimesString.isNotEmpty()) {
            val times = alarmTimesString.split(",")
            alarmTimes.addAll(times.map { it.toLong() })
        }
    }
}