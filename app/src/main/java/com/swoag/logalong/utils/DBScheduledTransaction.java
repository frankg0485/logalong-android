package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LScheduledTransaction;

public class DBScheduledTransaction extends DBGeneric<LScheduledTransaction> {
    private static final String TAG = DBScheduledTransaction.class.getSimpleName();
    private static DBScheduledTransaction instance;

    private static final String[] schedule_columns = new String[] {
            DBHelper.TABLE_COLUMN_REPEAT_COUNT,
            DBHelper.TABLE_COLUMN_REPEAT_UNIT,
            DBHelper.TABLE_COLUMN_REPEAT_INTERVAL,
            DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP,
            DBHelper.TABLE_COLUMN_ENABLED
    };

    public DBScheduledTransaction() {
    }

    public static DBScheduledTransaction getInstance() {
        if (null == instance) {
            instance = new DBScheduledTransaction();
        }
        return instance;
    }

    @Override
    LScheduledTransaction getValues(Cursor cur, LScheduledTransaction sch) {
        if (null == sch) sch = new LScheduledTransaction();

        DBTransaction.getInstance().getValues(cur, sch);

        sch.setRepeatCount(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_REPEAT_COUNT)));
        sch.setRepeatUnit(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_REPEAT_UNIT)));
        sch.setRepeatInterval(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_REPEAT_INTERVAL)));
        sch.setNextTime(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP)));
        sch.setEnabled(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ENABLED)) == 0? false : true);

        return sch;
    }

    @Override
    ContentValues setValues(LScheduledTransaction sch, boolean update) {
        ContentValues cv = DBTransaction.getInstance().setValues(sch, update);

        cv.put(DBHelper.TABLE_COLUMN_REPEAT_COUNT, sch.getRepeatCount());
        cv.put(DBHelper.TABLE_COLUMN_REPEAT_UNIT, sch.getRepeatUnit());
        cv.put(DBHelper.TABLE_COLUMN_REPEAT_INTERVAL, sch.getRepeatInterval());
        cv.put(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP, sch.getNextTime());
        cv.put(DBHelper.TABLE_COLUMN_ENABLED, sch.isEnabled()? 1 : 0);
        return cv;
    }

    @Override
    String[] getColumns() {
        return concat(DBTransaction.getInstance().getColumns(), schedule_columns);
    }

    @Override
    Uri getUri() {
        return DBProvider.URI_SCHEDULED_TRANSACTIONS;
    }

    @Override
    long getId(LScheduledTransaction sch) {
        return sch.getId();
    }

    @Override
    void setId(LScheduledTransaction sch, long id) {
        sch.setId(id);
    }

    private String[] concat(String[] a, String[] b) {
        int aLen = a.length;
        int bLen = b.length;
        String[] c= new String[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public Cursor getCursor(String sortColumn) {
        Cursor cur = null;
        try {
            if (sortColumn != null)
                cur = LApp.ctx.getContentResolver().query(DBProvider.URI_SCHEDULED_TRANSACTIONS, null,
                        DBHelper.TABLE_COLUMN_STATE + "=?",
                        new String[]{"" + DBHelper.STATE_ACTIVE}, sortColumn + " ASC");
            else
                cur = LApp.ctx.getContentResolver().query(DBProvider.URI_SCHEDULED_TRANSACTIONS, null,
                        DBHelper.TABLE_COLUMN_STATE + "=?",
                        new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        } catch (Exception e) {
        }
        return cur;
    }

    public void deleteByAccount(long accountId) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
        cv.put(DBHelper.TABLE_COLUMN_IRID, 0);
        LApp.ctx.getContentResolver().update(DBProvider.URI_SCHEDULED_TRANSACTIONS, cv,
                DBHelper.TABLE_COLUMN_STATE + "=? AND ( " +
                        DBHelper.TABLE_COLUMN_ACCOUNT + "=? OR " +
                        DBHelper.TABLE_COLUMN_ACCOUNT2 + "=? )",
                new String[]{"" + DBHelper.STATE_ACTIVE, "" + accountId, "" + accountId});
    }

    /*
    public static void scanAlarm() {
        Cursor cursor = getActiveCursor(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                LScheduledTransaction sch = new LScheduledTransaction();
                cursor.moveToFirst();
                do {
                    getValues(cursor, sch);
                    sch.scanNextTimeMs();
                    sch.setAlarm();
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    } */
}
