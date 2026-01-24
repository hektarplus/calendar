package com.calendar.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.BackHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import com.calendar.R
import com.calendar.data.Settings
import com.calendar.ui.viewmodel.CalendarViewModel
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.ChronoField
import java.util.Locale

@Composable
fun formatMonthYear(month: YearMonth, locale: Locale): String {
    val monthValue = month.monthValue
    val year = month.year
    
    if (locale.language == "uk" || locale.language == "kk" || locale.language == "ru" || locale.language == "tr" || locale.language == "es" || locale.language == "be" || locale.language == "fr" || locale.language == "bg" || locale.language == "it") {
        val monthNames = arrayOf(
            "", 
            stringResource(R.string.month_january),
            stringResource(R.string.month_february),
            stringResource(R.string.month_march),
            stringResource(R.string.month_april),
            stringResource(R.string.month_may),
            stringResource(R.string.month_june),
            stringResource(R.string.month_july),
            stringResource(R.string.month_august),
            stringResource(R.string.month_september),
            stringResource(R.string.month_october),
            stringResource(R.string.month_november),
            stringResource(R.string.month_december)
        )
        return "${monthNames[monthValue].replaceFirstChar { it.uppercaseChar() }} $year"
    }
    
    val formatter = DateTimeFormatterBuilder()
        .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL_STANDALONE)
        .appendLiteral(" ")
        .appendPattern("yyyy")
        .toFormatter(locale)
    
    return month.format(formatter)
}

@Composable
fun formatWorkPeriod(month: YearMonth, settings: Settings, locale: Locale): String {
    if (settings.monthStartDay <= 1) return formatMonthYear(month, locale)
    val (start, end) = settings.getWorkPeriodBounds(month)
    val f = DateTimeFormatter.ofPattern("d.MM.yyyy", locale)
    return "${start.format(f)} – ${end.format(f)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onSettingsClick: () -> Unit,
    onExit: () -> Unit
) {
    val calendarData by viewModel.calendarData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState(initial = null)
    val workDays = calendarData.workDays
    val advances = calendarData.advances
    val settings = calendarData.settings
    val month = calendarData.month
    
    var showDayDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var showStatisticsDialog by remember { mutableStateOf(false) }
    var selectedStatisticsMonth by remember { mutableStateOf<YearMonth?>(null) }

    val systemLocale = Locale.getDefault()
    val windowInfo = LocalWindowInfo.current
    val containerSize = windowInfo.containerSize
    val isLandscape = containerSize.width > containerSize.height

    BackHandler(enabled = !showDayDialog) {
        showExitDialog = true
    }
    
    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            showDayDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = formatMonthYear(month, systemLocale),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showMonthPickerDialog = true }
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = { viewModel.changeMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.previous_month))
                        }
                        IconButton(onClick = { viewModel.changeMonth(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.next_month))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val density = LocalDensity.current
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (isLandscape) {
                            Modifier
                                .widthIn(max = (containerSize.height / density.density - 32).dp)
                                .align(Alignment.Center)
                                .verticalScroll(rememberScrollState())
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                    .padding(if (isLandscape) 8.dp else 16.dp)
            ) {

                WeekDaysHeader(isLandscape = isLandscape)
                
                Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))

                CalendarGrid(
                    month = month,
                    workDays = workDays,
                    advances = advances,
                    isLandscape = isLandscape,
                    onDayClick = { date ->
                        viewModel.selectDate(date)
                    }
                )
                
                Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

                StatisticsButton(
                    month = month,
                    settings = settings,
                    systemLocale = systemLocale,
                    isLandscape = isLandscape,
                    onClick = {
                        selectedStatisticsMonth = month
                        showStatisticsDialog = true
                    }
                )
            }
        }
    }

    if (showDayDialog && selectedDate != null) {
        val hours = workDays[selectedDate.toString()] ?: 0f
        val advance = advances[selectedDate.toString()] ?: 0f
        DayHoursDialog(
            date = selectedDate!!,
            initialHours = hours,
            initialAdvance = advance,
            currency = settings.currency,
            onDismiss = {
                showDayDialog = false
                viewModel.clearSelection()
            },
            onSave = { hours, advanceAmount ->
                viewModel.saveWorkDay(selectedDate!!, hours)
                viewModel.saveAdvance(selectedDate!!, advanceAmount)
                showDayDialog = false
                viewModel.clearSelection()
            },
            onDelete = {
                viewModel.saveWorkDay(selectedDate!!, 0f)
                viewModel.saveAdvance(selectedDate!!, 0f)
                showDayDialog = false
                viewModel.clearSelection()
            }
        )
    }

    if (showExitDialog) {
        ExitDialog(
            onDismiss = { showExitDialog = false },
            onConfirm = {
                showExitDialog = false
                onExit()
            }
        )
    }
    
    if (showMonthPickerDialog) {
        MonthPickerDialog(
            currentMonth = month,
            onDismiss = { showMonthPickerDialog = false },
            onMonthSelected = { selectedMonth ->
                viewModel.setMonth(selectedMonth)
                showMonthPickerDialog = false
            }
        )
    }
    
    if (showStatisticsDialog && selectedStatisticsMonth != null) {
        StatisticsDialog(
            month = selectedStatisticsMonth!!,
            workDays = workDays,
            advances = advances,
            settings = settings,
            isLandscape = isLandscape,
            onDismiss = { 
                showStatisticsDialog = false
                selectedStatisticsMonth = null
            }
        )
    }
}

