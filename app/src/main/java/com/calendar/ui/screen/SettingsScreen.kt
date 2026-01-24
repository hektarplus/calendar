package com.calendar.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.calendar.R
import com.calendar.data.Settings
import com.calendar.ui.viewmodel.CalendarViewModel
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: CalendarViewModel,
    onBackClick: () -> Unit
) {
    val settings by viewModel.settings.collectAsState(initial = Settings())

    var priceText by remember(settings.pricePerHour) { 
        mutableStateOf(if (settings.pricePerHour > 0f) settings.pricePerHour.toString() else "") 
    }
    var currency by remember(settings.currency) { 
        mutableStateOf(settings.currency.ifEmpty { "$" }) 
    }
    var sickDayPriceText by remember(settings.sickDayPrice) { 
        mutableStateOf(if (settings.sickDayPrice > 0f) settings.sickDayPrice.toString() else "") 
    }

    var monthStartDayText by remember(settings.monthStartDay) { 
        mutableStateOf(settings.monthStartDay.coerceIn(1, 28).toString()) 
    }

    LaunchedEffect(settings.pricePerHour, settings.currency, settings.sickDayPrice) {
        val expectedPriceText = if (settings.pricePerHour > 0f) settings.pricePerHour.toString() else ""
        if (priceText.isEmpty()) {
            priceText = expectedPriceText
        }

        if (currency != settings.currency && settings.currency.isNotEmpty()) {
            currency = settings.currency
        }

        val expectedSickDayPriceText = if (settings.sickDayPrice > 0f) settings.sickDayPrice.toString() else ""
        if (sickDayPriceText.isEmpty()) {
            sickDayPriceText = expectedSickDayPriceText
        }
    }
    
    val currencies = listOf("$", "€", "£", "¥", "₴", "₸", "zł", "₺", "лв", "Br", "R$", "₽")

    var isPriceExpanded by remember { mutableStateOf(false) }
    var isCurrencyExpanded by remember { mutableStateOf(false) }
    var isRatesHistoryExpanded by remember { mutableStateOf(false) }
    var isMonthStartDayExpanded by remember { mutableStateOf(false) }

    var showAddRateDialog by remember { mutableStateOf(false) }
    var newRateDateText by remember { mutableStateOf("") }
    var newRateValueText by remember { mutableStateOf("") }

    val sortedRatesHistory = remember(settings.hourlyRatesHistory) {
        settings.hourlyRatesHistory.toList().sortedBy { (dateStr, _) ->
            try {
                LocalDate.parse(dateStr).toEpochDay()
            } catch (_: Exception) {
                Long.MIN_VALUE
            }
        }
    }

    val rateOne = stringResource(R.string.rate_one)
    val rateMany = stringResource(R.string.rate_many)
    val notSet = stringResource(R.string.not_set)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPriceExpanded = !isPriceExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.price_per_hour),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Column {
                                Text(
                                    text = if (priceText.isNotEmpty() && priceText.toFloatOrNull() != null) {
                                        "${stringResource(R.string.work_day)}: $priceText $currency/${stringResource(R.string.hours_short)}"
                                    } else {
                                        "${stringResource(R.string.work_day)}: ${stringResource(R.string.not_set)}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (sickDayPriceText.isNotEmpty() && sickDayPriceText.toFloatOrNull() != null) {
                                        "${stringResource(R.string.sick_day)}: $sickDayPriceText $currency/${stringResource(R.string.hours_short)}"
                                    } else {
                                        "${stringResource(R.string.sick_day)}: ${stringResource(R.string.not_set)}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isPriceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = isPriceExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = priceText,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        priceText = newValue
                                    }
                                },
                                label = { Text("${stringResource(R.string.price_per_hour_label)} (${stringResource(R.string.work_day)})") },
                                placeholder = { Text("0.0") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                            OutlinedTextField(
                                value = sickDayPriceText,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        sickDayPriceText = newValue
                                    }
                                },
                                label = { Text("${stringResource(R.string.price_per_hour_label)} (${stringResource(R.string.sick_day)})") },
                                placeholder = { Text("0.0") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRatesHistoryExpanded = !isRatesHistoryExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.rates_history),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (sortedRatesHistory.isNotEmpty()) {
                                    val n = sortedRatesHistory.size
                                    val word = if (n == 1) rateOne else rateMany
                                    "$n $word"
                                } else {
                                    notSet
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (isRatesHistoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = isRatesHistoryExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedButton(
                                onClick = {
                                    newRateDateText = LocalDate.now().toString()
                                    newRateValueText = ""
                                    showAddRateDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.add_rate), style = MaterialTheme.typography.bodyMedium)
                            }

                            if (sortedRatesHistory.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                sortedRatesHistory.forEachIndexed { index, (dateStr, rate) ->
                                    RateHistoryItem(
                                        dateStr = dateStr,
                                        rate = rate,
                                        currency = currency,
                                        isLast = index == sortedRatesHistory.size - 1,
                                        onDelete = {
                                            try {
                                                val date = LocalDate.parse(dateStr)
                                                viewModel.removeHourlyRate(date)
                                            } catch (_: Exception) {

                                            }
                                        }
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_rates_saved),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCurrencyExpanded = !isCurrencyExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (isCurrencyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = isCurrencyExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currencies.forEach { curr ->
                                    FilterChip(
                                        selected = currency == curr,
                                        onClick = { currency = curr },
                                        label = { Text(curr) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMonthStartDayExpanded = !isMonthStartDayExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.month_start_day),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.month_start_day_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${stringResource(R.string.month_start_day_label)}: ${monthStartDayText.ifEmpty { "1" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (isMonthStartDayExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                    AnimatedVisibility(
                        visible = isMonthStartDayExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = monthStartDayText,
                                onValueChange = { newValue ->

                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        monthStartDayText = newValue
                                    }
                                },
                                label = { Text(stringResource(R.string.month_start_day_label)) },
                                placeholder = { Text("1") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val price = priceText.toFloatOrNull() ?: 0f
                    val sickDayPrice = sickDayPriceText.toFloatOrNull() ?: 0f
                    val monthStartDay = monthStartDayText.toIntOrNull()?.coerceIn(1, 28) ?: 1

                    viewModel.saveSettings(Settings(
                        pricePerHour = price,
                        currency = currency,
                        sickDayPrice = sickDayPrice,
                        hourlyRatesHistory = settings.hourlyRatesHistory,
                        monthStartDay = monthStartDay
                    ))
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showAddRateDialog) {
        AddRateDialog(
            initialDateText = newRateDateText,
            initialRateText = newRateValueText,
            currency = currency,
            onDateChange = { newRateDateText = it },
            onRateChange = { newRateValueText = it },
            onDismiss = { showAddRateDialog = false },
            onSave = { dateStr, rate ->
                try {
                    val date = LocalDate.parse(dateStr)
                    viewModel.addHourlyRate(date, rate)
                    showAddRateDialog = false
                } catch (_: Exception) {

                }
            }
        )
    }
}

@Composable
fun RateHistoryItem(
    dateStr: String,
    rate: Float,
    currency: String,
    isLast: Boolean = false,
    onDelete: () -> Unit
) {
    val systemLocale = Locale.getDefault()

    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", systemLocale)
    
    val date = try {
        LocalDate.parse(dateStr)
    } catch (_: Exception) {
        null
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (date != null) {
                        date.format(dateFormatter)
                    } else {
                        dateStr
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$rate $currency/${stringResource(R.string.hours_short)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (!isLast) {
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRateDialog(
    initialDateText: String,
    initialRateText: String,
    currency: String,
    onDateChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, Float) -> Unit
) {

    val initialDate = try {
        if (initialDateText.isNotEmpty()) {
            LocalDate.parse(initialDateText)
        } else {
            LocalDate.now()
        }
    } catch (_: Exception) {
        LocalDate.now()
    }
    
    var selectedDate by remember { mutableStateOf(initialDate) }
    var rateText by remember { mutableStateOf(initialRateText) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    LaunchedEffect(initialDateText, initialRateText) {

        val newDate = try {
            if (initialDateText.isNotEmpty()) {
                LocalDate.parse(initialDateText)
            } else {
                LocalDate.now()
            }
        } catch (_: Exception) {
            LocalDate.now()
        }
        selectedDate = newDate
        rateText = initialRateText
    }
    
    val systemLocale = Locale.getDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", systemLocale)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_rate)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                OutlinedTextField(
                    value = selectedDate.format(dateFormatter),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.rate_effective_date)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(stringResource(R.string.rate_choose))
                        }
                    }
                )

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = selectedDate
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli(),
                        yearRange = (LocalDate.now().year - 10)..(LocalDate.now().year + 10)
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val date = Instant.ofEpochMilli(millis)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate()
                                        selectedDate = date
                                        onDateChange(date.toString())
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text(stringResource(R.string.rate_choose))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            rateText = newValue
                            onRateChange(newValue)
                        }
                    },
                    label = { Text(stringResource(R.string.price_per_hour_label)) },
                    placeholder = { Text("28.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    trailingIcon = {
                        Text(
                            text = "$currency/${stringResource(R.string.hours_short)}",
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
                    val rate = rateText.toFloatOrNull()
                    if (rate != null && rate > 0f) {
                        onSave(selectedDate.toString(), rate)
                    }
                },
                enabled = rateText.toFloatOrNull() != null && rateText.toFloatOrNull()!! > 0f
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

