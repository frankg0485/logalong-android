package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LTag;

import java.util.HashSet;

public class DBTag {
    private static final String TAG = DBTag.class.getSimpleName();

    private static ContentValues setValues(LTag tag) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, tag.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, tag.getState());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, tag.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_GID, tag.getGid());
        return cv;
    }

    private static void getValues(Cursor cur, LTag tag) {
        tag.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        tag.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        tag.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        tag.setGid(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_GID)));
        tag.setId(cur.getLong(0));
    }

    public static long add(LTag tag) {
        return add(LApp.ctx, tag);
    }

    public static long add(Context context, LTag tag) {
        long id = -1;
        try {
            ContentValues cv = setValues(tag);
            Uri uri = context.getContentResolver().insert(DBProvider.URI_TAGS, cv);
            id = ContentUris.parseId(uri);
        } catch (Exception e) {
        }
        return id;
    }

    public static boolean update(LTag tag) {
        return update(LApp.ctx, tag);
    }

    public static boolean update(Context context, LTag tag) {
        try {
            ContentValues cv = setValues(tag);
            context.getContentResolver().update(DBProvider.URI_TAGS, cv, "_id=?", new String[]{"" + tag.getId()});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void deleteById(long id) {
        updateColumnById(id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static LTag getById(long id) {
        return getById(LApp.ctx, id);
    }

    public static LTag getById(Context context, long id) {
        if (id <= 0) return null;

        LTag tag = new LTag();

        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_TAGS, null,
                    "_id=?", new String[]{"" + id}, null);

            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find tag with id: " + id);
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, tag);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get tag with id: " + id + ":" + e.getMessage());
            tag = null;
        }
        return tag;
    }

    public static LTag getByName(String name) {
        return getByName(LApp.ctx, name);
    }

    public static LTag getByName(Context context, String name) {
        LTag tag = new LTag();

        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_TAGS, null,
                    DBHelper.TABLE_COLUMN_NAME + "=? COLLATE NOCASE AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{name, "" + DBHelper.STATE_ACTIVE}, null);

            if (csr != null) {
                if (csr.getCount() > 1 || csr.getCount() < 1) {
                    LLog.w(TAG, "unable to find tag with name: " + name + " count: " + csr.getCount());
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, tag);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get tag with name: " + name + ":" + e.getMessage());
            tag = null;
        }
        return tag;
    }

    public static LTag getByGid(int gid) {
        return getByGid(LApp.ctx, gid);
    }

    public static LTag getByGid(Context context, int gid) {
        LTag tag = new LTag();

        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_TAGS, null,
                    DBHelper.TABLE_COLUMN_RID + "=?", new String[]{"" + gid}, null);

            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find tag with rid: " + gid + " count: " + csr.getCount());
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, tag);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get tag with rid: " + gid + ":" + e.getMessage());
            tag = null;
        }
        return tag;
    }

    public static Cursor getCursorSortedBy(String sortColumn) {
        return getCursorSortedBy(LApp.ctx, sortColumn);
    }

    public static Cursor getCursorSortedBy(Context context, String sortColumn) {
        Cursor cur;
        if (sortColumn != null)
            cur = context.getContentResolver().query(DBProvider.URI_TAGS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE},
                    sortColumn + " ASC");
        else
            cur = context.getContentResolver().query(DBProvider.URI_TAGS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        return cur;
    }

    public static boolean updateColumnById(long id, String column, String value) {
        return DBAccess.updateColumnById(DBProvider.URI_TAGS, id, column, value);
    }

    public static boolean updateColumnById(long id, String column, int value) {
        return DBAccess.updateColumnById(DBProvider.URI_TAGS, id, column, value);
    }

    public static long getIdByName(String name) {
        return DBAccess.getIdByName(DBProvider.URI_TAGS, name);
    }

    public static String getNameById(long id) {
        return DBAccess.getStringFromDbById(DBProvider.URI_TAGS, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static int getDbIndexById(long id) {
        return DBAccess.getDbIndexById(LApp.ctx, DBProvider.URI_TAGS, id);
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
