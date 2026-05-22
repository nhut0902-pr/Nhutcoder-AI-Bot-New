package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.util.Calendar

class SpaceZWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.spacez_widget_layout)

        // Set dynamic quote of the day
        val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val quotes = listOf(
            "Mọi vấn đề phức tạp đều có thể giải quyết nếu chia đủ nhỏ. ✨",
            "Mỗi dòng code đều là một bước tiến mới của sáng tạo. 💻",
            "Sức mạnh vô hạn của AI kết nối tinh hoa công nghệ SpaceZ! 🌌",
            "Càng học hỏi càng khám phá thêm nhiều hành tinh tri thức mới! 🪐",
            "Hãy luôn tò mò, sáng tạo ra các giải pháp kỳ diệu hôm nay. 🧠",
            "Bắt đầu ngày mới tràn đầy cảm hứng cùng SpaceZ AI! 🚀"
        )
        val selectedQuote = quotes[day % quotes.size]
        views.setTextViewText(R.id.widget_subtitle, selectedQuote)

        // Bind clicks with pending intents targeting MainActivity with specific actions
        views.setOnClickPendingIntent(
            R.id.btn_morning,
            getPendingIntent(context, "com.example.ACTION_MORNING_GREETING")
        )
        views.setOnClickPendingIntent(
            R.id.btn_aichat,
            getPendingIntent(context, "com.example.ACTION_AI_CHAT")
        )
        views.setOnClickPendingIntent(
            R.id.btn_background,
            getPendingIntent(context, "com.example.ACTION_BACKGROUND_MODE")
        )
        views.setOnClickPendingIntent(
            R.id.btn_share,
            getPendingIntent(context, "com.example.ACTION_SHARE_SCREEN")
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
