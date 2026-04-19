package com.example.wheremymoneylost.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.wheremymoneylost.data.local.DataStore
import com.example.wheremymoneylost.data.model.*
import com.example.wheremymoneylost.notification.BudgetNotificationHelper
import com.example.wheremymoneylost.ui.component.CategorySuggestion
import com.example.wheremymoneylost.widget.SimpleExpenseWidget
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = DataStore(application)
    private val notificationHelper = BudgetNotificationHelper(application)

    val categories = mutableStateListOf<Category>()
    val expenses = mutableStateListOf<Expense>()
    val recurringExpenses = mutableStateListOf<RecurringExpense>()
    val bills = mutableStateListOf<Bill>()
    val savingGoals = mutableStateListOf<SavingGoal>()
    
    val monthlyBudget = mutableDoubleStateOf(10000.0)
    val isDarkMode = mutableStateOf(false)
    val alertPercent = mutableIntStateOf(80)
    val ongoingNotificationEnabled = mutableStateOf(true)
    val streak = mutableIntStateOf(0)

    val selectedMonth = mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    val selectedYear = mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))

    private var nextId = 1
    private val notifiedAlerts = mutableSetOf<String>()

    init {
        categories.addAll(dataStore.loadCategories())
        expenses.addAll(dataStore.loadExpenses())
        recurringExpenses.addAll(dataStore.loadRecurringExpenses())
        bills.addAll(dataStore.loadBills())
        savingGoals.addAll(dataStore.loadSavingGoals())
        
        monthlyBudget.doubleValue = dataStore.loadBudget()
        isDarkMode.value = dataStore.loadDarkMode()
        alertPercent.intValue = dataStore.loadAlertPercent()
        ongoingNotificationEnabled.value = dataStore.loadOngoingNotification()
        streak.intValue = dataStore.loadStreak()
        nextId = dataStore.loadNextId()

        checkAndApplyRecurring()
        ensureSavingsCategory()
        checkBillReminders()
        SimpleExpenseWidget.updateAllWidgets(getApplication())
    }

    private fun ensureSavingsCategory() {
        if (categories.none { it.name == "เงินออม" }) {
            addCategory("เงินออม", "savings", "#4ADE80")
        }
    }

    private fun checkBillReminders() {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val nextDay = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        bills.filter { !it.isPaid && it.dueDate in tomorrow..nextDay }.forEach { bill ->
            notificationHelper.notifyBillReminder(bill.name, bill.amount.toInt())
        }
    }

    // --- Bills Logic ---
    fun addBill(name: String, amount: Double, dueDate: Long, categoryId: Int = 4) {
        val id = (bills.maxOfOrNull { it.id } ?: 0) + 1
        val bill = Bill(id, name, amount, dueDate, false, categoryId)
        bills.add(bill)
        dataStore.saveBills(bills.toList())
    }

    fun markBillAsPaid(id: Int) {
        val index = bills.indexOfFirst { it.id == id }
        if (index >= 0) {
            val bill = bills[index]
            if (!bill.isPaid) {
                bills[index] = bill.copy(isPaid = true)
                addExpense(bill.amount, bill.categoryId, "จ่ายบิล: ${bill.name}")
                dataStore.saveBills(bills.toList())
            }
        }
    }

    fun deleteBill(id: Int) {
        bills.removeAll { it.id == id }
        dataStore.saveBills(bills.toList())
    }

    fun getBillsForDate(year: Int, month: Int, day: Int): List<Bill> {
        return bills.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dueDate }
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
        }
    }

    fun getUpcomingBills(): List<Bill> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return bills.filter { !it.isPaid && it.dueDate >= today }.sortedBy { it.dueDate }
    }

    // --- Saving Goals Logic ---
    fun addSavingGoal(name: String, target: Double, deadline: Long? = null) {
        val id = (savingGoals.maxOfOrNull { it.id } ?: 0) + 1
        val goal = SavingGoal(id, name, target, 0.0, deadline)
        savingGoals.add(goal)
        dataStore.saveSavingGoals(savingGoals.toList())
    }

    fun contributeToGoal(id: Int, amount: Double) {
        val index = savingGoals.indexOfFirst { it.id == id }
        if (index >= 0) {
            val goal = savingGoals[index]
            val newAmount = goal.currentAmount + amount
            savingGoals[index] = goal.copy(currentAmount = newAmount)
            // When contributing to a goal, it's counted as an expense from the budget
            addExpense(amount, categories.find { it.name == "เงินออม" }?.id ?: 5, "ออมเงิน: ${goal.name}")
            
            if (newAmount >= goal.targetAmount && goal.currentAmount < goal.targetAmount) {
                notificationHelper.notifyGoalSuccess(goal.name)
            }
            
            dataStore.saveSavingGoals(savingGoals.toList())
        }
    }

    fun editSavingGoal(id: Int, newName: String, newTarget: Double) {
        val index = savingGoals.indexOfFirst { it.id == id }
        if (index >= 0) {
            val goal = savingGoals[index]
            savingGoals[index] = goal.copy(name = newName, targetAmount = newTarget)
            dataStore.saveSavingGoals(savingGoals.toList())
        }
    }

    fun deleteSavingGoal(id: Int) {
        savingGoals.removeAll { it.id == id }
        dataStore.saveSavingGoals(savingGoals.toList())
    }

    // --- Spending Velocity Meter ---
    fun getSpendingVelocity(): Float {
        val totalSpent = currentMonthExpenses().sumOf { it.amount }
        val budget = monthlyBudget.doubleValue
        if (budget <= 0) return 0f
        
        val cal = Calendar.getInstance()
        val timePercent = cal.get(Calendar.DAY_OF_MONTH).toFloat() / cal.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat()
        val spentPercent = (totalSpent / budget).toFloat()
        
        return if (timePercent > 0) (spentPercent / timePercent) * 0.5f else 0f
    }

    // --- Smart Auto-Categorize ---
    fun suggestCategoryFromMemo(text: String): Int? {
        val keywords = mapOf(
            "อาหาร" to listOf("กิน", "ข้าว", "บุฟเฟ่ต์", "ชาบู", "หิว", "น้ำ", "กาแฟ", "ขนม", "Food", "KFC", "MD"),
            "เดินทาง" to listOf("รถ", "น้ำมัน", "Grab", "Lineman", "Bolt", "MRT", "BTS", "วิน", "Taxi", "Gas"),
            "ช้อปปิ้ง" to listOf("ซื้อ", "ห้าง", "Shopee", "Lazada", "เสื้อ", "ผ้า", "Mall", "Shop"),
            "บิล/ค่าน้ำไฟ" to listOf("บิล", "ไฟ", "น้ำ", "เน็ต", "โทรศัพท์", "Bill", "Internet", "Rent"),
            "สุขภาพ" to listOf("ยา", "หมอ", "โรงพยาบาล", "คลินิก", "วิตามิน", "Health", "Clinic"),
            "บันเทิง" to listOf("หนัง", "เกม", "Steam", "Netflix", "เที่ยว", "เหล้า", "เบียร์", "Party")
        )

        for ((catName, words) in keywords) {
            if (words.any { text.contains(it, ignoreCase = true) }) {
                return categories.find { it.name == catName }?.id
            }
        }
        return null
    }

    // --- Recurring Logic ---
    private fun checkAndApplyRecurring() {
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        recurringExpenses.forEach { rec ->
            if (currentDay >= rec.dayOfMonth) {
                val alreadyAdded = expenses.any { exp ->
                    val expCal = Calendar.getInstance().apply { timeInMillis = exp.timestamp }
                    exp.categoryId == rec.categoryId && 
                    exp.amount == rec.amount && 
                    expCal.get(Calendar.MONTH) == currentMonth && 
                    expCal.get(Calendar.YEAR) == currentYear &&
                    exp.memo == "อัตโนมัติ: ${rec.name}"
                }
                if (!alreadyAdded) {
                    addExpense(rec.amount, rec.categoryId, "อัตโนมัติ: ${rec.name}")
                }
            }
        }
    }

    fun addRecurringExpense(name: String, amount: Double, categoryId: Int, day: Int) {
        val id = (recurringExpenses.maxOfOrNull { it.id } ?: 0) + 1
        val rec = RecurringExpense(id, amount, categoryId, name, day)
        recurringExpenses.add(rec)
        dataStore.saveRecurringExpenses(recurringExpenses.toList())
        checkAndApplyRecurring()
    }

    fun deleteRecurringExpense(id: Int) {
        recurringExpenses.removeAll { it.id == id }
        dataStore.saveRecurringExpenses(recurringExpenses.toList())
    }

    // --- Daily Limit ---
    fun getDailyLimit(): Double {
        val cal = Calendar.getInstance()
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val remainingDays = (totalDays - currentDay + 1).toDouble()
        
        val spentThisMonth = currentMonthExpenses().sumOf { it.amount }
        val remainingBudget = (monthlyBudget.doubleValue - spentThisMonth).coerceAtLeast(0.0)
        
        return remainingBudget / remainingDays
    }

    // --- CRUD ---
    fun addExpense(amount: Double, categoryId: Int, memo: String = "") {
        val expense = Expense(id = nextId++, amount = amount, categoryId = categoryId, memo = memo)
        expenses.add(expense)
        dataStore.saveExpenses(expenses.toList())
        dataStore.saveNextId(nextId)
        checkBudgetAndNotify()
        updateOngoingNotification()
        SimpleExpenseWidget.updateAllWidgets(getApplication())
    }

    fun editExpense(id: Int, newAmount: Double, newCategoryId: Int, newMemo: String) {
        val index = expenses.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = expenses[index]
            expenses[index] = old.copy(amount = newAmount, categoryId = newCategoryId, memo = newMemo)
            dataStore.saveExpenses(expenses.toList())
            updateOngoingNotification()
            SimpleExpenseWidget.updateAllWidgets(getApplication())
        }
    }

    fun deleteExpense(id: Int) {
        expenses.removeAll { it.id == id }
        dataStore.saveExpenses(expenses.toList())
        updateOngoingNotification()
        SimpleExpenseWidget.updateAllWidgets(getApplication())
    }

    // --- Monthly Navigation ---
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

    // --- Settings & Metadata ---
    fun setBudget(amount: Double) {
        monthlyBudget.doubleValue = amount
        dataStore.saveBudget(amount)
        notifiedAlerts.clear()
        updateOngoingNotification()
        SimpleExpenseWidget.updateAllWidgets(getApplication())
    }

    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        dataStore.saveDarkMode(enabled)
        SimpleExpenseWidget.updateAllWidgets(getApplication())
    }

    fun setAlertPercent(percent: Int) {
        alertPercent.intValue = percent
        dataStore.saveAlertPercent(percent)
        notifiedAlerts.clear()
    }

    fun setOngoingNotification(enabled: Boolean) {
        ongoingNotificationEnabled.value = enabled
        dataStore.saveOngoingNotification(enabled)
        if (enabled) updateOngoingNotification()
        else notificationHelper.cancelOngoingNotification()
    }

    fun addCategory(name: String, iconName: String, colorHex: String, budgetLimit: Double = 0.0) {
        val maxId = categories.maxOfOrNull { it.id } ?: 0
        categories.add(Category(id = maxId + 1, name = name, iconName = iconName, colorHex = colorHex, budgetLimit = budgetLimit))
        dataStore.saveCategories(categories.toList())
    }

    fun deleteCategory(id: Int) {
        categories.removeAll { it.id == id }
        dataStore.saveCategories(categories.toList())
    }

    fun updateCategory(id: Int, name: String, iconName: String, colorHex: String, budgetLimit: Double) {
        val index = categories.indexOfFirst { it.id == id }
        if (index >= 0) {
            categories[index] = Category(id = id, name = name, iconName = iconName, colorHex = colorHex, budgetLimit = budgetLimit)
            dataStore.saveCategories(categories.toList())
        }
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
                val suggested = (Math.ceil(avg / 100) * 100)
                suggestions.add(CategorySuggestion(cat, suggested, avg))
            }
        }
        return suggestions
    }

    fun applySuggestions() {
        getSuggestions().forEach { updateCategoryBudget(it.category.id, it.suggestedBudget) }
    }

    // --- Export ---
    fun formatExportText(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, selectedMonth.intValue)
            set(Calendar.YEAR, selectedYear.intValue)
        }
        val monthLabel = getMonthName(selectedMonth.intValue)
        val yearLabel = selectedYear.intValue + 543
        val currentExpenses = getExpensesForSelectedMonth()
        val total = currentExpenses.sumOf { it.amount }
        val budget = monthlyBudget.doubleValue
        val percent = if (budget > 0) (total/budget*100).toInt() else 0

        val sb = StringBuilder()
        sb.append("💰 สรุปเดือน $monthLabel $yearLabel\n")
        sb.append("งบ: ฿${budget.toInt()} | ใช้ไป: ฿${total.toInt()} ($percent%)\n")
        
        categories.forEach { cat ->
            val catSpent = currentExpenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
            if (catSpent > 0) {
                val catPct = (catSpent / total * 100).toInt()
                val icon = when (cat.name) {
                    "อาหาร" -> "🍔"
                    "เดินทาง" -> "🚗"
                    "ช้อปปิ้ง" -> "🛒"
                    "บิล/ค่าน้ำไฟ" -> "📄"
                    "สุขภาพ" -> "🏥"
                    "บันเทิง" -> "🎬"
                    else -> "📁"
                }
                sb.append("$icon ${cat.name}: ฿${catSpent.toInt()} ($catPct%)\n")
            }
        }
        return sb.toString()
    }

    // --- Notifications ---
    private fun checkBudgetAndNotify() {
        val totalSpent = currentMonthExpenses().sumOf { it.amount }
        val budget = monthlyBudget.doubleValue
        if (budget <= 0) return
        val percent = ((totalSpent / budget) * 100).toInt()
        val alertPct = alertPercent.intValue
        if (percent >= alertPct && "alert" !in notifiedAlerts) {
            notifiedAlerts.add("alert")
            notificationHelper.notifyAlertPercent(totalSpent.toInt(), budget.toInt(), percent)
        }
        if (percent >= 100 && "over" !in notifiedAlerts) {
            notifiedAlerts.add("over")
            notificationHelper.notifyBudgetExceeded(totalSpent.toInt(), budget.toInt())
        }
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
