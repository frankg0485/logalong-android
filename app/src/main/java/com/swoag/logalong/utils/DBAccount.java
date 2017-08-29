package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccount;

import java.util.HashSet;

public class DBAccount extends DBGeneric<LAccount> {
    private static final String TAG = DBAccount.class.getSimpleName();
    private static DBAccount instance;

    private static final String[] account_columns = new String[]{
            "_id",
            DBHelper.TABLE_COLUMN_GID,
            DBHelper.TABLE_COLUMN_NAME,
            DBHelper.TABLE_COLUMN_STATE,
            DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE,
            DBHelper.TABLE_COLUMN_SHOW_BALANCE,
            DBHelper.TABLE_COLUMN_SHARE};

    public DBAccount() {
    }

    public static DBAccount getInstance() {
        if (null == instance) {
            instance = new DBAccount();
        }
        return instance;
    }

    @Override
    LAccount getValues(Cursor cur, LAccount account) {
        if (null == account) account = new LAccount();
        account.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        account.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        account.setGid(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_GID)));
        account.setSharedIdsString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHARE)));
        account.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        account.setShowBalance((0 == cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHOW_BALANCE))) ?
                false : true);
        account.setId(cur.getLong(0));
        return account;
    }

    @Override
    ContentValues setValues(LAccount account) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, account.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, account.getState());
        cv.put(DBHelper.TABLE_COLUMN_GID, account.getGid());
        cv.put(DBHelper.TABLE_COLUMN_SHARE, account.getShareIdsString());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, account.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_SHOW_BALANCE, account.isShowBalance() ? 1 : 0);
        return cv;
    }

    @Override
    String[] getColumns() {
        return account_columns;
    }

    @Override
    Uri getUri() {
        return DBProvider.URI_ACCOUNTS;
    }

    @Override
    long getId(LAccount Account) {
        return Account.getId();
    }

    @Override
    void setId(LAccount Account, long id) {
        Account.setId(id);
    }

    public LAccount getByCursor(Cursor cur) {
        return getValues(cur, null);
    }

    public HashSet<Long> getAllShareUser() {
        LAccount account = new LAccount();
        HashSet<Long> set = new HashSet<>();

        Cursor cur = LApp.ctx.getContentResolver().query(DBProvider.URI_ACCOUNTS, new String[]{DBHelper
                        .TABLE_COLUMN_SHARE},
                DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        if (cur != null && cur.getCount() > 0) {

            cur.moveToFirst();
            do {
                String str = cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHARE));
                if (str != null) {
                    account.setSharedIdsString(str);
                    if (account.getShareIds() != null) {
                        for (long ii : account.getShareIds()) {
                            set.add(ii);
                        }
                    }
                }
            } while (cur.moveToNext());
        }
        if (cur != null) cur.close();
        return set;
    }

    public static class MyAccountDeleteTask extends AsyncTask<Long, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Long... params) {
            Long accountId = params[0];

            DBTransaction.getInstance().deleteByAccount(accountId);
            DBScheduledTransaction.deleteByAccount(accountId);

            DBAccountBalance.deleteByAccountId(accountId);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
        }

        @Override
        protected void onPreExecute() {
        }
    }
}
