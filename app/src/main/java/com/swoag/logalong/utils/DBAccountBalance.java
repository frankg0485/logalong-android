package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccountBalance;
import com.swoag.logalong.entities.LAccountSummary;
import com.swoag.logalong.entities.LTransaction;

public class DBAccountBalance {
    private static final String TAG = DBAccountBalance.class.getSimpleName();

    private static ContentValues setValues(LAccountBalance balance) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_STATE, balance.getState());
        cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, balance.getAccountId());
        cv.put(DBHelper.TABLE_COLUMN_YEAR, balance.getYear());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, balance.getLastChangeTimestamp());
        cv.put(DBHelper.TABLE_COLUMN_BALANCE, balance.getBalance());
        return cv;
    }

    private static void getValues(Cursor cur, LAccountBalance balance) {
        balance.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        balance.setAccountId(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT)));
        balance.setYear(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_YEAR)));
        balance.setLastChangeTimestamp(cur.getLong(cur.getColumnIndexOrThrow(DBHelper
                .TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        balance.setBalance(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_BALANCE)));
        balance.setId(cur.getLong(0));
    }

    public static LAccountBalance getByAccountId(long accountId, int year) {
        return getByAccountId(LApp.ctx, accountId, year);
    }

    public static LAccountBalance getByAccountId(Context context, long accountId, int year) {
        if (accountId <= 0) return null;

        LAccountBalance balance = new LAccountBalance();
        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_ACCOUNT_BALANCES, null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND " +
                            DBHelper.TABLE_COLUMN_ACCOUNT + "=? AND " + DBHelper.TABLE_COLUMN_YEAR + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + accountId, "" + year}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find account balance with id: " + accountId + " @ year: " + year + " " +
                            "count: " + csr.getCount());
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, balance);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with id: " + accountId + ":" + e.getMessage());
            balance = null;
        }
        return balance;
    }

    public static void deleteByAccountId(long accountId) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
        LApp.ctx.getContentResolver().update(DBProvider.URI_ACCOUNT_BALANCES, cv, DBHelper.TABLE_COLUMN_ACCOUNT +
                "=?", new String[]{"" + accountId});
    }

    public static void deleteAll() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
        LApp.ctx.getContentResolver().update(DBProvider.URI_ACCOUNT_BALANCES, cv, DBHelper.TABLE_COLUMN_STATE + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE});
    }

    public static long add(LAccountBalance balance) {
        return add(LApp.ctx, balance);
    }

    public static long add(Context context, LAccountBalance balance) {
        ContentValues cv = setValues(balance);
        long id = -1;
        try {
            Uri uri = context.getContentResolver().insert(DBProvider.URI_ACCOUNT_BALANCES, cv);
            id = ContentUris.parseId(uri);
        } catch (Exception e) {
            LLog.w(TAG, "unable to add account balance: " + e.getMessage());
        }
        return id;
    }

    public static boolean update(LAccountBalance balance) {
        return update(LApp.ctx, balance);
    }

    public static boolean update(Context context, LAccountBalance balance) {
        try {
            ContentValues cv = setValues(balance);
            context.getContentResolver().update(DBProvider.URI_ACCOUNT_BALANCES, cv, "_id=?", new String[]{"" +
                    balance.getId()});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void deleteById(long id) {
        updateColumnById(id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static boolean updateColumnById(long id, String column, int value) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(column, value);
            LApp.ctx.getContentResolver().update(DBProvider.URI_ACCOUNT_BALANCES, cv, "_id=?", new String[]{"" + id});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void setAutoBalanceUpdateEnabled(boolean enable) {
        ContentValues cv = new ContentValues();
        cv.put("enable", enable);
        LApp.ctx.getContentResolver().insert(DBProvider.URI_META_ACCOUNT_BALANCE_UPDATE, cv);
    }

    public static void getAccountSummaryForCurrentCursor(LAccountSummary summary, Cursor cursor, long[] accountIds) {
        double income = 0;
        double expense = 0;

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                double value = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                long account1 = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT));
                long account2 = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT2));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                if (type == LTransaction.TRANSACTION_TYPE_INCOME) income += value;
                else if (type == LTransaction.TRANSACTION_TYPE_EXPENSE) expense += value;
                int considerTransfer = 2;
                if (null != accountIds) {
                    for (int ii = 0; ii < accountIds.length; ii++) {
                        if (accountIds[ii] == account1 || accountIds[ii] == account2) {
                            considerTransfer++;
                        }
                    }
                }

                if (considerTransfer != 2) {
                    if (type == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                        expense += value;
                    } else if (type == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
                        income += value;
                    }
                }
            } while (cursor.moveToNext());
        }
        summary.setBalance(income - expense);
        summary.setIncome(income);
        summary.setExpense(expense);
    }
}
