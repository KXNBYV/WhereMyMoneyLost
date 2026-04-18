package com.example.wheremymoneylost.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wheremymoneylost.ui.component.AddCategoryDialog
import com.example.wheremymoneylost.ui.component.SuggestionCard
import com.example.wheremymoneylost.ui.viewmodel.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedExpenses = viewModel.getExpensesForSelectedMonth()
    val categories = viewModel.categories
    val budget = viewModel.monthlyBudget.doubleValue

    val totalSpent = selectedExpenses.sumOf { it.amount }
    val budgetUsedPercent = if (budget > 0) (totalSpent / budget).toFloat().coerceIn(0f, 1.5f) else 0f
    val isWarning = budgetUsedPercent >= 0.8f
    val isOver = budgetUsedPercent >= 1.0f

    var currentInput by remember { mutableStateOf("") }
    var showAddCategory by remember { mutableStateOf(false) }
    var showDeleteCategory by remember { mutableStateOf<Int?>(null) }

    val animatedProgress by animateFloatAsState(
        targetValue = budgetUsedPercent.coerceAtMost(1f),
        animationSpec = tween(durationMillis = 800), label = "donut"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("เงินหายไปไหน 💸", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))

        // --- เลือกเดือน ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                    Icon(Icons.Default.ChevronLeft, "เดือนก่อน")
                }
                TextButton(onClick = { viewModel.goToCurrentMonth() }) {
                    Text(
                        "${viewModel.getMonthName(viewModel.selectedMonth.intValue)} ${viewModel.selectedYear.intValue + 543}",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
                IconButton(onClick = { viewModel.navigateMonth(1) }) {
                    Icon(Icons.Default.ChevronRight, "เดือนถัดไป")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Donut Chart ---
        if (!viewModel.isFutureMonth()) {
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOver && viewModel.isCurrentMonth()) Color(0xFFFFEBEE)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val warningColor = Color(0xFFFF5722)
                        val overColor = Color(0xFFD32F2F)
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant

                        Canvas(modifier = Modifier.size(150.dp)) {
                            val strokeWidth = 18.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2f
                            val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
                            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                                topLeft = topLeft, size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                            val chartColor = when { isOver -> overColor; isWarning -> warningColor; else -> primaryColor }
                            drawArc(color = chartColor, startAngle = -90f, sweepAngle = animatedProgress * 360f,
                                useCenter = false, topLeft = topLeft, size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("฿${totalSpent.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = when { isOver -> Color(0xFFD32F2F); isWarning -> Color(0xFFFF5722)
                                    else -> MaterialTheme.colorScheme.onSurface })
                            Text("จาก ฿${budget.toInt()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val emoji = when { isOver -> "🚨"; isWarning -> "⚠️"; budgetUsedPercent < 0.3f -> "✅"; else -> "💰" }
                    Text("$emoji ใช้ไป ${(budgetUsedPercent * 100).toInt()}% ของงบ", fontWeight = FontWeight.SemiBold,
                        color = when { isOver -> Color(0xFFD32F2F); isWarning -> Color(0xFFFF5722)
                            else -> MaterialTheme.colorScheme.primary })

                    // เปรียบเทียบเดือนก่อน
                    if (viewModel.isCurrentMonth()) {
                        val lastTotal = viewModel.lastMonthExpenses().sumOf { it.amount }
                        if (lastTotal > 0) {
                            val diff = totalSpent - lastTotal
                            val diffPct = ((diff / lastTotal) * 100).toInt()
                            Text("เทียบเดือนก่อน: ${if (diff > 0) "📈 +$diffPct%" else "📉 $diffPct%"}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // --- Suggestion สำหรับเดือนอนาคต ---
        if (viewModel.isFutureMonth()) {
            val suggestions = viewModel.getSuggestions()
            SuggestionCard(suggestions = suggestions, onApply = { viewModel.applySuggestions() })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- สรุปหมวดหมู่ + แถบวงเงิน ---
        if (selectedExpenses.isNotEmpty() && categories.isNotEmpty()) {
            Text("สรุปรายจ่าย", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val categoryTotals = categories.associateWith { cat ->
                selectedExpenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
            }.filterValues { it > 0 }

            categoryTotals.forEach { (cat, amount) ->
                val percent = if (totalSpent > 0) (amount / totalSpent * 100).toInt() else 0
                val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.Gray }
                val catBudgetPercent = if (cat.budgetLimit > 0) (amount / cat.budgetLimit).toFloat().coerceAtMost(1.5f) else 0f

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cat.name, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text("฿${amount.toInt()} ($percent%)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (cat.budgetLimit > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val barColor = if (catBudgetPercent > 1f) Color(0xFFD32F2F) else color
                        LinearProgressIndicator(
                            progress = { catBudgetPercent.coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = barColor, trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text("วงเงิน ฿${cat.budgetLimit.toInt()}", fontSize = 11.sp,
                            color = if (catBudgetPercent > 1f) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else if (!viewModel.isFutureMonth()) {
            Text("ยังไม่มีการใช้จ่าย", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // --- Numpad (เฉพาะเดือนปัจจุบัน) ---
        if (viewModel.isCurrentMonth()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = if (currentInput.isEmpty()) "฿ 0" else "฿ $currentInput",
                fontSize = 42.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            val keyRows = listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf(".","0","⌫"))
            keyRows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        Button(onClick = {
                            when (key) {
                                "⌫" -> if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
                                "." -> if (!currentInput.contains(".")) currentInput += "."
                                else -> if (currentInput.length < 10) currentInput += key
                            }
                        }, modifier = Modifier.weight(1f).padding(3.dp).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface)
                        ) { Text(key, fontSize = 20.sp, fontWeight = FontWeight.Medium) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("เลือกหมวดหมู่เพื่อบันทึก", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // --- หมวดหมู่ + ปุ่มเพิ่ม ---
            val allItems = categories.toList()
            val rows = allItems.chunked(3)
            rows.forEach { rowCats ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    rowCats.forEach { cat ->
                        val icon = getIconForCategory(cat.iconName)
                        val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.Gray }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        val amount = currentInput.toDoubleOrNull()
                                        if (amount != null && amount > 0) {
                                            viewModel.addExpense(amount, cat.id)
                                            currentInput = ""
                                        }
                                    },
                                    onLongClick = { showDeleteCategory = cat.id }
                                )
                                .padding(8.dp)
                        ) {
                            Box(modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f)))),
                                contentAlignment = Alignment.Center
                            ) { Icon(icon, contentDescription = cat.name, tint = Color.White, modifier = Modifier.size(26.dp)) }
                            Text(cat.name, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    // เพิ่มปุ่ม "+" ในแถวสุดท้าย
                    if (rowCats == rows.lastOrNull() && rowCats.size < 3) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { showAddCategory = true }.padding(8.dp)
                        ) {
                            Box(modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Add, "เพิ่ม", modifier = Modifier.size(26.dp)) }
                            Text("เพิ่ม", fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            // ถ้าจำนวนหมวดหมู่หาร 3 ลงตัว ให้เพิ่มแถวใหม่สำหรับปุ่ม "+"
            if (allItems.size % 3 == 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showAddCategory = true }.padding(8.dp)
                    ) {
                        Box(modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Add, "เพิ่ม", modifier = Modifier.size(26.dp)) }
                        Text("เพิ่ม", fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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

    showDeleteCategory?.let { catId ->
        val cat = categories.find { it.id == catId }
        if (cat != null && categories.size > 2) {
            AlertDialog(
                onDismissRequest = { showDeleteCategory = null },
                title = { Text("ลบหมวด \"${cat.name}\"?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteCategory(catId); showDeleteCategory = null }) {
                        Text("ลบ", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteCategory = null }) { Text("ยกเลิก") } }
            )
        } else {
            showDeleteCategory = null
        }
    }
}

fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        "fastfood" -> Icons.Default.Fastfood
        "directions_car" -> Icons.Default.DirectionsCar
        "shopping_cart" -> Icons.Default.ShoppingCart
        "receipt" -> Icons.Default.Receipt
        "health" -> Icons.Default.FavoriteBorder
        "movie" -> Icons.Default.Movie
        "coffee" -> Icons.Default.Coffee
        "home" -> Icons.Default.Home
        "school" -> Icons.Default.School
        "fitness" -> Icons.Default.FitnessCenter
        "pets" -> Icons.Default.Pets
        else -> Icons.Default.Star
    }
}
