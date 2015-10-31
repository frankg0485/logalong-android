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

    public static boolean update(LTag tag) {
        try {
            synchronized (DBAccess.dbLock) {
                SQLiteDatabase db = DBAccess.getWriteDb();
                ContentValues cv = setValues(tag);
                db.update(DBHelper.TABLE_TAG_NAME, cv, "_id=?", new String[]{"" + tag.getId()});
            }
            DBAccess.dirty = true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static void deleteById(long id) {
        DBAccess.updateColumnById(DBHelper.TABLE_TAG_NAME, id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static LTag getById(long id) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        LTag tag = new LTag();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TAG_NAME + " WHERE _id=?", new String[]{"" + id});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find tag with id: " + id);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, tag);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get tag with id: " + id + ":" + e.getMessage());
            tag = null;
        }
        if (csr != null) csr.close();
        return tag;
    }

    public static LTag getByName(String name) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        LTag tag = new LTag();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TAG_NAME + " WHERE "
                            + DBHelper.TABLE_COLUMN_NAME + "=? AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{name, "" + DBHelper.STATE_ACTIVE});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find tag with name: " + name);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, tag);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get tag with name: " + name + ":" + e.getMessage());
            tag = null;
        }
        if (csr != null) csr.close();
        return tag;
    }

    public static LTag getByRid(String rid) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        LTag tag = new LTag();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TAG_NAME + " WHERE "
                    + DBHelper.TABLE_COLUMN_RID + "=?", new String[]{rid});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find tag with rid: " + rid);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getValues(csr, tag);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get tag with rid: " + rid + ":" + e.getMessage());
            tag = null;
        }
        if (csr != null) csr.close();
        return tag;
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

    public static String getNameById(long id) {
        return DBAccess.getStringFromDbById(DBHelper.TABLE_TAG_NAME, DBHelper.TABLE_COLUMN_NAME, id);
    }
}
