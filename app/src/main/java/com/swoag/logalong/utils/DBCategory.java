package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.swoag.logalong.LApp;
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
        return getCursorSortedBy(LApp.ctx, sortColumn);
    }

    public static Cursor getCursorSortedBy(Context context, String sortColumn) {
        Cursor cur;
        if (sortColumn != null)
            cur = context.getContentResolver().query(DBProvider.URI_CATEGORIES, null,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE},
                    sortColumn + " ASC");
        else
            cur = context.getContentResolver().query(DBProvider.URI_CATEGORIES, null,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        return cur;
    }

    public static long add(LCategory category) {
        return add(LApp.ctx, category);
    }

    public static long add(Context context, LCategory category) {
        long id = -1;
        try {
            ContentValues cv = setValues(category);
            Uri uri = context.getContentResolver().insert(DBProvider.URI_CATEGORIES, cv);
            id = ContentUris.parseId(uri);
        } catch (Exception e) {
        }
        return id;
    }

    public static boolean update(LCategory category) {
        return update(LApp.ctx, category);
    }

    public static boolean update(Context context, LCategory category) {
        try {
            ContentValues cv = setValues(category);
            context.getContentResolver().update(DBProvider.URI_CATEGORIES, cv, "_id=?", new String[]{"" + category.getId()});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean updateColumnById(long id, String column, String value) {
        return DBAccess.updateColumnById(DBProvider.URI_CATEGORIES, id, column, value);
    }

    public static boolean updateColumnById(long id, String column, int value) {
        return DBAccess.updateColumnById(DBProvider.URI_CATEGORIES, id, column, value);
    }

    public static void deleteById(long id) {
        updateColumnById(id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static LCategory getById(long id) {
        return getById(LApp.ctx, id);
    }

    public static LCategory getById(Context context, long id) {
        Cursor csr = null;
        LCategory category = new LCategory();

        try {
            csr = context.getContentResolver().query(DBProvider.URI_CATEGORIES, null,
                    "_id=?", new String[]{"" + id}, null);
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
        return getByName(LApp.ctx, name);
    }

    public static LCategory getByName(Context context, String name) {
        Cursor csr = null;
        LCategory category = new LCategory();

        try {
            csr = context.getContentResolver().query(DBProvider.URI_CATEGORIES, null,
                    DBHelper.TABLE_COLUMN_NAME + "=? COLLATE NOCASE AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{name, "" + DBHelper.STATE_ACTIVE}, null);
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

    public static LCategory getByRid(String rid) {
        return getByRid(LApp.ctx, rid);
    }

    public static LCategory getByRid(Context context, String rid) {
        Cursor csr = null;
        LCategory category = new LCategory();

        try {
            csr = context.getContentResolver().query(DBProvider.URI_CATEGORIES, null,
                    DBHelper.TABLE_COLUMN_RID + "=?", new String[]{rid}, null);
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find category with rid: " + rid);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, category);
            category.setId(csr.getLong(0));
        } catch (Exception e) {
            LLog.w(TAG, "unable to get category with rid: " + rid + ":" + e.getMessage());
            category = null;
        }
        if (csr != null) csr.close();
        return category;
    }

    public static long getIdByName(String name) {
        return DBAccess.getIdByName(DBProvider.URI_CATEGORIES, name);
    }

    public static String getNameById(long id) {
        return DBAccess.getStringFromDbById(DBProvider.URI_CATEGORIES, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static long getIdByRid(String rid) {
        return DBAccess.getIdByRid(DBProvider.URI_CATEGORIES, rid);
    }

    public static int getDbIndexById(long id) {
        return DBAccess.getDbIndexById(LApp.ctx, DBProvider.URI_CATEGORIES, id);
    }
}
