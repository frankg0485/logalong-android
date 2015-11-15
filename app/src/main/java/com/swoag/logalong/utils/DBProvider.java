package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.sql.SQLException;
import java.util.HashMap;

public class DBProvider extends ContentProvider {
    private static final String PROVIDER_NAME = "com.swoag.logalong.utils.DBProvider";
    private static DBHelper helper;
    private SQLiteDatabase db;

    private static HashMap<String, String> PROJECTION_MAP;

    private static final String TRANSACTIONS = "trans";
    private static final String TRANSACTIONS_ACCOUNT = "trans/account";
    private static final String TRANSACTIONS_CATEGORY = "trans/category";
    private static final String TRANSACTIONS_TAG = "trans/tag";
    private static final String TRANSACTIONS_VENDOR = "trans/vendor";
    private static final String ACCOUNTS = "accounts";
    private static final String CATEGORIES = "categories";
    private static final String TAGS = "tags";
    private static final String VENDORS = "vendors";

    private static final int TRANSACTIONS_ID = 1;
    private static final int TRANSACTIONS_ACCOUNT_ID = 2;
    private static final int TRANSACTIONS_CATEGORY_ID = 3;
    private static final int TRANSACTIONS_TAG_ID = 4;
    private static final int TRANSACTIONS_VENDOR_ID = 5;
    private static final int ACCOUNTS_ID = 10;
    private static final int CATEGORIES_ID = 20;
    private static final int TAGS_ID = 30;
    private static final int VENDORS_ID = 40;

    public static final Uri URI_TRANSACTIONS = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS);
    public static final Uri URI_TRANSACTIONS_ACCOUNT = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS_ACCOUNT);
    public static final Uri URI_TRANSACTIONS_CATEGORY = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS_CATEGORY);
    public static final Uri URI_TRANSACTIONS_TAG = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS_TAG);
    public static final Uri URI_TRANSACTIONS_VENDOR = Uri.parse("content://" + PROVIDER_NAME + "/" + TRANSACTIONS_VENDOR);
    public static final Uri URI_ACCOUNTS = Uri.parse("content://" + PROVIDER_NAME + "/" + ACCOUNTS);
    public static final Uri URI_CATEGORIES = Uri.parse("content://" + PROVIDER_NAME + "/" + CATEGORIES);
    public static final Uri URI_TAGS = Uri.parse("content://" + PROVIDER_NAME + "/" + TAGS);
    public static final Uri URI_VENDORS = Uri.parse("content://" + PROVIDER_NAME + "/" + VENDORS);

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
        }

        if (table0 != null) {
            count = db.update(table0, values, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);

            /*if (uriMatcher.match(uri) == TRANSACTIONS_ID)*/
            notifyTransactionChange();
        }

        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table0 = null;
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
        }

        if (table0 != null) {
            long row = db.insert(table0, "", values);
            if (row > 0) {
                Uri uri1 = ContentUris.withAppendedId(uri, row);
                getContext().getContentResolver().notifyChange(uri1, null);

                /*if (uriMatcher.match(uri) == TRANSACTIONS_ID)*/
                notifyTransactionChange();
                return uri1;
            }
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        String table0 = null;
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
        }

        if (table0 != null) {
            count = db.delete(table0, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);

            /*if (uriMatcher.match(uri) == TRANSACTIONS_ID)*/
            notifyTransactionChange();
        }

        return count;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor ret = null;
        String tableName = null;
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setProjectionMap(PROJECTION_MAP);

        switch (uriMatcher.match(uri)) {
            case TRANSACTIONS_ID:
                qb.setTables(DBHelper.TABLE_TRANSACTION_NAME);
                break;
            case TRANSACTIONS_ACCOUNT_ID:
                tableName = DBHelper.TABLE_ACCOUNT_NAME;
                break;
            case TRANSACTIONS_CATEGORY_ID:
                tableName = DBHelper.TABLE_CATEGORY_NAME;
                break;
            case TRANSACTIONS_TAG_ID:
                tableName = DBHelper.TABLE_TAG_NAME;
                break;
            case TRANSACTIONS_VENDOR_ID:
                tableName = DBHelper.TABLE_VENDOR_NAME;
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
        }

        if (uriMatcher.match(uri) != TRANSACTIONS_ID && tableName != null) {
            qb.setTables(DBHelper.TABLE_TRANSACTION_NAME + " AS a LEFT JOIN " + tableName + " AS b "
                    + "ON a." + DBHelper.TABLE_COLUMN_ACCOUNT + " = b._id ");
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
}
