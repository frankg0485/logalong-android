package com.swoag.logalong.utils;
/* Copyright (C) 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;

public abstract class DBGeneric<T> {
    private static final String TAG = DBAccess.class.getSimpleName();

    abstract Uri getUri();
    abstract String[] getColumns();
    abstract T getValues(Cursor cursor, T t);
    abstract ContentValues setValues(T t);
    abstract long getId(T t);
    abstract void setId(T t, long id);

    public T getByName(String name) {
        try {
            Cursor csr = LApp.ctx.getContentResolver().query(getUri(), getColumns(),
                    DBHelper.TABLE_COLUMN_NAME + "=? COLLATE NOCASE AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{name, "" + DBHelper.STATE_ACTIVE}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find entry with name: " + name + " count: " + csr.getCount());
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                T t = getValues(csr, null);
                csr.close();
                return t;
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get entry with name: " + name + ":" + e.getMessage());
        }
        return null;
    }

    private T getByIdGid(Context context, long id, boolean byId, boolean active) {
        if (id <= 0) return null;

        try {
            Cursor csr;
            String id_str;
            id_str = (byId)? "_id" : DBHelper.TABLE_COLUMN_GID;
            if (active)
                csr = context.getContentResolver().query(getUri(), getColumns(),
                        id_str + "=? AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                        new String[]{"" + id, "" + DBHelper.STATE_ACTIVE}, null);
            else
                csr = context.getContentResolver().query(getUri(), getColumns(),
                        id_str + "=?", new String[]{"" + id}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find entry with id: " + id);
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                T t = getValues(csr, null);
                csr.close();
                return t;
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get entry with id: " + id + ":" + e.getMessage());
        }
        return null;
    }

    public T getByIdAll(long id) {
        return getByIdGid(LApp.ctx, id, true, false);
    }

    public T getById(long id) {
        return getByIdGid(LApp.ctx, id, true, true);
    }

    public T getByGidAll(int gid) {
        return getByIdGid(LApp.ctx, gid, false, false);
    }

    public T getByGid(int gid) {
        return getByIdGid(LApp.ctx, gid, false, true);
    }

    public long add(T t) {
        ContentValues cv = setValues(t);
        long id = -1;
        try {
            Uri uri = LApp.ctx.getContentResolver().insert(getUri(), cv);
            id = ContentUris.parseId(uri);
            setId(t, id);
        } catch (Exception e) {
            LLog.w(TAG, "unable to add entry: " + e.getMessage());
        }
        return id;
    }

    public boolean update(T t) {
        try {
            ContentValues cv = setValues(t);
            LApp.ctx.getContentResolver().update(getUri(), cv, "_id=?", new String[]{"" + getId(t)});
        } catch (Exception e) {
            LLog.w(TAG, "unable to update entry: " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean updateColumnById(long id, String column, int value) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(column, value);
            LApp.ctx.getContentResolver().update(getUri(), cv, "_id=?", new String[]{"" + id});
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    public void deleteById(long id) {
        updateColumnById(id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

}
