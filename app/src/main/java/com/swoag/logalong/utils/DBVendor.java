package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LVendor;

import java.util.HashSet;
import java.util.UUID;

public class DBVendor {
    private static final String TAG = DBVendor.class.getSimpleName();

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

    public static LVendor getById(long id) {
        SQLiteDatabase db = DBAccess.getReadDb();
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

    public static long getIdByRid(UUID rid) {
        return DBAccess.getIdByRid(DBHelper.TABLE_VENDOR_NAME, rid);
    }

    public static Cursor getCursorSortedBy(String sortColumn) {
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor cur;
        if (sortColumn != null)
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_VENDOR_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=? ORDER BY " + sortColumn + " ASC",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        else
            cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_VENDOR_NAME
                            + " WHERE " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE});
        return cur;
    }

    public static HashSet<Long> getCategories(long vendor) {
        SQLiteDatabase db = DBAccess.getReadDb();
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
            LLog.w(TAG, "unable to get vendor categories: " + e.getMessage());
        }
        return cats;
    }

    public static void setCategories(long vendor, HashSet<Long> categories) {
        boolean exists = false;
        HashSet<Long> oldCategories = getCategories(vendor);
        HashSet<Long> and = new HashSet<Long>(oldCategories);
        and.retainAll(categories);

        for (Long ll : oldCategories) {
            if (!and.contains(ll)) {
                do_updateCategory(vendor, ll, false, true);
            }
        }

        for (Long ll : categories) {
            if (!and.contains(ll)) {
                do_updateCategory(vendor, ll, true, true);
            }
        }
    }

    private static void do_updateCategory(long vendor, long category, boolean add, boolean writeJournal) {
        boolean exists = false;
        SQLiteDatabase db = DBAccess.getReadDb();
        Cursor csr = null;
        long id = 0;
        boolean changed = false;

        try {
            csr = db.rawQuery("SELECT * FROM "
                            + DBHelper.TABLE_VENDOR_CATEGORY_NAME + " WHERE "
                            + DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_VENDOR + "=? AND "
                            + DBHelper.TABLE_COLUMN_CATEGORY + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + vendor, "" + category});
            if (csr != null) {
                if (csr.getCount() > 0) {
                    exists = true;
                    csr.moveToFirst();
                    id = csr.getLong(0);
                }

                csr.close();
            }

            if (!exists && add) {
                synchronized (DBAccess.dbLock) {
                    db = DBAccess.getWriteDb();
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_ACTIVE);
                    cv.put(DBHelper.TABLE_COLUMN_VENDOR, vendor);
                    cv.put(DBHelper.TABLE_COLUMN_CATEGORY, category);
                    db.insert(DBHelper.TABLE_VENDOR_CATEGORY_NAME, "", cv);
                    DBAccess.dirty = true;
                    changed = true;
                }
            } else if (exists && !add) {
                synchronized (DBAccess.dbLock) {
                    db = DBAccess.getWriteDb();
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
                    db.update(DBHelper.TABLE_VENDOR_CATEGORY_NAME, cv, "_id=?", new String[]{"" + id});
                    DBAccess.dirty = true;
                    changed = true;
                }
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to update vendor category: " + e.getMessage());
        }

        if (changed && writeJournal) {
            LVendor vend = getById(vendor);
            LCategory cat = DBCategory.getById(category);

            LJournal journal = new LJournal();
            journal.updateVendorCategory(add, vend.getRid(), cat.getRid());
        }
    }

    public static void updateCategory(long vendor, long category, boolean add) {
        do_updateCategory(vendor, category, add, false);
    }
}
