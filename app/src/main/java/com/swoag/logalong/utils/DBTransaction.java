package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTransaction;

import java.util.Calendar;
import java.util.UUID;

public class DBTransaction {
    private static final String TAG = DBTransaction.class.getSimpleName();

    public static void setValues(ContentValues cv, LTransaction trans) {
        cv.put(DBHelper.TABLE_COLUMN_TYPE, trans.getType());
        cv.put(DBHelper.TABLE_COLUMN_STATE, trans.getState());
        cv.put(DBHelper.TABLE_COLUMN_CATEGORY, trans.getCategory());
        cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, trans.getAccount());
        cv.put(DBHelper.TABLE_COLUMN_ACCOUNT2, trans.getAccount2());
        cv.put(DBHelper.TABLE_COLUMN_MADEBY, trans.getBy());
        cv.put(DBHelper.TABLE_COLUMN_AMOUNT, trans.getValue());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP, trans.getTimeStamp());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, trans.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_NOTE, trans.getNote());
        cv.put(DBHelper.TABLE_COLUMN_TAG, trans.getTag());
        cv.put(DBHelper.TABLE_COLUMN_VENDOR, trans.getVendor());
        cv.put(DBHelper.TABLE_COLUMN_RID, trans.getRid());
    }

    public static void getValues(Cursor cursor, LTransaction trans) {
        trans.setType(cursor.getInt(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_TYPE)));
        trans.setState(cursor.getInt(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_STATE)));
        trans.setAccount(cursor.getLong(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_ACCOUNT)));
        trans.setAccount2(cursor.getLong(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_ACCOUNT2)));
        trans.setCategory(cursor.getLong(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_CATEGORY)));
        trans.setTag(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TAG)));
        trans.setVendor(cursor.getLong(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_VENDOR)));
        trans.setValue(cursor.getDouble(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_AMOUNT)));
        trans.setNote(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NOTE)));
        trans.setBy(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_MADEBY)));
        trans.setTimeStamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)));
        trans.setTimeStampLast(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        trans.setRid(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID)));
        trans.setId(cursor.getLong(0));
    }

    private static ContentValues setValues(LTransaction trans) {
        ContentValues cv = new ContentValues();
        setValues(cv, trans);
        return cv;
    }

    public static void add(LTransaction item) {
        add(LApp.ctx, item);
    }

    public static void add(Context context, LTransaction item) {
        ContentValues cv = setValues(item);
        context.getContentResolver().insert(DBProvider.URI_TRANSACTIONS, cv);
    }

    public static void add(LTransaction item, boolean duplicateTransfer, boolean postJournal) {
        add(LApp.ctx, item, duplicateTransfer, postJournal);
    }

    public static void add(Context context, LTransaction item, boolean duplicateTransfer, boolean postJournal) {
        ContentValues cv = setValues(item);
        context.getContentResolver().insert(DBProvider.URI_TRANSACTIONS, cv);

        LJournal journal = null;
        if (postJournal) {
            journal = new LJournal();
            journal.updateItem(item);
        }

        if (duplicateTransfer) {
            //duplicate record for transfer
            if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                item.setType(LTransaction.TRANSACTION_TYPE_TRANSFER_COPY);
                item.setRid(item.getRid() + "2");
                long accountId = item.getAccount();
                item.setAccount(item.getAccount2());
                item.setAccount2(accountId);
                DBTransaction.add(context, item);
                if (postJournal) {
                    journal.updateItem(item);
                }
            }
        }
    }

    public static void update(LTransaction item) {
        update(LApp.ctx, item);
    }

    public static void update(Context context, LTransaction item) {
        ContentValues cv = setValues(item);
        context.getContentResolver().update(DBProvider.URI_TRANSACTIONS, cv, "_id=?", new String[]{"" + item.getId()});
    }

    public static void update(LTransaction item, boolean postJournal) {
        update(LApp.ctx, item, postJournal);
    }

    public static void update(Context context, LTransaction item, boolean postJournal) {
        update(context, item);

        LJournal journal = null;
        if (postJournal) {
            journal = new LJournal();
            journal.updateItem(item);
        }

        //duplicate record for transfer
        if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
            long dbid = 0;
            dbid = DBAccess.getIdByRid(context, DBProvider.URI_TRANSACTIONS, item.getRid() + "2");
            if (dbid > 0) {
                LTransaction item2 = new LTransaction(item);
                item2.setType(LTransaction.TRANSACTION_TYPE_TRANSFER_COPY);
                item2.setRid(item.getRid() + "2");
                item2.setId(dbid);
                item2.setAccount(item.getAccount2());
                item2.setAccount2(item.getAccount());
                update(context, item2);
                if (postJournal) {
                    journal.updateItem(item2);
                }
            }
        }
    }

    public static LTransaction getById(long id) {
        return getById(LApp.ctx, id);
    }

    public static LTransaction getById(Context context, long id) {
        if (id <= 0) return null;

        Cursor csr = null;
        String str = "";
        LTransaction item = new LTransaction();
        try {
            csr = context.getContentResolver().query(DBProvider.URI_TRANSACTIONS, null,
                    "_id=?", new String[]{"" + id}, null);
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find id: " + id + " from log table");
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, item);
            item.setId(id);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get with id: " + id + ":" + e.getMessage());
            item = null;
        }
        if (csr != null) csr.close();
        return item;
    }

    public static long getIdByRid(String rid) {
        return DBAccess.getIdByRid(DBProvider.URI_TRANSACTIONS, rid);
    }

    public static LTransaction getByRid(String rid) {
        return getByRid(LApp.ctx, rid);
    }

    public static LTransaction getByRid(Context context, String rid) {
        Cursor cur = context.getContentResolver().query(DBProvider.URI_TRANSACTIONS, null,
                DBHelper.TABLE_COLUMN_RID + " =?", new String[]{rid}, null);
        if (cur != null && cur.getCount() > 0) {
            if (cur.getCount() != 1) {
                LLog.e(TAG, "unexpected error: duplicated record");
            }
            cur.moveToFirst();
            LTransaction item = new LTransaction();
            getValues(cur, item);
            cur.close();
            return item;
        }
        if (cur != null) cur.close();
        return null;
    }

    public static void deleteByAccount(long accountId) {
        deleteByAccount(LApp.ctx, accountId);
    }

    public static void deleteByAccount(Context context, long accountId) {
        Cursor cursor = getCursorByAccount(context, accountId);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            LTransaction item = new LTransaction();
            do {
                DBTransaction.getValues(cursor, item);
                item.setTimeStampLast(System.currentTimeMillis());
                item.setState(DBHelper.STATE_DELETED);
                update(context, item, true);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
    }

    public static Cursor getCursorByAccount(long accountId) {
        return getCursorByAccount(LApp.ctx, accountId);
    }

    public static Cursor getCursorByAccount(Context context, long accountId) {
        Cursor cur = context.getContentResolver().query(DBProvider.URI_TRANSACTIONS, null,
                DBHelper.TABLE_COLUMN_STATE + " =? AND " + DBHelper.TABLE_COLUMN_ACCOUNT + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE, "" + accountId}, null);
        return cur;
    }

    public static Cursor getAllCursor() {
        return getAllCursor(LApp.ctx);
    }

    public static Cursor getAllCursor(Context context) {
        Cursor cur = context.getContentResolver().query(DBProvider.URI_TRANSACTIONS, null,
                DBHelper.TABLE_COLUMN_STATE + " =?", new String[]{"" + DBHelper.STATE_ACTIVE},
                DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC");
        return cur;
    }

    public static LTransaction getLastItemOfTheDay(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, 0, 0, 0);
        long startMs = calendar.getTimeInMillis();
        long endMs = startMs + 24 * 3600 * 1000;
        LTransaction item = null;

        Cursor cursor = LApp.ctx.getContentResolver().query(DBProvider.URI_TRANSACTIONS, null,
                DBHelper.TABLE_COLUMN_STATE + " =? AND " + DBHelper.TABLE_COLUMN_TIMESTAMP + ">? AND "
                + DBHelper.TABLE_COLUMN_TIMESTAMP + "<?", new String[]{"" + DBHelper.STATE_ACTIVE, "" + startMs, "" + endMs},
                DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC");

        if (cursor != null && cursor.getCount() > 0) {
            item = new LTransaction();
            cursor.moveToLast();
            DBTransaction.getValues(cursor, item);
        }
        if (cursor != null) cursor.close();
        return item;
    }
}
