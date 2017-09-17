package com.swoag.logalong.utils;
/* Copyright (C) 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;

import java.util.HashSet;

public abstract class DBGeneric<T> {
    private static final String TAG = DBGeneric.class.getSimpleName();

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
                    //LLog.w(TAG, "unable to find entry with name: " + name + " count: " + csr.getCount());
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
            id_str = (byId) ? "_id" : DBHelper.TABLE_COLUMN_GID;
            if (active)
                csr = context.getContentResolver().query(getUri(), getColumns(),
                        id_str + "=? AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                        new String[]{"" + id, "" + DBHelper.STATE_ACTIVE}, null);
            else
                csr = context.getContentResolver().query(getUri(), getColumns(),
                        id_str + "=?", new String[]{"" + id}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find entry with " + (byId ? "id" : "gid") + ": " + id + "@" + getUri() + "" +
                            " count: " + csr.getCount());
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

    public T getByGidAll(long gid) {
        return getByIdGid(LApp.ctx, gid, false, false);
    }

    public T getByGid(long gid) {
        return getByIdGid(LApp.ctx, gid, false, true);
    }

    private long getIdByColumn(String column, String value, boolean caseSensitive) {
        long id = 0;

        try {
            Cursor csr = null;
            if (caseSensitive) {
                csr = LApp.ctx.getContentResolver().query(getUri(), new String[]{"_id"}, column + "=? AND "
                                + DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + value, "" + DBHelper
                                .STATE_ACTIVE},
                        null);
            } else {
                csr = LApp.ctx.getContentResolver().query(getUri(), new String[]{"_id"}, column + "=? COLLATE NOCASE " +
                                "AND "
                                + DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + value, "" + DBHelper
                                .STATE_ACTIVE},
                        null);
            }
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to get " + column + ": " + value + " in table: " + getUri());
                    csr.close();
                    return 0;
                }

                csr.moveToFirst();
                id = csr.getLong(0);
                csr.close();
            }

        } catch (Exception e) {
            LLog.w(TAG, "unable to get " + column + ": " + value + " in table: " + getUri());
        }
        return id;
    }

    public long getIdByGid(long gid) {
        long id = 0;

        try {
            Cursor csr = null;
            csr = LApp.ctx.getContentResolver().query(getUri(), new String[]{"_id"}, DBHelper.TABLE_COLUMN_GID + "=? " +
                            "AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + gid, "" + DBHelper.STATE_ACTIVE}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to get unique gid: " + gid + " in table: " + getUri() + " count: " + csr
                            .getCount());
                    csr.close();
                    return 0;
                }

                csr.moveToFirst();
                id = csr.getLong(0);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get GID: " + gid + " in table: " + getUri());
        }
        return id;
    }

    public long getIdByName(String name) {
        return getIdByColumn(DBHelper.TABLE_COLUMN_NAME, name, true);
    }

    public String getNameById(long id) {
        if (id <= 0) return "";

        String str = "";
        try {
            Cursor csr = LApp.ctx.getContentResolver().query(getUri(), new String[]{DBHelper.TABLE_COLUMN_NAME},
                    "_id=? AND " + DBHelper.TABLE_COLUMN_STATE + " =?",
                    new String[]{"" + id, "" + DBHelper.STATE_ACTIVE}, null);
            if (csr != null) {
                if (csr.getCount() > 0) {
                    csr.moveToFirst();
                    str = csr.getString(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME));
                }
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get with id: " + id + ":" + e.getMessage());
        }
        return str;
    }

    public int getDbIndexById(long id) {
        if (id <= 0) return -1;

        Cursor csr = null;
        int index = 0;
        int ret = -1;
        try {
            csr = LApp.ctx.getContentResolver().query(getUri(), new String[]{"_id"},
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE},
                    DBHelper.TABLE_COLUMN_NAME + " ASC");

            csr.moveToFirst();
            while (true) {
                if (id == csr.getLong(0)) {
                    ret = index;
                    break;
                }
                csr.moveToNext();
                index++;
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get with id: " + id + ":" + e.getMessage());
        }
        if (csr != null) csr.close();
        return ret;
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

    public boolean updateColumnById(long id, String column, long value) {
        if (id <= 0) return false;

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
        if (id <= 0) return;

        updateColumnById(id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public Cursor getCursorSortedBy(String sortColumn) {
        Cursor cur;
        if (sortColumn != null)
            cur = LApp.ctx.getContentResolver().query(getUri(), getColumns(),
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE},
                    sortColumn + " ASC");
        else
            cur = LApp.ctx.getContentResolver().query(getUri(), null,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        return cur;
    }

    public HashSet<Long> getAllActiveIds() {
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
