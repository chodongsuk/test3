package com.transitcard.reader;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

public class NFCReader {
    private static final String TAG = "NFCReader";

    // FCI 응답에서 찾을 AID (카드 구분용)
    private static final byte[] HIPASS_AID_IN_FCI = {
            (byte) 0xA0, 0x00, 0x00, 0x02, 0x45, 0x00, 0x01
    };
    private static final byte[] EZL_AID_IN_FCI = {
            (byte) 0xD4, 0x10, 0x00, 0x00, 0x14, 0x00, 0x01
    };

    // FCI 응답 저장용 (카드 구분에 사용)
    private byte[] lastFciResponse = null;

    public TransitCardData readCard(Tag tag) {
        Log.d(TAG, "=== Starting card read ===");

        try {
            byte[] cardId = tag.getId();
            Log.d(TAG, "Card ID: " + bytesToHex(cardId));

            // 태그가 지원하는 기술 목록 확인
            String[] techList = tag.getTechList();
            Log.d(TAG, "Tag supports " + techList.length + " technologies:");
            for (String tech : techList) {
                Log.d(TAG, "  - " + tech);
            }

            IsoDep isoDep = IsoDep.get(tag);
            if (isoDep == null) {
                Log.e(TAG, "IsoDep not available - card does not support ISO-DEP");
                Log.e(TAG, "This card cannot be read as a transit card");
                return null;
            }

            Log.d(TAG, "IsoDep obtained successfully");
            return readIsoDepCard(isoDep, cardId);

        } catch (Exception e) {
            Log.e(TAG, "Error reading card", e);
            e.printStackTrace();
            return null;
        }
    }

    private TransitCardData readIsoDepCard(IsoDep isoDep, byte[] cardId) {
        try {
            Log.d(TAG, "Connecting to card...");
            isoDep.connect();
            Log.d(TAG, "Connected to card successfully");

            isoDep.setTimeout(2000);
            Log.d(TAG, "Timeout set to 2000ms");

            // 카드 타입 감지
            Log.d(TAG, "Starting card type detection...");
            CardType cardType = detectCardType(isoDep);
            Log.i(TAG, "Detected card type: " + cardType);

            if (cardType == CardType.UNKNOWN) {
                Log.e(TAG, "Unknown card type - cannot proceed");
                isoDep.close();
                return null;
            }

            // 파서 선택 및 데이터 읽기
            TransitCardData result = null;
            Log.d(TAG, "Starting to parse card data...");

            switch (cardType) {
                case TMONEY:
                    Log.d(TAG, "Using TMoneyParser");
                    result = new TMoneyParser().parse(isoDep, cardId);
                    break;
                case EZL:
                    Log.d(TAG, "Using EZLParser");
                    result = new EZLParser().parse(isoDep, cardId);
                    break;
                case HIPASS:
                    Log.d(TAG, "Using HipassParser with primary FCI");
                    result = new HipassParser().parse(isoDep, cardId, lastFciResponse);
                    break;
                default:
                    Log.w(TAG, "Unsupported card type: " + cardType);
                    isoDep.close();
                    return null;
            }

            if (result != null) {
                Log.i(TAG, "Card data parsed successfully");
                Log.i(TAG, "  Type: " + result.getCardType());
                Log.i(TAG, "  Number: " + result.getCardNumber());
                Log.i(TAG, "  Balance: " + result.getBalance());
                Log.i(TAG, "  Transactions: " + (result.getTransactionHistory() != null ? result.getTransactionHistory().size() : 0));
            } else {
                Log.e(TAG, "Parser returned null - failed to parse card data");
            }

            isoDep.close();
            Log.d(TAG, "Connection closed");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error reading IsoDep card", e);
            e.printStackTrace();
            try {
                if (isoDep.isConnected()) {
                    isoDep.close();
                    Log.d(TAG, "Connection closed after error");
                }
            } catch (Exception closeError) {
                Log.e(TAG, "Error closing connection", closeError);
            }
            return null;
        }
    }

