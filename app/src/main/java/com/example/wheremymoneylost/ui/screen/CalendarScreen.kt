package com.example.wheremymoneylost.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wheremymoneylost.data.model.*
import com.example.wheremymoneylost.ui.viewmodel.MainViewModel
import java.util.*

@Composable
fun CalendarScreen(viewModel: MainViewModel) {
    val cal = Calendar.getInstance()
    var currentYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var showAddBill by remember { mutableStateOf(false) }

    val billsForSelectedDate = viewModel.getBillsForDate(currentYear, currentMonth, selectedDay)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "ตารางรายจ่าย To-Do",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (currentMonth == 0) {
                    currentMonth = 11
                    currentYear--
                } else {
                    currentMonth--
                }
            }) {
                Text("<")
            }
            Text(
                "${viewModel.getMonthName(currentMonth)} ${currentYear + 543}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                if (currentMonth == 11) {
                    currentMonth = 0
                    currentYear++
                } else {
                    currentMonth++
                }
            }) {
                Text(">")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of Week Header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("อา", "จ", "อ", "พ", "พฤ", "ศ", "ส").forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid
        val daysInMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val firstDayOfWeek = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(280.dp)
        ) {
            // Empty slots for start of month
            items(firstDayOfWeek) { Box(modifier = Modifier.size(40.dp)) }
            
            // Days of the month
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val isSelected = day == selectedDay
                val billsOnThisDay = viewModel.getBillsForDate(currentYear, currentMonth, day)
                val allPaid = billsOnThisDay.isNotEmpty() && billsOnThisDay.all { it.isPaid }
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                            else Color.Transparent
                        )
                        .clickable { selectedDay = day },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$day",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (billsOnThisDay.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (allPaid) Color.Gray else Color.Red)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Selected Day To-Do List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "รายการวันที่ $selectedDay ${viewModel.getMonthName(currentMonth)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showAddBill = true }) {
                Icon(Icons.Default.Add, "Add Bill", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (billsForSelectedDate.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("ไม่มีรายการบิลในวันนี้", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(billsForSelectedDate) { bill ->
                    BillListItem(bill, onTogglePaid = { viewModel.markBillAsPaid(bill.id) }, onDelete = { viewModel.deleteBill(bill.id) })
                }
            }
        }
    }

    if (showAddBill) {
        AddBillDialog(
            categories = viewModel.categories,
            onDismiss = { showAddBill = false },
            onAdd = { name, amount, categoryId ->
                val dueDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                viewModel.addBill(name, amount, dueDate, categoryId)
                showAddBill = false
            }
        )
    }
}

@Composable
fun BillListItem(bill: Bill, onTogglePaid: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = bill.isPaid,
                onCheckedChange = { if (!bill.isPaid) onTogglePaid() },
                enabled = !bill.isPaid
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bill.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (bill.isPaid) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    color = if (bill.isPaid) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "฿${bill.amount.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddBillDialog(categories: List<Category>, onDismiss: () -> Unit, onAdd: (String, Double, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableIntStateOf(categories.firstOrNull()?.id ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มบิลใหม่") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ชื่อบิล (เช่น ค่าน้ำ, ค่าเน็ต)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("จำนวนเงิน") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("เลือกหมวดหมู่:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.height(120.dp)) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCatId = cat.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedCatId == cat.id, onClick = { selectedCatId = cat.id })
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (name.isNotEmpty() && amt > 0) {
                    onAdd(name, amt, selectedCatId)
                }
            }) {
                Text("ตกลง")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
