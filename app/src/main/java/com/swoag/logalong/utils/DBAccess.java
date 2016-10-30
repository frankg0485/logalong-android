package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountSummary;
import com.swoag.logalong.entities.LBoxer;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LVendor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class DBAccess {
    private static final String TAG = DBAccess.class.getSimpleName();

    public static String getStringFromDbById(Uri uri, String column, long id) {
        return getStringFromDbById(LApp.ctx, uri, column, id);
    }

    private static String getStringFromDbById(Context context, Uri uri, String column, long id) {
        String str = "";
        try {
            Cursor csr = context.getContentResolver().query(uri, new String[]{column}, "_id=? AND "
                    + DBHelper.TABLE_COLUMN_STATE + " =?", new String[]{"" + id, "" + DBHelper.STATE_ACTIVE}, null);
            if (csr != null) {
                if (csr.getCount() > 0) {
                    csr.moveToFirst();
                    str = csr.getString(csr.getColumnIndexOrThrow(column));
                }
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get with id: " + id + ":" + e.getMessage());
        }
        return str;
    }

    public static HashSet<Integer> getAllAccountsConfirmedShareUser() {
        return getAllAccountsConfirmedShareUser(LApp.ctx);
    }

    public static HashSet<Integer> getAllAccountsConfirmedShareUser(Context context) {
        LAccount account = new LAccount();
        HashSet<Integer> set = new HashSet<Integer>();
        Cursor cur = context.getContentResolver().query(DBProvider.URI_ACCOUNTS,
                new String[]{DBHelper.TABLE_COLUMN_SHARE}, DBHelper.TABLE_COLUMN_STATE + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        if (cur != null && cur.getCount() > 0) {

            cur.moveToFirst();
            do {
                String str = cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHARE));
                if (str != null) {
                    account.setSharedIdsString(str);
                    if (account.getShareIds() != null) {
                        for (int ii = 0; ii < account.getShareIds().size(); ii++) {
                            if (account.getShareStates().get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED_SYNCED) {
                                set.add(account.getShareIds().get(ii));
                            }
                        }
                    }
                }
            } while (cur.moveToNext());
        }
        if (cur != null) cur.close();
        return set;
    }

    public static int getDbIndexById(Context context, Uri uri, long id) {
        Cursor csr = null;
        int index = 0;
        int ret = -1;
        try {
            csr = context.getContentResolver().query(uri, new String[]{"_id"},
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, DBHelper.TABLE_COLUMN_NAME + " ASC");

            csr.moveToFirst();
            while (true) {
                if (id == csr.getLong(0)) {
                    ret = index;
                    break;
                }
                csr.moveToNext();
                index++;
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get with id: " + id + ":" + e.getMessage());
        }
        if (csr != null) csr.close();
        return ret;
    }

    public static void getAccountSummaryForCurrentCursor(LAccountSummary summary, Cursor cursor, boolean considerTransfer) {
        double income = 0;
        double expense = 0;

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                double value = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                if (type == LTransaction.TRANSACTION_TYPE_INCOME) income += value;
                else if (type == LTransaction.TRANSACTION_TYPE_EXPENSE) expense += value;
                if (considerTransfer) {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Journal support
    private static ContentValues setJournalValues(LJournal journal) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_STATE, journal.getState());
        cv.put(DBHelper.TABLE_COLUMN_TO_USER, journal.getUserId());
        cv.put(DBHelper.TABLE_COLUMN_RECORD, journal.getRecord());
        return cv;
    }

    private static void getJournalValues(Cursor cur, LJournal journal) {
        journal.setState(cur.getInt(cur.getColumnIndex(DBHelper.TABLE_COLUMN_STATE)));
        journal.setUserId(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TO_USER)));
        journal.setRecord(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RECORD)));
    }

    public static long addJournal(LJournal journal) {
        return addJournal(LApp.ctx, journal);
    }

    public static long addJournal(Context context, LJournal journal) {
        long id = -1;
        try {
            ContentValues cv = setJournalValues(journal);
            Uri uri = context.getContentResolver().insert(DBProvider.URI_JOURNALS, cv);
            id = ContentUris.parseId(uri);
        } catch (Exception e) {
        }
        return id;
    }

    /*public static boolean updateJournal(LJournal journal) {
        return updateJournal(LApp.ctx, journal);
    }

    public static boolean updateJournal(Context context, LJournal journal) {
        try {
            ContentValues cv = setJournalValues(journal);
            context.getContentResolver().update(DBProvider.URI_JOURNALS, cv, "_id=?", new String[]{"" + journal.getId()});
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    */

    /*public static boolean deleteJournalAll() {
        return deleteJournalAll(LApp.ctx);
    }

    public static boolean deleteJournalAll(Context context) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
            context.getContentResolver().update(DBProvider.URI_JOURNALS, cv,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE});
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    */

    public static boolean deleteJournalById(long id) {
        return updateColumnById(DBProvider.URI_JOURNALS, id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static Cursor getAllActiveJournalCursor() {
        return getAllActiveJournalCursor(LApp.ctx);
    }

    public static Cursor getAllActiveJournalCursor(Context context) {
        Cursor cur = context.getContentResolver().query(DBProvider.URI_JOURNALS, null,
                DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        return cur;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static boolean updateColumnById(Uri uri, long id, String column, String value) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(column, value);
            LApp.ctx.getContentResolver().update(uri, cv, "_id=?", new String[]{"" + id});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean updateColumnById(Uri uri, long id, String column, int value) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(column, value);
            LApp.ctx.getContentResolver().update(uri, cv, "_id=?", new String[]{"" + id});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /*
    private static Uri table2uri(String table) {
        if (table.contentEquals(DBHelper.TABLE_TRANSACTION_NAME)) {
            return DBProvider.URI_TRANSACTIONS;
        } else if (table.contentEquals(DBHelper.TABLE_ACCOUNT_NAME)) {
            return DBProvider.URI_ACCOUNTS;
        } else if (table.contentEquals(DBHelper.TABLE_CATEGORY_NAME)) {
            return DBProvider.URI_CATEGORIES;
        } else if (table.contentEquals(DBHelper.TABLE_TAG_NAME)) {
            return DBProvider.URI_TAGS;
        } else if (table.contentEquals(DBHelper.TABLE_VENDOR_NAME)) {
            return DBProvider.URI_VENDORS;
        }
        return null;
    }
    */

    private static long getIdByColumn(Context context, Uri uri, String column, String value, boolean caseSensitive) {
        long id = 0;

        try {
            Cursor csr = null;
            if (caseSensitive) {
                csr = context.getContentResolver().query(uri, new String[]{"_id"}, column + "=? AND "
                        + DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + value, "" + DBHelper.STATE_ACTIVE}, null);
            } else {
                csr = context.getContentResolver().query(uri, new String[]{"_id"}, column + "=? COLLATE NOCASE AND "
                        + DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + value, "" + DBHelper.STATE_ACTIVE}, null);
            }
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to get " + column + ": " + value + " in table: " + uri);
                    csr.close();
                    return 0;
                }

                csr.moveToFirst();
                id = csr.getLong(0);
                csr.close();
            }

        } catch (Exception e) {
            LLog.w(TAG, "unable to get " + column + ": " + value + " in table: " + uri);
        }
        return id;
    }

    public static long getIdByName(Uri uri, String name) {
        return getIdByName(LApp.ctx, uri, name);
    }

    public static long getIdByName(Context context, Uri uri, String name) {
        return getIdByColumn(context, uri, DBHelper.TABLE_COLUMN_NAME, name, false);
    }

    public static long getIdByRid(Uri uri, String rid) {
        return getIdByRid(LApp.ctx, uri, rid);
    }

    public static long getIdByRid(Context context, Uri uri, String rid) {
        return getIdByColumn(context, uri, DBHelper.TABLE_COLUMN_RID, rid, false);
    }
}
