package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LCategory;

import java.util.HashSet;

public class DBCategory extends DBGeneric<LCategory> {
    private static final String TAG = DBCategory.class.getSimpleName();
    private static DBCategory instance;

    private static final String[] category_columns = new String[]{
            "_id",
            DBHelper.TABLE_COLUMN_GID,
            DBHelper.TABLE_COLUMN_NAME,
            DBHelper.TABLE_COLUMN_STATE,
            DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE};

    public DBCategory() {
    }

    public static DBCategory getInstance() {
        if (null == instance) {
            instance = new DBCategory();
        }
        return instance;
    }

    @Override
    LCategory getValues(Cursor cur, LCategory category) {
        if (null == category) category = new LCategory();
        category.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        category.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        category.setGid(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_GID)));
        category.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        category.setId(cur.getLong(0));
        return category;
    }

    @Override
    ContentValues setValues(LCategory category) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, category.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, category.getState());
        cv.put(DBHelper.TABLE_COLUMN_GID, category.getGid());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, category.getTimeStampLast());
        return cv;
    }

    @Override
    String[] getColumns() {
        return category_columns;
    }

    @Override
    Uri getUri() {
        return DBProvider.URI_CATEGORIES;
    }

    @Override
    long getId(LCategory category) {
        return category.getId();
    }

    @Override
    void setId(LCategory category, long id) {
        category.setId(id);
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

    public static boolean updateColumnById(long id, String column, String value) {
        return DBAccess.updateColumnById(DBProvider.URI_CATEGORIES, id, column, value);
    }

    public static boolean updateColumnById(long id, String column, int value) {
        return DBAccess.updateColumnById(DBProvider.URI_CATEGORIES, id, column, value);
    }

    public static long getIdByName(String name) {
        return DBAccess.getIdByName(DBProvider.URI_CATEGORIES, name);
    }

    public static String getNameById(long id) {
        return DBAccess.getStringFromDbById(DBProvider.URI_CATEGORIES, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static long getIdByGid(long gid) {
        return DBAccess.getIdByGid(DBProvider.URI_CATEGORIES, gid);
    }

    public static int getDbIndexById(long id) {
        return DBAccess.getDbIndexById(LApp.ctx, DBProvider.URI_CATEGORIES, id);
    }

    public static HashSet<Long> getAllActiveIds() {
        HashSet<Long> set = new HashSet<Long>();
        Cursor cur = getCursorSortedBy(null);
        if (cur != null && cur.getCount() > 0) {

            cur.moveToFirst();
            do {
                set.add(cur.getLong(0));
            } while (cur.moveToNext());
        }
        if (cur != null) cur.close();
        return set;
    }
}
