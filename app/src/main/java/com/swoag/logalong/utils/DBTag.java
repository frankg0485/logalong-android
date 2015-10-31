package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LTag;

import java.util.UUID;

public class DBTag {
    private static final String TAG = DBTag.class.getSimpleName();

    private static ContentValues setValues(LTag tag) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, tag.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, tag.getState());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, tag.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_RID, tag.getRid().toString());
        return cv;
    }

    private static void getValues(Cursor cur, LTag tag) {
        tag.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        tag.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        tag.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        tag.setRid(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID)));
        tag.setId(cur.getLong(0));
    }

    public static long add(LTag tag) {
        long id = -1;
        synchronized (DBAccess.dbLock) {
            SQLiteDatabase db = DBAccess.getWriteDb();
            ContentValues cv = setValues(tag);
            id = db.insert(DBHelper.TABLE_TAG_NAME, "", cv);
            DBAccess.dirty = true;
        }
        return id;
    }

    public static Cursor getCursorSortedBy(String sortColumn) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur;
        if (sortColumn != null)
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TAG_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=? ORDER BY " + sortColumn + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        else
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TAG_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        return cur;
    }

    public static boolean updateColumnById(long id, String column, String value) {
        return DBAccess.updateColumnById(DBHelper.TABLE_TAG_NAME, id, column, value);
    }

    public static long getIdByName(String name) {
        return DBAccess.getIdByName(DBHelper.TABLE_TAG_NAME, name);
    }
}
