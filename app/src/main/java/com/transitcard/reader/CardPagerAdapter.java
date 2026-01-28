package com.transitcard.reader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.transitcard.reader.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CardPagerAdapter - ViewPager2용 어댑터
 *
 * 역할:
 * - ViewPager2에서 카드를 좌우로 스와이프하며 보여주는 어댑터
 * - 각 페이지에 카드 정보만 표시 (거래내역은 MainActivity에서 별도 표시)
 * - 카드 삭제 버튼 처리
 */
public class CardPagerAdapter extends RecyclerView.Adapter<CardPagerAdapter.CardViewHolder> {

    private List<CardWithTransactions> cards = new ArrayList<>();
    private OnCardDeleteListener deleteListener;

    public interface OnCardDeleteListener {
        void onCardDelete(CardWithTransactions card);
    }

    public void setOnCardDeleteListener(OnCardDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setCards(List<CardWithTransactions> cards) {
        this.cards = cards;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_page, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardWithTransactions cardWithTrans = cards.get(position);
        holder.bind(cardWithTrans);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    /**
     * CardViewHolder - 카드 정보만 표시
     */
    class CardViewHolder extends RecyclerView.ViewHolder {
        private TextView cardTypeTextView;
        private TextView cardNumberTextView;
        private TextView balanceTextView;
        private Button deleteButton;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTypeTextView = itemView.findViewById(R.id.cardTypeTextView);
            cardNumberTextView = itemView.findViewById(R.id.cardNumberTextView);
            balanceTextView = itemView.findViewById(R.id.balanceTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(CardWithTransactions cardWithTrans) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

            // 카드 정보 표시
            cardTypeTextView.setText(cardWithTrans.card.getCardType());
            cardNumberTextView.setText(formatCardNumber(cardWithTrans.card.getCardNumber()));
            balanceTextView.setText(numberFormat.format(cardWithTrans.card.getBalance()) + "원");

            // 삭제 버튼 클릭 리스너
            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onCardDelete(cardWithTrans);
                }
            });
        }

        private String formatCardNumber(String cardNumber) {
            String digitsOnly = cardNumber.replace(" ", "");

            if (digitsOnly.length() >= 16) {
                return digitsOnly.substring(0, 4) + " " +
                        digitsOnly.substring(4, 8) + " " +
                        digitsOnly.substring(8, 12) + " " +
                        digitsOnly.substring(12, 16);
            } else if (digitsOnly.length() > 8) {
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digitsOnly.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digitsOnly.charAt(i));
                }
                return formatted.toString();
            }
            return cardNumber;
        }
    }
}
