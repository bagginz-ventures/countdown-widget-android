package com.bagginzventures.countdownwidget

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bagginzventures.countdownwidget.data.AccentTheme
import com.bagginzventures.countdownwidget.data.CountdownCalculator
import com.bagginzventures.countdownwidget.data.CountdownConfig
import com.bagginzventures.countdownwidget.data.CountdownRepository
import com.bagginzventures.countdownwidget.ui.theme.CountdownWidgetTheme
import com.bagginzventures.countdownwidget.widget.CountdownAppWidgetProvider
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = CountdownRepository(applicationContext)
        setContent {
            CountdownWidgetTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CountdownApp(
                        repository = repository,
                        appContext = applicationContext
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownApp(
    repository: CountdownRepository,
    appContext: Context
) {
    val config by repository.config.collectAsState(initial = CountdownConfig())
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(config.title) }
    var targetDate by remember { mutableStateOf(config.targetDate) }
    var accentTheme by remember { mutableStateOf(config.accentTheme) }

    LaunchedEffect(config) {
        title = config.title
        targetDate = config.targetDate
        accentTheme = config.accentTheme
    }

    CountdownScreen(
        title = title,
        targetDate = targetDate,
        accentTheme = accentTheme,
        onTitleChanged = { title = it },
        onTargetDateChanged = { targetDate = it },
        onAccentThemeChanged = { accentTheme = it },
        onSave = {
            scope.launch {
                repository.save(
                    CountdownConfig(
                        title = title,
                        targetDate = targetDate,
                        accentTheme = accentTheme
                    )
                )
                CountdownAppWidgetProvider.updateAllWidgets(appContext)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CountdownScreen(
    title: String,
    targetDate: LocalDate,
    accentTheme: AccentTheme,
    onTitleChanged: (String) -> Unit,
    onTargetDateChanged: (LocalDate) -> Unit,
    onAccentThemeChanged: (AccentTheme) -> Unit,
    onSave: () -> Unit
) {
    val presentation = remember(title, targetDate, accentTheme) {
        CountdownCalculator.presentation(
            CountdownConfig(
                title = title,
                targetDate = targetDate,
                accentTheme = accentTheme
            )
        )
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onTargetDateChanged(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text("Set date") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C1018))
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentTheme.surfaceTintComposeColor,
                                accentTheme.surfaceComposeColor
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(accentTheme.accentComposeColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Countdown preview",
                            color = Color(0xFFC5D0E0),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Text(
                        text = title.ifBlank { "Untitled countdown" },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = presentation.daysValue,
                        color = Color.White,
                        fontSize = 84.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 84.sp
                    )
                    Text(
                        text = presentation.statusLabel,
                        color = Color(0xFFCAD5E2),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = targetDate.format(dateFormatter),
                        color = Color(0xFF91A4BB),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121927)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Widget details",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(targetDate.format(dateFormatter))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Accent",
                            color = Color(0xFFD7E0EC),
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AccentTheme.entries.forEach { option ->
                                FilterChip(
                                    selected = accentTheme == option,
                                    onClick = { onAccentThemeChanged(option) },
                                    label = { Text(option.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = option.accentComposeColor.copy(alpha = 0.18f),
                                        selectedLabelColor = option.accentComposeColor
                                    )
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & refresh widget")
                    }
                }
            }
            Text(
                text = "Add the widget from your launcher to pin this countdown to your home screen.",
                color = Color(0xFF72839B),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
