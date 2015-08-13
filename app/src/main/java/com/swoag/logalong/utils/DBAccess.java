package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LItem;

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

    private static void open () {
        if (helper == null) helper = new DBHelper(LApp.ctx, DBHelper.DB_VERSION);
    }

    public static void close () {
        synchronized(dbLock) {
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
    private static LItem yaventLItem ()
    {
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

    private static ContentValues setLItemValues(LItem LItem) {
        ContentValues cv = new ContentValues();
        //YLog.d(TAG, "id: " + LItem.getId() + " name: " + LItem.getName() + " full: " + LItem.getFullName());
        /*
        cv.put(DBHelper.TABLE_LItemS_COLUMN_ID, LItem.getAccountId());
        cv.put(DBHelper.TABLE_LItemS_COLUMN_STATUS, LItem.getStatus());
        cv.put(DBHelper.TABLE_LItemS_COLUMN_REAL_NAME, LItem.getFullName());
        cv.put(DBHelper.TABLE_LItemS_COLUMN_NAME, LItem.getName());
        cv.put(DBHelper.TABLE_LItemS_COLUMN_NUMBER, LItem.getNumber());
        cv.put(DBHelper.TABLE_LItemS_COLUMN_ICON, LItem.getIcon());
        */
        return cv;
    }

    private static void getLItemValues(Cursor cur, LItem LItem) {
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

    private static int getLItemList(ArrayList<LItem> LItems, Cursor cur, boolean sort){
        while(cur.moveToNext()){
            LItem LItem = new LItem();
            getLItemValues(cur, LItem);
            LItems.add(LItem);
        }
        if (sort) {
            Collections.sort(LItems, new Comparator<LItem>() {
                public int compare(LItem c1, LItem c2) {
                    return c1.getName().compareToIgnoreCase(c2.getName());
                }
            });
        }
        LItems.add(yaventLItem());
        return 0;
    }

    public static int getAllLItems (ArrayList<LItem> LItems, boolean sort) {
        synchronized(dbLock) {
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

    public static int getAllActiveLItems (ArrayList<LItem> LItems, boolean sort) {
        synchronized(dbLock) {
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

    public static LItem getLItemByAccountId (int id) {
        synchronized(dbLock) {

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

    public static void addLItem (LItem LItem) {
        synchronized(dbLock) {
            /*
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setLItemValues(LItem);
            db.insert(DBHelper.TABLE_LItemS_NAME, "", cv);
            */
            dirty = true;
        }
    }

    public static void updateLItem (LItem LItem) {
        synchronized(dbLock) {
            /*
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setLItemValues(LItem);
            db.update(DBHelper.TABLE_LItemS_NAME, cv, "_id=?", new String[]{"" + LItem.getId()});
            */
            dirty = true;
        }
    }
}
