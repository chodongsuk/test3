package com.transitcard.reader;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.transitcard.reader.CardPagerAdapter;
import com.transitcard.reader.AppDatabase;
import com.transitcard.reader.CardDao;
import com.transitcard.reader.CardEntity;
import com.transitcard.reader.CardWithTransactions;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // NFC 관련
    private NfcAdapter nfcAdapter;
    private NFCReader nfcReader;
    private PendingIntent pendingIntent;

    // Database 관련
    private AppDatabase database;
    private CardDao cardDao;

    // UI 관련
    private TextView statusTextView;
    private TextView scanInstructionTextView;
    private TextView emptyStateTextView;
    private ViewPager2 cardViewPager;
    private CardPagerAdapter cardAdapter;

    // 이용내역 관련
    private LinearLayout transactionSection;
    private RecyclerView transactionRecyclerView;
    private TextView emptyTransactionTextView;
    private TransactionAdapter transactionAdapter;

    private List<CardWithTransactions> currentCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: MainActivity 시작");
        setContentView(R.layout.activity_main);

        // Database 초기화
        initDatabase();
        initNFC();
        initViews();
        setupViewPager();
        setupTransactionRecyclerView();
        checkNfcAvailability();
        createPendingIntent();
        observeCards();
        handleIntent(getIntent());
    }

    private void initDatabase() {
        database = AppDatabase.getInstance(this);
        cardDao = database.cardDao();
        Log.d(TAG, "Database 초기화 완료");
    }

    private void initNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcReader = new NFCReader();
    }

    private void initViews() {
        statusTextView = findViewById(R.id.statusTextView);
        scanInstructionTextView = findViewById(R.id.scanInstructionTextView);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        cardViewPager = findViewById(R.id.cardViewPager);
        transactionSection = findViewById(R.id.transactionSection);
        transactionRecyclerView = findViewById(R.id.transactionRecyclerView);
        emptyTransactionTextView = findViewById(R.id.emptyTransactionTextView);
    }

    private void setupViewPager() {
        cardAdapter = new CardPagerAdapter();
        cardViewPager.setAdapter(cardAdapter);
        cardAdapter.setOnCardDeleteListener(this::showDeleteConfirmDialog);

        // ViewPager2 페이지 간격 및 미리보기 효과 설정 - 카드가 80% 이상 차지하도록 조정
        cardViewPager.setOffscreenPageLimit(1);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        // 양옆 마진을 최소화 (5dp 정도만)
        compositePageTransformer.addTransformer(new MarginPageTransformer(5));
        compositePageTransformer.addTransformer((page, position) -> {
            // 스케일 효과도 최소화하여 현재 카드가 거의 꽉 차게
            float scaleFactor = 0.95f + (1 - Math.abs(position)) * 0.05f;
            page.setScaleY(scaleFactor);
        });

        cardViewPager.setPageTransformer(compositePageTransformer);

        // 페이지 변경 리스너 - 선택된 카드의 이용내역 표시
        cardViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTransactionList(position);
            }
        });
    }

    private void setupTransactionRecyclerView() {
        transactionAdapter = new TransactionAdapter();
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionRecyclerView.setAdapter(transactionAdapter);
    }

    private void createPendingIntent() {
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
    }

    private void observeCards() {
        cardDao.getAllCardsWithTransactions().observe(this, cards -> {
            Log.d(TAG, "카드 목록 변경: " + (cards != null ? cards.size() : 0) + "개");

            currentCards = cards;

            if (cards != null && !cards.isEmpty()) {
                cardAdapter.setCards(cards);
                cardViewPager.setVisibility(View.VISIBLE);
                transactionSection.setVisibility(View.VISIBLE);
                emptyStateTextView.setVisibility(View.GONE);
                scanInstructionTextView.setVisibility(View.GONE);

                // 첫 번째 카드의 이용내역 표시
                updateTransactionList(cardViewPager.getCurrentItem());
            } else {
                cardViewPager.setVisibility(View.GONE);
                transactionSection.setVisibility(View.GONE);
                emptyStateTextView.setVisibility(View.VISIBLE);
                scanInstructionTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateTransactionList(int position) {
        if (currentCards == null || position < 0 || position >= currentCards.size()) {
            return;
        }

        CardWithTransactions selectedCard = currentCards.get(position);
        List<Transaction> transactions = selectedCard.transactions;

        if (transactions != null && !transactions.isEmpty()) {
            transactionAdapter.setTransactions(transactions);
            transactionRecyclerView.setVisibility(View.VISIBLE);
            emptyTransactionTextView.setVisibility(View.GONE);
        } else {
            transactionRecyclerView.setVisibility(View.GONE);
            emptyTransactionTextView.setVisibility(View.VISIBLE);
        }
    }

    // ==================== NFC 관련 ====================

    private void checkNfcAvailability() {
        if (nfcAdapter == null) {
            showStatus("NFC를 지원하지 않는 기기입니다");
            scanInstructionTextView.setVisibility(View.GONE);
        } else if (!nfcAdapter.isEnabled()) {
            showStatus("NFC를 켜주세요");
        } else {
            hideStatus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
            hideStatus();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                readCard(tag);
            }
        }
    }

    private void readCard(Tag tag) {
        Log.d(TAG, "카드 읽기 시작");
        showStatus("카드를 읽고 있습니다...");

        new Thread(() -> {
            TransitCardData cardData = null;
            try {
                cardData = nfcReader.readCard(tag);
            } catch (Exception e) {
                Log.e(TAG, "카드 읽기 오류", e);
            }

            final TransitCardData finalCardData = cardData;
            runOnUiThread(() -> {
                hideStatus();
                if (finalCardData != null) {
                    saveOrUpdateCard(finalCardData);
                    Toast.makeText(this, "카드 인식 완료!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "카드를 읽을 수 없습니다", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // ==================== Database 관련 ====================

    private void saveOrUpdateCard(TransitCardData cardData) {
        new Thread(() -> {
            try {
//                CardEntity existingCard = cardDao.getCardByNumber(cardData.getCardNumber());
//
//                if (existingCard != null) {
//                    // 기존 카드 업데이트
//                    updateCard(existingCard, cardData);
//                } else {
//                    // 새 카드 추가
//                    insertCard(cardData);
//                }

                insertCard(cardData);
            } catch (Exception e) {
                Log.e(TAG, "카드 저장 오류", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void updateCard(CardEntity existingCard, TransitCardData cardData) {
        Log.d(TAG, "카드 업데이트: " + existingCard.getCardNumber());

        // 1. 카드 정보 업데이트
        existingCard.setBalance(cardData.getBalance());
        existingCard.setLastUpdated(System.currentTimeMillis());
        cardDao.updateCard(existingCard);

        // 2. 거래내역 완전 교체 (핵심!)
        Log.d(TAG, "기존 거래내역 삭제");
        cardDao.deleteTransactionsByCardId(existingCard.getId());

        Log.d(TAG, "새 거래내역 추가");
        saveTransactions(existingCard.getId(), cardData.getTransactionHistory());

        runOnUiThread(() ->
                Toast.makeText(this, "카드 정보 업데이트 완료", Toast.LENGTH_SHORT).show()
        );
    }

    private void insertCard(TransitCardData cardData) {
        Log.d(TAG, "새 카드 추가: " + cardData.getCardNumber());

        // 1. 카드 추가
        CardEntity newCard = new CardEntity(
                cardData.getCardNumber(),
                cardData.getCardType().getDisplayName(),
                cardData.getBalance()
        );
        long cardId = cardDao.insertCard(newCard);

        // 2. 거래내역 추가
        saveTransactions((int) cardId, cardData.getTransactionHistory());

        runOnUiThread(() -> {
            Toast.makeText(this, "새 카드 등록 완료", Toast.LENGTH_SHORT).show();
            cardViewPager.setCurrentItem(0, true);
        });
    }

    private void saveTransactions(int cardId, List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            Log.d(TAG, "거래내역 없음");
            return;
        }

        // 각 거래에 cardId 설정
        for (Transaction transaction : transactions) {
            transaction.setCardId(cardId);
        }

        // 전부 삽입
        cardDao.insertTransactions(transactions);
        Log.d(TAG, "거래내역 저장 완료: " + transactions.size() + "개");
    }

    // ==================== 카드 삭제 ====================

    private void showDeleteConfirmDialog(CardWithTransactions card) {
        new AlertDialog.Builder(this)
                .setTitle("카드 삭제")
                .setMessage("이 카드를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteCard(card))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteCard(CardWithTransactions card) {
        new Thread(() -> {
            try {
                cardDao.deleteCard(card.card);
                // CASCADE로 거래내역도 자동 삭제됨!
                Log.d(TAG, "카드 삭제 완료");

                runOnUiThread(() ->
                        Toast.makeText(this, "카드 삭제 완료", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                Log.e(TAG, "카드 삭제 오류", e);
            }
        }).start();
    }

    // ==================== UI 헬퍼 ====================

    private void showStatus(String message) {
        statusTextView.setText(message);
        statusTextView.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusTextView.setVisibility(View.GONE);
    }
}