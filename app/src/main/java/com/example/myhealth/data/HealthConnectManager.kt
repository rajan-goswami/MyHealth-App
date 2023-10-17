/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.myhealth.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources.NotFoundException
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource
import android.health.connect.datatypes.MealType
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_UNAVAILABLE
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodGlucoseRecord.Companion.RELATION_TO_MEAL_AFTER_MEAL
import androidx.health.connect.client.records.BloodGlucoseRecord.Companion.SPECIMEN_SOURCE_WHOLE_BLOOD
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Velocity
import com.example.myhealth.R
import com.example.myhealth.roundOffDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.round
import kotlin.random.Random
import kotlin.reflect.KClass

// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

/**
 * Demonstrates reading and writing from Health Connect.
 */
class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val healthConnectCompatibleApps by lazy {
        val intent = Intent("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE")

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )
        }

        packages.associate {
            val icon = try {
                context.packageManager.getApplicationIcon(it.activityInfo.packageName)
            } catch (e: NotFoundException) {
                null
            }
            val label = context.packageManager.getApplicationLabel(it.activityInfo.applicationInfo)
                .toString()
            it.activityInfo.packageName to
                    HealthConnectAppInfo(
                        packageName = it.activityInfo.packageName,
                        icon = icon,
                        appLabel = label
                    )
        }
    }

    var availability = mutableStateOf(SDK_UNAVAILABLE)
        private set

    fun checkAvailability() {
        availability.value = HealthConnectClient.getSdkStatus(context)
    }

    init {
        checkAvailability()
    }

    /**
     * Determines whether all the specified permissions are already granted. It is recommended to
     * call [PermissionController.getGrantedPermissions] first in the permissions flow, as if the
     * permissions are already granted then there is no need to request permissions via
     * [PermissionController.createRequestPermissionResultContract].
     */
    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions()
            .containsAll(permissions)
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    suspend fun revokeAllPermissions() {
        healthConnectClient.permissionController.revokeAllPermissions()
    }

    /**
     * Obtains a list of [ExerciseSessionRecord]s in a specified time frame. An Exercise Session Record is a
     * period of time given to an activity, that would make sense to a user, e.g. "Afternoon run"
     * etc. It does not necessarily mean, however, that the user was *running* for that entire time,
     * more that conceptually, this was the activity being undertaken.
     */
    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    /**
     * Writes an [ExerciseSessionRecord] to Health Connect, and additionally writes underlying data for
     * the session too, such as [StepsRecord], [DistanceRecord] etc.
     */
    suspend fun writeExerciseSession(
        start: ZonedDateTime,
        end: ZonedDateTime
    ): InsertRecordsResponse {
        return healthConnectClient.insertRecords(
            listOf(
                ExerciseSessionRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                    title = "My Run #${Random.nextInt(0, 60)}"
                ),
                StepsRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    count = (1000 + 1000 * Random.nextInt(3)).toLong()
                ),
                DistanceRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    distance = Length.meters((1000 + 100 * Random.nextInt(20)).toDouble())
                ),
                TotalCaloriesBurnedRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    energy = Energy.calories((140 + Random.nextInt(20)) * 0.01)
                )
            ) + buildHeartRateSeries(start, end) + buildSpeedSeries(start, end)
        )
    }

    /**
     * Deletes an [ExerciseSessionRecord] and underlying data.
     */
    suspend fun deleteExerciseSession(uid: String) {
        val exerciseSession = healthConnectClient.readRecord(ExerciseSessionRecord::class, uid)
        healthConnectClient.deleteRecords(
            ExerciseSessionRecord::class,
            recordIdsList = listOf(uid),
            clientRecordIdsList = emptyList()
        )
        val timeRangeFilter = TimeRangeFilter.between(
            exerciseSession.record.startTime,
            exerciseSession.record.endTime
        )
        val rawDataTypes: Set<KClass<out Record>> = setOf(
            HeartRateRecord::class,
            SpeedRecord::class,
            DistanceRecord::class,
            StepsRecord::class,
            TotalCaloriesBurnedRecord::class
        )
        rawDataTypes.forEach { rawType ->
            healthConnectClient.deleteRecords(rawType, timeRangeFilter)
        }
    }

    /**
     * Reads aggregated data and raw data for selected data types, for a given [ExerciseSessionRecord].
     */
    suspend fun readAssociatedSessionData(
        uid: String
    ): ExerciseSessionData {
        val exerciseSession = healthConnectClient.readRecord(ExerciseSessionRecord::class, uid)
        // Use the start time and end time from the session, for reading raw and aggregate data.
        val timeRangeFilter = TimeRangeFilter.between(
            startTime = exerciseSession.record.startTime,
            endTime = exerciseSession.record.endTime
        )
        val aggregateDataTypes = setOf(
            ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
            StepsRecord.COUNT_TOTAL,
            DistanceRecord.DISTANCE_TOTAL,
            TotalCaloriesBurnedRecord.ENERGY_TOTAL,
            HeartRateRecord.BPM_AVG,
            HeartRateRecord.BPM_MAX,
            HeartRateRecord.BPM_MIN,
            SpeedRecord.SPEED_AVG,
            SpeedRecord.SPEED_MAX,
            SpeedRecord.SPEED_MIN
        )
        // Limit the data read to just the application that wrote the session. This may or may not
        // be desirable depending on the use case: In some cases, it may be useful to combine with
        // data written by other apps.
        val dataOriginFilter = setOf(exerciseSession.record.metadata.dataOrigin)
        val aggregateRequest = AggregateRequest(
            metrics = aggregateDataTypes,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = dataOriginFilter
        )
        val aggregateData = healthConnectClient.aggregate(aggregateRequest)
        val speedData = readData<SpeedRecord>(timeRangeFilter, dataOriginFilter)
        val heartRateData = readData<HeartRateRecord>(timeRangeFilter, dataOriginFilter)

        return ExerciseSessionData(
            uid = uid,
            totalActiveTime = aggregateData[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL],
            totalSteps = aggregateData[StepsRecord.COUNT_TOTAL],
            totalDistance = aggregateData[DistanceRecord.DISTANCE_TOTAL],
            totalEnergyBurned = aggregateData[TotalCaloriesBurnedRecord.ENERGY_TOTAL],
            minHeartRate = aggregateData[HeartRateRecord.BPM_MIN],
            maxHeartRate = aggregateData[HeartRateRecord.BPM_MAX],
            avgHeartRate = aggregateData[HeartRateRecord.BPM_AVG],
            heartRateSeries = heartRateData,
            speedRecord = speedData,
            minSpeed = aggregateData[SpeedRecord.SPEED_MIN],
            maxSpeed = aggregateData[SpeedRecord.SPEED_MAX],
            avgSpeed = aggregateData[SpeedRecord.SPEED_AVG],
        )
    }

    /**
     * Deletes all existing glucose data.
     */
    suspend fun deleteAllGlucoseData() {
        val now = Instant.now()
        healthConnectClient.deleteRecords(BloodGlucoseRecord::class, TimeRangeFilter.before(now))
    }

    /**
     * Generates a week's worth of glucose data using a [BloodGlucoseRecord] to describe the overall
     * period of glucose data. For the purposes of this sample, the glucose data is generated randomly.
     */
    suspend fun generateGlucoseData() {
        val records = mutableListOf<Record>()
        // Make yesterday the last day of the sleep data
        val lastDay = ZonedDateTime.now().minusDays(1).truncatedTo(ChronoUnit.DAYS)
        for (i in 0..7) {
            val breakfastTime = lastDay.minusDays(i.toLong())
                .withHour(Random.nextInt(6, 8))
                .withMinute(Random.nextInt(0, 60))
            val lunchTime = lastDay.minusDays(i.toLong())
                .withHour(Random.nextInt(11, 13))
                .withMinute(Random.nextInt(0, 60))
            val breakfastGlucoseRecord = BloodGlucoseRecord(
                time = breakfastTime.toInstant(),
                zoneOffset = breakfastTime.offset,
                level = BloodGlucose.milligramsPerDeciliter(roundOffDecimal(Random.nextDouble(80.0, 100.0))),
                specimenSource = SpecimenSource.SPECIMEN_SOURCE_WHOLE_BLOOD,
                mealType = MealType.MEAL_TYPE_BREAKFAST,
                relationToMeal = RELATION_TO_MEAL_AFTER_MEAL,
            )
            records.add(breakfastGlucoseRecord)
            lunchTime.zone.id

            val lunchGlucoseRecord = BloodGlucoseRecord(
                time = lunchTime.toInstant(),
                zoneOffset = lunchTime.offset,
                level = BloodGlucose.milligramsPerDeciliter(roundOffDecimal(Random.nextDouble(120.0, 160.0))),
                specimenSource = SPECIMEN_SOURCE_WHOLE_BLOOD,
                mealType = MealType.MEAL_TYPE_LUNCH,
                relationToMeal = RELATION_TO_MEAL_AFTER_MEAL,
            )
            records.add(lunchGlucoseRecord)
        }
        healthConnectClient.insertRecords(records)
    }

    /**
     * Reads glucose measurements for the previous seven days (from yesterday) to show a week's worth of
     * glucose data.
     */
    suspend fun readGlucoseMeasurements(): List<GlucoseData> {
        val lastDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
            .minusDays(1)
            .withHour(12)
        val firstDay = lastDay
            .minusDays(7)

        val request = ReadRecordsRequest(
            recordType = BloodGlucoseRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
        )
        val glucoseDataList = mutableListOf<GlucoseData>()
        val glucoseRecords = healthConnectClient.readRecords(request)
        glucoseRecords.records.forEach { record ->
            glucoseDataList.add(
                GlucoseData(
                    id = record.metadata.id,
                    glucose = record.level,
                    time = record.time,
                    zoneOffset = record.zoneOffset,
                    mealType = record.mealType,
                    specimenSource = record.specimenSource,
                    relationToMeal = record.relationToMeal
                )
            )
        }
        return glucoseDataList
    }

    /**
     * Deletes all existing sleep data.
     */
    suspend fun deleteAllSleepData() {
        val now = Instant.now()
        healthConnectClient.deleteRecords(SleepSessionRecord::class, TimeRangeFilter.before(now))
    }

    /**
     * Generates a week's worth of sleep data using a [SleepSessionRecord] to describe the overall
     * period of sleep, with multiple [SleepSessionRecord.Stage] periods which cover the entire
     * [SleepSessionRecord]. For the purposes of this sample, the sleep stage data is generated randomly.
     */
    suspend fun generateSleepData() {
        val records = mutableListOf<Record>()
        // Make yesterday the last day of the sleep data
        val lastDay = ZonedDateTime.now().minusDays(1).truncatedTo(ChronoUnit.DAYS)
        val notes = context.resources.getStringArray(R.array.sleep_notes_array)
        // Create 7 days-worth of sleep data
        for (i in 0..7) {
            val wakeUp = lastDay.minusDays(i.toLong())
                .withHour(Random.nextInt(7, 10))
                .withMinute(Random.nextInt(0, 60))
            val bedtime = wakeUp.minusDays(1)
                .withHour(Random.nextInt(19, 22))
                .withMinute(Random.nextInt(0, 60))
            val sleepSession = SleepSessionRecord(
                notes = notes[Random.nextInt(0, notes.size)],
                startTime = bedtime.toInstant(),
                startZoneOffset = bedtime.offset,
                endTime = wakeUp.toInstant(),
                endZoneOffset = wakeUp.offset,
                stages = generateSleepStages(bedtime, wakeUp)
            )
            records.add(sleepSession)
        }
        healthConnectClient.insertRecords(records)
    }

    /**
     * Reads sleep sessions for the previous seven days (from yesterday) to show a week's worth of
     * sleep data.
     *
     * In addition to reading [SleepSessionRecord]s, for each session, the duration is calculated to
     * demonstrate aggregation, and the underlying [SleepSessionRecord.Stage] data is also read.
     */
    suspend fun readSleepSessions(): List<SleepSessionData> {
        val lastDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
            .minusDays(1)
            .withHour(12)
        val firstDay = lastDay
            .minusDays(7)

        val sessions = mutableListOf<SleepSessionData>()
        val sleepSessionRequest = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(firstDay.toInstant(), lastDay.toInstant()),
            ascendingOrder = false
        )
        val sleepSessions = healthConnectClient.readRecords(sleepSessionRequest)
        sleepSessions.records.forEach { session ->
            val sessionTimeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
            val durationAggregateRequest = AggregateRequest(
                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                timeRangeFilter = sessionTimeFilter
            )
            val aggregateResponse = healthConnectClient.aggregate(durationAggregateRequest)
            sessions.add(
                SleepSessionData(
                    uid = session.metadata.id,
                    title = session.title,
                    notes = session.notes,
                    startTime = session.startTime,
                    startZoneOffset = session.startZoneOffset,
                    endTime = session.endTime,
                    endZoneOffset = session.endZoneOffset,
                    duration = aggregateResponse[SleepSessionRecord.SLEEP_DURATION_TOTAL],
                    stages = session.stages
                )
            )
        }
        return sessions
    }

    /**
     * Writes [WeightRecord] to Health Connect.
     */
    /*suspend fun writeWeightInput(weight: WeightRecord) {
        val records = listOf(weight)
        healthConnectClient.insertRecords(records)
    }*/

    /**
     * Reads in existing [WeightRecord]s.
     */
    suspend fun readWeightInputs(start: Instant, end: Instant): List<WeightRecord> {
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    /**
     * Returns the weekly average of [WeightRecord]s.
     */
    suspend fun computeWeeklyAverage(start: Instant, end: Instant): Mass? {
        val request = AggregateRequest(
            metrics = setOf(WeightRecord.WEIGHT_AVG),
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.aggregate(request)
        return response[WeightRecord.WEIGHT_AVG]
    }

    /**
     * Deletes a [WeightRecord]s.
     */
    suspend fun deleteWeightInput(uid: String) {
        healthConnectClient.deleteRecords(
            WeightRecord::class,
            recordIdsList = listOf(uid),
            clientRecordIdsList = emptyList()
        )
    }

    /**
     * Creates a random sleep stage that spans the specified [start] to [end] time.
     */
    private fun generateSleepStages(
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<SleepSessionRecord.Stage> {
        val sleepStages = mutableListOf<SleepSessionRecord.Stage>()
        var stageStart = start
        while (stageStart < end) {
            val stageEnd = stageStart.plusMinutes(Random.nextLong(30, 120))
            val checkedEnd = if (stageEnd > end) end else stageEnd
            sleepStages.add(
                SleepSessionRecord.Stage(
                    stage = randomSleepStage(),
                    startTime = stageStart.toInstant(),
                    endTime = checkedEnd.toInstant()
                )
            )
            stageStart = checkedEnd
        }
        return sleepStages
    }

    /**
     * Convenience function to reuse code for reading data.
     */
    private suspend inline fun <reified T : Record> readData(
        timeRangeFilter: TimeRangeFilter,
        dataOriginFilter: Set<DataOrigin> = setOf()
    ): List<T> {
        val request = ReadRecordsRequest(
            recordType = T::class,
            dataOriginFilter = dataOriginFilter,
            timeRangeFilter = timeRangeFilter
        )
        return healthConnectClient.readRecords(request).records
    }

    private fun buildHeartRateSeries(
        sessionStartTime: ZonedDateTime,
        sessionEndTime: ZonedDateTime
    ): HeartRateRecord {
        val samples = mutableListOf<HeartRateRecord.Sample>()
        var time = sessionStartTime
        while (time.isBefore(sessionEndTime)) {
            samples.add(
                HeartRateRecord.Sample(
                    time = time.toInstant(),
                    beatsPerMinute = (80 + Random.nextInt(80)).toLong()
                )
            )
            time = time.plusSeconds(30)
        }
        return HeartRateRecord(
            startTime = sessionStartTime.toInstant(),
            startZoneOffset = sessionStartTime.offset,
            endTime = sessionEndTime.toInstant(),
            endZoneOffset = sessionEndTime.offset,
            samples = samples
        )
    }

    private fun buildSpeedSeries(
        sessionStartTime: ZonedDateTime,
        sessionEndTime: ZonedDateTime
    ) = SpeedRecord(
        startTime = sessionStartTime.toInstant(),
        startZoneOffset = sessionStartTime.offset,
        endTime = sessionEndTime.toInstant(),
        endZoneOffset = sessionEndTime.offset,
        samples = listOf(
            SpeedRecord.Sample(
                time = sessionStartTime.toInstant(),
                speed = Velocity.metersPerSecond(2.5)
            ),
            SpeedRecord.Sample(
                time = sessionStartTime.toInstant().plus(5, ChronoUnit.MINUTES),
                speed = Velocity.metersPerSecond(2.7)
            ),
            SpeedRecord.Sample(
                time = sessionStartTime.toInstant().plus(10, ChronoUnit.MINUTES),
                speed = Velocity.metersPerSecond(2.9)
            )
        )
    )

    // Represents the two types of messages that can be sent in a Changes flow.
    sealed class ChangesMessage {
        data class NoMoreChanges(val nextChangesToken: String) : ChangesMessage()
        data class ChangeList(val changes: List<Change>) : ChangesMessage()
    }
}