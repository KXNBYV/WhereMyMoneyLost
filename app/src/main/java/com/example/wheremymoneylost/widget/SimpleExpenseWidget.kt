package com.example.wheremymoneylost.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.example.wheremymoneylost.R
import com.example.wheremymoneylost.data.local.DataStore
import java.util.Calendar

class SimpleExpenseWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        try {
            val dataStore = DataStore(context)
            val budget = dataStore.loadBudget()
            val expenses = dataStore.loadExpenses()
            
            // Calculate today's spent
            val cal = Calendar.getInstance()
            val today = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val todaySpent = expenses.filter { it.timestamp >= today }.sumOf { it.amount }
            
            // Calculate monthly spent
            cal.timeInMillis = System.currentTimeMillis()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)
            
            val monthSpent = expenses.filter {
                val expCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                expCal.get(Calendar.MONTH) == currentMonth && expCal.get(Calendar.YEAR) == currentYear
            }.sumOf { it.amount }
            
            val percent = if (budget > 0) (monthSpent / budget * 100).toInt() else 0
            val isDarkMode = try { dataStore.loadDarkMode() } catch (e: Exception) { false }

            // Theme colors
            val primaryTextColor = if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            val secondaryTextColor = if (isDarkMode) android.graphics.Color.parseColor("#CAC4D0") else android.graphics.Color.parseColor("#666666")
            val titleColor = if (isDarkMode) android.graphics.Color.parseColor("#D0BCFF") else android.graphics.Color.parseColor("#6750A4")
            val bgColorRes = if (isDarkMode) R.drawable.widget_bg_dark else R.drawable.widget_bg

            // Status color logic (restored for Phase 3B)
            val statusColor = when {
                percent >= 100 -> android.graphics.Color.parseColor("#F87171")
                percent >= 80 -> android.graphics.Color.parseColor("#FBBF24")
                else -> android.graphics.Color.parseColor("#4ADE80")
            }

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            // Apply Theme
            views.setInt(R.id.widget_root, "setBackgroundResource", bgColorRes)
            views.setTextColor(R.id.widget_title, titleColor)
            views.setTextColor(R.id.today_spent, primaryTextColor)
            views.setTextColor(R.id.monthly_status, statusColor)
            
            // Set Text
            views.setTextViewText(R.id.today_spent, "฿ ${todaySpent.toInt()}")
            views.setTextViewText(R.id.monthly_status, "ใช้ไป ฿${monthSpent.toInt()} ($percent%)")
            
            // Update Progress Bar
            views.setProgressBar(R.id.budget_progress, 100, percent.coerceAtMost(100), false)

            // Click interaction: Open MainActivity
            val appIntent = android.content.Intent(context, com.example.wheremymoneylost.MainActivity::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, appIntent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val intent = android.content.Intent(context, SimpleExpenseWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, SimpleExpenseWidget::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
