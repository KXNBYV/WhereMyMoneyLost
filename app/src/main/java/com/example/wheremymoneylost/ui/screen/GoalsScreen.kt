package com.example.wheremymoneylost.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wheremymoneylost.data.model.SavingGoal
import com.example.wheremymoneylost.ui.viewmodel.MainViewModel

@Composable
fun GoalsScreen(viewModel: MainViewModel) {
    val goals = viewModel.savingGoals
    var showAddGoal by remember { mutableStateOf(false) }
    var goalToContribute by remember { mutableStateOf<SavingGoal?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "เป้าหมายการออม",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { showAddGoal = true }) {
                Icon(Icons.Default.Add, "Add Goal", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Savings, "Savings", modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ยังไม่มีเป้าหมายการออม", color = Color.LightGray)
                    TextButton(onClick = { showAddGoal = true }) {
                        Text("เริ่มตั้งเป้าหมายแรก")
                    }
                }
            }
        } else {
            LazyColumn {
                items(goals) { goal ->
                    GoalCard(
                        goal = goal,
                        onContribute = { goalToContribute = goal },
                        onDelete = { viewModel.deleteSavingGoal(goal.id) }
                    )
                }
            }
        }
    }

    if (showAddGoal) {
        AddGoalDialog(
            onDismiss = { showAddGoal = false },
            onAdd = { name, target ->
                viewModel.addSavingGoal(name, target)
                showAddGoal = false
            }
        )
    }

    if (goalToContribute != null) {
        ContributeDialog(
            goalName = goalToContribute?.name ?: "",
            onDismiss = { goalToContribute = null },
            onConfirm = { amount ->
                goalToContribute?.let { viewModel.contributeToGoal(it.id, amount) }
                goalToContribute = null
            }
        )
    }
}

@Composable
fun GoalCard(goal: SavingGoal, onContribute: () -> Unit, onDelete: () -> Unit) {
    val progress = (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    goal.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "฿${goal.currentAmount.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "เป้าหมาย ฿${goal.targetAmount.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onContribute,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = progress < 1f
            ) {
                Text(if (progress >= 1f) "สำเร็จแล้ว! 🎉" else "หยอดกระปุก")
            }
        }
    }
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onAdd: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ตั้งเป้าหมายการออม") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("เป้าหมายคืออะไร? (เช่น ซื้อไอโฟน)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("จำนวนเงินที่ต้องการ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val t = target.toDoubleOrNull() ?: 0.0
                if (name.isNotEmpty() && t > 0) {
                    onAdd(name, t)
                }
            }) {
                Text("สร้างเป้าหมาย")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}

@Composable
fun ContributeDialog(goalName: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ออมเงินสำหรับ $goalName") },
        text = {
            Column {
                Text("ใส่จำนวนเงินที่คุณต้องการออมเพิ่มในวันนี้", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("จำนวนเงิน (฿)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                if (a > 0) {
                    onConfirm(a)
                }
            }) {
                Text("ยืนยันการออม")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
