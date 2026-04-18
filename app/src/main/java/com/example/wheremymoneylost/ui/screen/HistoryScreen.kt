package com.example.wheremymoneylost.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.wheremymoneylost.data.model.Expense
import com.example.wheremymoneylost.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val expenses = viewModel.expenses.sortedByDescending { it.timestamp }
    val categories = viewModel.categories

    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📋 ประวัติรายจ่าย", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text("ทั้งหมด ${expenses.size} รายการ • รวม ฿${expenses.sumOf { it.amount }.toInt()}",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ยังไม่มีรายการ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(expenses, key = { it.id }) { expense ->
                    val catName = categories.find { it.id == expense.categoryId }?.name ?: "อื่นๆ"
                    val catColor = categories.find { it.id == expense.categoryId }?.colorHex ?: "#999999"

                    ExpenseItem(
                        expense = expense,
                        categoryName = catName,
                        categoryColor = catColor,
                        onEdit = { editingExpense = expense },
                        onDelete = { viewModel.deleteExpense(expense.id) }
                    )
                }
            }
        }
    }

    // --- Edit Dialog ---
    editingExpense?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            categories = categories.map { it.id to it.name },
            currentCategoryId = expense.categoryId,
            onDismiss = { editingExpense = null },
            onSave = { newAmount, newCatId ->
                viewModel.editExpense(expense.id, newAmount, newCatId)
                editingExpense = null
            }
        )
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    categoryName: String,
    categoryColor: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val color = try { Color(android.graphics.Color.parseColor(categoryColor)) } catch (e: Exception) { Color.Gray }
    val dateFormat = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale("th"))
    val dateStr = dateFormat.format(Date(expense.timestamp))

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(color))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(categoryName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("฿${expense.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error)
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "แก้ไข", modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "ลบ", modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("ลบรายการนี้?") },
            text = { Text("ลบรายจ่าย ฿${expense.amount.toInt()} หมวด $categoryName") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("ลบ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("ยกเลิก") } }
        )
    }
}

@Composable
fun EditExpenseDialog(
    expense: Expense,
    categories: List<Pair<Int, String>>,
    currentCategoryId: Int,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit
) {
    var amountInput by remember { mutableStateOf(expense.amount.toInt().toString()) }
    var selectedCatId by remember { mutableIntStateOf(currentCategoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ แก้ไขรายจ่าย") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("จำนวนเงิน") },
                    leadingIcon = { Text("฿", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("เลือกหมวดหมู่:", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

                categories.forEach { (catId, catName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCatId = catId }
                            .background(
                                if (selectedCatId == catId) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedCatId == catId, onClick = { selectedCatId = catId })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(catName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountInput.toDoubleOrNull()
                if (amount != null && amount > 0) onSave(amount, selectedCatId)
            }) { Text("บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } }
    )
}
