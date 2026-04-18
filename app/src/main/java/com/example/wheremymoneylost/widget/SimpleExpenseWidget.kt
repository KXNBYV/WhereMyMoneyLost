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

        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        views.setTextViewText(R.id.today_spent, "฿ ${todaySpent.toInt()}")
        views.setTextViewText(R.id.monthly_status, "จากงบ ฿${budget.toInt()} ($percent%)")

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
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
