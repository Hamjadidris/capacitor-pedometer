package com.hamjad.capacitor.pedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.fitness.FitnessLocal
import com.google.android.gms.fitness.data.LocalDataType
import com.google.android.gms.fitness.request.LocalDataReadRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit


class StepSensor : AppCompatActivity(), SensorEventListener {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(this.applicationContext)
    }

    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var sensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request permission for Activity Recognition (Android 10+)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return activityResult(
                RESULT_CANCELED,
                "Activity recognition not available on this device"
            )
        }

        // define sensor and sensor manager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (sensor == null) {
            return activityResult(RESULT_CANCELED, "Android Sensor is not present on this device")
        }

        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            registerStepSensor()
            return
        }

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    registerStepSensor()
                } else {
                    activityResult(RESULT_CANCELED, "User denied Activity request")
                }
            }

        requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    @SuppressLint("MissingPermission")
    private fun registerStepSensor() {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        val hasMinPlayServices =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        if (hasMinPlayServices != ConnectionResult.SUCCESS) {
            return activityResult(
                RESULT_CANCELED,
                "Prompt user to update their device's Google Play services app"
            )
        }

        val localRecordingClient = FitnessLocal.getLocalRecordingClient(this)

        // Subscribe to steps data
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return activityResult(RESULT_CANCELED, "Activity permission is currently not granted")
        }

        localRecordingClient.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener {
                useCentescriptLog("local recording client successfully subscribed!")
                activityResult(RESULT_CANCELED, "Recording client subscribed")
                val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                val startTime = endTime.minus(Duration.ofMinutes(5))

                val readRequest =
                    LocalDataReadRequest.Builder()
                        .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
                        .bucketByTime(5, TimeUnit.MINUTES)
                        .setTimeRange(
                            startTime.toEpochSecond(),
                            endTime.toEpochSecond(),
                            TimeUnit.SECONDS
                        )
                        .build()

                localRecordingClient.readData(readRequest).addOnSuccessListener { response ->
                    useCentescriptLog("Local recording client read data $response")

                    for (dataSet in response.buckets.flatMap { it.dataSets }) {
                        for (dp in dataSet.dataPoints) {
                            for (field in dp.dataType.fields) {
                                val fieldName = field.name
                                val fieldValue = dp.getValue(field).asInt()
                                val startTimestamp = dp.getStartTime(TimeUnit.NANOSECONDS)
                                val endTimestamp = dp.getEndTime(TimeUnit.NANOSECONDS)
                                useCentescriptLog("\tLocalField: $fieldName LocalValue: $fieldValue")

                                CoroutineScope(Dispatchers.IO).launch {
                                    writeStepsData(fieldValue.toLong(), startTimestamp, endTimestamp)
                                }

                            }
                        }
                    }
                }.addOnFailureListener { e ->
                    println(e)
                    useCentescriptLog(e.message ?: "Recording client data read failed")
                    activityResult(
                        RESULT_CANCELED,
                        e.message ?: "Recording client data read failed"
                    )
                }
            }
            .addOnFailureListener { e ->
                println(e)
                useCentescriptLog(e.message ?: "Recording client subscription failed")
                activityResult(
                    RESULT_CANCELED,
                    e.message ?: "Recording client subscription failed"
                )
            }


        activityResult(RESULT_OK, "Data from sensor Activity")
    }


    override fun onResume() {
        super.onResume()
        sensorManager.registerListener( this , sensor , SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.let { event ->
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                useCentescriptLog("Sensor event triggered at ${event.timestamp}")

                CoroutineScope(Dispatchers.IO).launch {
                    writeStepsData(1, event.timestamp, null)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, value: Int) {
        TODO("Not yet implemented")
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private suspend fun writeStepsData(steps: Long, startTimestamp:Long?, endTimestamp: Long?) {
        val endTime = if (endTimestamp == null){
            Instant.now()
        }else {
            Instant.ofEpochMilli(endTimestamp)
        }

        val startTime = if (startTimestamp == null){
            Instant.now()
        }else {
            Instant.ofEpochMilli(startTimestamp)
        }

        try {
            val stepsRecord = StepsRecord(
                count = steps,
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata.autoRecorded(
                    device = Device(type = Device.TYPE_PHONE)
                )
            )
            healthConnectClient.insertRecords(listOf(stepsRecord))
        } catch (e: Exception) {
            throw e
        }
    }

    private fun activityResult(resultCode: Int, msg: String) {
        val resultIntent = Intent()
        resultIntent.putExtra("result", msg)
        setResult(resultCode, resultIntent)
        finish()
    }

    private var client: OkHttpClient = OkHttpClient()

    private fun useCentescriptLog(msg: String) {

        val mediaType = "application/json".toMediaType()
        val bodyString = "{\n    \"channel\": \"log-alerts-fe\",\n    \"message\": \"$msg\" \n}"
        val body = bodyString.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://master.centescript.com/send_log")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = client.newCall(request).execute()
                println("log res: $res")
            } catch (e: Exception) {
                println("log err: $e")
            }
        }

    }
}
