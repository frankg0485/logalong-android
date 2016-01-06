package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LVendor;

import java.util.HashSet;
import java.util.UUID;

public class DBVendor {
    private static final String TAG = DBVendor.class.getSimpleName();

    private static ContentValues setValues(LVendor vendor) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, vendor.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, vendor.getState());
        cv.put(DBHelper.TABLE_COLUMN_TYPE, vendor.getType());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, vendor.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_RID, vendor.getRid());
        return cv;
    }

    private static void getValues(Cursor cur, LVendor vendor) {
        vendor.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        vendor.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        vendor.setType(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE)));
        vendor.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        vendor.setRid(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID)));
        vendor.setId(cur.getLong(0));
    }

    public static long add(LVendor vendor) {
        return add(LApp.ctx, vendor);
    }

    public static long add(Context context, LVendor vendor) {
        long id = -1;
        try {
            ContentValues cv = setValues(vendor);
            Uri uri = context.getContentResolver().insert(DBProvider.URI_VENDORS, cv);
            id = ContentUris.parseId(uri);
        } catch (Exception e) {
        }
        return id;
    }

    public static boolean update(LVendor vendor) {
        return update(LApp.ctx, vendor);
    }

    public static boolean update(Context context, LVendor vendor) {
        try {
            ContentValues cv = setValues(vendor);
            context.getContentResolver().update(DBProvider.URI_VENDORS, cv, "_id=?", new String[]{"" + vendor.getId()});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void deleteById(long id) {
        updateColumnById(id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    }

    public static LVendor getById(long id) {
        return getById(LApp.ctx, id);
    }

    public static LVendor getById(Context context, long id) {
        LVendor vendor = new LVendor();

        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    "_id=?", new String[]{"" + id}, null);

            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find vendor with id: " + id);
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, vendor);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get vendor with id: " + id + ":" + e.getMessage());
            vendor = null;
        }
        return vendor;
    }

    public static LVendor getByRid(String rid) {
        return getByRid(LApp.ctx, rid);
    }

    public static LVendor getByRid(Context context, String rid) {
        LVendor vendor = new LVendor();

        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_RID + "=?", new String[]{rid}, null);

            if (csr != null) {
                if (csr.getCount() > 1 || csr.getCount() < 1) {
                    LLog.w(TAG, "unable to find vendor with rid: " + rid + " count: " + csr.getCount());
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, vendor);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get vendor with RID: " + rid + ":" + e.getMessage());
            vendor = null;
        }
        return vendor;
    }

    public static LVendor getByName(String name) {
        return getByName(LApp.ctx, name);
    }

    public static LVendor getByName(Context context, String name) {
        LVendor vendor = new LVendor();

        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_NAME + "=? COLLATE NOCASE AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{name, "" + DBHelper.STATE_ACTIVE}, null);

            if (csr != null) {
                if (csr.getCount() > 1 || csr.getCount() < 1) {
                    LLog.w(TAG, "unable to find vendor with name: " + name + " count: " + csr.getCount());
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, vendor);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get vendor with name: " + name + ":" + e.getMessage());
            vendor = null;
        }
        return vendor;
    }

    public static long getIdByRid(String rid) {
        return DBAccess.getIdByRid(DBProvider.URI_VENDORS, rid);
    }

    public static String getNameById(long id) {
        return DBAccess.getStringFromDbById(DBProvider.URI_VENDORS, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static boolean updateColumnById(long id, String column, String value) {
        return DBAccess.updateColumnById(DBProvider.URI_VENDORS, id, column, value);
    }

    public static boolean updateColumnById(long id, String column, int value) {
        return DBAccess.updateColumnById(DBProvider.URI_VENDORS, id, column, value);
    }

    public static long getIdByName(String name) {
        return DBAccess.getIdByName(DBProvider.URI_VENDORS, name);
    }

    private static int getDbIndexById(int type, long id) {
        return getDbIndexById(LApp.ctx, type, id);
    }

    private static int getDbIndexById(Context context, int type, long id) {
        Cursor csr = null;
        int index = 0;
        int ret = -1;
        try {
            csr = context.getContentResolver().query(DBProvider.URI_VENDORS, new String[]{"_id"},
                    DBHelper.TABLE_COLUMN_STATE + "=? AND ( "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? OR "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? )",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + type, "" + LVendor.TYPE_PAYEE_PAYER},
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

    public static int getPayerIndexById(long id) {
        return getDbIndexById(LVendor.TYPE_PAYER, id);
    }

    public static int getPayeeIndexById(long id) {
        return getDbIndexById(LVendor.TYPE_PAYEE, id);
    }

    public static Cursor getCursorSortedBy(String sortColumn) {
        return getCursorSortedBy(LApp.ctx, sortColumn);
    }

    public static Cursor getCursorSortedBy(Context context, String sortColumn) {
        Cursor cur;
        if (sortColumn != null)
            cur = context.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE},
                    sortColumn + " ASC");
        else
            cur = context.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        return cur;
    }

    public static Cursor getPayerCursorSortedBy(String sortColumn) {
        return getPayerCursorSortedBy(LApp.ctx, sortColumn);
    }

    public static Cursor getPayerCursorSortedBy(Context context, String sortColumn) {
        Cursor cur;
        if (sortColumn != null)
            cur = context.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND ( "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? OR "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? )",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + LVendor.TYPE_PAYER, "" + LVendor.TYPE_PAYEE_PAYER},
                    sortColumn + " ASC");
        else
            cur = context.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND ( "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? OR "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? )",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + LVendor.TYPE_PAYER, "" + LVendor.TYPE_PAYEE_PAYER}, null);
        return cur;
    }

    public static Cursor getPayeeCursorSortedBy(String sortColumn) {
        return getPayeeCursorSortedBy(LApp.ctx, sortColumn);
    }

    public static Cursor getPayeeCursorSortedBy(Context context, String sortColumn) {
        Cursor cur;
        if (sortColumn != null)
            cur = context.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND ( "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? OR "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? )",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + LVendor.TYPE_PAYEE, "" + LVendor.TYPE_PAYEE_PAYER},
                    sortColumn + " ASC");
        else
            cur = context.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND ( "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? OR "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? )",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + LVendor.TYPE_PAYEE, "" + LVendor.TYPE_PAYEE_PAYER}, null);
        return cur;
    }

    public static HashSet<Long> getCategories(long vendor) {
        return getCategories(LApp.ctx, vendor);
    }

    public static HashSet<Long> getCategories(Context context, long vendor) {
        Cursor csr = null;
        HashSet<Long> cats = new HashSet<Long>();
        try {
            csr = context.getContentResolver().query(DBProvider.URI_VENDORS_CATEGORY, null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_VENDOR + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + vendor}, null);
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
        setCategories(LApp.ctx, vendor, categories);
    }

    public static void setCategories(Context context, long vendor, HashSet<Long> categories) {
        boolean exists = false;
        HashSet<Long> oldCategories = getCategories(context, vendor);
        HashSet<Long> and = new HashSet<Long>(oldCategories);
        and.retainAll(categories);

        for (Long ll : oldCategories) {
            if (!and.contains(ll)) {
                do_updateCategory(context, vendor, ll, false, true);
            }
        }

        for (Long ll : categories) {
            if (!and.contains(ll)) {
                do_updateCategory(context, vendor, ll, true, true);
            }
        }
    }

    private static void do_updateCategory(Context context, long vendor, long category, boolean add, boolean writeJournal) {
        boolean exists = false;
        Cursor csr = null;
        long id = 0;
        boolean changed = false;

        try {
            csr = context.getContentResolver().query(DBProvider.URI_VENDORS_CATEGORY, null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_VENDOR + "=? AND "
                            + DBHelper.TABLE_COLUMN_CATEGORY + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + vendor, "" + category}, null);
            if (csr != null) {
                if (csr.getCount() > 0) {
                    exists = true;
                    csr.moveToFirst();
                    id = csr.getLong(0);
                }

                csr.close();
            }

            if (!exists && add) {
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_ACTIVE);
                cv.put(DBHelper.TABLE_COLUMN_VENDOR, vendor);
                cv.put(DBHelper.TABLE_COLUMN_CATEGORY, category);
                context.getContentResolver().insert(DBProvider.URI_VENDORS_CATEGORY, cv);
                changed = true;
            } else if (exists && !add) {
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
                context.getContentResolver().update(DBProvider.URI_VENDORS_CATEGORY, cv, "_id=?", new String[]{"" + id});
                changed = true;
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
        do_updateCategory(LApp.ctx, vendor, category, add, false);
    }
}
