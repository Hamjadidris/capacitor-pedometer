package com.hamjad.capacitor.pedometer

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import kotlin.reflect.KClass

val sourceNameMap = mapOf(
    0 to "UNKNOWN",
    1 to "WATCH",
    2 to "PHONE",
    3 to "SCALE",
    4 to "RING",
    5 to "HEAD_MOUNTED",
    6 to "FITNESS_BAND",
    7 to "CHEST_STRAP",
    8 to "SMART_DISPLAY",
)

private fun valueToDouble(value: Any?): Double {
    fun stepMapper(value: Long): Double {
        return value.toDouble()
    }

    fun calorieMapper(value: Energy): Double {
        return value.inKilocalories
    }

    fun distanceMapper(value: Length): Double {
        return value.inMeters
    }

    return when (value) {
        is Long -> stepMapper(value)

        is Energy -> calorieMapper(value)

        is Length -> distanceMapper(value)

        else -> 0.0
    }
}

@CapacitorPlugin(name = "Pedometer")
class Pedometer : Plugin() {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(this.context.applicationContext)
    }
    private val permissionContract by lazy {
        PermissionController.createRequestPermissionResultContract()
    }

    private var savedCall: PluginCall? = null

    private lateinit var permissionsLauncher: ActivityResultLauncher<Set<String>>
    override fun load() {
        super.load()

        permissionsLauncher = activity.registerForActivityResult(permissionContract) { result ->
            println(result)
            val res = getPermissionResult(result)
            savedCall?.resolve(res)
        }
    }

    @PluginMethod
    fun checkAvailability(call: PluginCall) {
        val availability =
            when (val status = HealthConnectClient.getSdkStatus(this.context)) {
                HealthConnectClient.SDK_AVAILABLE -> "AVAILABLE"
                HealthConnectClient.SDK_UNAVAILABLE -> "UNAVAILABLE"
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "NOTINSTALLED"
                else -> throw RuntimeException("Invalid sdk status: $status")
            }

        val res = JSObject().apply { put("result", availability) }
        call.resolve(res)
        return
    }

    private val permissions: MutableMap<String, String> = mutableMapOf(
        "readSteps" to HealthPermission.getReadPermission(StepsRecord::class),
        "writeSteps" to HealthPermission.getWritePermission(StepsRecord::class),
        "distance" to HealthPermission.getReadPermission(DistanceRecord::class),
        "calories" to HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    )

    private val permissionRecords = permissions.values.toSet()

    private fun <T : Record> getRecord(activityType: String?): KClass<T> {
        @Suppress("UNCHECKED_CAST")
        return when (activityType) {
            "steps" -> StepsRecord::class as KClass<T>

            "calories" -> DistanceRecord::class as KClass<T>

            "distance" -> TotalCaloriesBurnedRecord::class as KClass<T>

            else -> throw RuntimeException("Unsupported activityType: $activityType")
        }
    }


    @PluginMethod
    fun checkPermission(call: PluginCall) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val grantedPermissions =
                    healthConnectClient.permissionController.getGrantedPermissions()
                val res = getPermissionResult(grantedPermissions)

                call.resolve(res)
            } catch (e: Exception) {
                call.reject("Checking permissions failed: ${e.message}")
            }
        }
    }

    @PluginMethod
    fun requestPermission(call: PluginCall) {
        if (HealthConnectClient.getSdkStatus(this.context) == HealthConnectClient.SDK_AVAILABLE) {

            var grantedPermissions: Set<String>

            runBlocking {
                grantedPermissions =
                    healthConnectClient.permissionController.getGrantedPermissions()
            }

            val hasAllPermissions = grantedPermissions.containsAll(permissionRecords)
            if (hasAllPermissions) {
                val res = getPermissionResult(grantedPermissions)
                call.resolve(res)
                return
            }

            try {
                savedCall = call
                permissionsLauncher.launch(permissionRecords)
            } catch (e: Exception) {
                call.reject("Requesting permissions failed: ${e.message}")
            }

            return
        }

        call.reject("Health Connect is unavailable")
        return
    }

    @PluginMethod
    fun useStepSensor(call: PluginCall) {
        val savedContext = this.context
        var grantedPermissions: Set<String>

        runBlocking {
            grantedPermissions =
                healthConnectClient.permissionController.getGrantedPermissions()
        }

        try {
            if (!grantedPermissions.contains(permissions.getValue("writeSteps"))) {
                call.reject("Sensor requires permission to write steps")
            }

            val sensorIntent = Intent(savedContext, StepSensor::class.java)
            startActivityForResult(call, sensorIntent, "handleSensorResults")

        } catch (e: Exception) {
            call.reject("Registering Sensor failed: ${e.message}")
        }

    }

    @ActivityCallback
    fun handleSensorResults(call: PluginCall, result: ActivityResult) {

        val activityRes = result.data?.getStringExtra("result")

        val res = JSObject()
        res.put("result", activityRes)

        if (result.resultCode == RESULT_OK) {
            call.resolve(res)
        } else {
            call.reject(res.getString("result"))
        }
    }

    private fun getPermissionResult(grantedPermissions: Set<String>): JSObject {
        val readPermissions = JSObject()

        for ((name, recordType) in permissions) {
            readPermissions.put(name, grantedPermissions.contains(recordType))
        }

        val hasAllPermissions = grantedPermissions.containsAll(permissionRecords)

        val result = JSObject()
        result.apply {
            put("permissions", readPermissions)
            put("allGranted", hasAllPermissions)
        }
        return result

    }

    @PluginMethod
    fun queryAggregatedActivity(call: PluginCall) {
        try {
            val startDate = call.getString("startDate")
            val endDate = call.getString("endDate")
            val activityType = call.getString("activityType")
            val bucket = call.getString("bucket")

            var period: Period? = null
            var duration: Duration? = null

            if (startDate == null || endDate == null || activityType == null || bucket == null) {
                call.reject("Missing required parameters: startDate, endDate, activityType, or bucket")
                return
            }

            val startDateTime = Instant.parse(startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val endDateTime = Instant.parse(endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()

            val activityAggregatedQueryMetaData = getAggregatedQueryMetaData(activityType)

            if (bucket == "hour") {
                duration = Duration.ofHours(1)
            } else {
                period = when (bucket) {
                    "day" -> Period.ofDays(1)
                    "week" -> Period.ofWeeks(1)
                    "month" -> Period.ofWeeks(1)
                    else -> throw RuntimeException("Unsupported bucket: $bucket")
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val res = queryAggregatedData(
                        activityAggregatedQueryMetaData,
                        TimeRangeFilter.between(startDateTime, endDateTime),
                        period,
                        duration
                    )

                    val aggregatedList = JSArray()
                    res.forEach { aggregatedList.put(it.toAggregatedResultObject()) }

                    val finalResult = JSObject()
                    finalResult.put("aggregatedData", aggregatedList)
                    call.resolve(finalResult)

                } catch (e: Exception) {
                    call.reject("Error querying aggregated data: ${e.message}")
                }
            }
        } catch (e: Exception) {
            call.reject(e.message)
            return
        }
    }

    private suspend fun queryAggregatedData(
        activityAggregatedQueryMetaData: AggregateMetricData,
        timeRange: TimeRangeFilter,
        period: Period?,
        duration: Duration?
    ): List<AggregatedSample> {

        if (duration != null) {
            val response: List<AggregationResultGroupedByDuration> =
                healthConnectClient.aggregateGroupByDuration(
                    AggregateGroupByDurationRequest(
                        metrics = setOf(activityAggregatedQueryMetaData.metric),
                        timeRangeFilter = timeRange,
                        timeRangeSlicer = duration,
                    )
                )

            return response.map {
                val mappedValue = activityAggregatedQueryMetaData.getValue(it.result)
                val startDateTime = getLocalDateTime(it.startTime)
                val endDateTime = getLocalDateTime(it.endTime)
                val source = it.result.dataOrigins.toString()
                AggregatedSample(startDateTime, endDateTime, source, mappedValue)
            }
        }

        if (period == null) {
            return emptyList()
        }

        val response: List<AggregationResultGroupedByPeriod> =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(activityAggregatedQueryMetaData.metric),
                    timeRangeFilter = timeRange,
                    timeRangeSlicer = period
                )
            )

        return response.map {
            val mappedValue = activityAggregatedQueryMetaData.getValue(it.result)
            val source = it.result.dataOrigins.toString()
            AggregatedSample(it.startTime, it.endTime, source, mappedValue)
        }

    }

    private fun getAggregatedQueryMetaData(activityType: String): AggregateMetricData {

        return when (activityType) {
            "steps" -> getAggregateMetricData(
                "steps",
                StepsRecord.COUNT_TOTAL,
                StepsRecord
            )

            "calories" -> getAggregateMetricData(
                "calories",
                TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                TotalCaloriesBurnedRecord
            )

            "distance" -> getAggregateMetricData(
                "distance",
                DistanceRecord.DISTANCE_TOTAL,
                DistanceRecord
            )

            else -> throw RuntimeException("Unsupported activityType: $activityType")
        }
    }


    private fun <T> getAggregateMetricData(
        name: String,
        metric: AggregateMetric<Any>,
        recordType: T
    ): AggregateMetricData {
        return AggregateMetricData(name, metric, recordType as Any)
    }

    private fun getLocalDateTime(time: Instant): LocalDateTime {
        return time.atZone(ZoneId.of("+0")).toLocalDateTime()
    }

    private fun getTimeRangeFilter(
        name: String?,
        startDate: String?,
        endDate: String?
    ): TimeRangeFilter {
        val startDateTime = Instant.parse(startDate)
        val endDateTime = Instant.parse(endDate)

        return when (name) {
            "before" -> TimeRangeFilter.before(endDateTime)
            "after" -> TimeRangeFilter.after(startDateTime)
            "between" -> TimeRangeFilter.between(startDateTime, endDateTime)
            else -> throw IllegalArgumentException("Unexpected TimeRange type: $name")
        }
    }

    private fun parseQueryResult(result: Any): QuerySample {

        return when (result) {
            is StepsRecord -> QuerySample(
                getLocalDateTime(result.startTime),
                getLocalDateTime(result.endTime),
                result.metadata,
                valueToDouble(result.count)
            )

            is DistanceRecord -> QuerySample(
                getLocalDateTime(result.startTime),
                getLocalDateTime(result.endTime),
                result.metadata,
                valueToDouble(result.distance)
            )

            is TotalCaloriesBurnedRecord -> QuerySample(
                getLocalDateTime(result.startTime),
                getLocalDateTime(result.endTime),
                result.metadata,
                valueToDouble(result.energy)
            )

            else -> throw RuntimeException("Unsupported result record")
        }
    }

    @PluginMethod
    fun queryActivity(call: PluginCall) {
        val activityType = call.getString("activityType") ?: ""
        val startDate = call.getString("startDate")
        val endDate = call.getString("endDate")
        val filterType = call.getString("filterType")
        val dataOriginFilter = call.getArray("dataOriginFilter") ?: JSArray()
        val limit = call.getInt("limit") ?: 1000
        val ascending = call.getBoolean("ascending") ?: true
        val pageToken = call.getString("pageToken")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = ReadRecordsRequest(
                    recordType = getRecord(activityType),
                    timeRangeFilter = getTimeRangeFilter(filterType, startDate, endDate),
                    dataOriginFilter = dataOriginFilter.toList<DataOrigin>().toSet(),
                    ascendingOrder = ascending,
                    pageSize = limit,
                    pageToken = pageToken,
                )

                val result = healthConnectClient.readRecords(request)

                val records = JSArray()
                result.records.forEach {
                    val parsedResult = parseQueryResult(it)
                    records.put(parsedResult.toQueryResultObject())
                }

                val res = JSObject()
                res.apply {
                    put("activities", records)
                    put("pageToken", result.pageToken)
                }
                call.resolve(res)
            } catch (e: Exception) {
                call.reject(e.message)
            }

        }
    }

    data class AggregateMetricData
        (
        val name: String,
        val metric: AggregateMetric<Any>,
        val recordType: Any
    ) {
        fun getValue(result: AggregationResult): Double {
            return valueToDouble(result[metric])
        }
    }

    data class AggregatedSample(
        val startDate: LocalDateTime,
        val endDate: LocalDateTime,
        val source: String?,
        val value: Double?
    ) {
        fun toAggregatedResultObject(): JSObject {
            val o = JSObject()
            o.put("startDate", startDate)
            o.put("endDate", endDate)
            o.put("sourceName", source)
            o.put("value", value ?: 0)

            return o
        }
    }

    data class QuerySample(
        val startDate: LocalDateTime,
        val endDate: LocalDateTime,
        val metadata: Metadata,
        val value: Double?,
    ) {
        fun toQueryResultObject(): JSObject {

            val o = JSObject()
            o.put("id", metadata.id)
            o.put("startDate", startDate)
            o.put("endDate", endDate)
            o.put("sourceName", metadata.dataOrigin.toString())
            o.put("sourceDevice", sourceNameMap.getOrDefault(metadata.device?.type, "UNKNOWN"))
            o.put("value", value ?: 0)

            return o
        }
    }

}

