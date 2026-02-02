package com.transitcard.reader;

import android.nfc.tech.IsoDep;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class RailplusParser implements CardParser {
    private static final String TAG = "RailplusParser";

    // Secondary AID 선택 명령어 (레일플러스 AID)
    private static final byte[] CMD_SELECT_SECONDARY_AID = {
            0x00, (byte) 0xA4, 0x04, 0x00, 0x08,
            (byte) 0xD4, 0x10, 0x00, 0x00, 0x29, 0x00, 0x00, 0x01,
            0x00
    };

    // 레일플러스 명령어
    private static final byte[] CMD_BALANCE_RAILPLUS = {(byte) 0x90, 0x4C, 0x00, 0x00, 0x04};

    // 거래내역이 저장된 SFI
    private static final byte TRANSACTION_SFI = 0x74;

    @Override
    public TransitCardData parse(IsoDep isoDep, byte[] cardId) {
        return parse(isoDep, cardId, null);
    }

    public TransitCardData parse(IsoDep isoDep, byte[] cardId, byte[] primaryFci) {
        try {
            // 1. Primary FCI에서 카드번호 추출
            String cardNumber = null;
            if (primaryFci != null) {
                cardNumber = extractCardNumberFromFCI(primaryFci);
                if (cardNumber != null) {
                    Log.i(TAG, "Card number found from primary FCI: " + cardNumber);
                }
            }

            // 2. 최종적으로 Card ID 사용
            if (cardNumber == null) {
                cardNumber = bytesToHex(cardId);
                Log.w(TAG, "Using Card ID as card number");
            }

            // 3. Secondary AID 선택
            selectSecondaryAid(isoDep);

            // 4. 잔액 및 거래내역 읽기
            int balance = readBalance(isoDep);
            List<Transaction> transactions = readTransactionHistory(isoDep);

            return new TransitCardData(CardType.RAILPLUS, cardNumber, balance, transactions);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Railplus card", e);
            return new TransitCardData(CardType.RAILPLUS, bytesToHex(cardId), 0, new ArrayList<>());
        }
    }

    private boolean selectSecondaryAid(IsoDep isoDep) {
        try {
            byte[] response = isoDep.transceive(CMD_SELECT_SECONDARY_AID);
            Log.d(TAG, "Secondary AID response: " + bytesToHex(response));

            if (response != null && response.length >= 2) {
                int sw1 = response[response.length - 2] & 0xFF;
                int sw2 = response[response.length - 1] & 0xFF;

                if (sw1 == 0x90 || sw1 == 0x62) {
                    Log.d(TAG, "Secondary AID selected: " + String.format("%02X%02X", sw1, sw2));
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "selectSecondaryAid error", e);
            return false;
        }
    }

    private int readBalance(IsoDep isoDep) {
        try {
            byte[] response = isoDep.transceive(CMD_BALANCE_RAILPLUS);

            if (response.length >= 6 && isSuccess(response)) {
                int balance = ((response[0] & 0xFF) << 24) |
                        ((response[1] & 0xFF) << 16) |
                        ((response[2] & 0xFF) << 8) |
                        (response[3] & 0xFF);
                Log.i(TAG, "Balance: " + balance + "원");
                return balance;
            }
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "readBalance error", e);
            return 0;
        }
    }

    private String extractCardNumberFromFCI(byte[] data) {
        if (data == null || data.length < 10) return null;

        int length = data.length - 2;
        int sw1 = data[data.length - 2] & 0xFF;

        if (sw1 != 0x90 && sw1 != 0x62) return null;

        if ((data[0] & 0xFF) == 0x6F) {
            // 태그 12 (Card Number) 찾기
            for (int i = 0; i < length - 2; i++) {
                if ((data[i] & 0xFF) == 0x12) {
                    int tagLength = data[i + 1] & 0xFF;

                    if (tagLength >= 8 && i + 2 + 8 <= length) {
                        String cardNum = formatBcdCardNumber(data, i + 2, 8);
                        if (isValidCardNumber(cardNum)) {
                            return cardNum;
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<Transaction> readTransactionHistory(IsoDep isoDep) {
        List<Transaction> transactions = new ArrayList<>();

        for (int record = 1; record <= 20; record++) {
            try {
                byte[] cmd = {0x00, (byte) 0xB2, (byte) record, TRANSACTION_SFI, 0x00};
                byte[] response = isoDep.transceive(cmd);

                String hexResponse = bytesToHex(response);
                Log.i(TAG, hexResponse);

                Transaction tx = parseTransactionRecord(response);

                if (tx != null) {
                    transactions.add(tx);
                } else if (response != null && response.length >= 2) {
                    int sw1 = response[response.length - 2] & 0xFF;
                    if (sw1 == 0x6A) break;  // No more records
                }
            } catch (Exception e) {
                break;
            }
        }

        Log.i(TAG, "Found " + transactions.size() + " transactions");
        return transactions;
    }

    private Transaction parseTransactionRecord(byte[] data) {
        if (data == null || data.length < 22) return null;

        int sw1 = data[data.length - 2] & 0xFF;
        int sw2 = data[data.length - 1] & 0xFF;
        if (sw1 != 0x90 || sw2 != 0x00) return null;

        int dataLength = data.length - 2;

        // 빈 레코드 체크
        boolean isEmpty = true;
        for (int i = 0; i < Math.min(16, dataLength); i++) {
            if (data[i] != 0 && (data[i] & 0xFF) != 0xFF) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) return null;

        try {
            // offset 0: 거래 타입 (0x01=사용, 0x02=충전)
            int recordType = data[0] & 0xFF;

            // 거래 금액 (offset 2-5, Big Endian)
            int amount = ((data[2] & 0xFF) << 24) |
                    ((data[3] & 0xFF) << 16) |
                    ((data[4] & 0xFF) << 8) |
                    (data[5] & 0xFF);

            // 거래 후 잔액 (offset 10-13, Big Endian)
            int balance = ((data[10] & 0xFF) << 24) |
                    ((data[11] & 0xFF) << 16) |
                    ((data[12] & 0xFF) << 8) |
                    ((data[13] & 0xFF));

            // 유효성 검사
            if (amount <= 0 || amount > 500000) return null;
            if (balance < 0 || balance > 500000) return null;

            // 거래 타입 판별
            TransactionType txType;
            String location;

            // 금액 == 잔액이면 충전, 아니면 사용
            if (recordType == 0x02) {
                txType = TransactionType.CHARGE;
                location = "충전";
            } else {
                txType = TransactionType.USE;
                location = "사용";
            }

            Log.i(TAG, location + " | " + amount + "원 | 잔액: " + balance + "원");

            return new Transaction("", location, amount, balance, txType);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing transaction", e);
            return null;
        }
    }

    private boolean isSuccess(byte[] response) {
        if (response == null || response.length < 2) return false;
        return (response[response.length - 2] & 0xFF) == 0x90 &&
                (response[response.length - 1] & 0xFF) == 0x00;
    }

    private boolean isValidCardNumber(String cardNum) {
        if (cardNum == null) return false;
        String digits = cardNum.replace(" ", "");
        if (digits.length() < 16) return false;

        for (char c : digits.toCharArray()) {
            if (c < '0' || c > '9') return false;
        }

        for (char c : digits.toCharArray()) {
            if (c != '0') return true;
        }
        return false;
    }

    private String formatBcdCardNumber(byte[] data, int offset, int len) {
        if (offset + len > data.length - 2) return null;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < len; i++) {
            int high = (data[offset + i] >> 4) & 0x0F;
            int low = data[offset + i] & 0x0F;

            if (high > 9 || low > 9) return null;

            sb.append(high).append(low);
        }

        String raw = sb.toString();

        if (raw.length() >= 16) {
            return raw.substring(0, 4) + " " + raw.substring(4, 8) + " " +
                    raw.substring(8, 12) + " " + raw.substring(12, 16);
        }

        return null;
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}