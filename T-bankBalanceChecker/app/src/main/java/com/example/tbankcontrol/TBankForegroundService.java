package com.example.tbankcontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;

public class TBankForegroundService extends Service {
    private static final String TAG = "TBankService";
    private static final long ONE_MINUTE = 60000; // 1 минута в миллисекундах

    private Handler m_handler;
    private Runnable m_checkRunnable;
    private TBankApiService apiService;

    private double m_balanceThreshold = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        m_handler = new Handler(Looper.getMainLooper());
        apiService = new TBankApiService(this);

        NotificationHelper.createNotificationChannel(this);
        // Настраиваем периодическое обновление
        m_checkRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Фоновый запрос баланса...");

                apiService.fetchBankAccounts(new TBankApiService.ApiCallback() {
                    @Override
                    public void onSuccess(List<AccountAdapter.AccountItem> accountsList) {
                        Log.d(TAG, "Данные обновлены в фоне");
                        //Считываем состояние из файла (если было сохранено)
                        for (AccountAdapter.AccountItem item : accountsList) {
                            item.isTracked = AppSettings.getInstance().
                                isNotificationEnabled(item.number);

                            if (item.isTracked && item.balance < m_balanceThreshold) {
                                NotificationHelper.showNotification(TBankForegroundService.this,
                                    getString(R.string.NotificationTitle),
                                    getString(R.string.NotificationPrefix, item.shortNumber));
                            }
                        }

                        //Сохраняем данные локально (чтобы при открытии приложения данные всегда были доступны)
                        String jsonString = AppSettings.getInstance().
                                saveAccounts(accountsList);

                        //Отправляем Intent-сигнал для MainActivity
                        Intent intent = new Intent("com.example.tbankcontrol.UPDATE_ACCOUNTS_ACTION");

                        intent.putExtra("accounts_json", jsonString);
                        intent.setPackage(getPackageName());

                        Log.d(TAG, "Broadcast успешно отправлен с флагом setPackage!");

                        sendBroadcast(intent); // Рассылаем сигнал в систему
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Ошибка обновления в фоне: " + errorMessage);
                    }
                });

                // Планируем повтор ровно через 1 минуту
                m_handler.postDelayed(this, ONE_MINUTE);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand: Сервис пытается запуститься!");
        try {
            m_balanceThreshold = intent.getDoubleExtra("BALANCE_THRESHOLD", 0);
            // Создаем системное уведомление
            Notification notification = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                    .setContentTitle("Т-Банк контроль")
                    .setContentText("Мониторинг баланса запущен в фоне")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            // Запускаем службу в приоритетном режиме Foreground
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(1, notification);
            }
            // Запускаем цикл обновлений
            m_handler.post(m_checkRunnable);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка запуска сервиса: " + e.getMessage(), e);
        }

        return START_STICKY; // Приказывает системе воскресить службу, если она упадет
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        if (m_handler != null && m_checkRunnable != null) {
            m_handler.removeCallbacks(m_checkRunnable);
        }

        //Убираем сервис из режима Foreground и удаляем постоянное уведомление
        stopForeground(STOP_FOREGROUND_REMOVE);

        Log.d(TAG, "TBankForegroundService: Сервис полностью остановлен и уничтожен!");
        super.onDestroy();
    }
}
