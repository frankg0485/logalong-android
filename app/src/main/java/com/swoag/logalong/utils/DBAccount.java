package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.swoag.logalong.LApp;
import com.swoag.logalong.MainService;
import com.swoag.logalong.entities.LAccount;

import java.util.HashSet;

public class DBAccount {
    private static final String TAG = DBAccount.class.getSimpleName();

    private static ContentValues setValues(LAccount account) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.TABLE_COLUMN_NAME, account.getName());
        cv.put(DBHelper.TABLE_COLUMN_STATE, account.getState());
        cv.put(DBHelper.TABLE_COLUMN_NUMBER, account.getGid());
        cv.put(DBHelper.TABLE_COLUMN_SHARE, account.getShareIdsString());
        cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE, account.getTimeStampLast());
        cv.put(DBHelper.TABLE_COLUMN_RID, account.getRid());
        return cv;
    }

    private static void getValues(Cursor cur, LAccount account) {
        account.setName(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME)));
        account.setState(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)));
        account.setGid(cur.getInt(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NUMBER)));
        account.setSharedIdsString(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHARE)));
        account.setTimeStampLast(cur.getLong(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)));
        account.setRid(cur.getString(cur.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID)));
        account.setId(cur.getLong(0));
    }

    public static LAccount getByCursor(Cursor cursor) {
        LAccount account = new LAccount();
        getValues(cursor, account);
        return account;
    }

    public static LAccount getById(long id) {
        return getById(LApp.ctx, id);
    }

    public static LAccount getById(Context context, long id) {
        if (id <= 0) return null;

        LAccount account = new LAccount();
        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, null,
                    "_id=?", new String[]{"" + id}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "unable to find account with id: " + id);
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, account);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with id: " + id + ":" + e.getMessage());
            account = null;
        }
        return account;
    }

    public static LAccount getByGid(int gid) {
        return getByGid(LApp.ctx, gid);
    }

    private static LAccount getByGid(Context context, int gid) {
        if (gid <= 0) return null;

        LAccount account = new LAccount();
        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, null,
                    DBHelper.TABLE_COLUMN_NUMBER + "=? AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + gid, "" + DBHelper.STATE_ACTIVE}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "GID not unique: unable to find account with gid: " + gid);
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, account);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with gid: " + gid + ":" + e.getMessage());
            account = null;
        }
        return account;
    }

    public static void resetGidIfNotUnique(int gid) {
        if (gid <= 0) return;

        LAccount account = new LAccount();
        try {
            Cursor csr = LApp.ctx.getContentResolver().query(DBProvider.URI_ACCOUNTS, null,
                    DBHelper.TABLE_COLUMN_NUMBER + "=? AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + gid, "" + DBHelper.STATE_ACTIVE}, null);
            if (csr != null) {
                if (csr.getCount() != 1) {
                    LLog.w(TAG, "GID not unique: reset all to zero from: " + gid);
                    int[] ids = new int[csr.getCount()];
                    int ii = 0;
                    csr.moveToFirst();
                    do {
                        ids[ii++] = csr.getInt(0);
                    } while (csr.moveToNext());

                    for (ii = 0; ii < ids.length; ii++) {
                        updateColumnById(ids[ii], DBHelper.TABLE_COLUMN_NUMBER, 0);
                    }
                }
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with gid: " + gid + ":" + e.getMessage());
        }
    }

    public static LAccount getByName(String name) {
        return getByName(LApp.ctx, name);
    }

    public static LAccount getByName(Context context, String name) {
        LAccount account = new LAccount();

        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, null,
                    DBHelper.TABLE_COLUMN_NAME + "=? COLLATE NOCASE AND " + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{name, "" + DBHelper.STATE_ACTIVE}, null);
            if (csr != null) {
                if (csr.getCount() < 1 || csr.getCount() > 1) {
                    LLog.w(TAG, "unable to find account with name: " + name + " count: " + csr.getCount());
                    csr.close();
                    return null;
                }

                csr.moveToFirst();
                getValues(csr, account);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with name: " + name + ":" + e.getMessage());
            account = null;
        }
        return account;
    }

    //public static LAccount getByRid(String rid) {
    //    return getByRid(LApp.ctx, rid);
    //}

    /*
    public static LAccount getByRid(Context context, String rid) {
        LAccount account = null;
        try {
            Cursor csr = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, null,
                    DBHelper.TABLE_COLUMN_RID + "=?", new String[]{rid}, null);
            if (csr != null) {

                if (csr.getCount() < 1 || csr.getCount() > 1) {
                    LLog.w(TAG, "unable to find account with uuid: " + rid + " count: " + csr.getCount());
                    csr.close();
                    return null;
                }

                account = new LAccount();
                csr.moveToFirst();
                getValues(csr, account);
                csr.close();
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to get account with rid: " + rid + ":" + e.getMessage());
        }
        return account;
    }
    */

    public static long add(LAccount acccount) {
        return add(LApp.ctx, acccount);
    }

    public static long add(Context context, LAccount acccount) {
        ContentValues cv = setValues(acccount);
        long id = -1;
        try {
            Uri uri = context.getContentResolver().insert(DBProvider.URI_ACCOUNTS, cv);
            id = ContentUris.parseId(uri);
        } catch (Exception e) {
            LLog.w(TAG, "unable to add account: " + e.getMessage());
        }
        return id;
    }

    public static boolean update(LAccount account) {
        return update(LApp.ctx, account);
    }

    public static boolean update(Context context, LAccount account) {
        try {
            ContentValues cv = setValues(account);
            context.getContentResolver().update(DBProvider.URI_ACCOUNTS, cv, "_id=?", new String[]{"" + account.getId()});
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    //public static void deleteById(long id) {
    //    updateColumnById(id, DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_DELETED);
    //}

    private static boolean updateColumnById(long id, String column, int value) {
        return DBAccess.updateColumnById(DBProvider.URI_ACCOUNTS, id, column, value);
    }

    public static boolean updateColumnById(long id, String column, String value) {
        return DBAccess.updateColumnById(DBProvider.URI_ACCOUNTS, id, column, value);
    }

    public static long getIdByName(String name) {
        return DBAccess.getIdByName(DBProvider.URI_ACCOUNTS, name);
    }

    public static HashSet<Integer> getAllShareUser() {
        return getAllShareUser(LApp.ctx);
    }

    public static HashSet<Integer> getAllShareUser(Context context) {
        LAccount account = new LAccount();
        HashSet<Integer> set = new HashSet<Integer>();

        Cursor cur = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, new String[]{DBHelper.TABLE_COLUMN_SHARE},
                DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
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

    public static Cursor getCursorSortedBy(String sortColumn) {
        return getCursorSortedBy(LApp.ctx, sortColumn);
    }

    public static Cursor getCursorSortedBy(Context context, String sortColumn) {
        Cursor cur;
        if (sortColumn != null)
            cur = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE},
                    sortColumn + " ASC");
        else
            cur = context.getContentResolver().query(DBProvider.URI_ACCOUNTS, null,
                    DBHelper.TABLE_COLUMN_STATE + "=?", new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        return cur;
    }

    public static String getNameById(long id) {
        return DBAccess.getStringFromDbById(DBProvider.URI_ACCOUNTS, DBHelper.TABLE_COLUMN_NAME, id);
    }

    public static int getDbIndexById(long id) {
        return DBAccess.getDbIndexById(LApp.ctx, DBProvider.URI_ACCOUNTS, id);
    }

    public static HashSet<Long> getAllActiveAccountIds() {
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
