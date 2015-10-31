package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;

import java.util.UUID;

public class DBCategory {
    private static final String TAG = DBCategory.class.getSimpleName();

    private static ContentValues setValues(LCategory category) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, category.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, category.getState());
        cv.put(DBHelper.TABLE_COLUMN_RID, category.getRid().toString());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, category.getTimeStampLast());
        return cv;
    }

    private static void getValues(Cursor cur, LCategory category) {
        category.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        category.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        category.setRid(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID)));
        category.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        category.setId(cur.getLong(0));
    }

    public static Cursor getCursorSortedBy(String sortColumn) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur;
        if (sortColumn != null)
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_CATEGORY_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=? ORDER BY " + sortColumn + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        else
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_CATEGORY_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        return cur;
    }

    public static long add(LCategory category) {
        long id = -1;
        synchronized (DBAccess.dbLock) {
            SQLiteDatabase db = DBAccess.getWriteDb();
            ContentValues cv = setValues(category);
            id = db.insert(DBHelper.TABLE_CATEGORY_NAME, "", cv);
            DBAccess.dirty = true;
        }
        return id;
    }

    public static boolean updateColumnById(long id, String column, String value) {
        return DBAccess.updateColumnById(DBHelper.TABLE_CATEGORY_NAME, id, column, value);
    }

    public static LCategory getById(long id) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        LCategory category = new LCategory();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_CATEGORY_NAME + " WHERE _id=?", new String[]{"" + id});
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find category with id: " + id);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, category);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get category with id: " + id + ":" + e.getMessage());
            category = null;
        }
        if (csr != null) csr.close();
        return category;
    }

    public static LCategory getByName(String name) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        LCategory category = new LCategory();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_CATEGORY_NAME + " WHERE "
                    + DBHelper.TABLE_COLUMN_NAME + "=?", new String[]{name});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find category with name: " + name);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, category);
            category.setId(csr.getLong(0));
        } catch (Exception e) {
            LLog.w(TAG, "unable to get category with name: " + name + ":" + e.getMessage());
            category = null;
        }
        if (csr != null) csr.close();
        return category;
    }


    public static long getIdByName(String name) {
        return DBAccess.getIdByName(DBHelper.TABLE_CATEGORY_NAME, name);
    }

    public static long getIdByRid(String rid) {
        return DBAccess.getIdByRid(DBHelper.TABLE_CATEGORY_NAME, rid);
    }
}
