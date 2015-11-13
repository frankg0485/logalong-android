package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LTransaction;

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
        cv.put(DBHelper.TABLE_COLUMN_RID, trans.getRid().toString());
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
        context.getContentResolver().insert(DBProvider.URI_TRANSACTION, cv);

        //duplicate record for transfer
        if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
            cv.put(DBHelper.TABLE_COLUMN_TYPE, LTransaction.TRANSACTION_TYPE_TRANSFER_COPY);
            cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, item.getAccount2());
            cv.put(DBHelper.TABLE_COLUMN_ACCOUNT2, item.getAccount());
            cv.put(DBHelper.TABLE_COLUMN_RID, item.getRid() + "2");
            context.getContentResolver().insert(DBProvider.URI_TRANSACTION, cv);
        }
    }

    public static void update(LTransaction item) {
        synchronized (DBAccess.dbLock) {
            SQLiteDatabase db = DBAccess.getWriteDb();
            ContentValues cv = setValues(item);
            db.update(DBHelper.TABLE_TRANSACTION_NAME, cv, "_id=?", new String[]{"" + item.getId()});

            //duplicate record for transfer
            if ((item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) ||
                    (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY)) {
                long dbid = 0;
                LTransaction item2 = new LTransaction(item);
                if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                    item2.setType(LTransaction.TRANSACTION_TYPE_TRANSFER_COPY);
                    item2.setRid(item.getRid() + "2");
                } else if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
                    item2.setType(LTransaction.TRANSACTION_TYPE_TRANSFER);
                    item2.setRid(item.getRid().substring(0, item.getRid().length() - 1));
                }

                item2.setAccount(item.getAccount2());
                item2.setAccount2(item.getAccount());
                cv = setValues(item2);

                dbid = DBAccess.getIdByRid(DBHelper.TABLE_TRANSACTION_NAME, item2.getRid());
                if (dbid <= 0) {
                    db.insert(DBHelper.TABLE_TRANSACTION_NAME, "", cv);
                } else {
                    db.update(DBHelper.TABLE_TRANSACTION_NAME, cv, "_id=?", new String[]{"" + dbid});
                }
            }

            DBAccess.dirty = true;
        }
    }

    public static void updateOwnerById(int madeBy, long id) {
        synchronized (DBAccess.dbLock) {
            SQLiteDatabase db = DBAccess.getWriteDb();
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.TABLE_COLUMN_MADEBY, madeBy);
            db.update(DBHelper.TABLE_TRANSACTION_NAME, cv, "_id=?", new String[]{"" + id});
            DBAccess.dirty = true;
        }
    }

    public static LTransaction getById(long id) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        String str = "";
        LTransaction item = new LTransaction();
        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME + " WHERE _id=?", new String[]{"" + id});
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find id: " + id + " from log table");
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, item);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get with id: " + id + ":" + e.getMessage());
        }
        if (csr != null) csr.close();
        item.setId(id);
        return item;
    }

    public static long getIdByRid(String rid) {
        return DBAccess.getIdByRid(DBHelper.TABLE_TRANSACTION_NAME, rid);
    }

    public static LTransaction getByRid(String rid) {
        SQLiteDatabase db = DBAccess.getReadDb();

        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME + " WHERE " +
                        DBHelper.TABLE_COLUMN_RID + " =?",
                new String[]{rid});
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

    public static Cursor getCursorInRange(long start, long end) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur;
        if (start == -1 || end == -1) {
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + " =? ORDER BY " + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        } else {
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + " =? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + ">=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + "<? ORDER BY " + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + start, "" + end});
        }
        return cur;
    }

    private static Cursor getCursorInRangeSortBy(String table, String column, long start, long end) {
        String select = "SELECT a._id,"
                + "a." + DBHelper.TABLE_COLUMN_AMOUNT + ","
                + "a." + DBHelper.TABLE_COLUMN_CATEGORY + ","
                + "a." + DBHelper.TABLE_COLUMN_ACCOUNT + ","
                + "a." + DBHelper.TABLE_COLUMN_ACCOUNT2 + ","
                + "a." + DBHelper.TABLE_COLUMN_TAG + ","
                + "a." + DBHelper.TABLE_COLUMN_VENDOR + ","
                + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + ","
                /*
                + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + ","
                */
                + "a." + DBHelper.TABLE_COLUMN_TYPE + ","
                /*
                + "a." + DBHelper.TABLE_COLUMN_STATE + ","
                + "a." + DBHelper.TABLE_COLUMN_MADEBY + ","
                + "a." + DBHelper.TABLE_COLUMN_RID + ","
                */
                + "a." + DBHelper.TABLE_COLUMN_NOTE + ","
                + "b." + DBHelper.TABLE_COLUMN_NAME;

        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur;
        if (start == -1 || end == -1) {
            cur = db.rawQuery(select
                            + " FROM " + DBHelper.TABLE_TRANSACTION_NAME + " AS a LEFT JOIN " + table + " AS b "
                            + "ON a." + column + " = b._id "
                            + "WHERE a." + DBHelper.TABLE_COLUMN_STATE + " =? ORDER BY "
                            + "b." + DBHelper.TABLE_COLUMN_NAME + " ASC, "
                            + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        } else {
            cur = db.rawQuery(select
                            + " FROM " + DBHelper.TABLE_TRANSACTION_NAME + " AS a LEFT JOIN " + table + " AS b "
                            + "ON a." + column + " = b._id "
                            + "WHERE a." + DBHelper.TABLE_COLUMN_STATE + " =? AND "
                            + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + ">=? AND "
                            + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + "<? ORDER BY "
                            + "b." + DBHelper.TABLE_COLUMN_NAME + " ASC, "
                            + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + start, "" + end});
        }
        return cur;
    }

    public static Cursor getCursorInRangeSortByAccount(long start, long end) {
        return getCursorInRangeSortBy(DBHelper.TABLE_ACCOUNT_NAME, DBHelper.TABLE_COLUMN_ACCOUNT, start, end);
    }

    public static Cursor getCursorInRangeSortByCategory(long start, long end) {
        return getCursorInRangeSortBy(DBHelper.TABLE_CATEGORY_NAME, DBHelper.TABLE_COLUMN_CATEGORY, start, end);
    }

    public static Cursor getCursorInRangeSortByVendor(long start, long end) {
        return getCursorInRangeSortBy(DBHelper.TABLE_VENDOR_NAME, DBHelper.TABLE_COLUMN_VENDOR, start, end);
    }

    public static Cursor getCursorInRangeSortByTag(long start, long end) {
        return getCursorInRangeSortBy(DBHelper.TABLE_TAG_NAME, DBHelper.TABLE_COLUMN_TAG, start, end);
    }

    public static Cursor getCursorByAccount(long accountId) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                        + " WHERE " + DBHelper.TABLE_COLUMN_STATE + " =? AND " + DBHelper.TABLE_COLUMN_ACCOUNT + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE, "" + accountId});
        return cur;
    }

    public static Cursor getAllCursor() {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                        + " WHERE " + DBHelper.TABLE_COLUMN_STATE + " =? ORDER BY " + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                new String[]{"" + DBHelper.STATE_ACTIVE});
        return cur;
    }
}
