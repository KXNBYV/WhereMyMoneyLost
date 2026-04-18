package com.example.wheremymoneylost

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wheremymoneylost.ui.screen.*
import com.example.wheremymoneylost.ui.theme.WhereMyMoneyLostTheme
import com.example.wheremymoneylost.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()

            WhereMyMoneyLostTheme(darkTheme = viewModel.isDarkMode.value) {
                AppNavigation(viewModel)
            }
        }
    }
}

data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem("หน้าหลัก", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("ปฏิทิน", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavItem("เป้าหมาย", Icons.Filled.Flag, Icons.Outlined.Flag),
        NavItem("ประวัติ", Icons.Filled.History, Icons.Outlined.History),
        NavItem("ตั้งค่า", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = { 
                            Text(
                                item.label, 
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> MainScreen(viewModel)
                1 -> CalendarScreen(viewModel)
                2 -> GoalsScreen(viewModel)
                3 -> HistoryScreen(viewModel)
                4 -> SettingsScreen(viewModel)
            }
        }
    }
}
