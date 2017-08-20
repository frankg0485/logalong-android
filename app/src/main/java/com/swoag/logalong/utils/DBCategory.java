package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.entities.LCategory;

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
}
