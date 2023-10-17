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
package com.example.myhealth.presentation.component

import android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.BloodGlucoseRecord.Companion.SPECIMEN_SOURCE_INT_TO_STRING_MAP
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.units.BloodGlucose
import com.example.myhealth.R
import com.example.myhealth.data.GlucoseData
import com.example.myhealth.data.dateTimeWithOffsetOrDefault
import com.example.myhealth.presentation.theme.HealthConnectTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Creates a row to represent a [GlucoseData]
 */
@Composable
fun DiabetesReadingRow(
    glucoseData: GlucoseData,
    startExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(startExpanded) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clickable {
                expanded = !expanded
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val formatter = DateTimeFormatter.ofPattern("eee, d LLL")
        val startDateTime =
            dateTimeWithOffsetOrDefault(glucoseData.time, glucoseData.zoneOffset)
        Text(
            modifier = Modifier
                .weight(0.4f),
            color = MaterialTheme.colors.primary,
            text = startDateTime.format(formatter)
        )
        IconButton(
            onClick = { expanded = !expanded }
        ) {
            val icon = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown
            Icon(icon, stringResource(R.string.delete_button))
        }
    }
    if (expanded) {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        val recordDateTime = dateTimeWithOffsetOrDefault(glucoseData.time, glucoseData.zoneOffset)
        DiabetesReadingDetailRow(labelId = R.string.reading_time, item = formatter.format(recordDateTime))
        DiabetesReadingDetailRow(
            labelId = R.string.glucose_level,
            item = glucoseData.glucose.toString()
        )
        DiabetesReadingDetailRow(
            labelId = R.string.reading_source,
            item = SPECIMEN_SOURCE_INT_TO_STRING_MAP[glucoseData.specimenSource]?.uppercase()
        )
        DiabetesReadingDetailRow(
            labelId = R.string.reading_meal_type,
            item = MealType.MEAL_TYPE_INT_TO_STRING_MAP[glucoseData.mealType]?.uppercase()
        )
    }
}

@Preview
@Composable
fun DiabetesReadingRowPreview() {
    HealthConnectTheme {
        val end = ZonedDateTime.now()
        val start = end.minusHours(1)
        Column {
            DiabetesReadingRow(
                GlucoseData(
                    id = "123",
                    time = start.toInstant(),
                    zoneOffset = start.offset,
                    relationToMeal = RelationToMealType.RELATION_TO_MEAL_AFTER_MEAL,
                    mealType = MealType.MEAL_TYPE_BREAKFAST,
                    glucose = BloodGlucose.milligramsPerDeciliter(115.00),
                    specimenSource = SpecimenSource.SPECIMEN_SOURCE_WHOLE_BLOOD,
                ),
                startExpanded = true
            )
        }
    }
}
