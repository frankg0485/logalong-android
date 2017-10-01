package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String TAG = DBHelper.class.getSimpleName();

    public static final int STATE_ACTIVE = 10;
    public static final int STATE_DELETED = 20;

    public static final String DATABASE_NAME = "LogAlongDB";
    public static final int DB_VERSION = 3;

    public static final String TABLE_COLUMN_ACCOUNT = "Account";
    public static final String TABLE_COLUMN_ACCOUNT2 = "Account2";
    public static final String TABLE_COLUMN_AMOUNT = "Amount";
    public static final String TABLE_COLUMN_BALANCE = "Balance";
    public static final String TABLE_COLUMN_CATEGORY = "Category";
    public static final String TABLE_COLUMN_ENABLED = "Enabled";
    public static final String TABLE_COLUMN_ICON = "Icon";
    public static final String TABLE_COLUMN_SHARE = "Share";
    public static final String TABLE_COLUMN_MADEBY = "MadeBy";
    public static final String TABLE_COLUMN_CHANGEBY = "ChangeBy";
    public static final String TABLE_COLUMN_NAME = "Name";
    public static final String TABLE_COLUMN_NUMBER = "Number";
    public static final String TABLE_COLUMN_NOTE = "Note";
    public static final String TABLE_COLUMN_RECORD = "Record";
    public static final String TABLE_COLUMN_REPEAT_INTERVAL = "RptInterval";
    public static final String TABLE_COLUMN_REPEAT_UNIT = "RptUnit";
    public static final String TABLE_COLUMN_REPEAT_COUNT = "RptCount";
    public static final String TABLE_COLUMN_RID = "Rid";
    public static final String TABLE_COLUMN_GID = "Gid";
    public static final String TABLE_COLUMN_SCHEDULE_TIMESTAMP = "SchTimeStmp";
    public static final String TABLE_COLUMN_SHOW_BALANCE = "ShowBalance";
    public static final String TABLE_COLUMN_STATE = "State";
    public static final String TABLE_COLUMN_TAG = "Tag";
    public static final String TABLE_COLUMN_TIMESTAMP = "TimeStmp";
    public static final String TABLE_COLUMN_TIMESTAMP_CREATE = "TimeStmpCreate";
    public static final String TABLE_COLUMN_TIMESTAMP_LAST_CHANGE = "TimeStmpLast";
    public static final String TABLE_COLUMN_TO_USER = "ToUser";
    public static final String TABLE_COLUMN_TYPE = "Type";
    public static final String TABLE_COLUMN_VENDOR = "Vendor";
    public static final String TABLE_COLUMN_YEAR = "Year";

    public static final String TABLE_ACCOUNT_NAME = "LAccount";
    public static final String TABLE_ACCOUNT_BALANCE_NAME = "LAccountBalance";
    public static final String TABLE_TRANSACTION_NAME = "LTrans";
    public static final String TABLE_SCHEDULED_TRANSACTION_NAME = "LScheduledTrans";
    public static final String TABLE_CATEGORY_NAME = "LCategory";
    public static final String TABLE_TAG_NAME = "LTag";
    public static final String TABLE_VENDOR_NAME = "LVendor";
    public static final String TABLE_VENDOR_CATEGORY_NAME = "LVendorCategory";
    public static final String TABLE_JOURNAL_NAME = "LJournal";

    private static final String TABLE_TRANSACTION_ROWS =
            TABLE_COLUMN_AMOUNT + " REAL," +
                    TABLE_COLUMN_CATEGORY + " INTEGER," +
                    TABLE_COLUMN_ACCOUNT + " INTEGER," +
                    TABLE_COLUMN_ACCOUNT2 + " INTEGER," +
                    TABLE_COLUMN_TAG + " INTEGER," +
                    TABLE_COLUMN_VENDOR + " INTEGER," +
                    TABLE_COLUMN_TIMESTAMP + " INTEGER," +
                    TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + " INTEGER," +
                    TABLE_COLUMN_TYPE + " INTEGER," +
                    TABLE_COLUMN_STATE + " INTEGER," +
                    TABLE_COLUMN_MADEBY + " INTEGER," +
                    TABLE_COLUMN_RID + " INTEGER," +
                    TABLE_COLUMN_NOTE + " TEXT," +
                    TABLE_COLUMN_ICON + " BLOB," +
                    TABLE_COLUMN_CHANGEBY + " INTEGER," +
                    TABLE_COLUMN_TIMESTAMP_CREATE + " INTEGER," +
                    TABLE_COLUMN_GID + " INTEGER";

    //transaction table: VENDOR carries to_account for transfer.
    private static final String CREATE_TABLE_TRANSACTION = "CREATE TABLE " + TABLE_TRANSACTION_NAME +
            "( _id integer primary key autoincrement," + TABLE_TRANSACTION_ROWS + ");";

    private static final String CREATE_TABLE_SCHEDULED_TRANSACTION = "CREATE TABLE " + TABLE_SCHEDULED_TRANSACTION_NAME +
            "( _id integer primary key autoincrement," +
            TABLE_COLUMN_REPEAT_INTERVAL + " INTEGER," +
            TABLE_COLUMN_REPEAT_UNIT + " INTEGER," +
            TABLE_COLUMN_REPEAT_COUNT + " INTEGER," +
            TABLE_COLUMN_SCHEDULE_TIMESTAMP + " INTEGER," +
            TABLE_COLUMN_ENABLED + " INTEGER,"
            // plus TABLE_TRANSACTION columns
            + TABLE_TRANSACTION_ROWS + ");";


    private static final String CREATE_TABLE_ACCOUNT_BALANCE = "CREATE TABLE " + TABLE_ACCOUNT_BALANCE_NAME +
            "( _id integer primary key autoincrement," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_ACCOUNT + " INTEGER," +
            TABLE_COLUMN_YEAR + " INTEGER," +
            TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + " INTEGER," +
            TABLE_COLUMN_BALANCE + " TEXT" +
            ");";

    private static final String CREATE_TABLE_ACCOUNT = "CREATE TABLE " + TABLE_ACCOUNT_NAME +
            "( _id integer primary key autoincrement," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_NAME + " TEXT," +
            TABLE_COLUMN_SHARE + " TEXT," +
            TABLE_COLUMN_NUMBER + " INTEGER," +
            TABLE_COLUMN_RID + " TEXT," +
            TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + " INTEGER," +
            TABLE_COLUMN_ICON + " BLOB," +
            TABLE_COLUMN_GID + " INTEGER," +
            TABLE_COLUMN_SHOW_BALANCE + " INTEGER" +
            ");";

    private static final String CREATE_TABLE_CATEGORY = "CREATE TABLE " + TABLE_CATEGORY_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_NAME + " TEXT," +
            TABLE_COLUMN_RID + " TEXT," +
            TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + " INTEGER," +
            TABLE_COLUMN_ICON + " BLOB," +
            TABLE_COLUMN_GID + " INTEGER" +
            ");";

    private static final String CREATE_TABLE_TAG = "CREATE TABLE " + TABLE_TAG_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_NAME + " TEXT," +
            TABLE_COLUMN_RID + " TEXT," +
            TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + " INTEGER," +
            TABLE_COLUMN_ICON + " BLOB," +
            TABLE_COLUMN_GID + " INTEGER" +
            ");";

    private static final String CREATE_TABLE_VENDOR = "CREATE TABLE " + TABLE_VENDOR_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_TYPE + " INTEGER," +
            TABLE_COLUMN_NAME + " TEXT," +
            TABLE_COLUMN_RID + " TEXT," +
            TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + " INTEGER," +
            TABLE_COLUMN_ICON + " BLOB," +
            TABLE_COLUMN_GID + " INTEGER" +
            ");";

    private static final String CREATE_TABLE_VENDOR_CATEGORY = "CREATE TABLE " + TABLE_VENDOR_CATEGORY_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_VENDOR + " INTEGER," +
            TABLE_COLUMN_CATEGORY + " INTEGER" +
            ");";

    private static final String CREATE_TABLE_JOURNAL = "CREATE TABLE " + TABLE_JOURNAL_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_TO_USER + " INTEGER," +
            TABLE_COLUMN_RECORD + " TEXT" +
            ");";

    public DBHelper(Context context, int version) {
        super(context, DATABASE_NAME, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ACCOUNT);
        db.execSQL(CREATE_TABLE_ACCOUNT_BALANCE);
        db.execSQL(CREATE_TABLE_TRANSACTION);
        db.execSQL(CREATE_TABLE_SCHEDULED_TRANSACTION);
        db.execSQL(CREATE_TABLE_CATEGORY);
        db.execSQL(CREATE_TABLE_TAG);
        db.execSQL(CREATE_TABLE_VENDOR);
        db.execSQL(CREATE_TABLE_VENDOR_CATEGORY);
        db.execSQL(CREATE_TABLE_JOURNAL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //LLog.d(TAG, "upgrading Database from/to version: " + oldVersion + "/" + newVersion);
        /*if (oldVersion == 1 && newVersion == 2)*/ {
            String sql = "ALTER TABLE " + TABLE_ACCOUNT_NAME +
                    " ADD COLUMN " + TABLE_COLUMN_GID + " INTEGER;" +
                    " ADD COLUMN " + TABLE_COLUMN_SHOW_BALANCE + " INTEGER;" +
                    "ALTER TABLE " + TABLE_CATEGORY_NAME +
                    " ADD COLUMN " + TABLE_COLUMN_GID + " INTEGER; " +
                    "ALTER TABLE " + TABLE_TAG_NAME +
                    " ADD COLUMN " + TABLE_COLUMN_GID + " INTEGER; " +
                    "ALTER TABLE " + TABLE_VENDOR_NAME +
                    " ADD COLUMN " + TABLE_COLUMN_GID + " INTEGER; " +
                    "ALTER TABLE " + TABLE_TRANSACTION_NAME +
                    " ADD COLUMN " + TABLE_COLUMN_CHANGEBY + " INTEGER;" +
                    " ADD COLUMN " + TABLE_COLUMN_TIMESTAMP_CREATE +  " INTEGER;" +
                    " ADD COLUMN " + TABLE_COLUMN_GID + " INTEGER; " +
                    "ALTER TABLE " + TABLE_SCHEDULED_TRANSACTION_NAME +
                    " ADD COLUMN " + TABLE_COLUMN_GID + " INTEGER;" +
                    " ADD COLUMN " + TABLE_COLUMN_ENABLED + "INTEGER";
            db.execSQL(sql);
        }
    }
}
