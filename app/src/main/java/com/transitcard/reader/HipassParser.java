package com.transitcard.reader;

import android.nfc.tech.IsoDep;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class HipassParser implements CardParser {
    private static final String TAG = "HipassParser";

    // Secondary AID 선택 명령어
    private static final byte[] CMD_SELECT_SECONDARY_AID = {
            0x00, (byte) 0xA4, 0x04, 0x00, 0x07,
            (byte) 0xA0, 0x00, 0x00, 0x02, 0x45, 0x00, 0x01,
            0x00
    };

    // 하이패스 명령어
    private static final byte[] CMD_BALANCE_HIPASS = {(byte) 0x90, 0x5C, 0x00, 0x00, 0x04};
    private static final byte[] CMD_CARDINFO_HIPASS = {0x00, (byte) 0xB0, (byte) 0x88, 0x00, 0x0C};

    // 거래내역 SFI 값들
    private static final byte[] SFI_VALUES = {0x14, 0x1C, 0x24, 0x2C, 0x34};
    private static final byte LE_RECORD = 0x24;  // 36 bytes

    private byte[] secondaryAidResponse = null;

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

            // 2. Secondary AID 선택 (잔액/거래내역 읽기 필요)
            selectSecondaryAid(isoDep);

            // 3. Secondary AID 응답에서 카드번호 추출 시도
            if (cardNumber == null) {
                cardNumber = readCardNumberFromSecondaryAid();
            }

            // 4. CARDINFO 명령으로 시도
            if (cardNumber == null) {
                cardNumber = readCardNumberFromCardInfo(isoDep);
            }

            // 5. 최종적으로 Card ID 사용
            if (cardNumber == null) {
                cardNumber = bytesToHex(cardId);
                Log.w(TAG, "Using Card ID as card number");
            }

            // 잔액 및 거래내역 읽기
            int balance = readBalance(isoDep);
            List<Transaction> transactions = readTransactionHistory(isoDep);

            return new TransitCardData(CardType.HIPASS, cardNumber, balance, transactions);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Hipass card", e);
            return new TransitCardData(CardType.HIPASS, bytesToHex(cardId), 0, new ArrayList<>());
        }
    }

    private boolean selectSecondaryAid(IsoDep isoDep) {
        try {
            byte[] response = isoDep.transceive(CMD_SELECT_SECONDARY_AID);
            Log.d(TAG, "Secondary AID response: " + bytesToHex(response));

            if (response != null && response.length >= 2) {
                int sw1 = response[response.length - 2] & 0xFF;
                int sw2 = response[response.length - 1] & 0xFF;

                if (response.length > 2) {
                    secondaryAidResponse = response;
                }

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
            byte[] response = isoDep.transceive(CMD_BALANCE_HIPASS);

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

    private String readCardNumberFromSecondaryAid() {
        if (secondaryAidResponse != null) {
            String cardNum = extractCardNumberFromFCI(secondaryAidResponse);
            if (cardNum != null) {
                Log.i(TAG, "Card number found from Secondary AID");
                return cardNum;
            }
        }
        return null;
    }

    private String readCardNumberFromCardInfo(IsoDep isoDep) {
        try {
            byte[] response = isoDep.transceive(CMD_CARDINFO_HIPASS);

            if (response.length >= 14 && isSuccess(response)) {
                String cardNum = formatBcdCardNumber(response, 0, 8);
                if (isValidCardNumber(cardNum)) {
                    Log.i(TAG, "Card number found from CARDINFO");
                    return cardNum;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "readCardNumberFromCardInfo error", e);
        }
        return null;
    }

    private String extractCardNumberFromFCI(byte[] data) {
        if (data == null || data.length < 10) return null;

        int length = data.length - 2;
        int sw1 = data[data.length - 2] & 0xFF;

        if (sw1 != 0x90 && sw1 != 0x62) return null;

        if ((data[0] & 0xFF) == 0x6F) {
            // 태그 13 (Application Primary Account Number) 찾기
            for (int i = 0; i < length - 2; i++) {
                if ((data[i] & 0xFF) == 0x13) {
                    int cardNumLength = data[i + 1] & 0xFF;

                    if (cardNumLength == 8 && i + 2 + cardNumLength <= length) {
                        String cardNum = formatBcdCardNumber(data, i + 2, cardNumLength);
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

        for (byte sfi : SFI_VALUES) {
            for (int record = 1; record <= 10; record++) {
                try {
                    byte[] cmd = {0x00, (byte) 0xB2, (byte) record, sfi, LE_RECORD};
                    byte[] response = isoDep.transceive(cmd);

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

            if (!transactions.isEmpty()) {
                Log.i(TAG, "Found " + transactions.size() + " transactions in SFI 0x" +
                        String.format("%02X", sfi));
                break;
            }
        }

        return transactions;
    }

    private Transaction parseTransactionRecord(byte[] data) {
        if (data == null || data.length < 20) return null;

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
            // 거래 시퀀스 번호 (offset 7)
            int seqNum = data[7] & 0xFF;

            // 거래 금액 (offset 9-10, Big Endian)
            int amount = ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);

            // 거래 후 잔액 (offset 13-14, Big Endian)
            int balance = ((data[13] & 0xFF) << 8) | (data[14] & 0xFF);

            // 거래 타입 (offset 16: 04=충전, 05=사용)
            int txTypeCode = data[16] & 0xFF;

            // 유효성 검사
            if (amount <= 0 || amount > 500000) return null;
            if (balance < 0 || balance > 500000) return null;

            // 거래 타입 판별
            TransactionType txType;
            String location;

            if (txTypeCode == 0x04) {
                // 충전 확정 (로그로 확인됨)
                txType = TransactionType.CHARGE;
                location = "충전";
            } else {
                // 사용으로 추정 (실제 확인 필요)
                txType = TransactionType.USE;
                location = "하이패스";
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