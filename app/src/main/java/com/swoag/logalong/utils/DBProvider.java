package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.swoag.logalong.entities.LAccountBalance;
import com.swoag.logalong.entities.LTransaction;

import java.util.Calendar;
import java.util.HashMap;

public class DBProvider extends ContentProvider {
    private static final String TAG = DBProvider.class.getSimpleName();
    private static final String PROVIDER_NAME = "com.swoag.logalong.utils.DBProvider";
    private static DBHelper helper;
    private SQLiteDatabase db;

    private static HashMap<String, String> PROJECTION_MAP;
    private static boolean updatingAccountBalance = false;

    private static final String TRANSACTIONS = "trans";
    private static final String TRANSACTIONS_ACCOUNT = "trans/account";
    private static final String TRANSACTIONS_CATEGORY = "trans/category";
    private static final String TRANSACTIONS_TAG = "trans/tag";
    private static final String TRANSACTIONS_VENDOR = "trans/vendor";
    private static final String ACCOUNTS = "accounts";
    private static final String CATEGORIES = "categories";
    private static final String TAGS = "tags";
    private static final String VENDORS = "vendors";
    private static final String VENDORS_CATEGORY = "vendorscat";
    private static final String SCHEDULED_TRANSACTIONS = "scheduledtrans";
    private static final String JOURNALS = "journals";
    private static final String ACCOUNT_BALANCES = "accntbalances";
    private static final String META_ACCOUNT_BALANCE_UPDATE = "metabalanceupdt";

    private static final int TRANSACTIONS_ID = 1;
    private static final int TRANSACTIONS_ACCOUNT_ID = 2;
    private static final int TRANSACTIONS_CATEGORY_ID = 3;
    private static final int TRANSACTIONS_TAG_ID = 4;
    private static final int TRANSACTIONS_VENDOR_ID = 5;
    private static final int ACCOUNTS_ID = 10;
    private static final int CATEGORIES_ID = 20;
    private static final int TAGS_ID = 30;
    private static final int VENDORS_ID = 40;
    private static final int VENDORS_CATEGORY_ID = 50;
    private static final int SCHEDULED_TRANSACTIONS_ID = 60;
    private static final int JOURNALS_ID = 70;
    private static final int ACCOUNT_BALANCES_ID = 80;
    private static final int META_ACCOUNT_BALANCE_UPDATE_ID = 255;

