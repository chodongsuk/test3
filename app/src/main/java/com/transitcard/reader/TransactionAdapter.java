package com.transitcard.reader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
    private final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.KOREA);

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView typeTextView;
        private final TextView locationTextView;
        private final TextView dateTextView;
        private final TextView amountTextView;
        private final TextView balanceTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            typeTextView = itemView.findViewById(R.id.transactionTypeTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            balanceTextView = itemView.findViewById(R.id.balanceTextView);
        }

        public void bind(Transaction transaction) {
            // 거래 유형
            TransactionType type = transaction.getTransactionType();
            String typeDisplay;
            int amount = transaction.getAmount();
            String amountStr;


            if (type == TransactionType.USE) {
                typeDisplay = "사용";
            } else if (type == TransactionType.CHARGE) {
                typeDisplay = "충전";
            } else {
                typeDisplay = "알 수 없음";
            }
            typeTextView.setText(typeDisplay);

            // 위치/장소
            String location = transaction.getLocation();
            locationTextView.setText(location != null ? location : "");

            // 날짜 - date 필드 사용 (이미 포맷된 문자열)
            String dateStr = transaction.getDate();
            if (dateStr != null && !dateStr.isEmpty()) {
                dateTextView.setText(dateStr);
            } else {
                // date가 없으면 timestamp 사용
               dateTextView.setText(dateFormat.format(new Date(transaction.getTimestamp())));
               // dateTextView.setText("");
            }

            if (type == TransactionType.CHARGE) {
                // 충전 - 파란색
                amountStr = "+" + currencyFormat.format(amount) + "원";
                amountTextView.setTextColor(itemView.getContext().getColor(android.R.color.holo_blue_dark));
            } else {
                // 사용 - 빨간색
                amountStr = currencyFormat.format(amount) + "원";
                amountTextView.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
            }

            amountTextView.setText(amountStr);

            // 잔액
            balanceTextView.setText("잔액 " + currencyFormat.format(transaction.getBalanceAfter()) + "원");
        }
    }
}