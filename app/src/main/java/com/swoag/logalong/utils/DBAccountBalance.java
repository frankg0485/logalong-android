package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccountBalance;

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
        balance.setLastChangeTimestamp(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
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
                    LLog.w(TAG, "unable to find account balance with id: " + accountId + " @ year: " + year);
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
        LApp.ctx.getContentResolver().update(DBProvider.URI_ACCOUNT_BALANCES, cv, DBHelper.TABLE_COLUMN_ACCOUNT + "=?", new String[]{"" + accountId});
    }

    public static void deleteAll() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
        LApp.ctx.getContentResolver().update(DBProvider.URI_ACCOUNT_BALANCES, cv, DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE});
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
            context.getContentResolver().update(DBProvider.URI_ACCOUNT_BALANCES, cv, "_id=?", new String[]{"" + balance.getId()});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void deleteById(long id) {
        updateColumnById(id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static boolean updateColumnById(long id, String column, int value) {
        return DBAccess.updateColumnById(DBProvider.URI_ACCOUNT_BALANCES, id, column, value);
    }

    public static void setAutoBalanceUpdateEnabled(boolean enable) {
        ContentValues cv = new ContentValues();
        cv.put("enable", enable);
        LApp.ctx.getContentResolver().insert(DBProvider.URI_META_ACCOUNT_BALANCE_UPDATE, cv);
    }
}
