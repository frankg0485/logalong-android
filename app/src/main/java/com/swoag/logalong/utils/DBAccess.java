package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LItem;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LVendor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DBAccess {
    private static final String TAG = DBAccess.class.getSimpleName();

    private static DBHelper helper;
    private static SQLiteDatabase dbRead;
    private static SQLiteDatabase dbWrite;
    private static boolean dirty;
    private static final Object dbLock = new Object();

    private static void open() {
        if (helper == null) helper = new DBHelper(LApp.ctx, DBHelper.DB_VERSION);
    }

    public static void close() {
        synchronized (dbLock) {
            flush();
            helper = null;
        }
    }

    private static SQLiteDatabase getReadDb() {
        if (dirty) {
            if (dbWrite != null) {
                dbWrite.close();
                dbWrite = null;
            }
            dirty = false;
            if (dbRead != null) {
                dbRead.close();
                dbRead = null;
            }
        }
        if (dbRead == null) {
            open();
            dbRead = helper.getReadableDatabase();
        }
        //File dbFile = LApp.ctx.getDatabasePath("MY_DB_NAME");
        //YLog.i(TAG, "database at: " + dbFile.getAbsolutePath());

        return dbRead;
    }

    private static SQLiteDatabase getWriteDb() {
        if (dbWrite == null) {
            open();
            dbWrite = helper.getWritableDatabase();
        }
        return dbWrite;
    }

    private static void flush() {
        if (dbRead != null) {
            dbRead.close();
            dbRead = null;
        }
        if (dbWrite != null) {
            dbWrite.close();
            dbWrite = null;
        }
        dirty = false;
    }

    private static LItem gLogItem;

    private static LItem yaventLItem() {
        if (gLogItem == null) {
            gLogItem = new LItem();
/*
            gLogItem.setFullName(LApp.ctx.getString(R.string.yavent_team));
            Resources res = LApp.ctx.getResources();
            Drawable drawable = res.getDrawable(R.drawable.ic_action_person);
            Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] icon = stream.toByteArray();
            gLogItem.setIcon(icon);
            gLogItem.setAccountId(0);
            gLogItem.setStatus(LItem.STATUS_ACTIVE);
            */
        }
        return gLogItem;
    }

    private static ContentValues setCategoryValues(LCategory category) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_CATEGORY_COLUMN_NAME, category.getName());
        cv.put(DBHelper.TABLE_CATEGORY_COLUMN_STATE, category.getState());
        return cv;
    }

    private static ContentValues setTagValues(LTag tag) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_TAG_COLUMN_NAME, tag.getName());
        cv.put(DBHelper.TABLE_TAG_COLUMN_STATE, tag.getState());
        return cv;
    }

    private static ContentValues setVendorValues(LVendor vendor) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_VENDOR_COLUMN_NAME, vendor.getName());
        cv.put(DBHelper.TABLE_VENDOR_COLUMN_STATE, vendor.getState());
        return cv;
    }

    private static ContentValues setItemValues(LItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_LOG_COLUMN_TYPE, item.getType());
        cv.put(DBHelper.TABLE_LOG_COLUMN_CATEGORY, item.getCategory());
        cv.put(DBHelper.TABLE_LOG_COLUMN_FROM, item.getFrom());
        cv.put(DBHelper.TABLE_LOG_COLUMN_TO, item.getTo());
        cv.put(DBHelper.TABLE_LOG_COLUMN_BY, item.getBy());
        cv.put(DBHelper.TABLE_LOG_COLUMN_VALUE, item.getValue());
        cv.put(DBHelper.TABLE_LOG_COLUMN_TIMESTAMP, item.getTimeStamp());
        cv.put(DBHelper.TABLE_LOG_COLUMN_NOTE, item.getNote());
        cv.put(DBHelper.TABLE_LOG_COLUMN_TAG, item.getTag());
        cv.put(DBHelper.TABLE_LOG_COLUMN_VENDOR, item.getVendor());

        return cv;
    }

    private static void getItemValues(Cursor cur, LItem LItem) {
        /*
        LItem.setId(cur.getInt(0));
        LItem.setAccountId(cur.getInt(cur.getColumnIndex(DBHelper.TABLE_LItemS_COLUMN_ID)));
        LItem.setStatus(cur.getInt(cur.getColumnIndex(DBHelper.TABLE_LItemS_COLUMN_STATUS)));
        LItem.setFullName(cur.getString(cur.getColumnIndex(DBHelper.TABLE_LItemS_COLUMN_REAL_NAME)));
        LItem.setName(cur.getString(cur.getColumnIndex(DBHelper.TABLE_LItemS_COLUMN_NAME)));
        LItem.setNumber(cur.getString(cur.getColumnIndex(DBHelper.TABLE_LItemS_COLUMN_NUMBER)));
        LItem.setIcon(cur.getBlob(cur.getColumnIndex(DBHelper.TABLE_LItemS_COLUMN_ICON)));
        */
    }

    private static int getItemList(ArrayList<LItem> LItems, Cursor cur, boolean sort) {
        while (cur.moveToNext()) {
            LItem LItem = new LItem();
            getItemValues(cur, LItem);
            LItems.add(LItem);
        }
        if (sort) {
            Collections.sort(LItems, new Comparator<LItem>() {
                public int compare(LItem c1, LItem c2) {
                    int ret;
                    if (c1.getTimeStamp() > c2.getTimeStamp()) ret = 1;
                    else if (c1.getTimeStamp() == c2.getTimeStamp()) ret = 0;
                    else ret = -1;
                    return ret;
                }
            });
        }
        LItems.add(yaventLItem());
        return 0;
    }

    public static int getAllItems(ArrayList<LItem> LItems, boolean sort) {
        synchronized (dbLock) {
            /*
            SQLiteDatabase db = getReadDb();
            Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LItemS_NAME, null);
            getLItemList(LItems, cur, sort);
            int ii = LItems.size();
            while (ii-- > 0) {
                if (LItems.get(ii).status == LItem.STATUS_GROUP_INVITE) {
                    LItems.remove(ii);
                }
            }
            cur.close();
            */
            return 0;
        }
    }

    //TODO: not thread safe?
    public static Cursor getAllItemsCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LOG_NAME, null);
        return cur;
    }

    public static int getAllActiveItems(ArrayList<LItem> LItems, boolean sort) {
        synchronized (dbLock) {
            /*
            SQLiteDatabase db = getReadDb();
            Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LItemS_NAME + " WHERE Status=?",
                    new String[]{"" + LItem.STATUS_ACTIVE});
            getLItemList(LItems, cur, sort);
            cur.close();
            */
            return 0;
        }
    }

    public static LItem getItemByAccountId(int id) {
        synchronized (dbLock) {

            LItem LItem = null;
            /*
            if (id == 0) return yaventLItem();
            YLog.d(TAG, "ID: " + id);
            SQLiteDatabase db = getReadDb();
            Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LItemS_NAME + " WHERE Cid=?",
                    new String[]{"" + id});
            if (cur.getCount() > 0) {
                YLog.d(TAG, "count: " + cur.getCount());
                cur.moveToFirst();
                LItem = new LItem();
                getLItemValues(cur, LItem);
            }

            cur.close();
                        */
            return LItem;
        }
    }

    public static void addItem(LItem LItem) {
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setItemValues(LItem);
            db.insert(DBHelper.TABLE_LOG_NAME, "", cv);
            dirty = true;
        }
    }

    public static void updateItem(LItem LItem) {
        synchronized (dbLock) {
            /*
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setLItemValues(LItem);
            db.update(DBHelper.TABLE_LItemS_NAME, cv, "_id=?", new String[]{"" + LItem.getId()});
            */
            dirty = true;
        }
    }

    private static String getStringFromDbById(String table, String column, int id) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        String str = "";
        try {
            csr = db.rawQuery("SELECT * FROM " + table + " WHERE _id=?", new String[]{"" + id});
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find id: " + id + " from table: " + table + " column: " + column);
                csr.close();
                return "";
            }

            csr.moveToFirst();
            str = csr.getString(csr.getColumnIndexOrThrow(column));
        } catch (Exception e) {
            LLog.w(TAG, "unable to get with id: " + id + ":" + e.getMessage());
        }
        if (csr != null) csr.close();
        return str;
    }

    public static String getCategoryById(int id) {
        return getStringFromDbById(DBHelper.TABLE_CATEGORY_NAME, DBHelper.TABLE_CATEGORY_COLUMN_NAME, id);
    }

    public static String getVendorById(int id) {
        return getStringFromDbById(DBHelper.TABLE_VENDOR_NAME, DBHelper.TABLE_VENDOR_COLUMN_NAME, id);
    }

    public static String getTagById(int id) {
        return getStringFromDbById(DBHelper.TABLE_TAG_NAME, DBHelper.TABLE_TAG_COLUMN_NAME, id);
    }

    public static long addCategory(LCategory category) {
        long id = -1;
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setCategoryValues(category);
            id = db.insert(DBHelper.TABLE_CATEGORY_NAME, "", cv);
            dirty = true;
        }
        return id;
    }

    public static long addTag(LTag tag) {
        long id = -1;
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setTagValues(tag);
            id = db.insert(DBHelper.TABLE_TAG_NAME, "", cv);
            dirty = true;
        }
        return id;
    }

    public static long addVendor(LVendor vendor) {
        long id = -1;
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setVendorValues(vendor);
            id = db.insert(DBHelper.TABLE_VENDOR_NAME, "", cv);
            dirty = true;
        }
        return id;
    }
}
