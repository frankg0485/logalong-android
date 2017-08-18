package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccount;

import java.util.HashSet;

public class DBAccount {
    private static final String TAG = DBAccount.class.getSimpleName();

    private static final String[] account_columns = new String[]{
            "_id",
            DBHelper.TABLE_COLUMN_GID,
            DBHelper.TABLE_COLUMN_NAME,
            DBHelper.TABLE_COLUMN_STATE,
            DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE,
            DBHelper.TABLE_COLUMN_SHOW_BALANCE,
            DBHelper.TABLE_COLUMN_SHARE};

    private static ContentValues setValues(LAccount account) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, account.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, account.getState());
        cv.put(DBHelper.TABLE_COLUMN_GID, account.getGid());
        cv.put(DBHelper.TABLE_COLUMN_SHARE, account.getShareIdsString());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, account.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_SHOW_BALANCE, account.isShowBalance() ? 1 : 0);
        return cv;
    }

    private static void getValues(Cursor cur, LAccount account) {
        account.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        account.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        account.setGid(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_GID)));
        account.setSharedIdsString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHARE)));
        account.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        account.setShowBalance((0 == cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHOW_BALANCE))) ?
                false : true);
        account.setId(cur.getLong(0));
    }

    public static LAccount getByCursor(Cursor cursor) {
        LAccount account = new LAccount();
        getValues(cursor, account);
        return account;
    }

    public static LAccount getByIdAll(long id) {
        return getByIdGid(LApp.ctx, id, true, false);
    }

    public static LAccount getById(long id) {
        return getByIdGid(LApp.ctx, id, true, true);
    }

    private static LAccount getByIdGid(Context context, long id, boolean byId, boolean active) {
        if (id <= 0) return null;

        LAccount account = new LAccount();
        try {
            Cursor csr;
            String id_str;
            id_str = (byId)? "_id" : DBHelper.TABLE_COLUMN_GID;
            if (active)
                csr = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, account_columns,
                        id_str + "=? AND " + DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + id,
                                "" + DBHelper.STATE_ACTIVE}, null);
            else
                csr = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, account_columns,
                        id_str + "=?", new String[]{"" + id}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find account with id: " + id);
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, account);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with id: " + id + ":" + e.getMessage());
            account = null;
        }
        return account;
    }

    public static LAccount getByGidAll(int gid) {
        return getByIdGid(LApp.ctx, gid, false, false);
    }
    public static LAccount getByGid(int gid) {
        return getByIdGid(LApp.ctx, gid, false, true);
    }


    public static void resetGidIfNotUnique(int gid) {
        if (gid <= 0) return;

        LAccount account = new LAccount();
        try {
            Cursor csr = LApp.ctx.getContentResolver().query(DBProvider.URI_ACCOUNTS, null,
                    DBHelper.TABLE_COLUMN_NUMBER + "=? AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + gid, "" + DBHelper.STATE_ACTIVE}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "GID not unique: reset all to zero from: " + gid);
                    int[] ids = new int[csr.getCount()];
                    int ii = 0;
                    csr.moveToFirst();
                    do {
                        ids[ii++] = csr.getInt(0);
                    } while (csr.moveToNext());

                    for (ii = 0; ii < ids.length; ii++) {
                        updateColumnById(ids[ii], DBHelper.TABLE_COLUMN_NUMBER, 0);
                    }
                }
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with gid: " + gid + ":" + e.getMessage());
        }
    }

    public static LAccount getByName(String name) {
        return getByName(LApp.ctx, name);
    }

    public static LAccount getByName(Context context, String name) {
        LAccount account = new LAccount();

        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, account_columns,
                    DBHelper.TABLE_COLUMN_NAME + "=? COLLATE NOCASE AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{name, "" + DBHelper.STATE_ACTIVE}, null);
            if (csr != null) {
                if (csr.getCount() < 1 || csr.getCount() > 1) {
                    LLog.w(TAG, "unable to find account with name: " + name + " count: " + csr.getCount());
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, account);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with name: " + name + ":" + e.getMessage());
            account = null;
        }
        return account;
    }

    public static long add(LAccount acccount) {
        return add(LApp.ctx, acccount);
    }

    private static long add(Context context, LAccount acccount) {
        ContentValues cv = setValues(acccount);
        long id = -1;
        try {
            Uri uri = context.getContentResolver().insert(DBProvider.URI_ACCOUNTS, cv);
            id = ContentUris.parseId(uri);
            acccount.setId(id);
        } catch (Exception e) {
            LLog.w(TAG, "unable to add account: " + e.getMessage());
        }
        return id;
    }

    public static boolean update(LAccount account) {
        return update(LApp.ctx, account);
    }

    public static boolean update(Context context, LAccount account) {
        try {
            ContentValues cv = setValues(account);
            context.getContentResolver().update(DBProvider.URI_ACCOUNTS, cv, "_id=?", new String[]{"" + account.getId
                    ()});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void deleteById(long id) {
        updateColumnById(id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static boolean updateColumnById(long id, String column, int value) {
        return DBAccess.updateColumnById(DBProvider.URI_ACCOUNTS, id, column, value);
    }

    public static boolean updateColumnById(long id, String column, String value) {
        return DBAccess.updateColumnById(DBProvider.URI_ACCOUNTS, id, column, value);
    }

    public static long getIdByName(String name) {
        return DBAccess.getIdByName(DBProvider.URI_ACCOUNTS, name);
    }

    public static HashSet<Integer> getAllShareUser() {
        return getAllShareUser(LApp.ctx);
    }

    public static HashSet<Integer> getAllShareUser(Context context) {
        LAccount account = new LAccount();
        HashSet<Integer> set = new HashSet<Integer>();

        Cursor cur = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, new String[]{DBHelper
                        .TABLE_COLUMN_SHARE},
                DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        if (cur != null && cur.getCount() > 0) {

            cur.moveToFirst();
            do {
                String str = cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHARE));
                if (str != null) {
                    account.setSharedIdsString(str);
                    if (account.getShareIds() != null) {
                        for (int ii : account.getShareIds()) {
                            set.add(ii);
                        }
                    }
                }
            } while (cur.moveToNext());
        }
        if (cur != null) cur.close();
        return set;
    }

    public static Cursor getCursorSortedBy(String sortColumn) {
        return getCursorSortedBy(LApp.ctx, sortColumn);
    }

    public static Cursor getCursorSortedBy(Context context, String sortColumn) {
        Cursor cur;
        if (sortColumn != null)
            cur = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, account_columns,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE},
                    sortColumn + " ASC");
        else
            cur = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, account_columns,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        return cur;
    }

    public static String getNameById(long id) {
        return DBAccess.getStringFromDbById(DBProvider.URI_ACCOUNTS, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static int getDbIndexById(long id) {
        return DBAccess.getDbIndexById(LApp.ctx, DBProvider.URI_ACCOUNTS, id);
    }

    public static long getIdByGid(long gid) {
        return DBAccess.getIdByGid(DBProvider.URI_ACCOUNTS, gid);
    }

    public static HashSet<Long> getAllActiveIds() {
        HashSet<Long> set = new HashSet<Long>();
        Cursor cur = getCursorSortedBy(null);
        if (cur != null && cur.getCount() > 0) {

            cur.moveToFirst();
            do {
                set.add(cur.getLong(0));
            } while (cur.moveToNext());
        }
        if (cur != null) cur.close();
        return set;
    }
}
