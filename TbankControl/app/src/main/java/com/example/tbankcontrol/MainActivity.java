package com.example.tbankcontrol;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private TextView balanceTextView;
    private TBankApiService apiService;

    // Переменные для управления периодическим обновлением
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    // Интервал обновления в миллисекундах (30 000 мс = 30 секунд)
    private static final long UPDATE_INTERVAL = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        balanceTextView = findViewById(R.id.balanceTextView);
        Button refreshButton = findViewById(R.id.refreshButton);

        apiService = new TBankApiService(this);

        refreshButton.setOnClickListener(v -> loadAccountData());

        // Создаем задачу, которая будет выполняться циклически
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // 1. Вызываем метод загрузки данных
                loadAccountData();

                // 2. Планируем следующий запуск этой же задачи через заданный интервал
                timerHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    private void loadAccountData() {
        balanceTextView.setText("Загрузка данных...");

        apiService.fetchBankAccounts(new TBankApiService.ApiCallback() {
            @Override
            public void onSuccess(String accountDetails) {
                balanceTextView.setText(accountDetails);
            }

            @Override
            public void onError(String errorMessage) {
                balanceTextView.setText("Не удалось получить данные");

                View rootView = findViewById(R.id.main_layout);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("Ошибка") // Заголовок окна
                        .setMessage(errorMessage) // Текст сообщения
                        .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Код, который выполнится при нажатии на ОК (можно оставить пустым)
                                dialog.dismiss(); // Закрыть окно
                            }
                        });

                // Создаем и показываем окно
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Запускаем таймер, когда приложение становится видимым пользователю
        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ОБЯЗАТЕЛЬНО останавливаем таймер, когда приложение сворачивают,
        // чтобы оно не выполняло сетевые запросы впустую и не тратило батарею
        timerHandler.removeCallbacks(timerRunnable);
    }
}