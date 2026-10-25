package com.example.tbankcontrol;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    private static final String PREF_NAME = "app_prefs";
    private static AppSettings instance;
    private final SharedPreferences m_sharedPreferences;
    private AppSettings(Context context) {
        this.m_sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new AppSettings(context);
        }
    }
    public static AppSettings getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppSettings не был инициализирован. Вызовите init(Context) сначала.");
        }
        return instance;
    }
    public void setNotificationEnabled(String accountNumber, boolean isEnabled) {
        String uniqueKey = "notifications_enabled_" + accountNumber;
        m_sharedPreferences.edit().putBoolean(uniqueKey, isEnabled).apply();
    }
    public boolean isNotificationEnabled(String accountNumber) {
        String uniqueKey = "notifications_enabled_" + accountNumber;
        return m_sharedPreferences.getBoolean(uniqueKey, false);
    }
}
