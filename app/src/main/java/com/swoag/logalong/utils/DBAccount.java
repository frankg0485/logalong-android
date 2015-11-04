package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountSummary;
import com.swoag.logalong.entities.LBoxer;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LVendor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class DBAccount {
    private static final String TAG = DBAccount.class.getSimpleName();

    private static ContentValues setValues(LAccount account) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, account.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, account.getState());
        cv.put(DBHelper.TABLE_COLUMN_SHARE, account.getShareIdsString());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, account.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_RID, account.getRid().toString());
        return cv;
    }

    private static void getValues(Cursor cur, LAccount account) {
        account.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        account.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        account.setSharedIdsString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHARE)));
        account.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        account.setRid(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID)));
        account.setId(cur.getLong(0));
    }

    public static LAccount getById(long id) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        LAccount account = new LAccount();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME + " WHERE _id=?", new String[]{"" + id});
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find tag with id: " + id);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, account);
            account.setId(id);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with id: " + id + ":" + e.getMessage());
            account = null;
        }
        if (csr != null) csr.close();
        return account;
    }

    public static LAccount getByName(String name) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        LAccount account = new LAccount();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME + " WHERE "
                            + DBHelper.TABLE_COLUMN_NAME + "=? COLLATE NOCASE AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{name, "" + DBHelper.STATE_ACTIVE});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find account with name: " + name);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, account);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with name: " + name + ":" + e.getMessage());
            account = null;
        }
        if (csr != null) csr.close();
        return account;
    }

    public static LAccount getByRid(String rid) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        LAccount account = new LAccount();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME + " WHERE "
                    + DBHelper.TABLE_COLUMN_RID + "=?", new String[]{rid});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find account with rid: " + rid);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, account);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with rid: " + rid + ":" + e.getMessage());
            account = null;
        }
        if (csr != null) csr.close();
        return account;
    }

    public static long add(LAccount acccount) {
        long id = -1;
        synchronized (DBAccess.dbLock) {
            SQLiteDatabase db = DBAccess.getWriteDb();
            ContentValues cv = setValues(acccount);
            id = db.insert(DBHelper.TABLE_ACCOUNT_NAME, "", cv);
            DBAccess.dirty = true;
        }
        return id;
    }

    public static boolean update(LAccount account) {
        try {
            synchronized (DBAccess.dbLock) {
                SQLiteDatabase db = DBAccess.getWriteDb();
                ContentValues cv = setValues(account);
                db.update(DBHelper.TABLE_ACCOUNT_NAME, cv, "_id=?", new String[]{"" + account.getId()});
            }
            DBAccess.dirty = true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static void deleteById(long id) {
        DBAccess.updateColumnById(DBHelper.TABLE_ACCOUNT_NAME, id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static boolean updateColumnById(long id, String column, String value) {
        return DBAccess.updateColumnById(DBHelper.TABLE_ACCOUNT_NAME, id, column, value);
    }

    public static long getIdByName(String name) {
        return DBAccess.getIdByName(DBHelper.TABLE_ACCOUNT_NAME, name);
    }

    /*
    public static int updateNameById(long id, String name) {
        synchronized (DBAccess.dbLock) {
            SQLiteDatabase db = DBAccess.getWriteDb();
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.TABLE_COLUMN_NAME, name);
            db.update(DBHelper.TABLE_ACCOUNT_NAME, cv, "_id=?", new String[]{"" + id});
            DBAccess.dirty = true;
        }
        return 0;
    }
    */

    public static HashSet<Integer> getAllShareUser() {
        LAccount account = new LAccount();
        HashSet<Integer> set = new HashSet<Integer>();
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur = db.rawQuery("SELECT " + DBHelper.TABLE_COLUMN_SHARE + " FROM " + DBHelper.TABLE_ACCOUNT_NAME
                        + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE});
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
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur;
        if (sortColumn != null)
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=? ORDER BY " + sortColumn + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        else
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        return cur;
    }

    public static String getNameById(long id) {
        return DBAccess.getStringFromDbById(DBHelper.TABLE_ACCOUNT_NAME, DBHelper.TABLE_COLUMN_NAME, id);
    }
}
