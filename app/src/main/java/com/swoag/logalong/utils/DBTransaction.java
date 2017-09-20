package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LTransactionDetails;

import java.util.Calendar;

public class DBTransaction extends DBGeneric<LTransaction> {
    private static final String TAG = DBTransaction.class.getSimpleName();

    private static DBTransaction instance;

    private static final String[] transaction_columns = new String[]{
            "_id",
            DBHelper.TABLE_COLUMN_TYPE,
            DBHelper.TABLE_COLUMN_STATE,
            DBHelper.TABLE_COLUMN_CATEGORY,
            DBHelper.TABLE_COLUMN_ACCOUNT,
            DBHelper.TABLE_COLUMN_ACCOUNT2,
            DBHelper.TABLE_COLUMN_MADEBY,
            DBHelper.TABLE_COLUMN_CHANGEBY,
            DBHelper.TABLE_COLUMN_AMOUNT,
            DBHelper.TABLE_COLUMN_RID,
            DBHelper.TABLE_COLUMN_TIMESTAMP,
            DBHelper.TABLE_COLUMN_TIMESTAMP_CREATE,
            DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE,
            DBHelper.TABLE_COLUMN_NOTE,
            DBHelper.TABLE_COLUMN_TAG,
            DBHelper.TABLE_COLUMN_VENDOR,
            DBHelper.TABLE_COLUMN_GID};

    public DBTransaction() {
    }

    public static DBTransaction getInstance() {
        if (null == instance) {
            instance = new DBTransaction();
        }
        return instance;
    }

