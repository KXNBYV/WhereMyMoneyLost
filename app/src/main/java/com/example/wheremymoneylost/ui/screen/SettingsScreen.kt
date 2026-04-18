package com.example.wheremymoneylost.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wheremymoneylost.ui.component.AddCategoryDialog
import com.example.wheremymoneylost.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var budgetInput by remember { mutableStateOf(viewModel.monthlyBudget.doubleValue.toInt().toString()) }
    var showAddCategory by remember { mutableStateOf(false) }
    var editingCatBudget by remember { mutableStateOf<Int?>(null) }
    var catBudgetInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("⚙️ ตั้งค่า", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(20.dp))

        // --- งบประมาณ ---
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💰 งบประมาณรายเดือน", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("จำนวนเงิน (บาท)") },
                        singleLine = true,
                        leadingIcon = { Text("฿", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = {
                        val amount = budgetInput.toDoubleOrNull()
                        if (amount != null && amount > 0) viewModel.setBudget(amount)
                    }, shape = RoundedCornerShape(12.dp)) { Text("บันทึก") }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- เปอร์เซ็นต์เตือน ---
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🔔 ตั้งเปอร์เซ็นต์เตือน", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("เตือนเมื่อใช้จ่ายถึง ${viewModel.alertPercent.intValue}% ของงบ",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = viewModel.alertPercent.intValue.toFloat(),
                    onValueChange = { viewModel.setAlertPercent(it.toInt()) },
                    valueRange = 10f..100f,
                    steps = 8 // 10,20,30,...100
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("10%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${viewModel.alertPercent.intValue}%", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("100%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Ongoing Notification ---
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("📊 แสดงสถานะติดจอ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("แสดงยอดใช้จ่ายใน status bar", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = viewModel.ongoingNotificationEnabled.value,
                    onCheckedChange = { viewModel.setOngoingNotification(it) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Dark Mode ---
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌙", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Dark Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Switch(checked = viewModel.isDarkMode.value, onCheckedChange = { viewModel.setDarkMode(it) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- หมวดหมู่ + วงเงิน ---
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("📁 หมวดหมู่ + วงเงิน", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { showAddCategory = true }) {
                        Icon(Icons.Default.Add, "เพิ่ม", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                viewModel.categories.forEach { cat ->
                    val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.Gray }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color),
                            contentAlignment = Alignment.Center) {
                            Icon(getIconForCategory(cat.iconName), null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, fontSize = 14.sp)
                            Text(
                                if (cat.budgetLimit > 0) "วงเงิน ฿${cat.budgetLimit.toInt()}" else "ไม่จำกัดวงเงิน",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // ปุ่มตั้งวงเงิน
                        IconButton(onClick = {
                            editingCatBudget = cat.id
                            catBudgetInput = if (cat.budgetLimit > 0) cat.budgetLimit.toInt().toString() else ""
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, "ตั้งวงเงิน", modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        }
                        if (viewModel.categories.size > 2) {
                            IconButton(onClick = { viewModel.deleteCategory(cat.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "ลบ", modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("เงินหายไปไหน v2.0", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))
    }

    // --- Dialogs ---
    if (showAddCategory) {
        AddCategoryDialog(
            onDismiss = { showAddCategory = false },
            onAdd = { name, icon, color, limit ->
                viewModel.addCategory(name, icon, color, limit)
                showAddCategory = false
            }
        )
    }

    editingCatBudget?.let { catId ->
        AlertDialog(
            onDismissRequest = { editingCatBudget = null },
            title = { Text("ตั้งวงเงินหมวดหมู่") },
            text = {
                OutlinedTextField(
                    value = catBudgetInput,
                    onValueChange = { catBudgetInput = it.filter { c -> c.isDigit() } },
                    label = { Text("วงเงิน (0 = ไม่จำกัด)") },
                    leadingIcon = { Text("฿", fontSize = 16.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val limit = catBudgetInput.toDoubleOrNull() ?: 0.0
                    viewModel.updateCategoryBudget(catId, limit)
                    editingCatBudget = null
                }) { Text("บันทึก") }
            },
            dismissButton = { TextButton(onClick = { editingCatBudget = null }) { Text("ยกเลิก") } }
        )
    }
}
