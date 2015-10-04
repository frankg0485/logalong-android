package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountBalance;
import com.swoag.logalong.entities.LAccountSummary;
import com.swoag.logalong.entities.LBoxer;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LVendor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

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

    private static LTransaction gLogItem;

    private static ContentValues setCategoryValues(LCategory category) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, category.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, category.getState());
        cv.put(DBHelper.TABLE_COLUMN_RID, category.getRid().toString());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, category.getTimeStampLast());
        return cv;
    }

    private static void getCategoryValues(Cursor cur, LCategory category) {
        category.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        category.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        category.setRid(UUID.fromString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID))));
        category.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        category.setId(cur.getLong(0));
    }

    private static ContentValues setTagValues(LTag tag) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, tag.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, tag.getState());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, tag.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_RID, tag.getRid().toString());
        return cv;
    }

    private static void getTagValues(Cursor cur, LTag tag) {
        tag.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        tag.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        tag.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        tag.setRid(UUID.fromString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID))));
        tag.setId(cur.getLong(0));
    }

    private static ContentValues setVendorValues(LVendor vendor) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, vendor.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, vendor.getState());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, vendor.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_RID, vendor.getRid().toString());
        return cv;
    }

    private static void getVendorValues(Cursor cur, LVendor vendor) {
        vendor.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        vendor.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        vendor.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        vendor.setRid(UUID.fromString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID))));
        vendor.setId(cur.getLong(0));
    }

    private static ContentValues setAccountValues(LAccount account) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, account.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, account.getState());
        cv.put(DBHelper.TABLE_COLUMN_SHARE, account.getShareIdsString());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, account.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_RID, account.getRid().toString());
        return cv;
    }

    private static void getAccountValues(Cursor cur, LAccount account) {
        account.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        account.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        account.setSharedIdsString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHARE)));
        account.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        account.setRid(UUID.fromString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID))));
        account.setId(cur.getLong(0));
    }

    private static ContentValues setItemValues(LTransaction item) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_TYPE, item.getType());
        cv.put(DBHelper.TABLE_COLUMN_STATE, item.getState());
        cv.put(DBHelper.TABLE_COLUMN_CATEGORY, item.getCategory());
        cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, item.getAccount());
        cv.put(DBHelper.TABLE_COLUMN_MADEBY, item.getBy());
        cv.put(DBHelper.TABLE_COLUMN_AMOUNT, item.getValue());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP, item.getTimeStamp());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, item.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_NOTE, item.getNote());
        cv.put(DBHelper.TABLE_COLUMN_TAG, item.getTag());
        cv.put(DBHelper.TABLE_COLUMN_VENDOR, item.getVendor());
        cv.put(DBHelper.TABLE_COLUMN_RID, item.getRid().toString());
        return cv;
    }

    private static void getItemValues(Cursor cur, LTransaction item) {
        item.setType(cur.getInt(cur.getColumnIndex(DBHelper.TABLE_COLUMN_TYPE)));
        item.setState(cur.getInt(cur.getColumnIndex(DBHelper.TABLE_COLUMN_STATE)));
        item.setAccount(cur.getLong(cur.getColumnIndex(DBHelper.TABLE_COLUMN_ACCOUNT)));
        item.setCategory(cur.getLong(cur.getColumnIndex(DBHelper.TABLE_COLUMN_CATEGORY)));
        item.setTag(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TAG)));
        item.setVendor(cur.getLong(cur.getColumnIndex(DBHelper.TABLE_COLUMN_VENDOR)));
        item.setValue(cur.getDouble(cur.getColumnIndex(DBHelper.TABLE_COLUMN_AMOUNT)));
        item.setNote(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NOTE)));
        item.setBy(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_MADEBY)));
        item.setTimeStamp(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)));
        item.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        item.setRid(UUID.fromString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID))));
        item.setId(cur.getLong(0));
    }

    private static int getItemList(ArrayList<LTransaction> LTransactions, Cursor cur, boolean sort) {
        while (cur.moveToNext()) {
            LTransaction LTransaction = new LTransaction();
            getItemValues(cur, LTransaction);
            LTransactions.add(LTransaction);
        }
        if (sort) {
            Collections.sort(LTransactions, new Comparator<LTransaction>() {
                public int compare(LTransaction c1, LTransaction c2) {
                    int ret;
                    if (c1.getTimeStamp() > c2.getTimeStamp()) ret = 1;
                    else if (c1.getTimeStamp() == c2.getTimeStamp()) ret = 0;
                    else ret = -1;
                    return ret;
                }
            });
        }
        //LTransactions.add(yaventLItem());
        return 0;
    }

    public static int getAllItems(ArrayList<LTransaction> LTransactions, boolean sort) {
        synchronized (dbLock) {
            /*
            SQLiteDatabase db = getReadDb();
            Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LItemS_NAME, null);
            getLItemList(LTransactions, cur, sort);
            int ii = LTransactions.size();
            while (ii-- > 0) {
                if (LTransactions.get(ii).status == LTransaction.STATUS_GROUP_INVITE) {
                    LTransactions.remove(ii);
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
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                        + " WHERE State=? ORDER BY " + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                new String[]{"" + DBHelper.STATE_ACTIVE});
        return cur;
    }

    public static Cursor getActiveItemsCursorInRange(long start, long end) {
        SQLiteDatabase db = getReadDb();
        Cursor cur;
        if (start == -1 || end == -1) {
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                            + " WHERE State=? ORDER BY " + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        } else {
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                            + " WHERE State=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + ">=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + "<? ORDER BY " + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + start, "" + end});
        }
        return cur;
    }

    private static Cursor getActiveItemsCursorInRangeSortBy(String column, long start, long end) {
        SQLiteDatabase db = getReadDb();
        Cursor cur;
        if (start == -1 || end == -1) {
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                            + " WHERE State=? ORDER BY "
                            + column + " ASC, "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        } else {
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                            + " WHERE State=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + ">=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + "<? ORDER BY "
                            + column + " ASC, "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + start, "" + end});
        }
        return cur;
    }

    public static Cursor getActiveItemsCursorInRangeSortByAccount(long start, long end) {
        return getActiveItemsCursorInRangeSortBy(DBHelper.TABLE_COLUMN_ACCOUNT, start, end);
    }

    public static Cursor getActiveItemsCursorInRangeSortByCategory(long start, long end) {
        return getActiveItemsCursorInRangeSortBy(DBHelper.TABLE_COLUMN_CATEGORY, start, end);
    }

    public static Cursor getActiveItemsCursorInRangeSortByTag(long start, long end) {
        return getActiveItemsCursorInRangeSortBy(DBHelper.TABLE_COLUMN_TAG, start, end);
    }

    public static Cursor getActiveItemsCursorInRangeSortByVendor(long start, long end) {
        return getActiveItemsCursorInRangeSortBy(DBHelper.TABLE_COLUMN_VENDOR, start, end);
    }

    public static Cursor getActiveItemsCursorByAccount(long accountId) {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME
                        + " WHERE State=? AND " + DBHelper.TABLE_COLUMN_ACCOUNT + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE, "" + accountId});
        return cur;
    }

    public static int getAllActiveItems(ArrayList<LTransaction> LTransactions, boolean sort) {
        synchronized (dbLock) {
            /*
            SQLiteDatabase db = getReadDb();
            Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LItemS_NAME + " WHERE Status=?",
                    new String[]{"" + LTransaction.STATUS_ACTIVE});
            getLItemList(LTransactions, cur, sort);
            cur.close();
            */
            return 0;
        }
    }

    public static LTransaction getItemByAccountId(int id) {
        synchronized (dbLock) {

            LTransaction LTransaction = null;
            /*
            if (id == 0) return yaventLItem();
            YLog.d(TAG, "ID: " + id);
            SQLiteDatabase db = getReadDb();
            Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LItemS_NAME + " WHERE Cid=?",
                    new String[]{"" + id});
            if (cur.getCount() > 0) {
                YLog.d(TAG, "count: " + cur.getCount());
                cur.moveToFirst();
                LTransaction = new LTransaction();
                getLItemValues(cur, LTransaction);
            }

            cur.close();
                        */
            return LTransaction;
        }
    }

    public static LTransaction getItemByRid(String rid) {
        SQLiteDatabase db = getReadDb();

        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME + " WHERE " +
                        DBHelper.TABLE_COLUMN_RID + " =?",
                new String[]{rid});
        if (cur != null && cur.getCount() > 0) {
            if (cur.getCount() != 1) {
                LLog.e(TAG, "unexpected error: duplicated record");
            }
            cur.moveToFirst();
            LTransaction item = new LTransaction();
            getItemValues(cur, item);
            cur.close();
            return item;
        }
        if (cur != null) cur.close();
        return null;
    }

    public static LTransaction getItemByUserTimestamp(int madeBy, long timestamp) {
        SQLiteDatabase db = getReadDb();

        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME + " WHERE " +
                        DBHelper.TABLE_COLUMN_MADEBY + " =? AND " +
                        DBHelper.TABLE_COLUMN_TIMESTAMP + " =?",
                new String[]{"" + madeBy, "" + timestamp});
        if (cur != null && cur.getCount() > 0) {
            if (cur.getCount() != 1) {
                LLog.e(TAG, "unexpected error: duplicated record");
            }
            cur.moveToFirst();
            LTransaction item = new LTransaction();
            getItemValues(cur, item);
            cur.close();
            return item;
        }
        if (cur != null) cur.close();
        return null;
    }

    public static void addItem(LTransaction item) {
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setItemValues(item);
            db.insert(DBHelper.TABLE_TRANSACTION_NAME, "", cv);
            dirty = true;
        }
    }

    public static void updateItem(LTransaction item) {
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setItemValues(item);
            db.update(DBHelper.TABLE_TRANSACTION_NAME, cv, "_id=?", new String[]{"" + item.getId()});
            dirty = true;
        }
    }

    public static void updateItemOwnerById(int madeBy, long id) {
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.TABLE_COLUMN_MADEBY, madeBy);
            db.update(DBHelper.TABLE_TRANSACTION_NAME, cv, "_id=?", new String[]{"" + id});
            dirty = true;
        }
    }

    public static void deleteItemById(long id) {
        LTransaction item = getLogItemById(id);
        if (item != null) {
            item.setState(DBHelper.STATE_DELETED);
            updateItem(item);
        }
    }

    private static void updateStateById(String table, long id, int state) {
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.TABLE_COLUMN_STATE, state);
            db.update(table, cv, "_id=?", new String[]{"" + id});
            dirty = true;
        }
    }

    public static void deleteAccountById(long id) {
        updateStateById(DBHelper.TABLE_ACCOUNT_NAME, id, LAccount.ACCOUNT_STATE_DELETED);
    }

    public static void deleteCategoryById(long id) {
        updateStateById(DBHelper.TABLE_CATEGORY_NAME, id, LCategory.CATEGORY_STATE_DELETED);
    }

    public static void deleteVendorById(long id) {
        updateStateById(DBHelper.TABLE_VENDOR_NAME, id, LVendor.VENDOR_STATE_DELETED);
    }

    public static void deleteTagById(long id) {
        updateStateById(DBHelper.TABLE_TAG_NAME, id, LTag.TAG_STATE_DELETED);
    }

    public static LTransaction getLogItemById(long id) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        String str = "";
        LTransaction item = new LTransaction();
        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TRANSACTION_NAME + " WHERE _id=?", new String[]{"" + id});
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

    public static String getCategoryNameById(long id) {
        return getStringFromDbById(DBHelper.TABLE_CATEGORY_NAME, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static String getVendorNameById(long id) {
        return getStringFromDbById(DBHelper.TABLE_VENDOR_NAME, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static String getTagNameById(long id) {
        return getStringFromDbById(DBHelper.TABLE_TAG_NAME, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static String getAccountNameById(long id) {
        return getStringFromDbById(DBHelper.TABLE_ACCOUNT_NAME, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static LCategory getCategoryById(long id) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        LCategory category = new LCategory();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_CATEGORY_NAME + " WHERE _id=?", new String[]{"" + id});
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find category with id: " + id);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getCategoryValues(csr, category);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get category with id: " + id + ":" + e.getMessage());
            category = null;
        }
        if (csr != null) csr.close();
        return category;
    }

    public static LVendor getVendorById(long id) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        LVendor vendor = new LVendor();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_VENDOR_NAME + " WHERE _id=?", new String[]{"" + id});
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find vendor with id: " + id);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getVendorValues(csr, vendor);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get vendor with id: " + id + ":" + e.getMessage());
            vendor = null;
        }
        if (csr != null) csr.close();
        return vendor;
    }

    public static LTag getTagById(long id) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        LTag tag = new LTag();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TAG_NAME + " WHERE _id=?", new String[]{"" + id});
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find tag with id: " + id);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getTagValues(csr, tag);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get tag with id: " + id + ":" + e.getMessage());
            tag = null;
        }
        if (csr != null) csr.close();
        return tag;
    }

    public static LAccount getAccountById(long id) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        LAccount account = new LAccount();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME + " WHERE _id=?", new String[]{"" + id});
            if (csr.getCount() != 1) {
                LLog.w(TAG, "unable to find tag with id: " + id);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getAccountValues(csr, account);
            account.setId(id);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with id: " + id + ":" + e.getMessage());
            account = null;
        }
        if (csr != null) csr.close();
        return account;
    }

    public static LAccount getAccountByUuid(UUID uuid) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        LAccount account = new LAccount();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME + " WHERE "
                    + DBHelper.TABLE_COLUMN_RID + "=?", new String[]{uuid.toString()});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find account with UUID: " + uuid);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getAccountValues(csr, account);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with UUID: " + uuid + ":" + e.getMessage());
            account = null;
        }
        if (csr != null) csr.close();
        return account;
    }

    public static LAccount getAccountByName(String name) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        LAccount account = new LAccount();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME + " WHERE "
                    + DBHelper.TABLE_COLUMN_NAME + "=?", new String[]{name});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find account with name: " + name);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getAccountValues(csr, account);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with name: " + name + ":" + e.getMessage());
            account = null;
        }
        if (csr != null) csr.close();
        return account;
    }

    public static LCategory getCategoryByName(String name) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        LCategory category = new LCategory();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_CATEGORY_NAME + " WHERE "
                    + DBHelper.TABLE_COLUMN_NAME + "=?", new String[]{name});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find category with name: " + name);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getCategoryValues(csr, category);
            category.setId(csr.getLong(0));
        } catch (Exception e) {
            LLog.w(TAG, "unable to get category with name: " + name + ":" + e.getMessage());
            category = null;
        }
        if (csr != null) csr.close();
        return category;
    }

    public static LVendor getVendorByName(String name) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        LVendor vendor = new LVendor();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_VENDOR_NAME + " WHERE "
                    + DBHelper.TABLE_COLUMN_NAME + "=?", new String[]{name});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find category with name: " + name);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getVendorValues(csr, vendor);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get vendor with name: " + name + ":" + e.getMessage());
            vendor = null;
        }
        if (csr != null) csr.close();
        return vendor;
    }

    public static LTag getTagByName(String name) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        LTag tag = new LTag();

        try {
            csr = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TAG_NAME + " WHERE "
                    + DBHelper.TABLE_COLUMN_NAME + "=?", new String[]{name});
            if (csr != null && csr.getCount() != 1) {
                LLog.w(TAG, "unable to find tag with name: " + name);
                csr.close();
                return null;
            }

            csr.moveToFirst();
            getTagValues(csr, tag);
        } catch (Exception e) {
            LLog.w(TAG, "unable to get tag with name: " + name + ":" + e.getMessage());
            tag = null;
        }
        if (csr != null) csr.close();
        return tag;
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

    public static HashSet<Integer> getAllAccountsShareUser() {
        LAccount account = new LAccount();
        HashSet<Integer> set = new HashSet<Integer>();
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT " + DBHelper.TABLE_COLUMN_SHARE + " FROM " + DBHelper.TABLE_ACCOUNT_NAME
                        + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                new String[]{"" + LAccount.ACCOUNT_STATE_ACTIVE});
        if (cur != null && cur.getCount() > 0) {

            cur.moveToFirst();
            do {
                String str = cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHARE));
                if (str != null) {
                    account.setSharedIdsString(str);
                    if (account.getShareIds() != null) {
                        for (int ii : account.getShareIds()) {
                            set.add(ii);
                        }
                    }
                }
            } while (cur.moveToNext());
        }
        if (cur != null) cur.close();
        return set;
    }

    public static Cursor getAllAccountsCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_ACCOUNT_NAME
                        + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                new String[]{"" + LAccount.ACCOUNT_STATE_ACTIVE});
        return cur;
    }

    public static Cursor getAllCategoriesCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_CATEGORY_NAME
                        + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                new String[]{"" + LCategory.CATEGORY_STATE_ACTIVE});
        return cur;
    }

    public static Cursor getAllVendorsCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_VENDOR_NAME
                        + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                new String[]{"" + LVendor.VENDOR_STATE_ACTIVE});
        return cur;
    }

    public static Cursor getAllTagsCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_TAG_NAME
                        + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                new String[]{"" + LTag.TAG_STATE_ACTIVE});
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
        return getDbIndexById(DBHelper.TABLE_ACCOUNT_NAME, DBHelper.TABLE_COLUMN_STATE,
                LAccount.ACCOUNT_STATE_ACTIVE, id);
    }

    public static int getCategoryIndexById(long id) {
        return getDbIndexById(DBHelper.TABLE_CATEGORY_NAME, DBHelper.TABLE_COLUMN_STATE,
                LCategory.CATEGORY_STATE_ACTIVE, id);
    }

    public static int getVendorIndexById(long id) {
        return getDbIndexById(DBHelper.TABLE_VENDOR_NAME, DBHelper.TABLE_COLUMN_STATE,
                LVendor.VENDOR_STATE_ACTIVE, id);
    }

    public static int getTagIndexById(long id) {
        return getDbIndexById(DBHelper.TABLE_TAG_NAME, DBHelper.TABLE_COLUMN_STATE,
                LTag.TAG_STATE_ACTIVE, id);
    }


    public static int updateAccountNameById(long id, String name) {
        LAccount account = getAccountById(id);
        if (account == null) {
            LLog.e(TAG, "account no longer exists: " + id);
            return -1;
        }
        account.setName(name);
        //account.setState(DBHelper.STATE_ACTIVE);

        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setAccountValues(account);
            db.update(DBHelper.TABLE_ACCOUNT_NAME, cv, "_id=?", new String[]{"" + id});
            dirty = true;
        }
        return 0;
    }


    public static int updateCategoryNameById(long id, String name) {
        LCategory category = getCategoryById(id);
        if (category == null) {
            LLog.e(TAG, "category no longer exists: " + id);
            return -1;
        }
        category.setName(name);

        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setCategoryValues(category);
            db.update(DBHelper.TABLE_CATEGORY_NAME, cv, "_id=?", new String[]{"" + id});
            dirty = true;
        }
        return 0;
    }

    public static int updateVendorNameById(long id, String name) {
        LVendor vendor = getVendorById(id);
        if (vendor == null) {
            LLog.e(TAG, "vendor no longer exists: " + id);
            return -1;
        }
        vendor.setName(name);

        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setVendorValues(vendor);
            db.update(DBHelper.TABLE_VENDOR_NAME, cv, "_id=?", new String[]{"" + id});
            dirty = true;
        }
        return 0;
    }

    public static int updateTagNameById(long id, String name) {
        LTag tag = getTagById(id);
        if (tag == null) {
            LLog.e(TAG, "tag no longer exists: " + id);
            return -1;
        }
        tag.setName(name);

        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setTagValues(tag);
            db.update(DBHelper.TABLE_TAG_NAME, cv, "_id=?", new String[]{"" + id});
            dirty = true;
        }
        return 0;
    }

    public static void getAccountSummaryForCurrentCursor(LAccountSummary summary, long id, Cursor cursor) {
        double income = 0;
        double expense = 0;

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                double value = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                if (type == LTransaction.TRANSACTION_TYPE_INCOME) income += value;
                else if (type == LTransaction.TRANSACTION_TYPE_EXPENSE) expense += value;
            } while (cursor.moveToNext());
        }
        summary.setBalance(income - expense);
        summary.setIncome(income);
        summary.setExpense(expense);
    }

    /*public static void getSummaryForAll(LAccountSummary summary) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        double income = 0;
        double expense = 0;
        try {
            csr = db.rawQuery("SELECT "
                            + DBHelper.TABLE_LOG_COLUMN_TYPE + ","
                            + DBHelper.TABLE_LOG_COLUMN_VALUE + " FROM "
                            + DBHelper.TABLE_LOG_NAME + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + LTransaction.LOG_STATE_ACTIVE});

            csr.moveToFirst();
            do {
                double value = csr.getDouble(csr.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_VALUE));
                int type = csr.getInt(csr.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TYPE));
                if (type == LTransaction.TRANSACTION_TYPE_INCOME) income += value;
                else if (type == LTransaction.TRANSACTION_TYPE_EXPENSE) expense += value;
            } while (csr.moveToNext());
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
        if (csr != null) csr.close();

        summary.setBalance(income - expense);
        summary.setIncome(income);
        summary.setExpense(expense);
    }*/

    public static void addVendorCategory(long vendor, long category) {
        boolean exists = false;
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        try {
            csr = db.rawQuery("SELECT * FROM "
                            + DBHelper.TABLE_VENDOR_CATEGORY_NAME + " WHERE "
                            + DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_VENDOR + "=? AND "
                            + DBHelper.TABLE_COLUMN_CATEGORY + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + vendor, "" + category});
            if (csr != null) {
                if (csr.getCount() > 0) exists = true;
                csr.close();
            }

            if (!exists) {
                synchronized (dbLock) {
                    db = getWriteDb();
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_ACTIVE);
                    cv.put(DBHelper.TABLE_COLUMN_VENDOR, vendor);
                    cv.put(DBHelper.TABLE_COLUMN_CATEGORY, category);
                    db.insert(DBHelper.TABLE_VENDOR_CATEGORY_NAME, "", cv);
                    dirty = true;
                }
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
    }

    public static HashSet<Long> getVendorCategories(long vendor) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        HashSet<Long> cats = new HashSet<Long>();
        try {
            csr = db.rawQuery("SELECT * FROM "
                            + DBHelper.TABLE_VENDOR_CATEGORY_NAME + " WHERE "
                            + DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_VENDOR + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + vendor});
            if (csr != null) {
                if (csr.getCount() > 0) {
                    csr.moveToFirst();
                    do {
                        cats.add(csr.getLong(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CATEGORY)));
                    } while (csr.moveToNext());
                    return cats;
                }
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
        return cats;
    }

    public static ArrayList<String> getAccountBalance(long id, LBoxer boxer) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        ArrayList<String> balance = new ArrayList<String>();
        try {
            csr = db.rawQuery("SELECT " + DBHelper.TABLE_COLUMN_YEAR + ","
                            + DBHelper.TABLE_COLUMN_BALANCE + " FROM "
                            + DBHelper.TABLE_ACCOUNT_BALANCE_NAME + " WHERE _id=? ORDER BY "
                            + DBHelper.TABLE_COLUMN_YEAR + " ASC",
                    new String[]{"" + id});
            int startYear = -1, endYear = -1;
            if (csr != null && csr.getCount() > 0) {
                csr.moveToFirst();
                do {
                    int year = csr.getInt(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_YEAR));
                    if (startYear == -1) startYear = year;
                    endYear = year;
                    balance.add("" + year);
                    balance.add(csr.getString(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_BALANCE)));
                } while (csr.moveToNext());
                csr.close();
                boxer.x = startYear;
                boxer.y = endYear;
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
        return balance;
    }

    public static String getAccountBalance(long id, int year) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        String balance = "";
        try {
            csr = db.rawQuery("SELECT * FROM "
                            + DBHelper.TABLE_ACCOUNT_BALANCE_NAME + " WHERE _id=? AND "
                            + DBHelper.TABLE_COLUMN_YEAR + "=?",
                    new String[]{"" + id, "" + year});
            if (csr != null) {
                csr.moveToFirst();
                balance = csr.getString(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_BALANCE));
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
        return balance;
    }

    public static boolean updateCategory(LCategory category) {
        try {
            synchronized (dbLock) {
                SQLiteDatabase db = getWriteDb();
                ContentValues cv = setCategoryValues(category);
                db.update(DBHelper.TABLE_CATEGORY_NAME, cv, "_id=?", new String[]{"" + category.getId()});
            }
            dirty = true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean updateVendor(LVendor vendor) {
        try {
            synchronized (dbLock) {
                SQLiteDatabase db = getWriteDb();
                ContentValues cv = setVendorValues(vendor);
                db.update(DBHelper.TABLE_VENDOR_NAME, cv, "_id=?", new String[]{"" + vendor.getId()});
            }
            dirty = true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean updateTag(LTag tag) {
        try {
            synchronized (dbLock) {
                SQLiteDatabase db = getWriteDb();
                ContentValues cv = setTagValues(tag);
                db.update(DBHelper.TABLE_TAG_NAME, cv, "_id=?", new String[]{"" + tag.getId()});
            }
            dirty = true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean updateAccount(LAccount account) {
        try {
            synchronized (dbLock) {
                SQLiteDatabase db = getWriteDb();
                ContentValues cv = setAccountValues(account);
                db.update(DBHelper.TABLE_ACCOUNT_NAME, cv, "_id=?", new String[]{"" + account.getId()});
            }
            dirty = true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean updateAccountBalance(long id, int year, String balance) {
        boolean exists = false;
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        boolean ret = false;

        try {
            long rowId = 0;
            csr = db.rawQuery("SELECT * FROM "
                            + DBHelper.TABLE_ACCOUNT_BALANCE_NAME + " WHERE _id=? AND "
                            + DBHelper.TABLE_COLUMN_YEAR + "=?",
                    new String[]{"" + id, "" + year});
            if (csr != null) {
                if (csr.getCount() > 0) {
                    csr.moveToFirst();
                    rowId = csr.getInt(0);
                    exists = true;
                }
                csr.close();
            }

            ContentValues cv = new ContentValues();
            cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, id);
            cv.put(DBHelper.TABLE_COLUMN_YEAR, year);
            cv.put(DBHelper.TABLE_COLUMN_BALANCE, balance);

            synchronized (dbLock) {
                db = getWriteDb();
                if (!exists) {
                    db.insert(DBHelper.TABLE_ACCOUNT_BALANCE_NAME, "", cv);

                } else {
                    db.update(DBHelper.TABLE_ACCOUNT_BALANCE_NAME, cv, "_id=?", new String[]{"" + rowId});
                }
                ret = true;
            }
            dirty = true;
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }

        return ret;
    }

    // scans through transaction to compile account monthly balances, from the very first transaction to the latest, in that order.
    public static HashMap<Integer, double[]> scanAccountBalanceById(long id, LBoxer boxer) {
        SQLiteDatabase db = getReadDb();
        Cursor csr = null;
        HashMap<Integer, double[]> balances = new HashMap<Integer, double[]>();

        long timestamp;
        try {
            csr = db.rawQuery("SELECT "
                            + DBHelper.TABLE_COLUMN_AMOUNT + ","
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + ","
                            + DBHelper.TABLE_COLUMN_TYPE + ","
                            + DBHelper.TABLE_COLUMN_ACCOUNT + " FROM "
                            + DBHelper.TABLE_TRANSACTION_NAME + " WHERE "
                            + DBHelper.TABLE_COLUMN_ACCOUNT + "=? AND "
                            + DBHelper.TABLE_COLUMN_STATE + "=? ORDER BY " + DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + id, "" + DBHelper.STATE_ACTIVE});
            if (csr != null && csr.getCount() > 0) {
                csr.moveToFirst();

                Calendar now = Calendar.getInstance();
                double acc = 0;
                int lastMonth = -1;
                int lastYear = -1;
                int month;
                double[] lastAmount;
                boxer.x = -1; //startYear
                boxer.y = -1; //endYear
                do {
                    int type = csr.getInt(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                    double v = csr.getDouble(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                    if (type == LTransaction.TRANSACTION_TYPE_EXPENSE) v = -v;
                    timestamp = csr.getLong(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP));

                    now.setTimeInMillis(timestamp);

                    int year = now.get(Calendar.YEAR);
                    double[] amount = balances.get(year);
                    if (amount == null) {
                        amount = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        balances.put(year, amount);
                    }
                    acc += v;
                    month = now.get(Calendar.MONTH);

                    // fill up months in between
                    if (((lastMonth != -1) && (lastMonth != month))
                            || ((lastYear != -1) && (lastYear != year))) {
                        int nextMonth = lastMonth + 1;
                        int nextYear = lastYear;
                        if (nextMonth > 11) {
                            nextMonth = 0;
                            nextYear++;
                        }

                        while (((nextMonth != month) || (nextYear != year))) {
                            double[] am = balances.get(nextYear);
                            if (am == null) {
                                am = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                            }
                            am[nextMonth] = acc;
                            balances.put(nextYear, am);

                            nextMonth++;
                            if (nextMonth > 11) {
                                nextYear++;
                                nextMonth = 0;
                            }
                        }
                    }

                    amount[month] = acc;
                    lastMonth = month;
                    lastYear = year;
                    lastAmount = amount;

                    if (boxer.x == -1) boxer.x = year;
                    boxer.y = year;
                } while (csr.moveToNext());
                csr.close();

                // it is possible that the rest of months within the latest transaction year carry
                // balance of zero, let's fix them now.
                for (month = lastMonth + 1; month < 12; month++) {
                    lastAmount[month] = acc;
                }
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get log record: " + e.getMessage());
        }
        return balances;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Journal support
    private static ContentValues setJournalValues(LJournal journal) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_STATE, journal.getState());
        cv.put(DBHelper.TABLE_COLUMN_TO_USER, journal.getUserId());
        cv.put(DBHelper.TABLE_COLUMN_RECORD, journal.getRecord());
        return cv;
    }

    private static void getJournalValues(Cursor cur, LJournal journal) {
        journal.setState(cur.getInt(cur.getColumnIndex(DBHelper.TABLE_COLUMN_STATE)));
        journal.setUserId(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TO_USER)));
        journal.setRecord(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RECORD)));
        journal.setId(cur.getLong(0));
    }

    public static long addJournal(LJournal journal) {
        long id = -1;
        synchronized (dbLock) {
            SQLiteDatabase db = getWriteDb();
            ContentValues cv = setJournalValues(journal);
            id = db.insert(DBHelper.TABLE_JOURNAL_NAME, "", cv);
            dirty = true;
        }
        return id;
    }

    public static boolean updateJournal(LJournal journal) {
        try {
            synchronized (dbLock) {
                SQLiteDatabase db = getWriteDb();
                ContentValues cv = setJournalValues(journal);
                db.update(DBHelper.TABLE_JOURNAL_NAME, cv, "_id=?", new String[]{"" + journal.getId()});
            }
            dirty = true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean deleteJournalById(long id) {
        try {
            synchronized (dbLock) {
                SQLiteDatabase db = getWriteDb();
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.TABLE_COLUMN_STATE, LJournal.JOURNAL_STATE_DELETED);
                db.update(DBHelper.TABLE_JOURNAL_NAME, cv, "_id=?", new String[]{"" + id});
            }
            dirty = true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static Cursor getAllActiveJournalCursor() {
        SQLiteDatabase db = getReadDb();
        Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_JOURNAL_NAME
                + " WHERE State=?", new String[]{"" + DBHelper.STATE_ACTIVE});
        return cur;
    }
}