@Composable
fun WeekDaysHeader(isLandscape: Boolean = false) {
    val systemLocale = Locale.getDefault()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val weekDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
        
        weekDays.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, systemLocale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = if (isLandscape) 11.sp else MaterialTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CalendarGrid(
    month: YearMonth,
    workDays: Map<String, Float>,
    advances: Map<String, Float>,
    isLandscape: Boolean = false,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek
    val daysInMonth = month.lengthOfMonth()

    val offset = when (firstDayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        var dayIndex = 0
        while (dayIndex < daysInMonth + offset) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { weekDay ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (dayIndex < offset) {
                            
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        } else if (dayIndex - offset < daysInMonth) {
                            val day = dayIndex - offset + 1
                            val date = month.atDay(day)
                            val hours = workDays[date.toString()] ?: 0f
                            val advance = advances[date.toString()] ?: 0f
                            
                            CalendarDayCell(
                                day = day,
                                date = date,
                                hours = hours,
                                advance = advance,
                                isToday = date == LocalDate.now(),
                                isLandscape = isLandscape,
                                onClick = { onDayClick(date) }
                            )
                        } else {
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        }
                    }
                    dayIndex++
                }
            }
            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun CalendarDayCell(
    day: Int,
    date: LocalDate,
    hours: Float,
    advance: Float,
    isToday: Boolean,
    isLandscape: Boolean = false,
    onClick: () -> Unit
) {
    val isSickDay = hours < 0
    val hasAdvance = advance > 0f
    val backgroundColor = if (isToday) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (isSickDay) {
        Color(0xFFFFEBEE).copy(alpha = 0.5f)
    } else if (hasAdvance) {
        Color(0xFFE8F5E9).copy(alpha = 0.6f)
    } else if (hours > 0) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(if (isLandscape) 1.dp else 2.dp)
            .clip(RoundedCornerShape(if (isLandscape) 6.dp else 8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = if (isLandscape) 12.sp else MaterialTheme.typography.bodyMedium.fontSize,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (isSickDay) {
                Spacer(modifier = Modifier.height(if (isLandscape) 1.dp else 2.dp))
                val sickHours = kotlin.math.abs(hours)
                if (sickHours > 1f) {
                    Text(
                        text = "${sickHours.toInt()}${stringResource(R.string.hours_short)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (isLandscape) 8.sp else 10.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (hours > 0) {
                Spacer(modifier = Modifier.height(if (isLandscape) 1.dp else 2.dp))
                Text(
                    text = "${hours.toInt()}${stringResource(R.string.hours_short)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (isLandscape) 8.sp else 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (hasAdvance) {
                Spacer(modifier = Modifier.height(if (isLandscape) 1.dp else 2.dp))
                Text(
                    text = "💰",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (isLandscape) 10.sp else 12.sp
                )
            }
        }
    }
}

fun getMonthsWithData(
    workDays: Map<String, Float>,
    advances: Map<String, Float>,
    settings: Settings
): Set<YearMonth> {
    val months = mutableSetOf<YearMonth>()
    workDays.keys.forEach { dateStr ->
        try {
            val date = LocalDate.parse(dateStr)
            months.add(settings.dateToPeriodYearMonth(date))
        } catch (_: Exception) {}
    }
    advances.keys.forEach { dateStr ->
        try {
            val date = LocalDate.parse(dateStr)
            months.add(settings.dateToPeriodYearMonth(date))
        } catch (_: Exception) {}
    }
    return months
}

@Composable
fun StatisticsButton(
    month: YearMonth,
    settings: Settings,
    systemLocale: Locale,
    isLandscape: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = stringResource(R.string.statistics_for, formatWorkPeriod(month, settings, systemLocale)),
            modifier = Modifier.padding(if (isLandscape) 12.dp else 16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontSize = if (isLandscape) 14.sp else MaterialTheme.typography.titleMedium.fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatisticsDialog(
    month: YearMonth,
    workDays: Map<String, Float>,
    advances: Map<String, Float>,
    settings: Settings,
    isLandscape: Boolean,
    onDismiss: () -> Unit
) {
    val systemLocale = Locale.getDefault()
    var currentMonth by remember { mutableStateOf(month) }
    
    val monthsWithData = remember(workDays, advances, settings) {
        getMonthsWithData(workDays, advances, settings).sortedBy { it }
    }
    
    LaunchedEffect(month) {
        currentMonth = month
    }
    
    fun goToPreviousMonth() {
        val currentIndex = monthsWithData.indexOf(currentMonth)
        if (currentIndex > 0) {
            currentMonth = monthsWithData[currentIndex - 1]
        }
    }
    
    fun goToNextMonth() {
        val currentIndex = monthsWithData.indexOf(currentMonth)
        if (currentIndex < monthsWithData.size - 1) {
            currentMonth = monthsWithData[currentIndex + 1]
        }
    }
    
    val canGoPrevious = monthsWithData.indexOf(currentMonth) > 0
    val canGoNext = monthsWithData.indexOf(currentMonth) < monthsWithData.size - 1
    
    val (periodStart, periodEnd) = settings.getWorkPeriodBounds(currentMonth)
    val filteredWorkDays = workDays.filter { (dateStr, _) ->
        try {
            val date = LocalDate.parse(dateStr)
            !date.isBefore(periodStart) && !date.isAfter(periodEnd)
        } catch (_: Exception) {
            false
        }
    }
    val filteredAdvances = advances.filter { (dateStr, _) ->
        try {
            val date = LocalDate.parse(dateStr)
            !date.isBefore(periodStart) && !date.isAfter(periodEnd)
        } catch (_: Exception) {
            false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { goToPreviousMonth() },
                    enabled = canGoPrevious
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.previous_month),
                        tint = if (canGoPrevious) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
                Text(
                    text = formatWorkPeriod(currentMonth, settings, systemLocale),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { goToNextMonth() },
                    enabled = canGoNext
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.next_month),
                        tint = if (canGoNext) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) 400.dp else 600.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    MonthStatistics(
                        month = currentMonth,
                        workDays = filteredWorkDays,
                        advances = filteredAdvances,
                        settings = settings,
                        isLandscape = isLandscape
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun MonthStatistics(
    month: YearMonth,
    workDays: Map<String, Float>,
    advances: Map<String, Float>,
    settings: Settings,
    isLandscape: Boolean = false
) {
    val systemLocale = Locale.getDefault()
    val (periodStart, periodEnd) = settings.getWorkPeriodBounds(month)
    val monthWorkDays = workDays.filter { (dateStr, _) ->
        try {
            val date = LocalDate.parse(dateStr)
            !date.isBefore(periodStart) && !date.isAfter(periodEnd)
        } catch (_: Exception) {
            false
        }
    }
    val monthAdvances = advances.filter { (dateStr, _) ->
        try {
            val date = LocalDate.parse(dateStr)
            !date.isBefore(periodStart) && !date.isAfter(periodEnd)
        } catch (_: Exception) {
            false
        }
    }
    
    val regularHours = monthWorkDays.values.filter { it > 0 }.sum()
    val sickDays = monthWorkDays.values.filter { it < 0 }
    val sickDaysCount = sickDays.size
    val sickDaysWithHours = sickDays.filter { it < -1f }
    val sickDaysHours = sickDaysWithHours.sumOf { 
        val absValue = kotlin.math.abs(it)
        if (absValue > 1f) absValue.toDouble() else 0.0
    }.toFloat()
    val totalHours = regularHours
    
    val workEarnings = monthWorkDays.filter { (_, hours) -> hours > 0 }.map { (dateStr, hours) ->
        try {
            val date = LocalDate.parse(dateStr)
            val rateForDate = settings.getHourlyRateForDate(date)
            hours * rateForDate
        } catch (_: Exception) {
            hours * settings.pricePerHour
        }
    }.sum()
    
    val sickDayEarnings = if (sickDaysHours > 0) {
        monthWorkDays.filter { (_, hours) -> hours < 0 && hours < -1f }.map { (_, hours) ->
            val absHours = kotlin.math.abs(hours)
            if (absHours > 1f) {
                absHours * settings.sickDayPrice
            } else {
                0f
            }
        }.sum()
    } else {
        0f
    }
    val totalAdvances = monthAdvances.values.sum()
    val totalEarningsByHours = workEarnings + sickDayEarnings
    val totalEarnings = totalEarningsByHours - totalAdvances
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(if (isLandscape) 8.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.total_hours),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = if (isLandscape) 10.sp else MaterialTheme.typography.bodySmall.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${totalHours.toInt()} ${stringResource(R.string.hours_short)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = if (isLandscape) 16.sp else MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.earnings),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = if (isLandscape) 10.sp else MaterialTheme.typography.bodySmall.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(systemLocale, "%.2f", totalEarningsByHours)} ${settings.currency}",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = if (isLandscape) 16.sp else MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (sickDaysCount > 0) {
                Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.sick_days),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = if (isLandscape) 10.sp else MaterialTheme.typography.bodySmall.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (sickDaysHours > 0) {
                                "$sickDaysCount (${sickDaysHours.toInt()}${stringResource(R.string.hours_short)})"
                            } else {
                                "$sickDaysCount"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = if (isLandscape) 14.sp else MaterialTheme.typography.bodyLarge.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                    if (settings.sickDayPrice > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format(systemLocale, "%.2f", sickDayEarnings)} ${settings.currency}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = if (isLandscape) 14.sp else MaterialTheme.typography.bodyLarge.fontSize,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
            
            if (monthAdvances.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 12.dp))
                Text(
                    text = stringResource(R.string.advances),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = if (isLandscape) 10.sp else MaterialTheme.typography.bodySmall.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))
                
                val sortedAdvances = monthAdvances.toList().sortedBy { (dateStr, _) ->
                    try {
                        LocalDate.parse(dateStr)
                    } catch (_: Exception) {
                        LocalDate.MIN
                    }
                }
                
                val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", systemLocale)
                
                sortedAdvances.forEach { (dateStr, amount) ->
                    val date = try {
                        LocalDate.parse(dateStr)
                    } catch (_: Exception) {
                        null
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isLandscape) 2.dp else 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (date != null) {
                                date.format(dateFormatter)
                            } else {
                                dateStr
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = if (isLandscape) 12.sp else MaterialTheme.typography.bodyMedium.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${String.format(systemLocale, "%.2f", amount)} ${settings.currency}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = if (isLandscape) 12.sp else MaterialTheme.typography.bodyMedium.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (totalAdvances > 0f) {
                        stringResource(R.string.total_with_advances)
                    } else {
                        stringResource(R.string.earnings)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = if (isLandscape) 10.sp else MaterialTheme.typography.bodySmall.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))
                Text(
                    text = "${String.format(systemLocale, "%.2f", totalEarnings)} ${settings.currency}",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = if (isLandscape) 18.sp else MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun formatShortMonth(month: YearMonth, locale: Locale): String {
    val monthValue = month.monthValue
    
    if (locale.language == "uk" || locale.language == "kk" || locale.language == "ru" || locale.language == "tr" || locale.language == "es" || locale.language == "be" || locale.language == "fr" || locale.language == "bg" || locale.language == "it") {
        val monthNames = arrayOf(
            "",
            stringResource(R.string.month_short_january),
            stringResource(R.string.month_short_february),
            stringResource(R.string.month_short_march),
            stringResource(R.string.month_short_april),
            stringResource(R.string.month_short_may),
            stringResource(R.string.month_short_june),
            stringResource(R.string.month_short_july),
            stringResource(R.string.month_short_august),
            stringResource(R.string.month_short_september),
            stringResource(R.string.month_short_october),
            stringResource(R.string.month_short_november),
            stringResource(R.string.month_short_december)
        )
        return monthNames[monthValue]
    }
    
    val formatter = DateTimeFormatterBuilder()
        .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT_STANDALONE)
        .toFormatter(locale)
    
    return month.format(formatter)
}

@Composable
fun MonthPickerDialog(
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit
) {
    val systemLocale = Locale.getDefault()
    var selectedYear by remember { mutableIntStateOf(currentMonth.year) }
    
    LaunchedEffect(currentMonth.year) {
        selectedYear = currentMonth.year
    }
    
    val months = remember(selectedYear) {
        (1..12).map { monthValue ->
            YearMonth.of(selectedYear, monthValue)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedYear-- }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.previous_year)
                    )
                }
                Text(
                    text = "$selectedYear",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { selectedYear++ }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.next_year)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                months.chunked(3).forEach { rowMonths ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                        rowMonths.forEach { month ->
                            val isSelected = month == currentMonth
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable { onMonthSelected(month) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatShortMonth(month, systemLocale),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        repeat(3 - rowMonths.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun DayHoursDialog(
    date: LocalDate,
    initialHours: Float,
    initialAdvance: Float,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Float, Float) -> Unit,
    onDelete: () -> Unit
) {
    val displayHours = if (initialHours < 0 && initialHours != -1f) {
        kotlin.math.abs(initialHours)
    } else if (initialHours > 0) {
        initialHours
    } else {
        0f
    }
    var hoursText by remember { mutableStateOf(if (displayHours > 0) displayHours.toString() else "") }
    var advanceText by remember { mutableStateOf(if (initialAdvance > 0) initialAdvance.toString() else "") }
    var dayType by remember { mutableStateOf("work") }
    val systemLocale = Locale.getDefault()
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", systemLocale)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = date.format(formatter),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = dayType == "work",
                        onClick = { dayType = "work" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.work_day),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { dayType = "work" }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = dayType == "sick",
                        onClick = { dayType = "sick" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sick_day),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { dayType = "sick" }
                    )
                }
                
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            hoursText = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.work_hours)) },
                    placeholder = { Text("0.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                
                OutlinedTextField(
                    value = advanceText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            advanceText = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.advance_amount)) },
                    placeholder = { Text("0.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    trailingIcon = {
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hoursValue = hoursText.toFloatOrNull() ?: 0f
                    val advanceValue = advanceText.toFloatOrNull() ?: 0f
                    val hours = if (dayType == "sick" && hoursValue > 0) {
                        -hoursValue
                    } else if (dayType == "sick" && hoursValue == 0f) {
                        -1f
                    } else if (dayType == "work" && hoursValue > 0) {
                        hoursValue
                    } else {
                        0f
                    }
                    onSave(hours, advanceValue)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (initialHours != 0f || initialAdvance > 0f) {
                    TextButton(
                        onClick = onDelete
                    ) {
                        Text(
                            text = stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
