package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.swoag.logalong.LApp;
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
        return getCursor(LApp.ctx, sortColumn);
    }

    public static Cursor getCursor(Context context, String sortColumn) {
        Cursor cur = null;
        try {
            if (sortColumn != null)
                cur = context.getContentResolver().query(DBProvider.URI_SCHEDULED_TRANSACTIONS, null,
                        DBHelper.TABLE_COLUMN_STATE + "=? OR " + DBHelper.TABLE_COLUMN_STATE + "=?",
                        new String[]{"" + DBHelper.STATE_ACTIVE, "" + DBHelper.STATE_DISABLED}, sortColumn + " ASC");
            else
                cur = context.getContentResolver().query(DBProvider.URI_SCHEDULED_TRANSACTIONS, null,
                        DBHelper.TABLE_COLUMN_STATE + "=? OR " + DBHelper.TABLE_COLUMN_STATE + "=?",
                        new String[]{"" + DBHelper.STATE_ACTIVE, "" + DBHelper.STATE_DISABLED}, null);
        } catch (Exception e) {
        }
        return cur;
    }

    public static long add(LScheduledTransaction sch) {
        return add(LApp.ctx, sch);
    }

    public static long add(Context context, LScheduledTransaction sch) {
        long id = 0;
        try {
            ContentValues cv = setValues(sch);
            Uri uri = context.getContentResolver().insert(DBProvider.URI_SCHEDULED_TRANSACTIONS, cv);
            id = ContentUris.parseId(uri);
        } catch (Exception e) {
        }
        sch.getItem().setId(id);
        return id;
    }

    public static boolean update(LScheduledTransaction sch) {
        return update(LApp.ctx, sch);
    }

    public static boolean update(Context context, LScheduledTransaction sch) {
        try {
            ContentValues cv = setValues(sch);
            context.getContentResolver().update(DBProvider.URI_SCHEDULED_TRANSACTIONS,
                    cv, "_id=?", new String[]{"" + sch.getItem().getId()});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static LScheduledTransaction getById(long id) {
        return getById(LApp.ctx, id);
    }

    public static LScheduledTransaction getById(Context context, long id) {
        LScheduledTransaction sch = null;
        Cursor cur = null;

        try {
            cur = context.getContentResolver().query(DBProvider.URI_SCHEDULED_TRANSACTIONS, null,
                    "_id=?", new String[]{"" + id}, null);
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

    public static LScheduledTransaction getByRid(String rid) {
        return getByRid(LApp.ctx, rid);
    }

    public static LScheduledTransaction getByRid(Context context, String rid) {
        LScheduledTransaction sch = null;
        Cursor cur = null;

        try {
            cur = context.getContentResolver().query(DBProvider.URI_SCHEDULED_TRANSACTIONS, null,
                    DBHelper.TABLE_COLUMN_RID + "=?", new String[]{"" + rid}, null);
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
        DBAccess.updateColumnById(DBProvider.URI_SCHEDULED_TRANSACTIONS, id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static void scanAlarm() {
        Cursor cursor = getCursor(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                LScheduledTransaction sch = new LScheduledTransaction();
                cursor.moveToFirst();
                do {
                    getValues(cursor, sch);
                    sch.setAlarm();
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }
}
