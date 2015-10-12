package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;

import com.swoag.logalong.entities.LTransaction;

import java.util.UUID;

public class DBTransaction {
    private static final String TAG = DBTransaction.class.getSimpleName();

    public static void setValues(ContentValues cv, LTransaction trans) {
        cv.put(DBHelper.TABLE_COLUMN_TYPE, trans.getType());
        cv.put(DBHelper.TABLE_COLUMN_STATE, trans.getState());
        cv.put(DBHelper.TABLE_COLUMN_CATEGORY, trans.getCategory());
        cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, trans.getAccount());
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
}