    private CardType detectCardType(IsoDep isoDep) {
        Log.d(TAG, "=== Detecting card type ===");

        // 1. T-Money AID 시도
        byte[] tmoneyAid = {(byte) 0xD4, 0x10, 0x00, 0x00, 0x03, 0x00, 0x01};
        if (trySelectAID(isoDep, tmoneyAid, "T-Money")) {
            return CardType.TMONEY;
        }

        // 2. KFTC AID 시도 (EZL 또는 하이패스)
        byte[] kftcAid = {(byte) 0xA0, 0x00, 0x00, 0x04, 0x52, 0x00, 0x01};
        if (trySelectAID(isoDep, kftcAid, "KFTC")) {
            // FCI 응답을 분석하여 하이패스와 EZL 구분
            if (lastFciResponse != null) {
                if (containsAid(lastFciResponse, HIPASS_AID_IN_FCI)) {
                    Log.i(TAG, "✓ Detected as HIPASS (AID: A0000002450001)");
                    return CardType.HIPASS;
                } else if (containsAid(lastFciResponse, EZL_AID_IN_FCI)) {
                    Log.i(TAG, "✓ Detected as EZL (AID: D4100000140001)");
                    return CardType.EZL;
                }
            }
            // FCI 분석 실패시 기본적으로 EZL로 간주
            Log.w(TAG, "Cannot distinguish HIPASS/EZL from FCI, defaulting to EZL");
            return CardType.EZL;
        }

        // 3. 추가 EZL AID 시도
        byte[] ezlAid1 = {(byte) 0xD4, 0x10, 0x00, 0x00, 0x03, 0x00, 0x05};
        if (trySelectAID(isoDep, ezlAid1, "EZL-Alt1")) {
            return CardType.EZL;
        }

        byte[] ezlAid2 = {(byte) 0xD4, 0x10, 0x00, 0x00, 0x03, 0x00, 0x06};
        if (trySelectAID(isoDep, ezlAid2, "EZL-Alt2")) {
            return CardType.EZL;
        }

        Log.w(TAG, "No known card type detected - tried all known AIDs");
        return CardType.UNKNOWN;
    }

    private boolean trySelectAID(IsoDep isoDep, byte[] aid, String name) {
        try {
            Log.d(TAG, "Trying " + name + " AID: " + bytesToHex(aid));
            byte[] response = selectAID(isoDep, aid);

            if (response != null && response.length >= 2) {
                int sw1 = response[response.length - 2] & 0xFF;
                int sw2 = response[response.length - 1] & 0xFF;

                Log.d(TAG, name + " response SW: " + String.format("%02X%02X", sw1, sw2) +
                        " (length: " + response.length + ")");

                // FCI 응답 저장
                if (sw1 == 0x90 && sw2 == 0x00) {
                    lastFciResponse = response;
                    Log.i(TAG, "✓ " + name + " AID selected successfully");
                    Log.d(TAG, "FCI Response: " + bytesToHex(response));
                    return true;
                } else {
                    Log.d(TAG, "✗ " + name + " AID selection failed");
                }
            } else {
                Log.d(TAG, "✗ " + name + " AID - invalid response");
            }
        } catch (Exception e) {
            Log.d(TAG, "✗ " + name + " AID failed: " + e.getMessage());
        }
        return false;
    }

    private byte[] selectAID(IsoDep isoDep, byte[] aid) {
        byte[] selectCommand = new byte[6 + aid.length];
        selectCommand[0] = 0x00;              // CLA
        selectCommand[1] = (byte) 0xA4;       // INS (SELECT)
        selectCommand[2] = 0x04;              // P1
        selectCommand[3] = 0x00;              // P2
        selectCommand[4] = (byte) aid.length; // Lc
        System.arraycopy(aid, 0, selectCommand, 5, aid.length);
        selectCommand[selectCommand.length - 1] = 0x00; // Le

        try {
            Log.d(TAG, "Sending SELECT command: " + bytesToHex(selectCommand));
            byte[] response = isoDep.transceive(selectCommand);
            Log.d(TAG, "Received response: " + bytesToHex(response));
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error selecting AID", e);
            return null;
        }
    }

    /**
     * FCI 응답에서 특정 AID를 찾음
     */
    private boolean containsAid(byte[] fciResponse, byte[] targetAid) {
        if (fciResponse == null || targetAid == null) return false;
        if (fciResponse.length < targetAid.length + 2) return false;  // +2 for status word

        // FCI 응답에서 4F 태그 (AID) 찾기
        int dataLength = fciResponse.length - 2;  // Status Word 제외

        for (int i = 0; i < dataLength - targetAid.length; i++) {
            // 4F 태그 확인
            if ((fciResponse[i] & 0xFF) == 0x4F) {
                int aidLength = fciResponse[i + 1] & 0xFF;
                if (aidLength == targetAid.length) {
                    // AID 비교
                    boolean match = true;
                    for (int j = 0; j < targetAid.length; j++) {
                        if (fciResponse[i + 2 + j] != targetAid[j]) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        Log.d(TAG, "Found matching AID at offset " + i);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}