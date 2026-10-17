package com.example.tbankcontrol;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.text.MessageFormat;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TBankApiService {
    private static final String REST_SERVICE = "UsersService";
    private static final String REST_FUNCTION = "GetBankAccounts";
    private static final String API_URL = MessageFormat.format(
            "https://invest-public-api.tbank.ru/" +
                    "rest/tinkoff.public.invest.api.contract.v1.{0}/{1}",
            REST_SERVICE,
            REST_FUNCTION);
    private static final String TOKEN = BuildConfig.INVEST_TOKEN;
    private final OkHttpClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public TBankApiService(Context context) {
        this.client = createTrustedClient(context);
    }

    // Метод для настройки OkHttp с поддержкой сертификатов Минцифры
    private OkHttpClient createTrustedClient(Context context) {
        try {
            // 1. Загружаем сертификат Минцифры из папки assets
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = context.getAssets().open("Russian_Trusted_Root_CA.cer");
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
            } finally {
                caInput.close();
            }

            // 2. Создаем KeyStore и добавляем туда наш сертификат
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // 3. Создаем TrustManager, который доверяет нашему KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // 4. Создаем SSLContext с использованием нашего TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // 5. Собираем OkHttpClient
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            // Если что-то пошло не так, возвращаем стандартный клиент (вызовет ошибку ssl)
            return new OkHttpClient();
        }
    }

    public interface ApiCallback {
        void onSuccess(String accountsInfo);
        void onError(String errorMessage);
    }

    public void fetchBankAccounts(ApiCallback callback) {
        executor.execute(() -> {
            // Тело POST-запроса (передаем пустой JSON-объект)
            RequestBody body = RequestBody.create(
                    "{}",
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> callback.onError("Ошибка сети: " + e.getMessage()));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (Response responseBody = response) {
                        if (!responseBody.isSuccessful()) {
                            mainHandler.post(() -> callback.onError("Код ошибки: " + responseBody.code()));
                            return;
                        }

                        String responseData = responseBody.body().string();
                        String parsedData = parseBankAccountsJson(responseData);

                        mainHandler.post(() -> callback.onSuccess(parsedData));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("Ошибка обработки данных:\n" +
                                e.getMessage()));
                    }
                }
            });
        });
    }

    /**
     * Парсинг ответа от GetBankAccounts
     * Ответ содержит массив "accounts", где у каждого счета есть "money" (объект MoneyValue)
     */
    private String parseBankAccountsJson(String jsonStr) throws Exception {
        JSONObject jsonObject = new JSONObject(jsonStr);

        if (!jsonObject.has("bankAccounts")) {
            return "Банковские счета не найдены.";
        }

        JSONArray accounts = jsonObject.getJSONArray("bankAccounts");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < accounts.length(); i++) {
            JSONObject account = accounts.getJSONObject(i);

            // Получаем базовую информацию о счете
            String cardName = account.optString("name", "Дебетовая карта");
            String accountId = account.optString("id", "Не указан");

            // Обработка баланса (MoneyValue: units + nano)
            double balance = 0.0;
            String currency = "₽";

            if (account.has("money")) {
                JSONArray moneyArr = account.getJSONArray("money");

                if ( moneyArr.length() != 0 ) {
                    JSONObject moneyObj = moneyArr.getJSONObject(0);
                    long units = moneyObj.optLong("units", 0);
                    int nano = moneyObj.optInt("nano", 0);
                    currency = getCurrencySymbol(moneyObj.optString("currency", "rub"));

                    // Переводим nano-части в double (1 nano = 10^-9)
                    balance = units + ((double) nano / 1_000_000_000.0);

                    result.append("💳 ").append(cardName).append("\n")
                            .append("Номер: *").append(tail(accountId, 4)).append("\n")
                            .append("Баланс: ").append(String.format(Locale.getDefault(), "%,.2f %s", balance, currency))
                            .append("\n\n────────────────\n\n");
                }
            }
        }

        return result.length() > 0 ? result.toString() : "Список счетов пуст.";
    }

    // Вспомогательный метод для отображения знака валюты
    private String getCurrencySymbol(String isoCode) {
        switch (isoCode.toLowerCase()) {
            case "rub": return "₽";
            case "usd": return "$";
            case "eur": return "€";
            default: return isoCode.toUpperCase();
        }
    }

    // Получение последних N символов номера счета для маскирования
    private String tail(String src) {
        return tail(src, 1);
    }

    // Получение последних N символов номера счета для маскирования
    private String tail(String src, int count) {
        if (src == null || src.length() <= count) return src;
        return src.substring(src.length() - count);
    }
}
