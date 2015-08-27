package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountSummary;
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

    private static ContentValues setAccountValues(LAccount account) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_ACCOUNT_COLUMN_NAME, account.getName());
        cv.put(DBHelper.TABLE_ACCOUNT_COLUMN_STATE, account.getState());
        return cv;
    }

    private static ContentValues setItemValues(LItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_LOG_COLUMN_TYPE, item.getType());
        cv.put(DBHelper.TABLE_LOG_COLUMN_STATE, item.getState());
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

    private static void getItemValues(Cursor cur, LItem item) {
        item.setType(cur.getInt(cur.getColumnIndex(DBHelper.TABLE_LOG_COLUMN_TYPE)));
        item.setState(cur.getInt(cur.getColumnIndex(DBHelper.TABLE_LOG_COLUMN_STATE)));
        item.setFrom(cur.getLong(cur.getColumnIndex(DBHelper.TABLE_LOG_COLUMN_FROM)));
        item.setTo(cur.getLong(cur.getColumnIndex(DBHelper.TABLE_LOG_COLUMN_TO)));
        item.setCategory(cur.getLong(cur.getColumnIndex(DBHelper.TABLE_LOG_COLUMN_CATEGORY)));
        item.setTag(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TAG)));
        item.setVendor(cur.getLong(cur.getColumnIndex(DBHelper.TABLE_LOG_COLUMN_VENDOR)));
        item.setValue(cur.getDouble(cur.getColumnIndex(DBHelper.TABLE_LOG_COLUMN_VALUE)));
        item.setNote(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_NOTE)));
        item.setTimeStamp(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TIMESTAMP)));
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
        //LItems.add(yaventLItem());
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
    public static Cursor getAllActiveItemsCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LOG_NAME + " WHERE State=?",
                new String[]{"" + LItem.LOG_STATE_ACTIVE});
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

    public static void addItem(LItem item) {
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setItemValues(item);
            db.insert(DBHelper.TABLE_LOG_NAME, "", cv);
            dirty = true;
        }
    }

    public static void updateItem(LItem item) {
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setItemValues(item);
            db.update(DBHelper.TABLE_LOG_NAME, cv, "_id=?", new String[]{"" + item.getId()});
            dirty = true;
        }
    }

    public static void deleteItemById(long id) {
        LItem item = getLogItemById(id);
        if (item != null) {
            item.setState(LItem.LOG_STATE_DELETED);
            updateItem(item);
        }
    }

    public static LItem getLogItemById(long id) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        String str = "";
        LItem item = new LItem();
        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LOG_NAME + " WHERE _id=?", new String[]{"" + id});
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find id: " + id + " from log table");
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getItemValues(csr, item);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get with id: " + id + ":" + e.getMessage());
        }
        if (csr != null) csr.close();
        item.setId(id);
        return item;
    }

    private static String getStringFromDbById(String table, String column, long id) {
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

    public static String getCategoryById(long id) {
        return getStringFromDbById(DBHelper.TABLE_CATEGORY_NAME, DBHelper.TABLE_CATEGORY_COLUMN_NAME, id);
    }

    public static String getVendorById(long id) {
        return getStringFromDbById(DBHelper.TABLE_VENDOR_NAME, DBHelper.TABLE_VENDOR_COLUMN_NAME, id);
    }

    public static String getTagById(long id) {
        return getStringFromDbById(DBHelper.TABLE_TAG_NAME, DBHelper.TABLE_TAG_COLUMN_NAME, id);
    }

    public static String getAccountById(long id) {
        return getStringFromDbById(DBHelper.TABLE_ACCOUNT_NAME, DBHelper.TABLE_ACCOUNT_COLUMN_NAME, id);
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

    public static long addAccount(LAccount acccount) {
        long id = -1;
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setAccountValues(acccount);
            id = db.insert(DBHelper.TABLE_ACCOUNT_NAME, "", cv);
            dirty = true;
        }
        return id;
    }

    public static Cursor getAllAccountsCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME, null);
        return cur;
    }

    public static Cursor getAllCategoriesCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_CATEGORY_NAME, null);
        return cur;
    }

    public static Cursor getAllVendorsCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_VENDOR_NAME, null);
        return cur;
    }

    public static Cursor getAllTagsCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TAG_NAME, null);
        return cur;
    }

    private static int getDbIndexById(String table, String state, int actvState, long id) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        int index = 0;
        int ret = -1;
        try {
            csr = db.rawQuery("SELECT _id FROM " + table + " WHERE " + state + "=?",
                    new String[]{"" + actvState});

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

    public static int getAccountIndexById(long id) {
        return getDbIndexById(DBHelper.TABLE_ACCOUNT_NAME, DBHelper.TABLE_ACCOUNT_COLUMN_STATE,
                LAccount.ACCOUNT_STATE_ACTIVE, id);
    }

    public static int getCategoryIndexById(long id) {
        return getDbIndexById(DBHelper.TABLE_CATEGORY_NAME, DBHelper.TABLE_CATEGORY_COLUMN_STATE,
                LCategory.CATEGORY_STATE_ACTIVE, id);
    }

    public static int getVendorIndexById(long id) {
        return getDbIndexById(DBHelper.TABLE_VENDOR_NAME, DBHelper.TABLE_VENDOR_COLUMN_STATE,
                LVendor.VENDOR_STATE_ACTIVE, id);
    }

    public static int getTagIndexById(long id) {
        return getDbIndexById(DBHelper.TABLE_TAG_NAME, DBHelper.TABLE_TAG_COLUMN_STATE,
                LTag.TAG_STATE_ACTIVE, id);
    }

    public static void getSummaryForAll(LAccountSummary summary) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        double income = 0;
        double expense = 0;
        try {
            csr = db.rawQuery("SELECT "
                            + DBHelper.TABLE_LOG_COLUMN_TYPE + ","
                            + DBHelper.TABLE_LOG_COLUMN_VALUE + " FROM "
                            + DBHelper.TABLE_LOG_NAME + " WHERE " + DBHelper.TABLE_LOG_COLUMN_STATE + "=?",
                    new String[]{"" + LItem.LOG_STATE_ACTIVE});

            csr.moveToFirst();
            do {
                double value = csr.getDouble(csr.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_VALUE));
                int type = csr.getInt(csr.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TYPE));
                if (type == LItem.LOG_TYPE_INCOME) income += value;
                else if (type == LItem.LOG_TYPE_EXPENSE) expense += value;
            } while (csr.moveToNext());
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
        if (csr != null) csr.close();

        summary.setBalance(income - expense);
        summary.setIncome(income);
        summary.setExpense(expense);
    }
}
