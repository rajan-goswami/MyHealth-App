package com.example.myhealth.data

import android.health.connect.datatypes.MealType
import androidx.health.connect.client.units.BloodGlucose
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/*
 * Represents a glucose record and associated data.
 */
data class GlucoseData (
    val glucose: BloodGlucose,
    val id: String,
    val time: Instant,
    val zoneOffset: ZoneOffset?,
    val specimenSource: Int,
    val mealType: Int,
    val relationToMeal: Int,
//    val sourceAppInfo: HealthConnectAppInfo?
)