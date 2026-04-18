package com.example.wheremymoneylost.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.wheremymoneylost.data.model.Category

class BudgetNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val CHANNEL_ONGOING = "budget_ongoing"
        const val NOTIFICATION_ALERT = 1001
        const val NOTIFICATION_OVER = 1002
        const val NOTIFICATION_ONGOING = 2001
        const val NOTIFICATION_CATEGORY = 3000
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ID, "แจ้งเตือนงบประมาณ", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "แจ้งเตือนเมื่อใช้จ่ายถึงเปอร์เซ็นต์ที่ตั้งไว้" }

            val ongoingChannel = NotificationChannel(
                CHANNEL_ONGOING, "สถานะงบประมาณ", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "แสดงสถานะการใช้จ่ายตลอดเวลา" }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(alertChannel)
            manager.createNotificationChannel(ongoingChannel)
        }
    }

    // --- Ongoing Notification (ค้างใน status bar) ---
    fun updateOngoingNotification(spent: Int, budget: Int, percent: Int, topCategory: String) {
        if (!hasPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("💰 ใช้ไป ฿$spent / ฿$budget ($percent%)")
            .setContentText("หมวดที่ใช้มากสุด: $topCategory")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ONGOING, notification)
    }

    fun cancelOngoingNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ONGOING)
    }

    // --- Alert เมื่อถึง % ที่ตั้งไว้ ---
    fun notifyAlertPercent(spent: Int, budget: Int, percent: Int) {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ ใช้จ่ายถึง $percent% แล้ว!")
            .setContentText("คุณใช้ไป ฿$spent จากงบ ฿$budget")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .build()

        if (hasPermission()) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ALERT, notification)
        }
    }

    // --- Alert เมื่อเกินงบ ---
    fun notifyBudgetExceeded(spent: Int, budget: Int) {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 เกินงบแล้ว!")
            .setContentText("ใช้ไป ฿$spent เกินงบ ฿$budget ไปแล้ว ฿${spent - budget}!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 1000, 300, 1000))
            .setAutoCancel(true)
            .build()

        if (hasPermission()) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_OVER, notification)
        }
    }

    // --- Alert เมื่อหมวดหมู่เกินวงเงิน ---
    fun notifyCategoryExceeded(category: Category, spent: Int, limit: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🔴 ${category.name} เกินวงเงินแล้ว!")
            .setContentText("ใช้ไป ฿$spent / วงเงิน ฿$limit")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (hasPermission()) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_CATEGORY + category.id, notification)
        }
    }

    // --- Alert เตือนจ่ายบิลล่วงหน้า 1 วัน ---
    fun notifyBillReminder(billName: String, amount: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("📅 พรุ่งนี้มีบิลต้องจ่าย!")
            .setContentText("อย่าลืมจ่าย: $billName จำนวน ฿$amount")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (hasPermission()) {
            NotificationManagerCompat.from(context).notify(4000 + billName.hashCode(), notification)
        }
    }

    // --- Alert เมื่อทำเป้าหมายสำเร็จ ---
    fun notifyGoalSuccess(goalName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("🎉 ยินดีด้วย! ออมครบแล้ว")
            .setContentText("คุณทำเป้าหมาย '$goalName' สำเร็จแล้ว!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (hasPermission()) {
            NotificationManagerCompat.from(context).notify(5000 + goalName.hashCode(), notification)
        }
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
