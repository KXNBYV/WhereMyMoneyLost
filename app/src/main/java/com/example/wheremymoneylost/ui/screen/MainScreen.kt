package com.example.wheremymoneylost.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wheremymoneylost.ui.component.AddCategoryDialog
import com.example.wheremymoneylost.ui.component.SuggestionCard
import com.example.wheremymoneylost.ui.theme.Success
import com.example.wheremymoneylost.ui.theme.Warning
import com.example.wheremymoneylost.ui.theme.Error
import com.example.wheremymoneylost.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedExpenses = viewModel.getExpensesForSelectedMonth()
    val categories = viewModel.categories
    val budget = viewModel.monthlyBudget.doubleValue
    val totalSpent = selectedExpenses.sumOf { it.amount }
    val upcomingBills = viewModel.getUpcomingBills().take(3)
    
    var currentInput by remember { mutableStateOf("") }
    var memoInput by remember { mutableStateOf("") }
    var showAddCategory by remember { mutableStateOf(false) }
    var categoryToManage by remember { mutableStateOf<com.example.wheremymoneylost.data.model.Category?>(null) }
    var categoryToEdit by remember { mutableStateOf<com.example.wheremymoneylost.data.model.Category?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<com.example.wheremymoneylost.data.model.Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Professional Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "WHEREMYMONEYLOST",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (viewModel.streak.intValue > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        "🔥 ${viewModel.streak.intValue} วัน",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Month Navigation ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "เดือนก่อน")
            }
            Text(
                "${viewModel.getMonthName(viewModel.selectedMonth.intValue)} ${viewModel.selectedYear.intValue + 543}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { viewModel.navigateMonth(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "เดือนถัดไป")
            }
            if (!viewModel.isCurrentMonth()) {
                TextButton(onClick = { viewModel.goToCurrentMonth() }) {
                    Text("วันนี้", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Spending Analysis Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("สถานะการเงินเดือนนี้", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                SpendingDonutChart(
                    categories = categories.toList(),
                    expenses = selectedExpenses,
                    totalSpent = totalSpent,
                    budget = budget
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Upcoming Bills (Useful Feature) ---
        if (upcomingBills.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "บิลที่ใกล้ถึงกำหนด",
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                upcomingBills.forEach { bill ->
                    val dateStr = SimpleDateFormat("d MMM", Locale("th", "TH")).format(Date(bill.dueDate))
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { /* Maybe navigate to calendar */ },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bill.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("ครบกำหนด $dateStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text("฿${bill.amount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (viewModel.isFutureMonth()) {
            SuggestionCard(suggestions = viewModel.getSuggestions(), onApply = { viewModel.applySuggestions() })
        }

        // --- Categories Summary ---
        if (selectedExpenses.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text("สรุปรายหมวดหมู่", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                categories.filter { cat -> selectedExpenses.any { it.categoryId == cat.id } }.forEach { cat ->
                    val spent = selectedExpenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
                    val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.Gray }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                            Text("฿${spent.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (cat.budgetLimit > 0) {
                            val pct = (spent / cat.budgetLimit).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = if (pct >= 1f) Error else color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Quick Input Section ---
        if (viewModel.isCurrentMonth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("บันทึกรายจ่าย", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (currentInput.isEmpty()) "฿ 0" else "฿ $currentInput",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                TextField(
                    value = memoInput,
                    onValueChange = { memoInput = it },
                    placeholder = { Text("โน้ต...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Ghost Numpad (Integrated)
                val keys = listOf("1","2","3","4","5","6","7","8","9",".","0","⌫")
                keys.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { key ->
                            TextButton(
                                onClick = {
                                    when (key) {
                                        "⌫" -> if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
                                        "." -> if (!currentInput.contains(".")) currentInput += "."
                                        else -> if (currentInput.length < 10) currentInput += key
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(key, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categories (Professional Grid - 3 Columns)
                val chunkedCats = (categories.toList() + null).chunked(3)
                chunkedCats.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { cat ->
                            if (cat != null) {
                                val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.Gray }
                                val isSuggested = memoInput.isNotEmpty() && viewModel.suggestCategoryFromMemo(memoInput) == cat.id
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                val amount = currentInput.toDoubleOrNull()
                                                if (amount != null && amount > 0) {
                                                    viewModel.addExpense(amount, cat.id, memoInput)
                                                    currentInput = ""
                                                    memoInput = ""
                                                }
                                            },
                                            onLongClick = { categoryToManage = cat }
                                        )
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .border(if (isSuggested) 2.dp else 1.dp, if (isSuggested) color else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                            .background(if (isSuggested) color.copy(alpha = 0.1f) else Color.Transparent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            getIconForCategory(cat.iconName), 
                                            null,
                                            tint = color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Text(cat.name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAddCategory = true }.padding(4.dp)) {
                                    Box(modifier = Modifier.size(54.dp).border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Add, null, tint = Color.Gray)
                                    }
                                    Text("เพิ่ม", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showAddCategory) {
        AddCategoryDialog(onDismiss = { showAddCategory = false }, onAdd = { n, i, c, l ->
            viewModel.addCategory(n, i, c, l)
            showAddCategory = false
        })
    }

    if (categoryToEdit != null) {
        AddCategoryDialog(
            categoryToEdit = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onAdd = { n, i, c, l ->
                viewModel.updateCategory(categoryToEdit!!.id, n, i, c, l)
                categoryToEdit = null
            }
        )
    }

    if (categoryToManage != null) {
        AlertDialog(
            onDismissRequest = { categoryToManage = null },
            title = { Text("จัดการหมวดหมู่: ${categoryToManage!!.name}") },
            text = { Text("เลือกสิ่งที่คุณต้องการทำกับหมวดหมู่นี้") },
            confirmButton = {
                TextButton(onClick = {
                    categoryToEdit = categoryToManage
                    categoryToManage = null
                }) {
                    Text("แก้ไข")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = categoryToManage
                        categoryToManage = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ลบ")
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("ยืนยันการลบ") },
            text = { Text("คุณต้องการลบหมวดหมู่ \"${showDeleteConfirm!!.name}\" ใช่หรือไม่? การกระทำนี้ไม่สามารถย้อนกลับได้") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(showDeleteConfirm!!.id)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ยืนยันการลบ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
fun SpendingDonutChart(
    categories: List<com.example.wheremymoneylost.data.model.Category>,
    expenses: List<com.example.wheremymoneylost.data.model.Expense>,
    totalSpent: Double,
    budget: Double
) {
    // Build slices: category name, amount, color
    data class Slice(val name: String, val amount: Double, val color: Color)

    val slices = remember(categories, expenses) {
        categories.mapNotNull { cat ->
            val spent = expenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
            if (spent > 0) {
                val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { Color.Gray }
                Slice(cat.name, spent, color)
            } else null
        }
    }

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000), label = "donut"
    )

    val spentPercent = if (budget > 0) (totalSpent / budget * 100).toInt() else 0
    val statusColor by animateColorAsState(
        targetValue = when {
            spentPercent >= 100 -> Error
            spentPercent >= 80 -> Warning
            else -> Success
        }, label = "statusColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val strokeWidth = 28.dp.toPx()
                val diameter = size.width - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)

                if (slices.isEmpty()) {
                    // Empty state ring
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.25f),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                } else {
                    // Background track
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.1f),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    // Category slices
                    var startAngle = -90f
                    val gapAngle = if (slices.size > 1) 3f else 0f
                    val totalGap = gapAngle * slices.size
                    val availableSweep = (360f - totalGap) * animationProgress

                    slices.forEach { slice ->
                        val sweep = (slice.amount / totalSpent).toFloat() * availableSweep
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft, size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep + gapAngle
                    }
                }
            }

            // Center info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "฿${totalSpent.toInt()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                if (budget > 0) {
                    Text(
                        "${spentPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        "งบ ฿${budget.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        if (slices.isEmpty()) "ยังไม่มีรายจ่าย" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // Legend
        if (slices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            val rows = slices.chunked(2)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { slice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(slice.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${slice.name} ฿${slice.amount.toInt()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.glassBackground() = this.then(
    Modifier
        .shadow(2.dp, RoundedCornerShape(24.dp))
        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
)

fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        "fastfood" -> Icons.Default.Fastfood
        "directions_car" -> Icons.Default.DirectionsCar
        "shopping_cart" -> Icons.Default.ShoppingCart
        "receipt" -> Icons.Default.Receipt
        "health" -> Icons.Default.MedicalServices
        "movie" -> Icons.Default.Movie
        else -> Icons.Default.Category
    }
}
