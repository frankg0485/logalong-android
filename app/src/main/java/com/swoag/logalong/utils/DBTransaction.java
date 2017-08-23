package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

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
    public  LTransaction getValues(Cursor cursor, LTransaction trans) {
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
        trans.setTimeStamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)));
        trans.setTimeStampCreate(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_CREATE)));
        trans.setTimeStampLast(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper
                .TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        trans.setGid(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_GID)));
        trans.setId(cursor.getLong(0));
        return trans;
    }

    @Override
    ContentValues setValues(LTransaction trans) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_TYPE, trans.getType());
        cv.put(DBHelper.TABLE_COLUMN_STATE, trans.getState());
        cv.put(DBHelper.TABLE_COLUMN_CATEGORY, trans.getCategory());
        cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, trans.getAccount());
        cv.put(DBHelper.TABLE_COLUMN_ACCOUNT2, trans.getAccount2());
        cv.put(DBHelper.TABLE_COLUMN_MADEBY, trans.getCreateBy());
        cv.put(DBHelper.TABLE_COLUMN_CHANGEBY, trans.getChangeBy());
        cv.put(DBHelper.TABLE_COLUMN_AMOUNT, trans.getValue());
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


    /*

        public static void setValues(ContentValues cv, LTransaction trans) {
            cv.put(DBHelper.TABLE_COLUMN_TYPE, trans.getType());
            cv.put(DBHelper.TABLE_COLUMN_STATE, trans.getState());
            cv.put(DBHelper.TABLE_COLUMN_CATEGORY, trans.getCategory());
            cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, trans.getAccount());
            cv.put(DBHelper.TABLE_COLUMN_ACCOUNT2, trans.getAccount2());
            cv.put(DBHelper.TABLE_COLUMN_MADEBY, trans.getCreateBy());
            cv.put(DBHelper.TABLE_COLUMN_CHANGEBY, trans.getChangeBy());
            cv.put(DBHelper.TABLE_COLUMN_AMOUNT, trans.getValue());
            cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP, trans.getTimeStamp());
            cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_CREATE, trans.getTimeStampCreate());
            cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, trans.getTimeStampLast());
            cv.put(DBHelper.TABLE_COLUMN_NOTE, trans.getNote());
            cv.put(DBHelper.TABLE_COLUMN_TAG, trans.getTag());
            cv.put(DBHelper.TABLE_COLUMN_VENDOR, trans.getVendor());
            cv.put(DBHelper.TABLE_COLUMN_GID, trans.getGid());
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
            trans.setCreateBy(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_MADEBY)));
            trans.setChangeBy(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CHANGEBY)));
            trans.setTimeStamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)));
            trans.setTimeStampCreate(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper
            .TABLE_COLUMN_TIMESTAMP_CREATE)));
            trans.setTimeStampLast(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper
                    .TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
            trans.setGid(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_GID)));
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

        private static void add(Context context, LTransaction item) {
            ContentValues cv = setValues(item);
            context.getContentResolver().insert(DBProvider.URI_TRANSACTIONS, cv);
        }

        public static void add(LTransaction item, boolean duplicateTransfer, boolean postJournal) {
            add(LApp.ctx, item, duplicateTransfer, postJournal);
        }

        private static void add(Context context, LTransaction item, boolean duplicateTransfer, boolean postJournal) {
            ContentValues cv = setValues(item);
            //if (postJournal) DBAccountBalance.setAutoBalanceUpdateEnabled(true);

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
                    //TODO: item.setGid(item.getGid() + "2");
                    long accountId = item.getAccount();
                    item.setAccount(item.getAccount2());
                    item.setAccount2(accountId);
                    DBTransaction.add(context, item);
                    if (postJournal) {
                        journal.updateItem(item);
                    }
                }
            }
            //if (postJournal) DBAccountBalance.setAutoBalanceUpdateEnabled(false);
        }

        public static void update(LTransaction item) {
            update(LApp.ctx, item);
        }

        private static void update(Context context, LTransaction item) {
            ContentValues cv = setValues(item);
            context.getContentResolver().update(DBProvider.URI_TRANSACTIONS, cv, "_id=?", new String[]{"" + item
            .getId()});
        }

        public static void update(LTransaction item, boolean postJournal) {
            update(LApp.ctx, item, postJournal);
        }

        public static void update(Context context, LTransaction item, boolean postJournal) {
            //if (postJournal) DBAccountBalance.setAutoBalanceUpdateEnabled(true);
            update(context, item);

            LJournal journal = null;
            if (postJournal) {
                journal = new LJournal();
                journal.updateItem(item);
            }

            //duplicate record for transfer
            if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                long dbid = 0;
                //TODO: dbid = DBAccess.getIdByRid(context, DBProvider.URI_TRANSACTIONS, item.getRid() + "2");

                if (dbid > 0) {
                    LTransaction item2 = new LTransaction(item);
                    item2.setType(LTransaction.TRANSACTION_TYPE_TRANSFER_COPY);
                    //TODO: item2.setRid(item.getRid() + "2");
                    item2.setId(dbid);
                    item2.setAccount(item.getAccount2());
                    item2.setAccount2(item.getAccount());
                    update(context, item2);
                    if (postJournal) {
                        journal.updateItem(item2);
                    }
                }
            }
            //if (postJournal) DBAccountBalance.setAutoBalanceUpdateEnabled(false);
        }

        public static boolean updateColumnById(long id, String column, long value) {
            return DBAccess.updateColumnById(DBProvider.URI_TRANSACTIONS, id, column, value);
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
                if (csr != null) {
                    if (csr.getCount() != 1) {
                        LLog.w(TAG, "unable to find id: " + id + " from log table");
                        csr.close();
                        return null;
                    }

                    csr.moveToFirst();
                    getValues(csr, item);
                    item.setId(id);
                }
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

        public static LTransaction getByGid(long gid) {
            return getByGid(LApp.ctx, gid);
        }

        public static LTransaction getByGid(Context context, long gid) {
            Cursor cur = context.getContentResolver().query(DBProvider.URI_TRANSACTIONS, null,
                    DBHelper.TABLE_COLUMN_GID + "=?", new String[]{"" + gid}, null);
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
    */
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
