package com.example.wheremymoneylost.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.wheremymoneylost.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("where_my_money_lost", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- Expenses ---
    fun saveExpenses(expenses: List<Expense>) {
        prefs.edit().putString("expenses", gson.toJson(expenses)).apply()
    }

    fun loadExpenses(): MutableList<Expense> {
        val json = prefs.getString("expenses", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Expense>>() {}.type
        return gson.fromJson(json, type)
    }

    // --- Recurring Expenses ---
    fun saveRecurringExpenses(recurring: List<RecurringExpense>) {
        prefs.edit().putString("recurring_expenses", gson.toJson(recurring)).apply()
    }

    fun loadRecurringExpenses(): MutableList<RecurringExpense> {
        val json = prefs.getString("recurring_expenses", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<RecurringExpense>>() {}.type
        return gson.fromJson(json, type)
    }

    // --- Bills ---
    fun saveBills(bills: List<Bill>) {
        prefs.edit().putString("bills", gson.toJson(bills)).apply()
    }

    fun loadBills(): MutableList<Bill> {
        val json = prefs.getString("bills", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Bill>>() {}.type
        return gson.fromJson(json, type)
    }

    // --- Saving Goals ---
    fun saveSavingGoals(goals: List<SavingGoal>) {
        prefs.edit().putString("saving_goals", gson.toJson(goals)).apply()
    }

    fun loadSavingGoals(): MutableList<SavingGoal> {
        val json = prefs.getString("saving_goals", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<SavingGoal>>() {}.type
        return gson.fromJson(json, type)
    }

    // --- Categories ---
    fun saveCategories(categories: List<Category>) {
        prefs.edit().putString("categories", gson.toJson(categories)).apply()
    }

    fun loadCategories(): List<Category> {
        val json = prefs.getString("categories", null) ?: return defaultCategories()
        val type = object : TypeToken<List<Category>>() {}.type
        return gson.fromJson(json, type)
    }

    // --- Budget ---
    fun saveBudget(budget: Double) {
        prefs.edit().putLong("budget", budget.toLong()).apply()
    }

    fun loadBudget(): Double {
        return prefs.getLong("budget", 10000L).toDouble()
    }

    // --- Streak & Last Visit ---
    fun saveStreak(streak: Int) {
        prefs.edit().putInt("streak", streak).apply()
    }

    fun loadStreak(): Int {
        return prefs.getInt("streak", 0)
    }

    fun saveLastVisitDate(dateStr: String) { // YYYY-MM-DD
        prefs.edit().putString("last_visit_date", dateStr).apply()
    }

    fun loadLastVisitDate(): String {
        return prefs.getString("last_visit_date", "") ?: ""
    }

    // --- Settings ---
    fun saveDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun loadDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", false)
    }

    fun saveNextId(id: Int) {
        prefs.edit().putInt("next_id", id).apply()
    }

    fun loadNextId(): Int {
        return prefs.getInt("next_id", 1)
    }

    fun saveAlertPercent(percent: Int) {
        prefs.edit().putInt("alert_percent", percent).apply()
    }

    fun loadAlertPercent(): Int {
        return prefs.getInt("alert_percent", 80)
    }

    fun saveOngoingNotification(enabled: Boolean) {
        prefs.edit().putBoolean("ongoing_notification", enabled).apply()
    }

    fun loadOngoingNotification(): Boolean {
        return prefs.getBoolean("ongoing_notification", true)
    }

    companion object {
        fun defaultCategories() = listOf(
            Category(id = 1, name = "อาหาร", iconName = "fastfood", colorHex = "#F44336", budgetLimit = 0.0),
            Category(id = 2, name = "เดินทาง", iconName = "directions_car", colorHex = "#2196F3", budgetLimit = 0.0),
            Category(id = 3, name = "ช้อปปิ้ง", iconName = "shopping_cart", colorHex = "#E91E63", budgetLimit = 0.0),
            Category(id = 4, name = "บิล/ค่าน้ำไฟ", iconName = "receipt", colorHex = "#9C27B0", budgetLimit = 0.0),
            Category(id = 5, name = "สุขภาพ", iconName = "health", colorHex = "#4CAF50", budgetLimit = 0.0),
            Category(id = 6, name = "บันเทิง", iconName = "movie", colorHex = "#FF9800", budgetLimit = 0.0)
        )
    }
}
