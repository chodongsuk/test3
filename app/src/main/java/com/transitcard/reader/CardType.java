package com.transitcard.reader;

public enum CardType {
    TMONEY("티머니 (T-money)"),
    HANPAY("한페이 (Hanpay)"),
    RAILPLUS("레일플러스 (Rail+)"),
    HIPASS ("하이패스 (Hi-pass)"),
    EZL("이즐 (EZL)"),
    UNKNOWN("알 수 없는 카드");

    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
