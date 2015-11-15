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
import com.swoag.logalong.entities.LAccountBalance;
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

    public static String getStringFromDbById(String table, String column, long id) {
        return getStringFromDbById(LApp.ctx, table, column, id);
    }

    public static String getStringFromDbById(Context context, String table, String column, long id) {
        Uri uri = table2uri(table);
        if (uri == null) return null;

        Cursor csr = null;
        String str = "";
        try {
            csr = context.getContentResolver().query(uri, new String[]{column}, "_id=?", new String[]{"" + id}, null);
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find id: " + id + " from table: " + table + " column: " + column);
                csr.close();
                return "";
            }

            csr.moveToFirst();
            str = csr.getString(csr.getColumnIndexOrThrow(column));
        } catch (Exception e) {
            LLog.w(TAG, "unable to get with id: " + id + ":" + e.getMessage());
        }
        if (csr != null) csr.close();
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
                            if (account.getShareStates().get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED) {
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

    public static void getAccountSummaryForCurrentCursor(LAccountSummary summary, long id, Cursor cursor) {
        double income = 0;
        double expense = 0;

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                double value = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                if (type == LTransaction.TRANSACTION_TYPE_INCOME) income += value;
                else if (type == LTransaction.TRANSACTION_TYPE_EXPENSE) expense += value;
            } while (cursor.moveToNext());
        }
        summary.setBalance(income - expense);
        summary.setIncome(income);
        summary.setExpense(expense);
    }

    public static ArrayList<String> getAccountBalance(long id, LBoxer boxer) {
        return getAccountBalance(LApp.ctx, id, boxer);
    }

    public static ArrayList<String> getAccountBalance(Context context, long id, LBoxer boxer) {
        Cursor csr = null;
        ArrayList<String> balance = new ArrayList<String>();
        try {
            csr = context.getContentResolver().query(DBProvider.URI_ACCOUNT_BALANCES,
                    new String[]{DBHelper.TABLE_COLUMN_YEAR, DBHelper.TABLE_COLUMN_BALANCE}, "_id=?",
                    new String[]{"" + id},
                    DBHelper.TABLE_COLUMN_YEAR + " ASC");
            int startYear = -1, endYear = -1;
            if (csr != null && csr.getCount() > 0) {
                csr.moveToFirst();
                do {
                    int year = csr.getInt(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_YEAR));
                    if (startYear == -1) startYear = year;
                    endYear = year;
                    balance.add("" + year);
                    balance.add(csr.getString(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_BALANCE)));
                } while (csr.moveToNext());
                csr.close();
                boxer.x = startYear;
                boxer.y = endYear;
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
        return balance;
    }

    public static String getAccountBalance(long id, int year) {
        return getAccountBalance(LApp.ctx, id, year);
    }

    public static String getAccountBalance(Context context, long id, int year) {
        Cursor csr = null;
        String balance = "";
        try {
            csr = context.getContentResolver().query(DBProvider.URI_ACCOUNT_BALANCES, null,
                    "_id=? AND " + DBHelper.TABLE_COLUMN_YEAR + "=?",
                    new String[]{"" + id, "" + year}, null);
            if (csr != null) {
                csr.moveToFirst();
                balance = csr.getString(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_BALANCE));
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
        return balance;
    }

    public static boolean updateAccountBalance(long id, int year, String balance) {
        return updateAccountBalance(LApp.ctx, id, year, balance);
    }

    public static boolean updateAccountBalance(Context context, long id, int year, String balance) {
        boolean exists = false;
        Cursor csr = null;
        boolean ret = false;

        try {
            long rowId = 0;
            csr = context.getContentResolver().query(DBProvider.URI_ACCOUNT_BALANCES, null,
                    "_id=? AND " + DBHelper.TABLE_COLUMN_YEAR + "=?",
                    new String[]{"" + id, "" + year}, null);
            if (csr != null) {
                if (csr.getCount() > 0) {
                    csr.moveToFirst();
                    rowId = csr.getInt(0);
                    exists = true;
                }
                csr.close();
            }

            ContentValues cv = new ContentValues();
            cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, id);
            cv.put(DBHelper.TABLE_COLUMN_YEAR, year);
            cv.put(DBHelper.TABLE_COLUMN_BALANCE, balance);

            if (!exists) {
                context.getContentResolver().insert(DBProvider.URI_ACCOUNT_BALANCES, cv);

            } else {
                context.getContentResolver().update(DBProvider.URI_ACCOUNT_BALANCES, cv, "_id=?", new String[]{"" + rowId});
            }
            ret = true;
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }

        return ret;
    }

    // scans through transaction to compile account monthly balances, from the very first transaction to the latest, in that order.
    // return boxer.lx: start timestamp
    //              ly: end timestamp
    public static HashMap<Integer, double[]> scanAccountBalanceById(long id, LBoxer boxer) {
        return scanAccountBalanceById(LApp.ctx, id, boxer);
    }

    public static HashMap<Integer, double[]> scanAccountBalanceById(Context context, long id, LBoxer boxer) {
        Cursor csr = null;
        HashMap<Integer, double[]> balances = new HashMap<Integer, double[]>();

        long timestamp;
        boxer.lx = -1;
        boxer.ly = -1;
        try {
            csr = context.getContentResolver().query(DBProvider.URI_TRANSACTIONS,
                    new String[]{DBHelper.TABLE_COLUMN_AMOUNT,
                            DBHelper.TABLE_COLUMN_TIMESTAMP,
                            DBHelper.TABLE_COLUMN_TYPE,
                            DBHelper.TABLE_COLUMN_ACCOUNT},
                    DBHelper.TABLE_COLUMN_ACCOUNT + "=? AND "
                            + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + id, "" + DBHelper.STATE_ACTIVE},
                    DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC");
            if (csr != null && csr.getCount() > 0) {
                csr.moveToFirst();

                Calendar now = Calendar.getInstance();
                double acc = 0;
                int lastMonth = -1;
                int lastYear = -1;
                int month;
                double[] lastAmount;
                do {
                    int type = csr.getInt(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                    double v = csr.getDouble(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                    if (type == LTransaction.TRANSACTION_TYPE_EXPENSE || type == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                        v = -v;
                    }
                    timestamp = csr.getLong(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP));
                    if (boxer.lx == -1) boxer.lx = timestamp;
                    boxer.ly = timestamp;

                    now.setTimeInMillis(timestamp);

                    int year = now.get(Calendar.YEAR);
                    double[] amount = balances.get(year);
                    if (amount == null) {
                        amount = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        balances.put(year, amount);
                    }
                    month = now.get(Calendar.MONTH);

                    // fill up months in between
                    if (((lastMonth != -1) && (lastMonth != month))
                            || ((lastYear != -1) && (lastYear != year))) {
                        int nextMonth = lastMonth + 1;
                        int nextYear = lastYear;
                        if (nextMonth > 11) {
                            nextMonth = 0;
                            nextYear++;
                        }

                        while (((nextMonth != month) || (nextYear != year))) {
                            double[] am = balances.get(nextYear);
                            if (am == null) {
                                am = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                            }
                            am[nextMonth] = acc;
                            balances.put(nextYear, am);

                            nextMonth++;
                            if (nextMonth > 11) {
                                nextYear++;
                                nextMonth = 0;
                            }
                        }
                    }
                    acc += v;
                    amount[month] = acc;
                    lastMonth = month;
                    lastYear = year;
                    lastAmount = amount;
                } while (csr.moveToNext());
                csr.close();

                // it is possible that the rest of months within the latest transaction year carry
                // balance of zero, let's fix them now.
                for (month = lastMonth + 1; month < 12; month++) {
                    lastAmount[month] = acc;
                }
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
        return balances;
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
        journal.setId(cur.getLong(0));
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

    public static boolean updateJournal(LJournal journal) {
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

    public static boolean deleteJournalById(long id) {
        return updateColumnById(DBHelper.TABLE_JOURNAL_NAME, id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
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
    /*
    public static boolean isNameAvailable(String table, String name) {
        SQLiteDatabase db = DBAccess.getReadDb();
        try {
            Cursor csr = db.rawQuery("SELECT "
                            + DBHelper.TABLE_COLUMN_NAME + ","
                            + DBHelper.TABLE_COLUMN_STATE + " FROM "
                            + table + " WHERE UPPER("
                            + DBHelper.TABLE_COLUMN_NAME + ") =? AND "
                            + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{name.toUpperCase(), "" + DBHelper.STATE_ACTIVE});
            if (csr != null) {
                boolean ret = (csr.getCount() < 1);
                csr.close();
                return ret;
            }
        } catch (Exception e) {
        }
        return true;
    }
    */

    public static boolean updateColumnById(String table, long id, String column, String value) {
        try {
            Uri uri = table2uri(table);
            ContentValues cv = new ContentValues();
            cv.put(column, value);
            LApp.ctx.getContentResolver().update(uri, cv, "_id=?", new String[]{"" + id});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean updateColumnById(String table, long id, String column, int value) {
        try {
            Uri uri = table2uri(table);
            ContentValues cv = new ContentValues();
            cv.put(column, value);
            LApp.ctx.getContentResolver().update(uri, cv, "_id=?", new String[]{"" + id});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

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

    private static long getIdByColumn(Context context, String table, String column, String value, boolean caseSensitive) {
        Cursor csr = null;
        long id = 0;

        try {
            Uri uri = table2uri(table);
            if (caseSensitive) {
                csr = context.getContentResolver().query(uri, new String[]{"_id"}, column + "=? AND "
                        + DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + value, "" + DBHelper.STATE_ACTIVE}, null);
            } else {
                csr = context.getContentResolver().query(uri, new String[]{"_id"}, column + "=? COLLATE NOCASE AND "
                        + DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + value, "" + DBHelper.STATE_ACTIVE}, null);
            }
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to get " + column + ": " + value + " in table: " + table);
                csr.close();
                return 0;
            }

            csr.moveToFirst();
            id = csr.getLong(0);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get " + column + ": " + value + " in table: " + table);
        }
        if (csr != null) csr.close();
        return id;
    }

    public static long getIdByName(String table, String name) {
        return getIdByName(LApp.ctx, table, name);
    }

    public static long getIdByName(Context context, String table, String name) {
        return getIdByColumn(context, table, DBHelper.TABLE_COLUMN_NAME, name, false);
    }

    public static long getIdByRid(String table, String rid) {
        return getIdByRid(LApp.ctx, table, rid);
    }

    public static long getIdByRid(Context context, String table, String rid) {
        return getIdByColumn(context, table, DBHelper.TABLE_COLUMN_RID, rid, false);
    }
}
