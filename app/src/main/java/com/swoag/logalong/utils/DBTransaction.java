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

        //duplicate record for transfer
        if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
            cv.put(DBHelper.TABLE_COLUMN_TYPE, LTransaction.TRANSACTION_TYPE_TRANSFER_COPY);
            cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, item.getAccount2());
            cv.put(DBHelper.TABLE_COLUMN_ACCOUNT2, item.getAccount());
            cv.put(DBHelper.TABLE_COLUMN_RID, item.getRid() + "2");
            context.getContentResolver().insert(DBProvider.URI_TRANSACTIONS, cv);
        }
    }

    public static void update(LTransaction item) {
        update(LApp.ctx, item);
    }

    public static void update(Context context, LTransaction item) {
        ContentValues cv = setValues(item);
        context.getContentResolver().update(DBProvider.URI_TRANSACTIONS, cv, "_id=?", new String[]{"" + item.getId()});

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

            dbid = DBAccess.getIdByRid(context, DBProvider.URI_TRANSACTIONS, item2.getRid());
            if (dbid <= 0) {
                context.getContentResolver().insert(DBProvider.URI_TRANSACTIONS, cv);
            } else {
                context.getContentResolver().update(DBProvider.URI_TRANSACTIONS, cv,
                        "_id=?", new String[]{"" + dbid});
            }
        }
    }

    public static LTransaction getById(long id) {
        return getById(LApp.ctx, id);
    }

    public static LTransaction getById(Context context, long id) {
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
        /*ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
        context.getContentResolver().update(DBProvider.URI_TRANSACTIONS, cv,
                DBHelper.TABLE_COLUMN_STATE + "=? AND " +
                        DBHelper.TABLE_COLUMN_ACCOUNT + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE, "" + accountId});
        */
        Cursor cursor = getCursorByAccount(context, accountId);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            LTransaction item = new LTransaction();
            do {
                DBTransaction.getValues(cursor, item);
                if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER
                        || item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
                    LTransaction item2 = new LTransaction(item);
                    //item2.setTimeStampLast(System.currentTimeMillis());
                    item2.setAccount(item.getAccount2());

                    if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                        item2.setType(LTransaction.TRANSACTION_TYPE_INCOME);
                        item2.setRid(item.getRid());
                    } else if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
                        item2.setType(LTransaction.TRANSACTION_TYPE_EXPENSE);
                        item2.setRid(item.getRid().substring(0, item.getRid().length() - 1));
                    }
                    item2.setId(getIdByRid(item2.getRid()));

                    //delete both records first
                    item.setState(DBHelper.STATE_DELETED);
                    update(context, item);

                    //update the original record
                    //WARNING: this update changes the record state (from DELETED to ACTIVE), it also
                    //         may change the ACCOUNT, thus it has impact on how delete/undelete is
                    //         supported in DbProvider, when updating account balance.
                    update(context, item2);
                } else {
                    //do NOT change timestamp, otherwise, deleted record can not be revived by
                    //sharing account again from peer.
                    //item.setTimeStampLast(System.currentTimeMillis());
                    item.setState(DBHelper.STATE_DELETED);
                    update(context, item);
                }
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
}
