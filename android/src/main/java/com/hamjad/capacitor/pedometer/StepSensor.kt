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
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit


class StepSensor : ComponentActivity(), SensorEventListener {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(this.applicationContext)
    }

    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var sensor: Sensor? = null

    private var steps:Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        // define sensor and sensor manager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (sensor == null) {
            return activityResult(RESULT_CANCELED, "Android Sensor is not present on this device")
        }

        if (!packageManager.hasSystemFeature("android.hardware.sensor.stepdetector")) {
            return activityResult(
                RESULT_CANCELED,
                "Step detector Sensor is not present on this device"
            )
        }

        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            registerStepSensor()
            return
        }

        // Check and request permission for Activity Recognition (Android 10+)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)

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
                activityResult(RESULT_OK, "Recording client subscribed")
                val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                val startTime = endTime.minus(Duration.ofDays(1))

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
                    for (dataSet in response.buckets.flatMap { it.dataSets }) {
                        for (dp in dataSet.dataPoints) {
                            for (field in dp.dataType.fields) {
                                val fieldValue = dp.getValue(field).asInt()
                                val startTimestamp = dp.getStartTime(TimeUnit.MILLISECONDS)
                                val endTimestamp = dp.getEndTime(TimeUnit.MILLISECONDS)
                                CoroutineScope(Dispatchers.IO).launch {
                                    writeStepsData(
                                        fieldValue.toLong(),
                                        startTimestamp,
                                        endTimestamp
                                    )
                                }

                            }
                        }
                    }
                }.addOnFailureListener { e ->
                    activityResult(
                        RESULT_CANCELED,
                        e.message ?: "Recording client data read failed"
                    )
                }
            }
            .addOnFailureListener { e ->
                activityResult(
                    RESULT_CANCELED,
                    e.message ?: "Recording client subscription failed"
                )
            }
    }


    override fun onResume() {
        super.onResume()
        steps = 0
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {

        sensorEvent?.let { event ->
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                steps++
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
        return
    }

    override fun onPause() {
        CoroutineScope(Dispatchers.IO).launch {
            writeStepsData(steps, null, null)
        }
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        CoroutineScope(Dispatchers.IO).launch {
            writeStepsData(steps, null, null)
        }
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private suspend fun writeStepsData(steps: Long, startTimestamp: Long?, endTimestamp: Long?) {
        if (steps <= 0) return

        val startTime = if (startTimestamp == null) {
            Instant.now()
        } else {
            Instant.ofEpochMilli(startTimestamp)
        }

        val endTime = if (endTimestamp == null) {
            startTime.plus(Duration.ofSeconds(30))
        } else {
            Instant.ofEpochMilli(endTimestamp)
        }

        val clientRecordId = startTimestamp ?: System.currentTimeMillis()

        try {
            val stepsRecord = StepsRecord(
                count = steps,
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata.autoRecorded(
                    clientRecordId = clientRecordId.toString(),
                    device = Device(type = Device.TYPE_PHONE),
                )
            )
            healthConnectClient.insertRecords(listOf(stepsRecord))
        } catch (e: Exception) {
            return
        }
    }


    private fun activityResult(resultCode: Int, msg: String) {
        val resultIntent = Intent()
        resultIntent.putExtra("result", msg)
        setResult(resultCode, resultIntent)
        if (resultCode == RESULT_CANCELED) finish()
    }
}
