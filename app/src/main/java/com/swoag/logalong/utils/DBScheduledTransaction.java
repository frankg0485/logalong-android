package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LScheduledTransaction;
import com.swoag.logalong.entities.LVendor;

import java.util.HashSet;
import java.util.UUID;

public class DBScheduledTransaction {
    private static final String TAG = DBScheduledTransaction.class.getSimpleName();

    private static ContentValues setValues(LScheduledTransaction sch) {
        ContentValues cv = new ContentValues();

        DBTransaction.setValues(cv, sch.getItem());

        cv.put(DBHelper.TABLE_COLUMN_REPEAT_COUNT, sch.getRepeatCount());
        cv.put(DBHelper.TABLE_COLUMN_REPEAT_UNIT, sch.getRepeatUnit());
        cv.put(DBHelper.TABLE_COLUMN_REPEAT_INTERVAL, sch.getRepeatInterval());
        cv.put(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP, sch.getTimestamp());
        return cv;
    }

    private static void getValues(Cursor cur, LScheduledTransaction sch) {

        DBTransaction.getValues(cur, sch.getItem());

        sch.setRepeatCount(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_REPEAT_COUNT)));
        sch.setRepeatUnit(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_REPEAT_UNIT)));
        sch.setRepeatInterval(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_REPEAT_INTERVAL)));
        sch.setTimestamp(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP)));
    }

    public static Cursor getCursor(String sortColumn) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur = null;
        try {
            if (sortColumn != null)
                cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_SCHEDULED_TRANSACTION_NAME
                                + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=? ORDER BY " + sortColumn + " ASC",
                        new String[]{"" + DBHelper.STATE_ACTIVE});
            else
                cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_SCHEDULED_TRANSACTION_NAME
                                + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                        new String[]{"" + DBHelper.STATE_ACTIVE});
        } catch (Exception e) {
        }
        return cur;
    }

    public static long add(LScheduledTransaction sch) {
        long id = 0;
        synchronized (DBAccess.dbLock) {
            SQLiteDatabase db = DBAccess.getWriteDb();
            ContentValues cv = setValues(sch);
            id = db.insert(DBHelper.TABLE_SCHEDULED_TRANSACTION_NAME, "", cv);
            DBAccess.dirty = true;
        }
        return id;
    }

    public static LScheduledTransaction getById(long id) {
        SQLiteDatabase db = DBAccess.getReadDb();
        LScheduledTransaction sch = null;
        Cursor cur = null;

        try {
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_SCHEDULED_TRANSACTION_NAME
                    + " WHERE _id=?", new String[]{"" + id});
            if (cur != null && cur.getCount() > 0) {
                cur.moveToFirst();
                sch = new LScheduledTransaction();
                getValues(cur, sch);
            }

            if (cur != null) cur.close();
        } catch (Exception e) {
        }

        return sch;
    }

    public static void deleteById(long id) {
        DBAccess.updateStateById(DBHelper.TABLE_SCHEDULED_TRANSACTION_NAME, id, DBHelper.STATE_DELETED);
    }
}
