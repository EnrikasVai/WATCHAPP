package com.example.bakis.presentation

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.SensorRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleFitDataHandler(private val context: Context) {


    //Functions for retrieving Today's data steps, bpm, sleep, calories. Currently only used in home screen to show today's health data.
    interface StepDataListener {
        fun onStepDataReceived(stepCount: Int)
        fun onError(e: Exception)
    }
    fun readStepData(listener: StepDataListener) {
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = startCalendar.timeInMillis

        startCalendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = startCalendar.timeInMillis - 1

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context)!!)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val dataSet = response.buckets.flatMap { it.dataSets }.flatMap { it.dataPoints }
                val totalSteps = dataSet.sumOf { it.getValue(Field.FIELD_STEPS).asInt() }
                listener.onStepDataReceived(totalSteps)
                Toast.makeText(context, "G-FIT Total steps: $totalSteps", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFit", "There was a problem reading the data.", e)
                listener.onError(e)
            }
    }
    interface SleepDataListener {
        fun onSleepDataReceived(totalSleepMinutes: Int)
        fun onError(e: Exception)
    }

    fun readSleepData(listener: SleepDataListener) {
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = startCalendar.timeInMillis

        startCalendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = startCalendar.timeInMillis - 1

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_SLEEP_SEGMENT)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context)!!)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                var totalSleepMinutes = 0
                response.dataSets.flatMap { it.dataPoints }.forEach { dataPoint ->
                    val startTime = dataPoint.getStartTime(TimeUnit.MINUTES)
                    val endTime = dataPoint.getEndTime(TimeUnit.MINUTES)
                    totalSleepMinutes += (endTime - startTime).toInt()
                }
                listener.onSleepDataReceived(totalSleepMinutes)
                Toast.makeText(context, "G-FIT Total sleep: $totalSleepMinutes minutes", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFitSleep", "There was a problem reading the sleep data.", e)
                listener.onError(e)
            }
    }
    //Read Sensors Real time:
    interface HeartRateDataListener {
        fun onHeartRateDataReceived(bpm: Float)
        fun onError(e: Exception)
    }
    fun subscribeToHeartRate(listener: HeartRateDataListener) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            listener.onError(Exception("Not signed in to Google Fit."))
            return
        }

        val request = SensorRequest.Builder()
            .setDataType(DataType.TYPE_HEART_RATE_BPM)
            .setSamplingRate(1, TimeUnit.SECONDS)
            .build()

        Fitness.getSensorsClient(context, account)
            .add(request) { dataPoint ->
                val bpm = dataPoint.getValue(Field.FIELD_BPM).asFloat()
                listener.onHeartRateDataReceived(bpm)
            }
            .addOnSuccessListener {
                Log.d("GoogleFitDataHandler", "Successfully subscribed to heart rate sensor.")
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFitDataHandler", "There was a problem subscribing to the heart rate sensor.", e)
                listener.onError(e)
            }
    }
    fun writeHeartRateData(bpm: Float, timestamp: Long) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e("GoogleFitDataHandler", "Not signed in to Google Fit.")
            return
        }

        // Define the data source
        val dataSource = DataSource.Builder()
            .setAppPackageName(context)
            .setDataType(DataType.TYPE_HEART_RATE_BPM)
            .setType(DataSource.TYPE_RAW)
            .build()

        // Create a data point for the heart rate
        val dataPoint = DataPoint.builder(dataSource)
            .setField(Field.FIELD_BPM, bpm)
            .setTimestamp(timestamp, TimeUnit.MILLISECONDS)
            .build()

        // Create a data set
        val dataSet = DataSet.builder(dataSource)
            .add(dataPoint)
            .build()

        // Insert the data set into the user's Google Fit store
        Fitness.getHistoryClient(context, account)
            .insertData(dataSet)
            .addOnSuccessListener {
                Log.d("GoogleFitDataHandler", "Successfully wrote heart rate data to Google Fit.")
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFitDataHandler", "There was a problem writing the heart rate data to Google Fit.", e)
            }
    }
    //last bpm reading and time
    interface LastHeartRateDataListener {
        fun onHeartRateDataReceived(bpm: Float, timeString: String)
        fun onError(e: Exception)
    }

    fun readLastHeartRateData(listener: LastHeartRateDataListener) {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .setLimit(1)
            .build()

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context)!!)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val dataPoints = response.dataSets.flatMap { it.dataPoints }
                if (dataPoints.isNotEmpty()) {
                    val lastDataPoint = dataPoints.last()
                    val bpm = lastDataPoint.getValue(Field.FIELD_BPM).asFloat()
                    val timestamp = lastDataPoint.getTimestamp(TimeUnit.MILLISECONDS)
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val timeString = timeFormat.format(Date(timestamp))
                    listener.onHeartRateDataReceived(bpm, timeString)
                    Toast.makeText(context, "G-FIT Last heart rate: $bpm BPM at $timeString", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "No heart rate data available", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFit", "There was a problem reading the heart rate data.", e)
                listener.onError(e)
            }
    }
    //todays all bpm readings
    interface HeartRateDataListenerToday {
        fun onHeartRateDataReceived(readings: List<Pair<String, Float>>) // Pair of time string and BPM
        fun onError(e: Exception)
    }
    fun readTodaysHeartRateData(listener: HeartRateDataListenerToday) {
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = startCalendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context)!!)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val readings = response.dataSets.flatMap { dataSet ->
                    dataSet.dataPoints.map { dataPoint ->
                        val bpm = dataPoint.getValue(Field.FIELD_BPM).asFloat()
                        val timestamp = dataPoint.getTimestamp(TimeUnit.MILLISECONDS)
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val timeString = timeFormat.format(Date(timestamp))
                        Pair(timeString, bpm)
                    }
                }
                if (readings.isNotEmpty()) {
                    listener.onHeartRateDataReceived(readings)
                } else {
                    Toast.makeText(context, "No heart rate data available for today", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("GoogleFit", "There was a problem reading the heart rate data.", e)
                listener.onError(e)
            }
    }
    //todays min max bpm value
    interface MinMaxHeartRateListener {
        fun onMinMaxHeartRateFound(minBpm: Float, maxBpm: Float)
        fun onError(e: Exception)
    }

    fun fetchMinMaxHeartRateForToday(listener: MinMaxHeartRateListener) {
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endTime = System.currentTimeMillis()
        val startTime = startCalendar.timeInMillis

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_HEART_RATE_BPM)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context)!!)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val buckets = response.buckets
                var minBpm = Float.MAX_VALUE
                var maxBpm = Float.MIN_VALUE

                for (bucket in buckets) {
                    val dataSet = bucket.getDataSet(DataType.TYPE_HEART_RATE_BPM)
                    if (dataSet != null) {
                        for (dataPoint in dataSet.dataPoints) {
                            val bpm = dataPoint.getValue(Field.FIELD_BPM).asFloat()
                            if (bpm < minBpm) minBpm = bpm
                            if (bpm > maxBpm) maxBpm = bpm
                        }
                    }
                }
                if (minBpm != Float.MAX_VALUE && maxBpm != Float.MIN_VALUE) {
                    listener.onMinMaxHeartRateFound(minBpm, maxBpm)
                } else {
                    listener.onError(Exception("No heart rate data available for today."))
                }
            }
            .addOnFailureListener { e ->
                listener.onError(e)
            }
    }




}