package com.example.tbankcontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {
    private static final double BALANCE_THRESHOLD = BuildConfig.BALANCE_THRESHOLD;
    // Класс-модель для хранения данных одного счета
    public static class AccountItem {
        String name;
        String number;
        String currency;
        double balance;

        public AccountItem(String name, String number, double balance, String currency) {
            this.name = name;
            this.number = number;
            this.balance = balance;
            this.currency = currency;
        }

        public String balanceString() {
            return String.format(Locale.getDefault(),
                    "%,.2f %s", balance, currency);
        }
    }

    private final List<AccountItem> accountList = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    public void setAccounts(List<AccountItem> newList) {
        accountList.clear();
        accountList.addAll(newList);
        notifyDataSetChanged(); // Перерисовывает список при обновлении данных
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
        holder.tvNumber.setText(context.getString(R.string.CardNumberPrefix, item.number));
        holder.tvBalance.setText(item.balanceString());

        if ( item.balance < BALANCE_THRESHOLD ) {
            ( (MaterialCardView) holder.itemView ).setCardBackgroundColor(0xFFFFEBEE);
        } else {
            ( (MaterialCardView) holder.itemView ).setCardBackgroundColor(0xFFFFFFFF);
        }


    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvNumber, tvBalance;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAccountName);
            tvNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvBalance = itemView.findViewById(R.id.tvAccountBalance);
        }
    }
}
