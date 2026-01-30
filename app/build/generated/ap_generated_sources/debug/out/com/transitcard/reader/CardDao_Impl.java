package com.transitcard.reader;

import android.database.Cursor;
import androidx.collection.LongSparseArray;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class CardDao_Impl implements CardDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CardEntity> __insertionAdapterOfCardEntity;

  private final EntityInsertionAdapter<Transaction> __insertionAdapterOfTransaction;

  private final EntityDeletionOrUpdateAdapter<CardEntity> __deletionAdapterOfCardEntity;

  private final EntityDeletionOrUpdateAdapter<CardEntity> __updateAdapterOfCardEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTransactionsByCardId;

  public CardDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCardEntity = new EntityInsertionAdapter<CardEntity>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR IGNORE INTO `cards` (`id`,`cardNumber`,`cardType`,`balance`,`lastUpdated`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, CardEntity value) {
        stmt.bindLong(1, value.getId());
        if (value.getCardNumber() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getCardNumber());
        }
        if (value.getCardType() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getCardType());
        }
        stmt.bindLong(4, value.getBalance());
        stmt.bindLong(5, value.getLastUpdated());
      }
    };
    this.__insertionAdapterOfTransaction = new EntityInsertionAdapter<Transaction>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`cardId`,`date`,`location`,`amount`,`balanceAfter`,`transactionType`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Transaction value) {
        stmt.bindLong(1, value.getId());
        stmt.bindLong(2, value.getCardId());
        if (value.getDate() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getDate());
        }
        if (value.getLocation() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getLocation());
        }
        stmt.bindLong(5, value.getAmount());
        stmt.bindLong(6, value.getBalanceAfter());
        final String _tmp = TransactionTypeConverter.fromTransactionType(value.getTransactionType());
        if (_tmp == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, _tmp);
        }
        stmt.bindLong(8, value.getTimestamp());
      }
    };
    this.__deletionAdapterOfCardEntity = new EntityDeletionOrUpdateAdapter<CardEntity>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `cards` WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, CardEntity value) {
        stmt.bindLong(1, value.getId());
      }
    };
    this.__updateAdapterOfCardEntity = new EntityDeletionOrUpdateAdapter<CardEntity>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `cards` SET `id` = ?,`cardNumber` = ?,`cardType` = ?,`balance` = ?,`lastUpdated` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, CardEntity value) {
        stmt.bindLong(1, value.getId());
        if (value.getCardNumber() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getCardNumber());
        }
        if (value.getCardType() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getCardType());
        }
        stmt.bindLong(4, value.getBalance());
        stmt.bindLong(5, value.getLastUpdated());
        stmt.bindLong(6, value.getId());
      }
    };
    this.__preparedStmtOfDeleteTransactionsByCardId = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM transactions WHERE cardId = ?";
        return _query;
      }
    };
  }

  @Override
  public long insertCard(final CardEntity card) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      long _result = __insertionAdapterOfCardEntity.insertAndReturnId(card);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertTransaction(final Transaction transaction) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfTransaction.insert(transaction);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertTransactions(final List<Transaction> transactions) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfTransaction.insert(transactions);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteCard(final CardEntity card) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfCardEntity.handle(card);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateCard(final CardEntity card) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfCardEntity.handle(card);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteTransactionsByCardId(final int cardId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTransactionsByCardId.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, cardId);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteTransactionsByCardId.release(_stmt);
    }
  }

  @Override
  public CardEntity getCardByNumber(final String cardNumber) {
    final String _sql = "SELECT * FROM cards WHERE cardNumber = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (cardNumber == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, cardNumber);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfCardNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "cardNumber");
      final int _cursorIndexOfCardType = CursorUtil.getColumnIndexOrThrow(_cursor, "cardType");
      final int _cursorIndexOfBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "balance");
      final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
      final CardEntity _result;
      if(_cursor.moveToFirst()) {
        _result = new CardEntity();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _result.setId(_tmpId);
        final String _tmpCardNumber;
        if (_cursor.isNull(_cursorIndexOfCardNumber)) {
          _tmpCardNumber = null;
        } else {
          _tmpCardNumber = _cursor.getString(_cursorIndexOfCardNumber);
        }
        _result.setCardNumber(_tmpCardNumber);
        final String _tmpCardType;
        if (_cursor.isNull(_cursorIndexOfCardType)) {
          _tmpCardType = null;
        } else {
          _tmpCardType = _cursor.getString(_cursorIndexOfCardType);
        }
        _result.setCardType(_tmpCardType);
        final int _tmpBalance;
        _tmpBalance = _cursor.getInt(_cursorIndexOfBalance);
        _result.setBalance(_tmpBalance);
        final long _tmpLastUpdated;
        _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
        _result.setLastUpdated(_tmpLastUpdated);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<CardEntity>> getAllCardsLive() {
    final String _sql = "SELECT * FROM cards ORDER BY lastUpdated DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"cards"}, false, new Callable<List<CardEntity>>() {
      @Override
      public List<CardEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCardNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "cardNumber");
          final int _cursorIndexOfCardType = CursorUtil.getColumnIndexOrThrow(_cursor, "cardType");
          final int _cursorIndexOfBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "balance");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final List<CardEntity> _result = new ArrayList<CardEntity>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final CardEntity _item;
            _item = new CardEntity();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _item.setId(_tmpId);
            final String _tmpCardNumber;
            if (_cursor.isNull(_cursorIndexOfCardNumber)) {
              _tmpCardNumber = null;
            } else {
              _tmpCardNumber = _cursor.getString(_cursorIndexOfCardNumber);
            }
            _item.setCardNumber(_tmpCardNumber);
            final String _tmpCardType;
            if (_cursor.isNull(_cursorIndexOfCardType)) {
              _tmpCardType = null;
            } else {
              _tmpCardType = _cursor.getString(_cursorIndexOfCardType);
            }
            _item.setCardType(_tmpCardType);
            final int _tmpBalance;
            _tmpBalance = _cursor.getInt(_cursorIndexOfBalance);
            _item.setBalance(_tmpBalance);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _item.setLastUpdated(_tmpLastUpdated);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<CardEntity> getAllCards() {
    final String _sql = "SELECT * FROM cards ORDER BY lastUpdated DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfCardNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "cardNumber");
      final int _cursorIndexOfCardType = CursorUtil.getColumnIndexOrThrow(_cursor, "cardType");
      final int _cursorIndexOfBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "balance");
      final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
      final List<CardEntity> _result = new ArrayList<CardEntity>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final CardEntity _item;
        _item = new CardEntity();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpCardNumber;
        if (_cursor.isNull(_cursorIndexOfCardNumber)) {
          _tmpCardNumber = null;
        } else {
          _tmpCardNumber = _cursor.getString(_cursorIndexOfCardNumber);
        }
        _item.setCardNumber(_tmpCardNumber);
        final String _tmpCardType;
        if (_cursor.isNull(_cursorIndexOfCardType)) {
          _tmpCardType = null;
        } else {
          _tmpCardType = _cursor.getString(_cursorIndexOfCardType);
        }
        _item.setCardType(_tmpCardType);
        final int _tmpBalance;
        _tmpBalance = _cursor.getInt(_cursorIndexOfBalance);
        _item.setBalance(_tmpBalance);
        final long _tmpLastUpdated;
        _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
        _item.setLastUpdated(_tmpLastUpdated);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<CardWithTransactions>> getAllCardsWithTransactions() {
    final String _sql = "SELECT * FROM cards ORDER BY lastUpdated DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"transactions","cards"}, true, new Callable<List<CardWithTransactions>>() {
      @Override
      public List<CardWithTransactions> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfCardNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "cardNumber");
            final int _cursorIndexOfCardType = CursorUtil.getColumnIndexOrThrow(_cursor, "cardType");
            final int _cursorIndexOfBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "balance");
            final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
            final LongSparseArray<ArrayList<Transaction>> _collectionTransactions = new LongSparseArray<ArrayList<Transaction>>();
            while (_cursor.moveToNext()) {
              if (!_cursor.isNull(_cursorIndexOfId)) {
                final long _tmpKey = _cursor.getLong(_cursorIndexOfId);
                ArrayList<Transaction> _tmpTransactionsCollection = _collectionTransactions.get(_tmpKey);
                if (_tmpTransactionsCollection == null) {
                  _tmpTransactionsCollection = new ArrayList<Transaction>();
                  _collectionTransactions.put(_tmpKey, _tmpTransactionsCollection);
                }
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionsAscomTransitcardReaderTransaction(_collectionTransactions);
            final List<CardWithTransactions> _result = new ArrayList<CardWithTransactions>(_cursor.getCount());
            while(_cursor.moveToNext()) {
              final CardWithTransactions _item;
              final CardEntity _tmpCard;
              if (!(_cursor.isNull(_cursorIndexOfId) && _cursor.isNull(_cursorIndexOfCardNumber) && _cursor.isNull(_cursorIndexOfCardType) && _cursor.isNull(_cursorIndexOfBalance) && _cursor.isNull(_cursorIndexOfLastUpdated))) {
                _tmpCard = new CardEntity();
                final int _tmpId;
                _tmpId = _cursor.getInt(_cursorIndexOfId);
                _tmpCard.setId(_tmpId);
                final String _tmpCardNumber;
                if (_cursor.isNull(_cursorIndexOfCardNumber)) {
                  _tmpCardNumber = null;
                } else {
                  _tmpCardNumber = _cursor.getString(_cursorIndexOfCardNumber);
                }
                _tmpCard.setCardNumber(_tmpCardNumber);
                final String _tmpCardType;
                if (_cursor.isNull(_cursorIndexOfCardType)) {
                  _tmpCardType = null;
                } else {
                  _tmpCardType = _cursor.getString(_cursorIndexOfCardType);
                }
                _tmpCard.setCardType(_tmpCardType);
                final int _tmpBalance;
                _tmpBalance = _cursor.getInt(_cursorIndexOfBalance);
                _tmpCard.setBalance(_tmpBalance);
                final long _tmpLastUpdated;
                _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
                _tmpCard.setLastUpdated(_tmpLastUpdated);
              } else {
                _tmpCard = null;
              }
              ArrayList<Transaction> _tmpTransactionsCollection_1 = null;
              if (!_cursor.isNull(_cursorIndexOfId)) {
                final long _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
                _tmpTransactionsCollection_1 = _collectionTransactions.get(_tmpKey_1);
              }
              if (_tmpTransactionsCollection_1 == null) {
                _tmpTransactionsCollection_1 = new ArrayList<Transaction>();
              }
              _item = new CardWithTransactions();
              _item.card = _tmpCard;
              _item.transactions = _tmpTransactionsCollection_1;
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public CardWithTransactions getCardWithTransactions(final int cardId) {
    final String _sql = "SELECT * FROM cards WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, cardId);
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
      try {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
        final int _cursorIndexOfCardNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "cardNumber");
        final int _cursorIndexOfCardType = CursorUtil.getColumnIndexOrThrow(_cursor, "cardType");
        final int _cursorIndexOfBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "balance");
        final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
        final LongSparseArray<ArrayList<Transaction>> _collectionTransactions = new LongSparseArray<ArrayList<Transaction>>();
        while (_cursor.moveToNext()) {
          if (!_cursor.isNull(_cursorIndexOfId)) {
            final long _tmpKey = _cursor.getLong(_cursorIndexOfId);
            ArrayList<Transaction> _tmpTransactionsCollection = _collectionTransactions.get(_tmpKey);
            if (_tmpTransactionsCollection == null) {
              _tmpTransactionsCollection = new ArrayList<Transaction>();
              _collectionTransactions.put(_tmpKey, _tmpTransactionsCollection);
            }
          }
        }
        _cursor.moveToPosition(-1);
        __fetchRelationshiptransactionsAscomTransitcardReaderTransaction(_collectionTransactions);
        final CardWithTransactions _result;
        if(_cursor.moveToFirst()) {
          final CardEntity _tmpCard;
          if (!(_cursor.isNull(_cursorIndexOfId) && _cursor.isNull(_cursorIndexOfCardNumber) && _cursor.isNull(_cursorIndexOfCardType) && _cursor.isNull(_cursorIndexOfBalance) && _cursor.isNull(_cursorIndexOfLastUpdated))) {
            _tmpCard = new CardEntity();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _tmpCard.setId(_tmpId);
            final String _tmpCardNumber;
            if (_cursor.isNull(_cursorIndexOfCardNumber)) {
              _tmpCardNumber = null;
            } else {
              _tmpCardNumber = _cursor.getString(_cursorIndexOfCardNumber);
            }
            _tmpCard.setCardNumber(_tmpCardNumber);
            final String _tmpCardType;
            if (_cursor.isNull(_cursorIndexOfCardType)) {
              _tmpCardType = null;
            } else {
              _tmpCardType = _cursor.getString(_cursorIndexOfCardType);
            }
            _tmpCard.setCardType(_tmpCardType);
            final int _tmpBalance;
            _tmpBalance = _cursor.getInt(_cursorIndexOfBalance);
            _tmpCard.setBalance(_tmpBalance);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _tmpCard.setLastUpdated(_tmpLastUpdated);
          } else {
            _tmpCard = null;
          }
          ArrayList<Transaction> _tmpTransactionsCollection_1 = null;
          if (!_cursor.isNull(_cursorIndexOfId)) {
            final long _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
            _tmpTransactionsCollection_1 = _collectionTransactions.get(_tmpKey_1);
          }
          if (_tmpTransactionsCollection_1 == null) {
            _tmpTransactionsCollection_1 = new ArrayList<Transaction>();
          }
          _result = new CardWithTransactions();
          _result.card = _tmpCard;
          _result.transactions = _tmpTransactionsCollection_1;
        } else {
          _result = null;
        }
        __db.setTransactionSuccessful();
        return _result;
      } finally {
        _cursor.close();
        _statement.release();
      }
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Transaction> getTransactionsByCardId(final int cardId) {
    final String _sql = "SELECT * FROM transactions WHERE cardId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, cardId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfBalanceAfter = CursorUtil.getColumnIndexOrThrow(_cursor, "balanceAfter");
      final int _cursorIndexOfTransactionType = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionType");
      final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
      final List<Transaction> _result = new ArrayList<Transaction>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Transaction _item;
        _item = new Transaction();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final int _tmpCardId;
        _tmpCardId = _cursor.getInt(_cursorIndexOfCardId);
        _item.setCardId(_tmpCardId);
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _item.setDate(_tmpDate);
        final String _tmpLocation;
        if (_cursor.isNull(_cursorIndexOfLocation)) {
          _tmpLocation = null;
        } else {
          _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
        }
        _item.setLocation(_tmpLocation);
        final int _tmpAmount;
        _tmpAmount = _cursor.getInt(_cursorIndexOfAmount);
        _item.setAmount(_tmpAmount);
        final int _tmpBalanceAfter;
        _tmpBalanceAfter = _cursor.getInt(_cursorIndexOfBalanceAfter);
        _item.setBalanceAfter(_tmpBalanceAfter);
        final TransactionType _tmpTransactionType;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfTransactionType)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfTransactionType);
        }
        _tmpTransactionType = TransactionTypeConverter.toTransactionType(_tmp);
        _item.setTransactionType(_tmpTransactionType);
        final long _tmpTimestamp;
        _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
        _item.setTimestamp(_tmpTimestamp);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private void __fetchRelationshiptransactionsAscomTransitcardReaderTransaction(
      final LongSparseArray<ArrayList<Transaction>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    // check if the size is too big, if so divide;
    if(_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      LongSparseArray<ArrayList<Transaction>> _tmpInnerMap = new LongSparseArray<ArrayList<Transaction>>(androidx.room.RoomDatabase.MAX_BIND_PARAMETER_CNT);
      int _tmpIndex = 0;
      int _mapIndex = 0;
      final int _limit = _map.size();
      while(_mapIndex < _limit) {
        _tmpInnerMap.put(_map.keyAt(_mapIndex), _map.valueAt(_mapIndex));
        _mapIndex++;
        _tmpIndex++;
        if(_tmpIndex == RoomDatabase.MAX_BIND_PARAMETER_CNT) {
          __fetchRelationshiptransactionsAscomTransitcardReaderTransaction(_tmpInnerMap);
          _tmpInnerMap = new LongSparseArray<ArrayList<Transaction>>(RoomDatabase.MAX_BIND_PARAMETER_CNT);
          _tmpIndex = 0;
        }
      }
      if(_tmpIndex > 0) {
        __fetchRelationshiptransactionsAscomTransitcardReaderTransaction(_tmpInnerMap);
      }
      return;
    }
    StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `id`,`cardId`,`date`,`location`,`amount`,`balanceAfter`,`transactionType`,`timestamp` FROM `transactions` WHERE `cardId` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex ++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "cardId");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfCardId = 1;
      final int _cursorIndexOfDate = 2;
      final int _cursorIndexOfLocation = 3;
      final int _cursorIndexOfAmount = 4;
      final int _cursorIndexOfBalanceAfter = 5;
      final int _cursorIndexOfTransactionType = 6;
      final int _cursorIndexOfTimestamp = 7;
      while(_cursor.moveToNext()) {
        if (!_cursor.isNull(_itemKeyIndex)) {
          final long _tmpKey = _cursor.getLong(_itemKeyIndex);
          ArrayList<Transaction> _tmpRelation = _map.get(_tmpKey);
          if (_tmpRelation != null) {
            final Transaction _item_1;
            _item_1 = new Transaction();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _item_1.setId(_tmpId);
            final int _tmpCardId;
            _tmpCardId = _cursor.getInt(_cursorIndexOfCardId);
            _item_1.setCardId(_tmpCardId);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            _item_1.setDate(_tmpDate);
            final String _tmpLocation;
            if (_cursor.isNull(_cursorIndexOfLocation)) {
              _tmpLocation = null;
            } else {
              _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            }
            _item_1.setLocation(_tmpLocation);
            final int _tmpAmount;
            _tmpAmount = _cursor.getInt(_cursorIndexOfAmount);
            _item_1.setAmount(_tmpAmount);
            final int _tmpBalanceAfter;
            _tmpBalanceAfter = _cursor.getInt(_cursorIndexOfBalanceAfter);
            _item_1.setBalanceAfter(_tmpBalanceAfter);
            final TransactionType _tmpTransactionType;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfTransactionType)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfTransactionType);
            }
            _tmpTransactionType = TransactionTypeConverter.toTransactionType(_tmp);
            _item_1.setTransactionType(_tmpTransactionType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item_1.setTimestamp(_tmpTimestamp);
            _tmpRelation.add(_item_1);
          }
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
