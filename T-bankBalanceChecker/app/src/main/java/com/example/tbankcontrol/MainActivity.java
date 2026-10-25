package com.example.tbankcontrol;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final double BALANCE_THRESHOLD = BuildConfig.BALANCE_THRESHOLD;
    private TBankApiService apiService;
    private AccountAdapter adapter;
    // Переменные для управления периодическим обновлением
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    // Интервал обновления в миллисекундах (30 000 мс = 30 секунд)
    private static final long UPDATE_INTERVAL = 30000;
    //Ссылка на диалог
    private AlertDialog m_activeDialog = null;

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

        // Создаем задачу, которая будет выполняться циклически
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                //Вызываем метод загрузки данных
                loadAccountData();
                //Планируем следующий запуск этой же задачи через заданный интервал
                timerHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    private void loadAccountData() {

        apiService.fetchBankAccounts(new TBankApiService.ApiCallback() {
            @Override
            public void onSuccess(java.util.List<AccountAdapter.AccountItem> accountsList) {

                //Считываем состояние из файла (если было сохранено)
                for (AccountAdapter.AccountItem item : accountsList) {
                    item.isTracked = AppSettings.getInstance().
                            isNotificationEnabled(item.number);

                    if (item.isTracked) {
                        NotificationHelper.showNotification(MainActivity.this,
                                getString(R.string.NotificationTitle),
                                getString(R.string.NotificationPrefix, item.shortNumber));
                    }
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


                Log.e("MyServiceTAG", "MainActivity: ОnError: " + errorMessage);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Запускаем таймер
        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ОБЯЗАТЕЛЬНО останавливаем таймер, когда приложение сворачивают,
        // чтобы оно не выполняло сетевые запросы впустую и не тратило батарею
        //timerHandler.removeCallbacks(timerRunnable);
    }
}