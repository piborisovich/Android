package com.example.tbankcontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final double BALANCE_THRESHOLD = BuildConfig.BALANCE_THRESHOLD;
    private TBankApiService apiService;
    private AccountAdapter adapter;
    //Ссылка на диалог
    private AlertDialog m_activeDialog = null;

    private final BroadcastReceiver serviceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null && "com.example.tbankcontrol.UPDATE_ACCOUNTS_ACTION".equals(intent.getAction())) {
                String jsonStr = intent.getStringExtra("accounts_json");

                if (jsonStr != null) {
                    // Распаковываем JSON обратно в список объектов для адаптера
                    List<AccountAdapter.AccountItem> newList = AppSettings.parseJsonToAdapterList(jsonStr);

                    // Передаем новый список в адаптер
                    if (adapter != null) {
                        adapter.setAccounts(newList);
                    }
                }
            }
        }
    };

    private void loadCachedData() {
        //Загрузка аккаунтов из КЭША
        List<AccountAdapter.AccountItem> cachedAccounts = AppSettings.getInstance().cachedAccounts();

        for (AccountAdapter.AccountItem item: cachedAccounts){
            item.isTracked = AppSettings.getInstance().
                    isNotificationEnabled(item.number);
        }

        adapter.setAccounts(cachedAccounts);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        AppSettings.init(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        androidx.constraintlayout.widget.ConstraintLayout mainLayout = findViewById(R.id.main_layout);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                mainLayout,
                (v, windowInsets) -> {
                    androidx.core.graphics.Insets systemBars = windowInsets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    );

                    // Устанавливаем внутренние отступы (padding) для всего экрана
                    // systemBars.bottom — это высота нижней панели "Домой"
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return windowInsets;
                });

        RecyclerView accountsView = findViewById(R.id.accountsRecyclerView);
        Button refreshButton = findViewById(R.id.refreshButton);

        accountsView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AccountAdapter(BALANCE_THRESHOLD);
        accountsView.setAdapter(adapter);

        adapter.setOnTrackStatusChangeListener(new AccountAdapter.OnTrackStatusChangeListener() {
            @Override
            public void onTrackStatusChanged(String accountNumber, boolean isChecked) {
                AppSettings.getInstance().setNotificationEnabled(accountNumber, isChecked);
            }
        });

        apiService = new TBankApiService(this);

        refreshButton.setOnClickListener(v -> loadAccountData());

        // ЗАПУСК ПОСТОЯННОЙ СЛУЖБЫ ПЕРИОДИЧЕСКОГО ОБНОВЛЕНИЯ
        Intent serviceIntent = new Intent(this, TBankForegroundService.class);
        serviceIntent.putExtra("BALANCE_THRESHOLD", BALANCE_THRESHOLD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            stopTbankService();
        }
    }

    private void stopTbankService() {
        Intent serviceIntent = new Intent(this, TBankForegroundService.class);
        stopService(serviceIntent); // Отправляет сервису команду завершения
        Log.d("TBankService", "MainActivity: Вызвана остановка сервиса");
    }

    private void loadAccountData() {

        apiService.fetchBankAccounts(new TBankApiService.ApiCallback() {
            @Override
            public void onSuccess(java.util.List<AccountAdapter.AccountItem> accountsList) {
                for (AccountAdapter.AccountItem item: accountsList){
                    item.isTracked = AppSettings.getInstance().
                            isNotificationEnabled(item.number);
                }
                adapter.setAccounts(accountsList);
            }

            @Override
            public void onError(String errorMessage) {

                runOnUiThread(() -> {
                    if ( isFinishing() || isDestroyed() ) return;
                    // Если диалог уже открыт
                    if ( m_activeDialog != null && m_activeDialog.isShowing() ) {
                        m_activeDialog.setMessage(errorMessage);
                        return;
                    }

                    View rootView = findViewById(R.id.main_layout);

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setTitle("Ошибка") // Заголовок окна
                            .setMessage(errorMessage) // Текст сообщения
                            .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    // Код, который выполнится при нажатии на ОК
                                    dialog.dismiss(); // Закрыть окно
                                }
                            });

                    // Создаем и показываем окно
                    m_activeDialog = builder.create();
                    m_activeDialog.show();
                });


                Log.e("TBankService", "MainActivity: ОnError: " + errorMessage);
            }
        });
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        //Привязываем приемник к нашему уникальному системному экшену
        IntentFilter filter = new android.content.IntentFilter("com.example.tbankcontrol.UPDATE_ACCOUNTS_ACTION");

        // Для Android 14+ требуется явно указать флаг RECEIVER_EXPORTED или RECEIVER_NOT_EXPORTED
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceUpdateReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serviceUpdateReceiver, filter);
        }

        //Сразу же загружаем данные из SharedPreferences (чтобы не показывать пустой экран при старте)
        loadCachedData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Обязательно отписываемся от получения сообщений, когда MainActivity уходит с экрана
        unregisterReceiver(serviceUpdateReceiver);
    }
}