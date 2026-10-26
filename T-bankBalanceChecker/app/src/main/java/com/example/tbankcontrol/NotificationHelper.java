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
    public static final String CHANNEL_ID = "TBankBackgroundChannel";
    private static final int importance = NotificationManager.IMPORTANCE_DEFAULT; // Уровень важности
    private static final CharSequence channel_name = "main_channel";
    private static final String channel_description = "Channel for important app notifications";
    private static final int NOTIFICATION_ID = 101;
    private static NotificationChannel serviceChannel = null;

    public static NotificationChannel getChannel() {
        return serviceChannel;
    }
    public static void showNotification(Context context, String title, String message) {

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

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

        //Показ уведомления
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    public static NotificationChannel createNotificationChannel(Context context) {

        if ( serviceChannel != null ) {
            return serviceChannel;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if ( notificationManager == null ) {
            return null;
        }

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            serviceChannel = new NotificationChannel(CHANNEL_ID, channel_name, importance);
            serviceChannel.setDescription(channel_description);

            notificationManager.createNotificationChannel(serviceChannel);
        }

        return serviceChannel;
    }

}
