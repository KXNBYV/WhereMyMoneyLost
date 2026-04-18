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

    // Grouping by date
    val groupedExpenses = expenses.groupBy { 
        SimpleDateFormat("dd MMMM yyyy", Locale("th", "TH")).format(Date(it.timestamp))
    }

    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Text(
            "ประวัติรายจ่าย", 
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("ยังไม่มีรายการ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                groupedExpenses.forEach { (date, items) ->
                    item {
                        Text(
                            date, 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(items, key = { it.id }) { expense ->
                        val cat = categories.find { it.id == expense.categoryId }
                        val catColor = try { Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#999999")) } catch (e: Exception) { Color.Gray }
                        
                        MinimalExpenseItem(
                            expense = expense,
                            categoryName = cat?.name ?: "อื่นๆ",
                            categoryColor = catColor,
                            onEdit = { editingExpense = expense },
                            onDelete = { viewModel.deleteExpense(expense.id) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    editingExpense?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            categories = categories.map { it.id to it.name },
            currentCategoryId = expense.categoryId,
            onDismiss = { editingExpense = null },
            onSave = { newAmount, newCatId, newMemo ->
                viewModel.editExpense(expense.id, newAmount, newCatId, newMemo)
                editingExpense = null
            }
        )
    }
}

@Composable
fun MinimalExpenseItem(
    expense: Expense,
    categoryName: String,
    categoryColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val timeStr = SimpleDateFormat("HH:mm", Locale("th")).format(Date(expense.timestamp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (expense.memo.isNotEmpty()) {
                    Text(expense.memo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("฿${expense.amount.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("ลบรายการ?") },
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
    onSave: (Double, Int, String) -> Unit
) {
    var amountInput by remember { mutableStateOf(expense.amount.toInt().toString()) }
    var memoInput by remember { mutableStateOf(expense.memo) }
    var selectedCatId by remember { mutableIntStateOf(currentCategoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("แก้ไขรายจ่าย") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("จำนวนเงิน") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = memoInput,
                    onValueChange = { memoInput = it },
                    label = { Text("โน้ต") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("หมวดหมู่:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(categories) { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCatId = id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedCatId == id, onClick = { selectedCatId = id })
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountInput.toDoubleOrNull()
                if (amount != null) onSave(amount, selectedCatId, memoInput)
            }) { Text("บันทึก") }
        }
    )
}
