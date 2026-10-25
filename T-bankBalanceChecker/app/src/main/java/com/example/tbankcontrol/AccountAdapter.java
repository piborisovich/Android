package com.example.tbankcontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    AccountAdapter(double balanceThreshold) {
        super();
        this.m_balanceThreshold = balanceThreshold;
    }
    public interface OnTrackStatusChangeListener {
        void onTrackStatusChanged(String accountNumber, boolean isChecked);
    }

    private OnTrackStatusChangeListener m_listener;
    private double m_balanceThreshold;

    // Класс-модель для хранения данных одного счета
    public static class AccountItem {
        String name;
        String number;
        String shortNumber;
        String currency;
        double balance;
        boolean isTracked;

        public AccountItem(String name, String number, double balance, String currency) {
            this.name = name;
            this.number = number;
            this.shortNumber = tail(number, 4);
            this.balance = balance;
            this.currency = currency;
            this.isTracked = false;
        }

        public String balanceString() {
            return String.format(Locale.getDefault(),
                    "%,.2f %s", balance, currency);
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

    private final List<AccountItem> accountList = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    public void setAccounts(List<AccountItem> newList) {
        accountList.clear();
        accountList.addAll(newList);

        notifyDataSetChanged(); // Перерисовывает список при обновлении данных
    }

    public void setOnTrackStatusChangeListener(OnTrackStatusChangeListener listener) {
        this.m_listener = listener;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        AccountItem item = accountList.get(position);
        Context context = holder.itemView.getContext();
        holder.tvName.setText(item.name);
        holder.tvNumber.setText(context.getString(R.string.CardNumberPrefix, item.shortNumber));
        holder.tvBalance.setText(item.balanceString());

        if ( item.balance < m_balanceThreshold ) {
            ( (MaterialCardView) holder.itemView ).setCardBackgroundColor(0xFFFFEBEE);
        } else {
            ( (MaterialCardView) holder.itemView ).setCardBackgroundColor(0xFFFFFFFF);
        }

        holder.accountTracked.setOnCheckedChangeListener(null); // Отключаем старый слушатель перед изменением состояния
        holder.accountTracked.setChecked(item.isTracked);

        holder.accountTracked.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isTracked = isChecked;

            if (m_listener != null) {
                m_listener.onTrackStatusChanged(item.number, isChecked);
            }

        });

    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvNumber, tvBalance;
        SwitchCompat accountTracked;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAccountName);
            tvNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvBalance = itemView.findViewById(R.id.tvAccountBalance);
            accountTracked = itemView.findViewById(R.id.switchAccount);
        }
    }
}
