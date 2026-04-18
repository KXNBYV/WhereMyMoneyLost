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
    val velocity = viewModel.getSpendingVelocity()
    val upcomingBills = viewModel.getUpcomingBills().take(3)
    
    var currentInput by remember { mutableStateOf("") }
    var memoInput by remember { mutableStateOf("") }
    var showAddCategory by remember { mutableStateOf(false) }
    var showDeleteCategory by remember { mutableStateOf<Int?>(null) }

    val animatedVelocity by animateFloatAsState(
        targetValue = velocity.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000), label = "velocity"
    )

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
            Column {
                Text(
                    "เงินหายไปไหน", 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${viewModel.getMonthName(viewModel.selectedMonth.intValue)} ${viewModel.selectedYear.intValue + 543}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

        Spacer(modifier = Modifier.height(24.dp))

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
                SpendingVelocityMeter(animatedVelocity, totalSpent, budget)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Upcoming Bills (Useful Feature) ---
        if (upcomingBills.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "บิลที่ใกล้ถึงกำหนด (To-Do)", 
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
                                            onLongClick = { showDeleteCategory = cat.id }
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
}

@Composable
fun SpendingVelocityMeter(velocity: Float, totalSpent: Double, budget: Double) {
    val gaugeColor by animateColorAsState(
        targetValue = when {
            velocity > 0.7f -> Error
            velocity > 0.5f -> Warning
            else -> Success
        }, label = "gaugeColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp, 160.dp)) {
            Canvas(modifier = Modifier.size(280.dp, 280.dp).offset(y = 60.dp)) {
                val strokeWidth = 18.dp.toPx()
                val radius = (size.width - strokeWidth) / 2f
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = topLeft, size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.horizontalGradient(listOf(Success, gaugeColor)),
                    startAngle = 180f, sweepAngle = velocity * 180f, useCenter = false,
                    topLeft = topLeft, size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 30.dp)) {
                Text(
                    text = when {
                        velocity > 0.7f -> "ใช้เงินเร็วมาก"
                        velocity > 0.5f -> "ใช้เงินเริ่มเร็ว"
                        else -> "ความเร็วปกติ"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = gaugeColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "฿${totalSpent.toInt()}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("งบ ฿${budget.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
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
