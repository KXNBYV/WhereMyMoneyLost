package com.example.wheremymoneylost.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wheremymoneylost.data.model.Category
import com.example.wheremymoneylost.ui.component.AddCategoryDialog
import com.example.wheremymoneylost.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var budgetInput by remember { mutableStateOf(viewModel.monthlyBudget.doubleValue.toInt().toString()) }
    var showAddCategory by remember { mutableStateOf(false) }
    var showRecurringDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            "ตั้งค่า", 
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Budget Section ---
        SettingsCard(title = "งบประมาณรายเดือน") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    label = { Text("฿ จำนวนบาท") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = {
                    val amount = budgetInput.toDoubleOrNull()
                    if (amount != null) viewModel.setBudget(amount)
                }) { Text("ตกลง") }
            }
        }

        // --- Alerts Section ---
        SettingsCard(title = "การแจ้งเตือน") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("แสดงสถานะค้างหน้าจอ", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = viewModel.ongoingNotificationEnabled.value, onCheckedChange = { viewModel.setOngoingNotification(it) })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("เตือนเมื่อถึง ${viewModel.alertPercent.intValue}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = viewModel.alertPercent.intValue.toFloat(),
                onValueChange = { viewModel.setAlertPercent(it.toInt()) },
                valueRange = 10f..100f
            )
        }

        // --- Recurring Section ---
        SettingsCard(title = "รายจ่ายประจำ (อัตโนมัติ)") {
            viewModel.recurringExpenses.forEach { rec ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${rec.name} (วันที่ ${rec.dayOfMonth})", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("฿${rec.amount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteRecurringExpense(rec.id) }) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            TextButton(onClick = { showRecurringDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("เพิ่มรายจ่ายประจำ")
                }
            }
        }

        // --- Export & Misc ---
        SettingsCard(title = "จัดการข้อมูล") {
            Button(
                onClick = {
                    val text = viewModel.formatExportText()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "แชร์สรุป"))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ส่งออกสรุปของเดือนนี้")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("โหมดกลางคืน (Dark Mode)", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = viewModel.isDarkMode.value, onCheckedChange = { viewModel.setDarkMode(it) })
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showRecurringDialog) {
        AddRecurringDialog(
            categories = viewModel.categories,
            onDismiss = { showRecurringDialog = false },
            onAdd = { name, amount, catId, day ->
                viewModel.addRecurringExpense(name, amount, catId, day)
                showRecurringDialog = false
            }
        )
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun AddRecurringDialog(
    categories: List<com.example.wheremymoneylost.data.model.Category>,
    onDismiss: () -> Unit,
    onAdd: (String, Double, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("1") }
    var selectedCatId by remember { mutableIntStateOf(categories.firstOrNull()?.id ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มรายจ่ายประจำ") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ชื่อรายการ (เช่น Netflix)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("ยอดเงิน") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("จ่ายทุกวันที่ (1-31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                Text("เลือกหมวดหมู่:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(categories) { cat ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedCatId = cat.id }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedCatId == cat.id, onClick = { selectedCatId = cat.id })
                            Text(cat.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull()
                val d = day.toIntOrNull() ?: 1
                if (name.isNotEmpty() && amt != null) onAdd(name, amt, selectedCatId, d)
            }) { Text("เพิ่ม") }
        }
    )
}
