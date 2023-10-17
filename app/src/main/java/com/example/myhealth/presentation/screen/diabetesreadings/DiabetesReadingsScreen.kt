package com.example.myhealth.presentation.screen.diabetesreadings

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.BloodGlucoseRecord.Companion.RELATION_TO_MEAL_AFTER_MEAL
import androidx.health.connect.client.records.BloodGlucoseRecord.Companion.SPECIMEN_SOURCE_WHOLE_BLOOD
import androidx.health.connect.client.records.MealType.MEAL_TYPE_BREAKFAST
import androidx.health.connect.client.units.BloodGlucose
import com.example.myhealth.R
import com.example.myhealth.data.GlucoseData
import com.example.myhealth.data.HealthConnectAppInfo
import com.example.myhealth.presentation.component.DiabetesReadingRow
import com.example.myhealth.presentation.theme.HealthConnectTheme
import java.time.ZonedDateTime
import java.util.UUID

@Composable
fun DiabetesReadingsScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    readingsList: List<GlucoseData>,
    uiState: DiabetesReadingsViewModel.UiState,
    onInsertClick: () -> Unit = {},
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {},
    permissionsLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>?
) {
    // Remember the last error ID, such that it is possible to avoid re-launching the error
    // notification for the same error when the screen is recomposed, or configuration changes etc.
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    val showPermissions = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        // If the initial data load has not taken place, attempt to load the data.
        if (uiState is DiabetesReadingsViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }

        if (!permissionsGranted && uiState is DiabetesReadingsViewModel.UiState.Uninitialized) {
            permissionsLauncher?.launch(permissions)
        }

        // The [DiabetesReadingsViewModel.UiState] provides details of whether the last action was a
        // success or resulted in an error. Where an error occurred, for example in reading and
        // writing to Health Connect, the user is notified, and where the error is one that can be
        // recovered from, an attempt to do so is made.
        if (uiState is DiabetesReadingsViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    if (uiState != DiabetesReadingsViewModel.UiState.Uninitialized) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!permissionsGranted) {
                item {
                    /*Button(
                        onClick = { onPermissionsLaunch(permissions) }
                    ) {
                        Text(text = stringResource(R.string.permissions_button_label))
                    }*/
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(id = R.string.grant_diabetes_permission),
                        textAlign = TextAlign.Justify
                    )
                }
            } else {
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(4.dp),
                        onClick = {
                            onInsertClick()
                        }
                    ) {
                        Text(stringResource(id = R.string.generate_diabetes_data))
                    }
                }

                items(readingsList) { reading ->
                     DiabetesReadingRow(reading)
                }
            }
        }
    }
}

@Preview
@Composable
fun DiabetesReadingsScreenPreview() {
    val context = LocalContext.current
    val now = ZonedDateTime.now()
    val yesterday = now.minusDays(1)
    val appInfo = HealthConnectAppInfo(
        packageName = "com.example.myfitnessapp",
        appLabel = "My Fitness App",
        icon = context.getDrawable(R.drawable.ic_launcher_foreground)!!
    )
    HealthConnectTheme(darkTheme = false) {
        DiabetesReadingsScreen(
            permissions = setOf(),
            permissionsGranted = true,
            readingsList = listOf(
                GlucoseData(
                    glucose = BloodGlucose.milligramsPerDeciliter(100.00),
                    id = UUID.randomUUID().toString(),
                    time = now.toInstant(),
                    zoneOffset = now.offset,
                    specimenSource = SPECIMEN_SOURCE_WHOLE_BLOOD,
                    mealType = MEAL_TYPE_BREAKFAST,
                    relationToMeal = RELATION_TO_MEAL_AFTER_MEAL,
                    // sourceAppInfo = appInfo
                ),
                GlucoseData(
                    glucose = BloodGlucose.milligramsPerDeciliter(110.00),
                    id = UUID.randomUUID().toString(),
                    time = yesterday.toInstant(),
                    zoneOffset = yesterday.offset,
                    specimenSource = SPECIMEN_SOURCE_WHOLE_BLOOD,
                    mealType = MEAL_TYPE_BREAKFAST,
                    relationToMeal = RELATION_TO_MEAL_AFTER_MEAL,
                    // sourceAppInfo = appInfo
                ),
            ),
            uiState = DiabetesReadingsViewModel.UiState.Done,
            permissionsLauncher = null,
        )
    }
}