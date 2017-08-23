package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountSummary;
import com.swoag.logalong.entities.LTransaction;

import java.util.HashSet;

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
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, DBHelper
                            .TABLE_COLUMN_NAME + " ASC");

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


    ////////////////////////////////////////////////////////////////////////////////////////////////
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

    private static long getIdByColumn(Context context, Uri uri, String column, String value, boolean caseSensitive) {
        long id = 0;

        try {
            Cursor csr = null;
            if (caseSensitive) {
                csr = context.getContentResolver().query(uri, new String[]{"_id"}, column + "=? AND "
                                + DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + value, "" + DBHelper
                                .STATE_ACTIVE},
                        null);
            } else {
                csr = context.getContentResolver().query(uri, new String[]{"_id"}, column + "=? COLLATE NOCASE AND "
                                + DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + value, "" + DBHelper
                                .STATE_ACTIVE},
                        null);
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

    private static long getIdByColumn(Context context, Uri uri, String column, long value) {
        long id = 0;

        try {
            Cursor csr = null;
            csr = context.getContentResolver().query(uri, new String[]{"_id"}, column + "=?",
                    new String[]{"" + value}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to get unique " + column + ": " + value + " in table: " + uri);
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

    public static long getIdByGid(Uri uri, long gid) {
        return getIdByColumn(LApp.ctx, uri, DBHelper.TABLE_COLUMN_GID, gid);
    }

}
