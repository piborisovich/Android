package com.example.tbankcontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public String saveAccounts(List<AccountAdapter.AccountItem> accounts) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (AccountAdapter.AccountItem item : accounts) {
                JSONObject obj = new JSONObject();
                obj.put("name", item.name);
                obj.put("number", item.number);
                obj.put("balance", item.balance);
                obj.put("currency", item.currency);
                obj.put("isTracked", item.isTracked);
                jsonArray.put(obj);


            }

            String jsonString = jsonArray.toString();

            m_sharedPreferences.edit().putString("cached_accounts", jsonString).apply();

            return jsonString;

        } catch (Exception e) {
            Log.e("TBankService", "Ошибка упаковки JSON (AppSettings.saveAccounts)", e);
        }

        return "";
    }
    public List<AccountAdapter.AccountItem> cachedAccounts() {
        String cachedJson = m_sharedPreferences.getString("cached_accounts", null);
        if (cachedJson != null) {
            return parseJsonToAdapterList(cachedJson);
        }

        return Collections.emptyList();
    }

    // Вспомогательный метод для распаковки JSON строки
    public static List<AccountAdapter.AccountItem> parseJsonToAdapterList(String jsonStr) {
        List<AccountAdapter.AccountItem> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                list.add(new AccountAdapter.AccountItem(
                        obj.getString("name"),
                        obj.getString("number"),
                        obj.getDouble("balance"),
                        obj.getString("currency"),
                        obj.getBoolean("isTracked")
                ));
            }

            return list;
        } catch (Exception e) {
            Log.e("TBankService", "parseJsonToAdapterList: " + e.getMessage());
        }
        return list;
        }
}