    public static final Uri URI_TRANSACTIONS = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS);
    public static final Uri URI_TRANSACTIONS_ACCOUNT = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS_ACCOUNT);
    public static final Uri URI_TRANSACTIONS_CATEGORY = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS_CATEGORY);
    public static final Uri URI_TRANSACTIONS_TAG = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS_TAG);
    public static final Uri URI_TRANSACTIONS_VENDOR = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS_VENDOR);
    public static final Uri URI_ACCOUNTS = Uri.parse("content://" + PROVIDER_NAME + "/" + ACCOUNTS);
    public static final Uri URI_CATEGORIES = Uri.parse("content://" + PROVIDER_NAME + "/" + CATEGORIES);
    public static final Uri URI_TAGS = Uri.parse("content://" + PROVIDER_NAME + "/" + TAGS);
    public static final Uri URI_VENDORS = Uri.parse("content://" + PROVIDER_NAME + "/" + VENDORS);
    public static final Uri URI_VENDORS_CATEGORY = Uri.parse("content://" + PROVIDER_NAME + "/" + VENDORS_CATEGORY);
    public static final Uri URI_SCHEDULED_TRANSACTIONS = Uri.parse("content://" + PROVIDER_NAME + "/" + SCHEDULED_TRANSACTIONS);
    public static final Uri URI_JOURNALS = Uri.parse("content://" + PROVIDER_NAME + "/" + JOURNALS);
    public static final Uri URI_ACCOUNT_BALANCES = Uri.parse("content://" + PROVIDER_NAME + "/" + ACCOUNT_BALANCES);
    public static final Uri URI_META_ACCOUNT_BALANCE_UPDATE = Uri.parse("content://" + PROVIDER_NAME + "/" + META_ACCOUNT_BALANCE_UPDATE);

    static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, TRANSACTIONS, TRANSACTIONS_ID);
        uriMatcher.addURI(PROVIDER_NAME, TRANSACTIONS_ACCOUNT, TRANSACTIONS_ACCOUNT_ID);
        uriMatcher.addURI(PROVIDER_NAME, TRANSACTIONS_CATEGORY, TRANSACTIONS_CATEGORY_ID);
        uriMatcher.addURI(PROVIDER_NAME, TRANSACTIONS_TAG, TRANSACTIONS_TAG_ID);
        uriMatcher.addURI(PROVIDER_NAME, TRANSACTIONS_VENDOR, TRANSACTIONS_VENDOR_ID);
        uriMatcher.addURI(PROVIDER_NAME, ACCOUNTS, ACCOUNTS_ID);
        uriMatcher.addURI(PROVIDER_NAME, CATEGORIES, CATEGORIES_ID);
        uriMatcher.addURI(PROVIDER_NAME, TAGS, TAGS_ID);
        uriMatcher.addURI(PROVIDER_NAME, VENDORS, VENDORS_ID);
        uriMatcher.addURI(PROVIDER_NAME, VENDORS_CATEGORY, VENDORS_CATEGORY_ID);
        uriMatcher.addURI(PROVIDER_NAME, SCHEDULED_TRANSACTIONS, SCHEDULED_TRANSACTIONS_ID);
        uriMatcher.addURI(PROVIDER_NAME, JOURNALS, JOURNALS_ID);
        uriMatcher.addURI(PROVIDER_NAME, ACCOUNT_BALANCES, ACCOUNT_BALANCES_ID);
        uriMatcher.addURI(PROVIDER_NAME, META_ACCOUNT_BALANCE_UPDATE, META_ACCOUNT_BALANCE_UPDATE_ID);
    }

    @Override
    public boolean onCreate() {
        helper = new DBHelper(getContext(), DBHelper.DB_VERSION);
        db = helper.getWritableDatabase();
        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        String table0 = null;
        boolean notify = true;

        switch (uriMatcher.match(uri)) {
            case TRANSACTIONS_ID:
                table0 = DBHelper.TABLE_TRANSACTION_NAME;
                break;

            case ACCOUNTS_ID:
                table0 = DBHelper.TABLE_ACCOUNT_NAME;
                break;

            case CATEGORIES_ID:
                table0 = DBHelper.TABLE_CATEGORY_NAME;
                break;

            case TAGS_ID:
                table0 = DBHelper.TABLE_TAG_NAME;
                break;

            case VENDORS_ID:
                table0 = DBHelper.TABLE_VENDOR_NAME;
                break;

            case VENDORS_CATEGORY_ID:
                table0 = DBHelper.TABLE_VENDOR_CATEGORY_NAME;
                notify = false;
                break;

            case SCHEDULED_TRANSACTIONS_ID:
                table0 = DBHelper.TABLE_SCHEDULED_TRANSACTION_NAME;
                notify = false;
                break;

            case JOURNALS_ID:
                table0 = DBHelper.TABLE_JOURNAL_NAME;
                notify = false;
                break;

            case ACCOUNT_BALANCES_ID:
                table0 = DBHelper.TABLE_ACCOUNT_BALANCE_NAME;
                notify = false;
                break;
        }

        if (table0 != null) {
            boolean updated = false;

            if (updatingAccountBalance && uriMatcher.match(uri) == TRANSACTIONS_ID) {
                SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
                qb.setTables(DBHelper.TABLE_TRANSACTION_NAME);
                Cursor cursor = qb.query(db, new String[]{DBHelper.TABLE_COLUMN_STATE,
                        DBHelper.TABLE_COLUMN_TYPE,
                        DBHelper.TABLE_COLUMN_TIMESTAMP,
                        DBHelper.TABLE_COLUMN_ACCOUNT,
                        DBHelper.TABLE_COLUMN_ACCOUNT2,
                        DBHelper.TABLE_COLUMN_AMOUNT}, selection, selectionArgs, null, null, null);
                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();

                        Integer newState = values.getAsInteger(DBHelper.TABLE_COLUMN_STATE);

                        int oldState = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE));
                        int oldType = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                        double oldAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                        long oldAccount = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT));
                        long oldAccount2 = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT2));
                        long oldTimeStamp = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP));

                        count = db.update(table0, values, selection, selectionArgs);
                        updated = true;
                        boolean done = false;

                        if (newState != null) {
                            if (newState.intValue() != oldState) {
                                if (newState.intValue() == DBHelper.STATE_DELETED) {
                                    //delete
                                    values.put(DBHelper.TABLE_COLUMN_TYPE, oldType);
                                    values.put(DBHelper.TABLE_COLUMN_ACCOUNT, oldAccount);
                                    values.put(DBHelper.TABLE_COLUMN_ACCOUNT2, oldAccount2);
                                    values.put(DBHelper.TABLE_COLUMN_TIMESTAMP, oldTimeStamp);
                                    values.put(DBHelper.TABLE_COLUMN_AMOUNT, -oldAmount);
                                } else {
                                    //undelete: restore old values, unless request has them
                                    if (!values.containsKey(DBHelper.TABLE_COLUMN_TYPE))
                                        values.put(DBHelper.TABLE_COLUMN_TYPE, oldType);
                                    if (!values.containsKey(DBHelper.TABLE_COLUMN_ACCOUNT))
                                        values.put(DBHelper.TABLE_COLUMN_ACCOUNT, oldAccount);
                                    if (!values.containsKey(DBHelper.TABLE_COLUMN_ACCOUNT2))
                                        values.put(DBHelper.TABLE_COLUMN_ACCOUNT2, oldAccount2);
                                    if (!values.containsKey(DBHelper.TABLE_COLUMN_TIMESTAMP))
                                        values.put(DBHelper.TABLE_COLUMN_TIMESTAMP, oldTimeStamp);

                                    if (values.containsKey(DBHelper.TABLE_COLUMN_AMOUNT)) {
                                        if (oldAmount != values.getAsDouble(DBHelper.TABLE_COLUMN_AMOUNT))
                                            LLog.w(TAG, "UNEXPECTED: amount changed whiling deleting/undeleting");
                                    }
                                    values.put(DBHelper.TABLE_COLUMN_AMOUNT, oldAmount);
                                }
                                updateAccountBalance(values);
                                done = true;
                            }
                        }

                        if (!done) {
                            updateAccountBalance(values);
                            values.put(DBHelper.TABLE_COLUMN_TYPE, oldType);
                            values.put(DBHelper.TABLE_COLUMN_ACCOUNT, oldAccount);
                            values.put(DBHelper.TABLE_COLUMN_ACCOUNT2, oldAccount2);
                            values.put(DBHelper.TABLE_COLUMN_TIMESTAMP, oldTimeStamp);
                            values.put(DBHelper.TABLE_COLUMN_AMOUNT, -oldAmount);
                            updateAccountBalance(values);
                        }
                    }
                    cursor.close();
                }
            }

            if (!updated)
                count = db.update(table0, values, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);

            if (notify)
                notifyTransactionChange();
        }

        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table0 = null;
        boolean notify = true;
        switch (uriMatcher.match(uri)) {
            case TRANSACTIONS_ID:
                table0 = DBHelper.TABLE_TRANSACTION_NAME;
                break;

            case ACCOUNTS_ID:
                table0 = DBHelper.TABLE_ACCOUNT_NAME;
                break;

            case CATEGORIES_ID:
                table0 = DBHelper.TABLE_CATEGORY_NAME;
                break;

            case TAGS_ID:
                table0 = DBHelper.TABLE_TAG_NAME;
                break;

            case VENDORS_ID:
                table0 = DBHelper.TABLE_VENDOR_NAME;
                break;

            case VENDORS_CATEGORY_ID:
                table0 = DBHelper.TABLE_VENDOR_CATEGORY_NAME;
                notify = false;
                break;

            case SCHEDULED_TRANSACTIONS_ID:
                table0 = DBHelper.TABLE_SCHEDULED_TRANSACTION_NAME;
                notify = false;
                break;

            case JOURNALS_ID:
                table0 = DBHelper.TABLE_JOURNAL_NAME;
                notify = false;
                break;

            case ACCOUNT_BALANCES_ID:
                table0 = DBHelper.TABLE_ACCOUNT_BALANCE_NAME;
                notify = false;
                break;

            case META_ACCOUNT_BALANCE_UPDATE_ID:
                updatingAccountBalance = values.getAsBoolean("enable");
                return null;
        }

        if (table0 != null) {
            long row = db.insert(table0, "", values);
            if (row > 0) {
                Uri uri1 = ContentUris.withAppendedId(uri, row);
                getContext().getContentResolver().notifyChange(uri1, null);

                if (updatingAccountBalance && uriMatcher.match(uri) == TRANSACTIONS_ID) {
                    updateAccountBalance(values);
                }

                if (notify)
                    notifyTransactionChange();
                return uri1;
            }
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;

        /*
        //TODO:
        int count = 0;
        String table0 = null;
        boolean notify = true;
        switch (uriMatcher.match(uri)) {
            case TRANSACTIONS_ID:
                table0 = DBHelper.TABLE_TRANSACTION_NAME;
                break;

            case ACCOUNTS_ID:
                table0 = DBHelper.TABLE_ACCOUNT_NAME;
                break;

            case CATEGORIES_ID:
                table0 = DBHelper.TABLE_CATEGORY_NAME;
                break;

            case TAGS_ID:
                table0 = DBHelper.TABLE_TAG_NAME;
                break;

            case VENDORS_ID:
                table0 = DBHelper.TABLE_VENDOR_NAME;
                break;

            case VENDORS_CATEGORY_ID:
                table0 = DBHelper.TABLE_VENDOR_CATEGORY_NAME;
                notify = false;
                break;

            case SCHEDULED_TRANSACTIONS_ID:
                table0 = DBHelper.TABLE_SCHEDULED_TRANSACTION_NAME;
                notify = false;
                break;

            case JOURNALS_ID:
                table0 = DBHelper.TABLE_JOURNAL_NAME;
                notify = false;
                break;

            case ACCOUNT_BALANCES_ID:
                table0 = DBHelper.TABLE_ACCOUNT_BALANCE_NAME;
                notify = false;
                break;
        }

        if (table0 != null) {
            count = db.delete(table0, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);

            if (notify)
                notifyTransactionChange();
        }

        return count;
        */
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor ret = null;
        String tableName = null;
        String columnName = null;
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setProjectionMap(PROJECTION_MAP);

        switch (uriMatcher.match(uri)) {
            case TRANSACTIONS_ID:
                qb.setTables(DBHelper.TABLE_TRANSACTION_NAME);
                break;
            case TRANSACTIONS_ACCOUNT_ID:
                tableName = DBHelper.TABLE_ACCOUNT_NAME;
                columnName = DBHelper.TABLE_COLUMN_ACCOUNT;
                break;
            case TRANSACTIONS_CATEGORY_ID:
                tableName = DBHelper.TABLE_CATEGORY_NAME;
                columnName = DBHelper.TABLE_COLUMN_CATEGORY;
                break;
            case TRANSACTIONS_TAG_ID:
                tableName = DBHelper.TABLE_TAG_NAME;
                columnName = DBHelper.TABLE_COLUMN_TAG;
                break;
            case TRANSACTIONS_VENDOR_ID:
                tableName = DBHelper.TABLE_VENDOR_NAME;
                columnName = DBHelper.TABLE_COLUMN_VENDOR;
                break;

            case ACCOUNTS_ID:
                qb.setTables(DBHelper.TABLE_ACCOUNT_NAME);
                break;

            case CATEGORIES_ID:
                qb.setTables(DBHelper.TABLE_CATEGORY_NAME);
                break;

            case TAGS_ID:
                qb.setTables(DBHelper.TABLE_TAG_NAME);
                break;

            case VENDORS_ID:
                qb.setTables(DBHelper.TABLE_VENDOR_NAME);
                break;

            case VENDORS_CATEGORY_ID:
                qb.setTables(DBHelper.TABLE_VENDOR_CATEGORY_NAME);
                break;

            case SCHEDULED_TRANSACTIONS_ID:
                qb.setTables(DBHelper.TABLE_SCHEDULED_TRANSACTION_NAME);
                break;

            case JOURNALS_ID:
                qb.setTables(DBHelper.TABLE_JOURNAL_NAME);
                break;

            case ACCOUNT_BALANCES_ID:
                qb.setTables(DBHelper.TABLE_ACCOUNT_BALANCE_NAME);
                break;
        }

        if (uriMatcher.match(uri) != TRANSACTIONS_ID && tableName != null && columnName != null) {
            qb.setTables(DBHelper.TABLE_TRANSACTION_NAME + " AS a LEFT JOIN " + tableName + " AS b "
                    + "ON a." + columnName + " = b._id ");
        }

        ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        if (ret != null) ret.setNotificationUri(getContext().getContentResolver(), uri);
        return ret;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    private void notifyTransactionChange() {
        getContext().getContentResolver().notifyChange(URI_TRANSACTIONS_ACCOUNT, null);
        getContext().getContentResolver().notifyChange(URI_TRANSACTIONS_CATEGORY, null);
        getContext().getContentResolver().notifyChange(URI_TRANSACTIONS_TAG, null);
        getContext().getContentResolver().notifyChange(URI_TRANSACTIONS_VENDOR, null);
    }

    private void updateAccountBalance(long id, double amount, long timeStamp) {
        boolean exists = false;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        long dbEntryId = 0;

        try {
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(DBHelper.TABLE_ACCOUNT_BALANCE_NAME);
            Cursor csr = qb.query(db, null, DBHelper.TABLE_COLUMN_ACCOUNT + "=? AND "
                            + DBHelper.TABLE_COLUMN_YEAR + "=? AND "
                            + DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + id, "" + year, "" + DBHelper.STATE_ACTIVE}, null, null, null);
            LAccountBalance accountBalance = null;
            if (csr != null) {
                if (csr.getCount() > 0) {
                    csr.moveToFirst();
                    accountBalance = new LAccountBalance(id, year,
                            csr.getString(csr.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_BALANCE)));
                    dbEntryId = csr.getLong(0);
                    exists = true;
                }
                csr.close();
            }
            if (null == accountBalance) {
                accountBalance = new LAccountBalance(id, year, null);
            }

            accountBalance.modify(year, month, amount);

            ContentValues cv = new ContentValues();
            cv.put(DBHelper.TABLE_COLUMN_STATE, DBHelper.STATE_ACTIVE);
            cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, id);
            cv.put(DBHelper.TABLE_COLUMN_YEAR, year);
            cv.put(DBHelper.TABLE_COLUMN_BALANCE, accountBalance.getYearBalanceString(year));

            if (!exists) {
                db.insert(DBHelper.TABLE_ACCOUNT_BALANCE_NAME, "", cv);
            } else {
                db.update(DBHelper.TABLE_ACCOUNT_BALANCE_NAME, cv, "_id=?", new String[]{"" + dbEntryId});
            }
        } catch (Exception e) {
            LLog.e(TAG, "unable to update account balance: " + e.getMessage());
        }
    }

    //trans must contain COLUMN_TYPE/AMOUNT/ACCOUNT/ACCOUNT2/TIMESTAMP
    private void updateAccountBalance(ContentValues trans) {
        double amount = trans.getAsDouble(DBHelper.TABLE_COLUMN_AMOUNT);
        long accountId = trans.getAsLong(DBHelper.TABLE_COLUMN_ACCOUNT);
        long account2Id = trans.getAsLong(DBHelper.TABLE_COLUMN_ACCOUNT2);
        long timeStamp = trans.getAsLong(DBHelper.TABLE_COLUMN_TIMESTAMP);

        switch (trans.getAsInteger(DBHelper.TABLE_COLUMN_TYPE)) {
            case LTransaction.TRANSACTION_TYPE_TRANSFER_COPY:
                updateAccountBalance(accountId, amount, timeStamp);
                return;
            case LTransaction.TRANSACTION_TYPE_TRANSFER:
                updateAccountBalance(accountId, -amount, timeStamp);
                break;
            case LTransaction.TRANSACTION_TYPE_EXPENSE:
                updateAccountBalance(accountId, -amount, timeStamp);
                break;
            case LTransaction.TRANSACTION_TYPE_INCOME:
                updateAccountBalance(accountId, amount, timeStamp);
                break;
        }
        getContext().getContentResolver().notifyChange(URI_ACCOUNT_BALANCES, null);
    }
}
