package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.entities.LTag;

public class DBTag extends DBGeneric<LTag> {
    private static final String TAG = DBTag.class.getSimpleName();
    private static DBTag instance;

    private static final String[] tag_columns = new String[]{
            "_id",
            DBHelper.TABLE_COLUMN_GID,
            DBHelper.TABLE_COLUMN_NAME,
            DBHelper.TABLE_COLUMN_STATE,
            DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE};

    public DBTag() {
    }

    public static DBTag getInstance() {
        if (null == instance) {
            instance = new DBTag();
        }
        return instance;
    }

    @Override
    LTag getValues(Cursor cur, LTag tag) {
        if (null == tag) tag = new LTag();
        tag.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        tag.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        tag.setGid(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_GID)));
        tag.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        tag.setId(cur.getLong(0));
        return tag;
    }

    @Override
    ContentValues setValues(LTag tag, boolean update) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, tag.getName());
        if (!update)
            cv.put(DBHelper.TABLE_COLUMN_STATE, tag.getState());
        cv.put(DBHelper.TABLE_COLUMN_GID, tag.getGid());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, tag.getTimeStampLast());
        return cv;
    }

    @Override
    String[] getColumns() {
        return tag_columns;
    }

    @Override
    Uri getUri() {
        return DBProvider.URI_TAGS;
    }

    @Override
    long getId(LTag tag) {
        return tag.getId();
    }

    @Override
    void setId(LTag tag, long id) {
        tag.setId(id);
    }
}
