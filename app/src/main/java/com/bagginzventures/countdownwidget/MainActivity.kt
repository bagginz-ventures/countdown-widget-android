package com.bagginzventures.countdownwidget

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bagginzventures.countdownwidget.data.AccentTheme
import com.bagginzventures.countdownwidget.data.CountdownCalculator
import com.bagginzventures.countdownwidget.data.CountdownConfig
import com.bagginzventures.countdownwidget.data.CountdownRepository
import com.bagginzventures.countdownwidget.data.DEFAULT_TITLE
import com.bagginzventures.countdownwidget.data.PhotoStorage
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
    val photoStorage = remember(appContext) { PhotoStorage(appContext) }

    var title by remember { mutableStateOf(config.title) }
    var targetDate by remember { mutableStateOf(config.targetDate) }
    var accentTheme by remember { mutableStateOf(config.accentTheme) }
    var backgroundPhotoPaths by remember { mutableStateOf(config.backgroundPhotoPaths) }
    var rotationHoursText by remember { mutableStateOf(config.rotationHours.toString()) }
    var photoStatus by remember { mutableStateOf<String?>(null) }

    suspend fun persistConfig(updatedPhotoPaths: List<String> = backgroundPhotoPaths) {
        val rotationHours = rotationHoursText.toIntOrNull()?.coerceIn(1, 168) ?: 24
        repository.save(
            CountdownConfig(
                title = title.ifBlank { DEFAULT_TITLE },
                targetDate = targetDate,
                accentTheme = accentTheme,
                backgroundPhotoPaths = updatedPhotoPaths,
                rotationHours = rotationHours
            )
        )
        CountdownAppWidgetProvider.updateAllWidgets(appContext)
    }

    suspend fun cacheAndApplyPhotos(uris: List<Uri>, sourceLabel: String) {
        val cached = photoStorage.replacePhotos(uris, backgroundPhotoPaths)
        backgroundPhotoPaths = cached
        persistConfig(updatedPhotoPaths = cached)
        photoStatus = if (cached.isEmpty()) {
            "No images were imported from $sourceLabel"
        } else {
            "${cached.size} background photo${if (cached.size == 1) "" else "s"} imported from $sourceLabel"
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                cacheAndApplyPhotos(uris, "Photos")
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                cacheAndApplyPhotos(uris, "Files")
            }
        }
    }

    LaunchedEffect(config) {
        title = config.title
        targetDate = config.targetDate
        accentTheme = config.accentTheme
        backgroundPhotoPaths = config.backgroundPhotoPaths
        rotationHoursText = config.rotationHours.toString()
    }

    CountdownScreen(
        title = title,
        targetDate = targetDate,
        accentTheme = accentTheme,
        backgroundPhotoCount = backgroundPhotoPaths.size,
        rotationHoursText = rotationHoursText,
        photoStatus = photoStatus,
        onTitleChanged = { title = it },
        onTargetDateChanged = { targetDate = it },
        onAccentThemeChanged = { accentTheme = it },
        onRotationHoursChanged = {
            rotationHoursText = it.filter { char -> char.isDigit() }.take(3)
        },
        onPickPhotos = {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onPickFiles = {
            filePickerLauncher.launch(arrayOf("image/*"))
        },
        onClearPhotos = {
            scope.launch {
                photoStorage.clearPhotos(backgroundPhotoPaths)
                backgroundPhotoPaths = emptyList()
                persistConfig(updatedPhotoPaths = emptyList())
                photoStatus = "Background photos cleared"
            }
        },
        onSave = {
            scope.launch {
                persistConfig()
                photoStatus = if (backgroundPhotoPaths.isEmpty()) {
                    "Countdown saved"
                } else {
                    "Countdown saved with ${backgroundPhotoPaths.size} photo${if (backgroundPhotoPaths.size == 1) "" else "s"}"
                }
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
    backgroundPhotoCount: Int,
    rotationHoursText: String,
    photoStatus: String?,
    onTitleChanged: (String) -> Unit,
    onTargetDateChanged: (LocalDate) -> Unit,
    onAccentThemeChanged: (AccentTheme) -> Unit,
    onRotationHoursChanged: (String) -> Unit,
    onPickPhotos: () -> Unit,
    onPickFiles: () -> Unit,
    onClearPhotos: () -> Unit,
    onSave: () -> Unit
) {
    val rotationHours = rotationHoursText.toIntOrNull()?.coerceIn(1, 168) ?: 24
    val presentation = remember(title, targetDate, accentTheme, backgroundPhotoCount, rotationHours) {
        CountdownCalculator.presentation(
            CountdownConfig(
                title = title,
                targetDate = targetDate,
                accentTheme = accentTheme,
                backgroundPhotoPaths = List(backgroundPhotoCount) { "preview" },
                rotationHours = rotationHours
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

    val scrollState = rememberScrollState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C1018))
                .verticalScroll(scrollState)
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
                    if (backgroundPhotoCount > 0) {
                        Text(
                            text = "$backgroundPhotoCount background photo${if (backgroundPhotoCount == 1) "" else "s"} • rotate every $rotationHours hour${if (rotationHours == 1) "" else "s"}",
                            color = Color(0xFFDDE7F4),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
                        Icon(
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
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121927)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Background photos",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Use Photos for normal gallery picks, or Files for Downloads and provider-backed image sources. Both paths stay image-only, and the app caches copies locally for reliable widget rendering.",
                        color = Color(0xFFCAD5E2),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = onPickPhotos) {
                            Icon(Icons.Rounded.Collections, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Choose from Photos")
                        }
                        OutlinedButton(onClick = onPickFiles) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Choose from Files")
                        }
                        if (backgroundPhotoCount > 0) {
                            OutlinedButton(onClick = onClearPhotos) {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Clear")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = rotationHoursText,
                        onValueChange = onRotationHoursChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Rotate every X hours") },
                        supportingText = { Text("Use 1–168 hours. If only one photo is chosen, rotation is ignored.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Text(
                        text = when {
                            photoStatus != null -> photoStatus
                            backgroundPhotoCount > 0 -> "$backgroundPhotoCount photos ready"
                            else -> "No background photos selected"
                        },
                        color = Color(0xFF93A4B8),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & refresh widget")
            }

            Text(
                text = "Add the widget from your launcher to pin this countdown to your home screen.",
                color = Color(0xFF72839B),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
