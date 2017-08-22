package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LVendor;

import java.util.HashSet;

public class DBVendor extends DBGeneric<LVendor> {
    private static final String tag = DBVendor.class.getSimpleName();
    private static DBVendor instance;

    private static final String[] vendor_columns = new String[]{
            "_id",
            DBHelper.TABLE_COLUMN_GID,
            DBHelper.TABLE_COLUMN_NAME,
            DBHelper.TABLE_COLUMN_STATE,
            DBHelper.TABLE_COLUMN_TYPE,
            DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE};

    public DBVendor() {
    }

    public static DBVendor getInstance() {
        if (null == instance) {
            instance = new DBVendor();
        }
        return instance;
    }

    @Override
    LVendor getValues(Cursor cur, LVendor vendor) {
        if (null == vendor) vendor = new LVendor();
        vendor.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        vendor.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        vendor.setType(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE)));
        vendor.setGid(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_GID)));
        vendor.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        vendor.setId(cur.getLong(0));
        return vendor;
    }

    @Override
    ContentValues setValues(LVendor vendor) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, vendor.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, vendor.getState());
        cv.put(DBHelper.TABLE_COLUMN_TYPE, vendor.getType());
        cv.put(DBHelper.TABLE_COLUMN_GID, vendor.getGid());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, vendor.getTimeStampLast());
        return cv;
    }

    @Override
    String[] getColumns() {
        return vendor_columns;
    }

    @Override
    Uri getUri() {
        return DBProvider.URI_VENDORS;
    }

    @Override
    long getId(LVendor vendor) {
        return vendor.getId();
    }

    @Override
    void setId(LVendor vendor, long id) {
        vendor.setId(id);
    }

    private int getDbIndexById(int type, long id) {
        Cursor csr = null;
        int index = 0;
        int ret = -1;
        try {
            csr = LApp.ctx.getContentResolver().query(DBProvider.URI_VENDORS, new String[]{"_id"},
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
            LLog.w(tag, "unable to get with id: " + id + ":" + e.getMessage());
        }
        if (csr != null) csr.close();
        return ret;
    }

    public int getPayerIndexById(long id) {
        return getDbIndexById(LVendor.TYPE_PAYER, id);
    }

    public int getPayeeIndexById(long id) {
        return getDbIndexById(LVendor.TYPE_PAYEE, id);
    }

    public Cursor getPayerPayeeCursorSortedBy(String sortColumn, boolean payer) {
        Cursor cur;
        if (sortColumn != null)
            cur = LApp.ctx.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND ( "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? OR "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? )",
                    new String[]{"" + DBHelper.STATE_ACTIVE,
                            payer? "" + LVendor.TYPE_PAYER : "" + LVendor.TYPE_PAYEE,
                            "" + LVendor.TYPE_PAYEE_PAYER},
                    sortColumn + " ASC");
        else
            cur = LApp.ctx.getContentResolver().query(DBProvider.URI_VENDORS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND ( "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? OR "
                            + DBHelper.TABLE_COLUMN_TYPE + "=? )",
                    new String[]{"" + DBHelper.STATE_ACTIVE,
                            payer? "" + LVendor.TYPE_PAYER : "" + LVendor.TYPE_PAYEE,
                            "" + LVendor.TYPE_PAYEE_PAYER}, null);
        return cur;
    }

    public Cursor getPayerCursorSortedBy(String sortColumn) {
        return getPayerPayeeCursorSortedBy(sortColumn, true);
    }

    public Cursor getPayeeCursorSortedBy(String sortColumn) {
        return getPayerPayeeCursorSortedBy(sortColumn, false);
    }

    public static HashSet<Long> getCategories(long vendor) {
        Cursor csr = null;
        HashSet<Long> cats = new HashSet<Long>();
        try {
            csr = LApp.ctx.getContentResolver().query(DBProvider.URI_VENDORS_CATEGORY, null,
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
            LLog.w(tag, "unable to get vendor categories: " + e.getMessage());
        }
        return cats;
    }

    public void setCategories(long vendor, HashSet<Long> categories) {
        HashSet<Long> oldCategories = getCategories(vendor);
        HashSet<Long> and = new HashSet<Long>(oldCategories);
        and.retainAll(categories);

        for (Long ll : oldCategories) {
            if (!and.contains(ll)) {
                do_updateCategory(vendor, ll, false);
            }
        }

        for (Long ll : categories) {
            if (!and.contains(ll)) {
                do_updateCategory(vendor, ll, true);
            }
        }
    }

    private void do_updateCategory(long vendor, long category, boolean add) {
        boolean exists = false;
        Cursor csr = null;
        long id = 0;

        try {
            csr = LApp.ctx.getContentResolver().query(DBProvider.URI_VENDORS_CATEGORY, null,
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
                LApp.ctx.getContentResolver().insert(DBProvider.URI_VENDORS_CATEGORY, cv);
            } else if (exists && !add) {
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
                LApp.ctx.getContentResolver().update(DBProvider.URI_VENDORS_CATEGORY, cv, "_id=?", new String[]{"" + id});
            }
        } catch (Exception e) {
            LLog.w(tag, "unable to update vendor category: " + e.getMessage());
        }
    }
}