    @Override
    public LTransaction getValues(Cursor cursor, LTransaction trans) {
        if (null == trans) trans = new LTransaction();
        trans.setType(cursor.getInt(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_TYPE)));
        trans.setState(cursor.getInt(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_STATE)));
        trans.setAccount(cursor.getLong(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_ACCOUNT)));
        trans.setAccount2(cursor.getLong(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_ACCOUNT2)));
        trans.setCategory(cursor.getLong(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_CATEGORY)));
        trans.setTag(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TAG)));
        trans.setVendor(cursor.getLong(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_VENDOR)));
        trans.setValue(cursor.getDouble(cursor.getColumnIndex(DBHelper.TABLE_COLUMN_AMOUNT)));
        trans.setNote(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NOTE)));
        trans.setCreateBy(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_MADEBY)));
        trans.setChangeBy(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CHANGEBY)));
        trans.setRid(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID)));
        trans.setTimeStamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)));
        trans.setTimeStampCreate(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_CREATE)));
        trans.setTimeStampLast(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper
                .TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        trans.setGid(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_GID)));
        trans.setId(cursor.getLong(0));
        return trans;
    }

    @Override
    ContentValues setValues(LTransaction trans, boolean update) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_TYPE, trans.getType());
        if (!update)
            cv.put(DBHelper.TABLE_COLUMN_STATE, trans.getState());
        cv.put(DBHelper.TABLE_COLUMN_CATEGORY, trans.getCategory());
        cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, trans.getAccount());
        cv.put(DBHelper.TABLE_COLUMN_ACCOUNT2, trans.getAccount2());
        cv.put(DBHelper.TABLE_COLUMN_MADEBY, trans.getCreateBy());
        cv.put(DBHelper.TABLE_COLUMN_CHANGEBY, trans.getChangeBy());
        cv.put(DBHelper.TABLE_COLUMN_AMOUNT, trans.getValue());
        cv.put(DBHelper.TABLE_COLUMN_RID, trans.getRid());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP, trans.getTimeStamp());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_CREATE, trans.getTimeStampCreate());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, trans.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_NOTE, trans.getNote());
        cv.put(DBHelper.TABLE_COLUMN_TAG, trans.getTag());
        cv.put(DBHelper.TABLE_COLUMN_VENDOR, trans.getVendor());
        cv.put(DBHelper.TABLE_COLUMN_GID, trans.getGid());
        return cv;
    }

    @Override
    String[] getColumns() {
        return transaction_columns;
    }

    @Override
    Uri getUri() {
        return DBProvider.URI_TRANSACTIONS;
    }

    @Override
    long getId(LTransaction trans) {
        return trans.getId();
    }

    @Override
    void setId(LTransaction trans, long id) {
        trans.setId(id);
    }

    public long add2(LTransaction t) {
        if (t.getType() != LTransaction.TRANSACTION_TYPE_TRANSFER) {
            LLog.e(TAG, "unexpected error, wrong add API called for invalid transaction type");
            return 0;
        }

        ContentValues cv = setValues(t, false);
        long id = -1;
        try {
            Uri uri = LApp.ctx.getContentResolver().insert(getUri(), cv);
            id = ContentUris.parseId(uri);
            setId(t, id);

            LTransaction t2 = new LTransaction(t);
            t2.setType(LTransaction.TRANSACTION_TYPE_TRANSFER_COPY);
            t2.setAccount(t.getAccount2());
            t2.setAccount2(t.getAccount());
            cv = setValues(t2, false);
            LApp.ctx.getContentResolver().insert(getUri(), cv);

        } catch (Exception e) {
            LLog.w(TAG, "unable to add entry: " + e.getMessage());
        }
        return id;
    }

    public boolean update2(LTransaction t) {
        if (t.getType() != LTransaction.TRANSACTION_TYPE_TRANSFER) {
            LLog.e(TAG, "unexpected error, wrong update API called for invalid transaction type");
            return false;
        }
        try {
            ContentValues cv = setValues(t, true);
            LApp.ctx.getContentResolver().update(getUri(), cv, "_id=?", new String[]{"" + t.getId()});

            LTransaction t2 = new LTransaction(t);
            t2.setType(LTransaction.TRANSACTION_TYPE_TRANSFER_COPY);
            t2.setAccount(t.getAccount2());
            t2.setAccount2(t.getAccount());
            cv = setValues(t2, true);

            LApp.ctx.getContentResolver().update(getUri(), cv, DBHelper.TABLE_COLUMN_RID + "=? AND "
                    + DBHelper.TABLE_COLUMN_TYPE + "=?", new String[]{"" + t2.getRid(),
                    "" + LTransaction.TRANSACTION_TYPE_TRANSFER_COPY});
        } catch (Exception e) {
            LLog.w(TAG, "unable to update entry: " + e.getMessage());
            return false;
        }
        return true;
    }

    public LTransaction getByRid(long rid, boolean copy) {
        if (rid == 0) return null;

        try {
            Cursor csr;

            csr = LApp.ctx.getContentResolver().query(getUri(), getColumns(),
                    DBHelper.TABLE_COLUMN_RID + "=? AND " + DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_TYPE + "=?",
                    new String[]{"" + rid, "" + DBHelper.STATE_ACTIVE,
                            "" + (copy ? LTransaction.TRANSACTION_TYPE_TRANSFER_COPY : LTransaction
                                    .TRANSACTION_TYPE_TRANSFER),}, null);

            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find entry with rid: " + rid + "@" + getUri());
                    return null;
                }

                csr.moveToFirst();
                LTransaction t = getValues(csr, null);
                csr.close();
                return t;
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get entry with rid: " + rid + ":" + e.getMessage());
        }
        return null;
    }

    public boolean deleteTransferByRid(long rid) {
        if (rid == 0) return false;

        try {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
            /*LApp.ctx.getContentResolver().update(getUri(), cv, DBHelper.TABLE_COLUMN_RID + "=? AND ("
                    + DBHelper.TABLE_COLUMN_TYPE + "=? OR "
                    + DBHelper.TABLE_COLUMN_TYPE + "=?)",
                    new String[]{"" + rid, "" + LTransaction.TRANSACTION_TYPE_TRANSFER_COPY,
                    "" + LTransaction.TRANSACTION_TYPE_TRANSFER});
            */
            // sucks that we have to delete twice, due to the way account balance is updated in DBProvider.
            LApp.ctx.getContentResolver().update(getUri(), cv, DBHelper.TABLE_COLUMN_RID + "=? AND "
                            + DBHelper.TABLE_COLUMN_TYPE + "=?",
                    new String[]{"" + rid,
                            "" + LTransaction.TRANSACTION_TYPE_TRANSFER});
            LApp.ctx.getContentResolver().update(getUri(), cv, DBHelper.TABLE_COLUMN_RID + "=? AND "
                            + DBHelper.TABLE_COLUMN_TYPE + "=?",
                    new String[]{"" + rid, "" + LTransaction.TRANSACTION_TYPE_TRANSFER_COPY});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void deleteByAccount(long accountId) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
        cv.put(DBHelper.TABLE_COLUMN_RID, "");
        LApp.ctx.getContentResolver().update(DBProvider.URI_TRANSACTIONS, cv,
                DBHelper.TABLE_COLUMN_STATE + "=? AND " +
                        DBHelper.TABLE_COLUMN_ACCOUNT + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE, "" + accountId});
    }

    public Cursor getCursorByAccount(long accountId) {
        Cursor cur = LApp.ctx.getContentResolver().query(DBProvider.URI_TRANSACTIONS, transaction_columns,
                DBHelper.TABLE_COLUMN_STATE + " =? AND " + DBHelper.TABLE_COLUMN_ACCOUNT + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE, "" + accountId}, null);
        return cur;
    }

    public Cursor getAllCursor() {
        Cursor cur = LApp.ctx.getContentResolver().query(DBProvider.URI_TRANSACTIONS, transaction_columns,
                DBHelper.TABLE_COLUMN_STATE + " =?", new String[]{"" + DBHelper.STATE_ACTIVE},
                DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC");
        if (cur == null || cur.getCount() < 1) {
            if (cur != null) cur.close();
            return null;
        }
        return cur;
    }

    public LTransaction getLastItemOfTheDay(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, 0, 0, 0);
        long startMs = calendar.getTimeInMillis();
        long endMs = startMs + 24 * 3600 * 1000;
        LTransaction item = null;

        Cursor cursor = LApp.ctx.getContentResolver().query(DBProvider.URI_TRANSACTIONS, transaction_columns,
                DBHelper.TABLE_COLUMN_STATE + " =? AND " + DBHelper.TABLE_COLUMN_TIMESTAMP + ">? AND "
                        + DBHelper.TABLE_COLUMN_TIMESTAMP + "<?", new String[]{"" + DBHelper.STATE_ACTIVE, "" +
                        startMs, "" + endMs},
                DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC");

        if (cursor != null && cursor.getCount() > 0) {
            item = new LTransaction();
            cursor.moveToLast();
            getValues(cursor, item);
        }
        if (cursor != null) cursor.close();
        return item;
    }

    public LTransactionDetails getDetailsByIdAll(long id) {
        LTransactionDetails details = null;
        LTransaction transaction = getByIdAll(id);
        if (null != transaction) {
            details = new LTransactionDetails();
            details.setTransaction(transaction);
        }
        return details;
    }

    public LTransactionDetails getDetailsById(long id) {
        LTransactionDetails details = null;

        Cursor cursor = LApp.ctx.getContentResolver().query(DBProvider.URI_TRANSACTIONS_ALL, new String[]{
                        "s." + DBHelper.TABLE_COLUMN_AMOUNT + " AS s_amount",
                        "s." + DBHelper.TABLE_COLUMN_TYPE + " AS s_type",
                        "s." + DBHelper.TABLE_COLUMN_MADEBY + " AS s_by",
                        "s." + DBHelper.TABLE_COLUMN_CHANGEBY + " AS s_cby",
                        "s." + DBHelper.TABLE_COLUMN_RID + " AS s_rid",
                        "s." + DBHelper.TABLE_COLUMN_TIMESTAMP + " AS s_timestamp",
                        "s." + DBHelper.TABLE_COLUMN_TIMESTAMP_CREATE + " AS s_timestamp_create",
                        "s." + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + " AS s_timestamp_last",
                        "s." + DBHelper.TABLE_COLUMN_GID + " AS s_gid",
                        "s." + DBHelper.TABLE_COLUMN_STATE + " AS s_state",
                        "s." + DBHelper.TABLE_COLUMN_NOTE + " AS s_note",
                        "s." + DBHelper.TABLE_COLUMN_ACCOUNT + " AS s_account",
                        "s." + DBHelper.TABLE_COLUMN_ACCOUNT2 + " AS s_account2",
                        "s." + DBHelper.TABLE_COLUMN_CATEGORY + " AS s_category",
                        "s." + DBHelper.TABLE_COLUMN_TAG + " AS s_tag",
                        "s." + DBHelper.TABLE_COLUMN_VENDOR + " AS s_vendor",
                        "a1." + DBHelper.TABLE_COLUMN_NAME + " AS a1_name",
                        "a1._id AS a1_id",
                        "a1." + DBHelper.TABLE_COLUMN_GID + " AS a1_gid",
                        "a2." + DBHelper.TABLE_COLUMN_NAME + " AS a2_name",
                        "a2._id AS a2_id",
                        "a2." + DBHelper.TABLE_COLUMN_GID + " AS a2_gid",
                        "v." + DBHelper.TABLE_COLUMN_NAME + " AS v_name",
                        "v._id AS v_id",
                        "v." + DBHelper.TABLE_COLUMN_GID + " AS v_gid",
                        "t." + DBHelper.TABLE_COLUMN_NAME + " AS t_name",
                        "t._id AS t_id",
                        "t." + DBHelper.TABLE_COLUMN_GID + " AS t_gid",
                        "c." + DBHelper.TABLE_COLUMN_NAME + " AS c_name",
                        "c._id AS c_id",
                        "c." + DBHelper.TABLE_COLUMN_GID + " AS c_gid"
                },
                "s._id=?", new String[]{"" + id}, null);

        if (cursor != null && cursor.getCount() != 0) {
            details = new LTransactionDetails();
            cursor.moveToFirst();
            details.getTransaction().setValue(cursor.getDouble(cursor.getColumnIndex("s_amount")));
            details.getTransaction().setType(cursor.getInt(cursor.getColumnIndex("s_type")));
            details.getTransaction().setCreateBy(cursor.getLong(cursor.getColumnIndex("s_by")));
            details.getTransaction().setChangeBy(cursor.getLong(cursor.getColumnIndex("s_cby")));
            details.getTransaction().setRid(cursor.getLong(cursor.getColumnIndex("s_rid")));
            details.getTransaction().setTimeStamp(cursor.getLong(cursor.getColumnIndex("s_timestamp")));
            details.getTransaction().setTimeStampCreate(cursor.getLong(cursor.getColumnIndex("s_timestamp_create")));
            details.getTransaction().setTimeStampLast(cursor.getLong(cursor.getColumnIndex("s_timestamp_last")));
            details.getTransaction().setId(id);
            details.getTransaction().setGid(cursor.getLong(cursor.getColumnIndex("s_gid")));
            details.getTransaction().setNote(cursor.getString(cursor.getColumnIndex("s_note")));
            details.getTransaction().setAccount(cursor.getLong(cursor.getColumnIndex("s_account")));
            details.getTransaction().setAccount2(cursor.getLong(cursor.getColumnIndex("s_account2")));
            details.getTransaction().setCategory(cursor.getLong(cursor.getColumnIndex("s_category")));
            details.getTransaction().setTag(cursor.getLong(cursor.getColumnIndex("s_tag")));
            details.getTransaction().setVendor(cursor.getLong(cursor.getColumnIndex("s_vendor")));
            details.getAccount().setName(cursor.getString(cursor.getColumnIndex("a1_name")));
            details.getAccount().setGid(cursor.getLong(cursor.getColumnIndex("a1_gid")));
            details.getAccount2().setName(cursor.getString(cursor.getColumnIndex("a2_name")));
            details.getAccount2().setGid(cursor.getLong(cursor.getColumnIndex("a2_gid")));
            details.getCategory().setName(cursor.getString(cursor.getColumnIndex("c_name")));
            details.getCategory().setGid(cursor.getLong(cursor.getColumnIndex("c_gid")));
            details.getTag().setName(cursor.getString(cursor.getColumnIndex("t_name")));
            details.getTag().setGid(cursor.getLong(cursor.getColumnIndex("t_gid")));
            details.getVendor().setName(cursor.getString(cursor.getColumnIndex("v_name")));
            details.getVendor().setGid(cursor.getLong(cursor.getColumnIndex("v_gid")));
        }

        if (cursor != null) cursor.close();
        return details;
    }
}
