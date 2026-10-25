package com.example.tbankcontrol;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    // Идентификатор канала и самого уведомления
    private static final String CHANNEL_ID = "my_channel_id";
    private static final int NOTIFICATION_ID = 101;
    public static void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Создание канала уведомлений
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            CharSequence name = "main_channel";
            String description = "Channel for important app notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT; // Уровень важности

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        //Настройка действия при клике на уведомление (открытие MainActivity)
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Флаг FLAG_IMMUTABLE обязателен для Android 12+
        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                PendingIntent.FLAG_IMMUTABLE : 0;

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, pendingIntentFlags
        );

        //Сборка самого уведомления
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Иконка (должна быть полностью белой на прозрачном фоне)
                .setContentTitle(title)                  // Заголовок
                .setContentText(message)                 // Текст сообщения
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Приоритет для старых версий
                .setContentIntent(pendingIntent)          // Добавляем действие при клике
                .setAutoCancel(true);                     // Удалять из шторки после клика

        // 4. Показ уведомления
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
