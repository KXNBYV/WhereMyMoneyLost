package com.example.wheremymoneylost.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wheremymoneylost.ui.screen.getIconForCategory

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, iconName: String, colorHex: String, budgetLimit: Double) -> Unit
) {
    var newCatName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    var selectedIcon by remember { mutableStateOf("star") }
    var budgetInput by remember { mutableStateOf("") }

    val availableColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#2196F3", "#03A9F4", "#009688", "#4CAF50",
        "#FF9800", "#FF5722", "#795548", "#607D8B"
    )
    val availableIcons = listOf(
        "fastfood", "directions_car", "shopping_cart", "receipt",
        "health", "movie", "coffee", "home",
        "school", "fitness", "pets", "star"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มหมวดหมู่ใหม่") },
        text = {
            Column {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("ชื่อหมวดหมู่") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                    label = { Text("วงเงินจำกัด (0 = ไม่จำกัด)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Text("฿", fontSize = 16.sp) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("เลือกสี:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                listOf(availableColors.take(6), availableColors.drop(6)).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .then(
                                        if (selectedColor == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("เลือกไอคอน:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                listOf(availableIcons.take(6), availableIcons.drop(6)).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { iconName ->
                            val isSelected = selectedIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getIconForCategory(iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newCatName.isNotBlank()) {
                    val limit = budgetInput.toDoubleOrNull() ?: 0.0
                    onAdd(newCatName.trim(), selectedIcon, selectedColor, limit)
                }
            }) {
                Text("เพิ่ม")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
