package com.example.wheremymoneylost.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.wheremymoneylost.data.local.DataStore
import com.example.wheremymoneylost.data.model.Category
import com.example.wheremymoneylost.data.model.Expense
import com.example.wheremymoneylost.notification.BudgetNotificationHelper
import com.example.wheremymoneylost.ui.component.CategorySuggestion
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = DataStore(application)
    private val notificationHelper = BudgetNotificationHelper(application)

    val categories = mutableStateListOf<Category>()
    val expenses = mutableStateListOf<Expense>()
    val monthlyBudget = mutableDoubleStateOf(10000.0)
    val isDarkMode = mutableStateOf(false)
    val alertPercent = mutableIntStateOf(80)
    val ongoingNotificationEnabled = mutableStateOf(true)

    // เดือนที่กำลังดู
    val selectedMonth = mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    val selectedYear = mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))

    private var nextId = 1
    private val notifiedAlerts = mutableSetOf<String>()

    init {
        categories.addAll(dataStore.loadCategories())
        expenses.addAll(dataStore.loadExpenses())
        monthlyBudget.doubleValue = dataStore.loadBudget()
        isDarkMode.value = dataStore.loadDarkMode()
        alertPercent.intValue = dataStore.loadAlertPercent()
        ongoingNotificationEnabled.value = dataStore.loadOngoingNotification()
        nextId = dataStore.loadNextId()
    }

    // --- ดูค่าใช้จ่ายตามเดือนที่เลือก ---
    fun getExpensesForSelectedMonth(): List<Expense> {
        return getExpensesForMonth(selectedMonth.intValue, selectedYear.intValue)
    }

    fun getExpensesForMonth(month: Int, year: Int): List<Expense> {
        return expenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }
    }

    fun currentMonthExpenses(): List<Expense> {
        val cal = Calendar.getInstance()
        return getExpensesForMonth(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    fun lastMonthExpenses(): List<Expense> {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        return getExpensesForMonth(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    fun isCurrentMonth(): Boolean {
        val cal = Calendar.getInstance()
        return selectedMonth.intValue == cal.get(Calendar.MONTH) &&
                selectedYear.intValue == cal.get(Calendar.YEAR)
    }

    fun isFutureMonth(): Boolean {
        val cal = Calendar.getInstance()
        val currentMonthVal = cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)
        val selectedMonthVal = selectedYear.intValue * 12 + selectedMonth.intValue
        return selectedMonthVal > currentMonthVal
    }

    // --- เลื่อนเดือน ---
    fun navigateMonth(direction: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear.intValue)
            set(Calendar.MONTH, selectedMonth.intValue)
            add(Calendar.MONTH, direction)
        }
        selectedMonth.intValue = cal.get(Calendar.MONTH)
        selectedYear.intValue = cal.get(Calendar.YEAR)
    }

    fun goToCurrentMonth() {
        val cal = Calendar.getInstance()
        selectedMonth.intValue = cal.get(Calendar.MONTH)
        selectedYear.intValue = cal.get(Calendar.YEAR)
    }

    // --- CRUD ---
    fun addExpense(amount: Double, categoryId: Int) {
        val expense = Expense(id = nextId++, amount = amount, categoryId = categoryId)
        expenses.add(expense)
        dataStore.saveExpenses(expenses.toList())
        dataStore.saveNextId(nextId)
        checkBudgetAndNotify()
        updateOngoingNotification()
    }

    fun deleteExpense(id: Int) {
        expenses.removeAll { it.id == id }
        dataStore.saveExpenses(expenses.toList())
        updateOngoingNotification()
    }

    fun editExpense(id: Int, newAmount: Double, newCategoryId: Int) {
        val index = expenses.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = expenses[index]
            expenses[index] = old.copy(amount = newAmount, categoryId = newCategoryId)
            dataStore.saveExpenses(expenses.toList())
            updateOngoingNotification()
        }
    }

    // --- งบ ---
    fun setBudget(amount: Double) {
        monthlyBudget.doubleValue = amount
        dataStore.saveBudget(amount)
        notifiedAlerts.clear()
    }

    // --- Dark Mode ---
    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        dataStore.saveDarkMode(enabled)
    }

    // --- Alert Percent ---
    fun setAlertPercent(percent: Int) {
        alertPercent.intValue = percent
        dataStore.saveAlertPercent(percent)
        notifiedAlerts.clear()
    }

    // --- Ongoing Notification ---
    fun setOngoingNotification(enabled: Boolean) {
        ongoingNotificationEnabled.value = enabled
        dataStore.saveOngoingNotification(enabled)
        if (enabled) updateOngoingNotification()
        else notificationHelper.cancelOngoingNotification()
    }

    // --- หมวดหมู่ ---
    fun addCategory(name: String, iconName: String, colorHex: String, budgetLimit: Double = 0.0) {
        val maxId = categories.maxOfOrNull { it.id } ?: 0
        categories.add(Category(id = maxId + 1, name = name, iconName = iconName, colorHex = colorHex, budgetLimit = budgetLimit))
        dataStore.saveCategories(categories.toList())
    }

    fun deleteCategory(id: Int) {
        categories.removeAll { it.id == id }
        dataStore.saveCategories(categories.toList())
    }

    fun updateCategoryBudget(id: Int, budgetLimit: Double) {
        val index = categories.indexOfFirst { it.id == id }
        if (index >= 0) {
            categories[index] = categories[index].copy(budgetLimit = budgetLimit)
            dataStore.saveCategories(categories.toList())
        }
    }

    // --- Suggestions ---
    fun getSuggestions(): List<CategorySuggestion> {
        val cal = Calendar.getInstance()
        val suggestions = mutableListOf<CategorySuggestion>()

        categories.forEach { cat ->
            var totalForCat = 0.0
            var monthsWithData = 0

            for (i in 1..3) {
                val pastCal = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
                val monthExpenses = getExpensesForMonth(pastCal.get(Calendar.MONTH), pastCal.get(Calendar.YEAR))
                val catTotal = monthExpenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
                if (catTotal > 0) {
                    totalForCat += catTotal
                    monthsWithData++
                }
            }

            if (monthsWithData > 0) {
                val avg = totalForCat / monthsWithData
                val suggested = (Math.ceil(avg / 100) * 100) // ปัดขึ้นเป็นหลักร้อย
                suggestions.add(CategorySuggestion(cat, suggested, avg))
            }
        }

        return suggestions
    }

    fun applySuggestions() {
        val suggestions = getSuggestions()
        suggestions.forEach { s ->
            updateCategoryBudget(s.category.id, s.suggestedBudget)
        }
    }

    // --- Notifications ---
    private fun checkBudgetAndNotify() {
        val totalSpent = currentMonthExpenses().sumOf { it.amount }
        val budget = monthlyBudget.doubleValue
        if (budget <= 0) return

        val percent = ((totalSpent / budget) * 100).toInt()
        val alertPct = alertPercent.intValue

        // เตือนเมื่อถึง % ที่ตั้งไว้
        if (percent >= alertPct && "alert" !in notifiedAlerts) {
            notifiedAlerts.add("alert")
            notificationHelper.notifyAlertPercent(totalSpent.toInt(), budget.toInt(), percent)
        }

        // เตือนเมื่อเกินงบ
        if (percent >= 100 && "over" !in notifiedAlerts) {
            notifiedAlerts.add("over")
            notificationHelper.notifyBudgetExceeded(totalSpent.toInt(), budget.toInt())
        }

        // เตือนเมื่อหมวดหมู่เกินวงเงิน
        categories.forEach { cat ->
            if (cat.budgetLimit > 0) {
                val catSpent = currentMonthExpenses().filter { it.categoryId == cat.id }.sumOf { it.amount }
                if (catSpent >= cat.budgetLimit && "cat_${cat.id}" !in notifiedAlerts) {
                    notifiedAlerts.add("cat_${cat.id}")
                    notificationHelper.notifyCategoryExceeded(cat, catSpent.toInt(), cat.budgetLimit.toInt())
                }
            }
        }
    }

    private fun updateOngoingNotification() {
        if (!ongoingNotificationEnabled.value) return

        val monthExpenses = currentMonthExpenses()
        val totalSpent = monthExpenses.sumOf { it.amount }
        val budget = monthlyBudget.doubleValue
        val percent = if (budget > 0) ((totalSpent / budget) * 100).toInt() else 0

        val topCat = categories.maxByOrNull { cat ->
            monthExpenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
        }?.name ?: "-"

        notificationHelper.updateOngoingNotification(totalSpent.toInt(), budget.toInt(), percent, topCat)
    }

    fun getMonthName(month: Int): String {
        return listOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
            "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")[month]
    }
}
