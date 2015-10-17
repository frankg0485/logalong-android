package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
        synchronized (DBAccess.dbLock) {
            SQLiteDatabase db = DBAccess.getWriteDb();
            ContentValues cv = setValues(item);
            db.insert(DBHelper.TABLE_TRANSACTION_NAME, "", cv);

            //duplicate record for transfer
            if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                cv.put(DBHelper.TABLE_COLUMN_TYPE, LTransaction.TRANSACTION_TYPE_TRANSFER_COPY);
                cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, item.getAccount2());
                cv.put(DBHelper.TABLE_COLUMN_ACCOUNT2, item.getAccount());
                cv.put(DBHelper.TABLE_COLUMN_RID, item.getRid() + "2");
                db.insert(DBHelper.TABLE_TRANSACTION_NAME, "", cv);
            }
            DBAccess.dirty = true;
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
}
