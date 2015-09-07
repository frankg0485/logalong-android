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
    public static final int DB_VERSION = 4;

    public static final String TABLE_COLUMN_STATE = "State";
    public static final String TABLE_COLUMN_NAME = "Name";
    public static final String TABLE_COLUMN_ACCOUNT = "Account";
    public static final String TABLE_COLUMN_CATEGORY = "Category";
    public static final String TABLE_COLUMN_VENDOR = "Vendor";
    public static final String TABLE_COLUMN_TAG = "Tag";

    public static final String TABLE_COLUMN_TYPE = "Type";
    public static final String TABLE_COLUMN_MADEBY = "MadeBy";
    public static final String TABLE_COLUMN_TIMESTAMP = "TimeStmp";
    public static final String TABLE_COLUMN_AMOUNT = "Amount";
    public static final String TABLE_COLUMN_NOTE = "Note";


    public static final String TABLE_reserved1 = "TabReserved1";
    public static final String TABLE_reserved2 = "TabReserved2";
    public static final String TABLE_reserved3 = "TabReserved3";
    public static final String TABLE_reserved4 = "TabReserved4";

    public static final String TABLE_COLUMN_reservedReal1 = "ReservedReal1";
    public static final String TABLE_COLUMN_reservedBlob1 = "ReservedBlob1";

    public static final String TABLE_COLUMN_reservedInteger1 = "ReservedInt1";
    public static final String TABLE_COLUMN_reservedInteger2 = "ReservedInt2";
    public static final String TABLE_COLUMN_reservedInteger3 = "ReservedInt3";
    public static final String TABLE_COLUMN_reservedInteger4 = "ReservedInt4";
    public static final String TABLE_COLUMN_reservedInteger5 = "ReservedInt5";
    public static final String TABLE_COLUMN_reservedInteger6 = "ReservedInt6";
    public static final String TABLE_COLUMN_reservedInteger7 = "ReservedInt7";
    public static final String TABLE_COLUMN_reservedInteger8 = "ReservedInt8";
    public static final String TABLE_COLUMN_reservedText1 = "ReservedText1";
    public static final String TABLE_COLUMN_reservedText2 = "ReservedText2";
    public static final String TABLE_COLUMN_reservedText3 = "ReservedText3";
    public static final String TABLE_COLUMN_reservedText4 = "ReservedText4";
    public static final String TABLE_COLUMN_reservedText5 = "ReservedText5";
    public static final String TABLE_COLUMN_reservedText6 = "ReservedText6";
    public static final String TABLE_COLUMN_reservedText7 = "ReservedText7";
    public static final String TABLE_COLUMN_reservedText8 = "ReservedText8";

    public static final String TABLE_ACCOUNT_NAME = "LAccount";
    public static final String TABLE_ACCOUNT_COLUMN_ICON = "Icon";
    public static final String TABLE_ACCOUNT_COLUMN_NUMBER = "Number";
    public static final String TABLE_ACCOUNT_COLUMN_INFO = "Info";

    public static final String TABLE_ACCOUNT_BALANCE_NAME = "LAccountBalance";
    public static final String TABLE_COLUMN_YEAR = "Year";
    public static final String TABLE_COLUMN_BALANCE = "Balance";

    public static final String TABLE_TRANSACTION_NAME = "LTrans";

    public static final String TABLE_CATEGORY_NAME = "LCategory";
    public static final String TABLE_TAG_NAME = "LTag";
    public static final String TABLE_VENDOR_NAME = "LVendor";
    public static final String TABLE_VENDOR_CATEGORY_NAME = "LVendorCategory";

    private static final String CREATE_TABLE_ACCOUNT = "CREATE TABLE " + TABLE_ACCOUNT_NAME +
            "( _id integer primary key autoincrement," +
            TABLE_COLUMN_NAME + " TEXT," +
            TABLE_ACCOUNT_COLUMN_INFO + " TEXT," +
            TABLE_ACCOUNT_COLUMN_NUMBER + " INTEGER," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_ACCOUNT_COLUMN_ICON + " BLOB," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedInteger2 + " INTEGER," +
            TABLE_COLUMN_reservedInteger3 + " INTEGER," +
            TABLE_COLUMN_reservedInteger4 + " INTEGER," +
            TABLE_COLUMN_reservedText1 + " TEXT," +
            TABLE_COLUMN_reservedText2 + " TEXT," +
            TABLE_COLUMN_reservedText3 + " TEXT," +
            TABLE_COLUMN_reservedText4 + " TEXT" +
            ");";

    private static final String CREATE_TABLE_ACCOUNT_BALANCE = "CREATE TABLE " + TABLE_ACCOUNT_BALANCE_NAME +
            "( _id integer primary key autoincrement," +
            TABLE_COLUMN_ACCOUNT + " INTEGER," +
            TABLE_COLUMN_YEAR + " INTEGER," +
            TABLE_COLUMN_BALANCE + " TEXT" +
            ");";

    private static final String CREATE_TABLE_TRANSACTION = "CREATE TABLE " + TABLE_TRANSACTION_NAME +
            "( _id integer primary key autoincrement," +
            TABLE_COLUMN_AMOUNT + " REAL," +
            TABLE_COLUMN_CATEGORY + " INTEGER," +
            TABLE_COLUMN_ACCOUNT + " INTEGER," +
            TABLE_COLUMN_TAG + " INTEGER," +
            TABLE_COLUMN_VENDOR + " INTEGER," +
            TABLE_COLUMN_TIMESTAMP + " INTEGER," +
            TABLE_COLUMN_TYPE + " INTEGER," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_MADEBY + " TEXT," +
            TABLE_COLUMN_NOTE + " TEXT," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedInteger2 + " INTEGER," +
            TABLE_COLUMN_reservedInteger3 + " INTEGER," +
            TABLE_COLUMN_reservedInteger4 + " INTEGER," +
            TABLE_COLUMN_reservedText1 + " TEXT," +
            TABLE_COLUMN_reservedText2 + " TEXT," +
            TABLE_COLUMN_reservedText3 + " TEXT," +
            TABLE_COLUMN_reservedText4 + " TEXT" +
            ");";

    private static final String CREATE_TABLE_CATEGORY = "CREATE TABLE " + TABLE_CATEGORY_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_NAME + " TEXT," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedText1 + " TEXT" +
            ");";

    private static final String CREATE_TABLE_TAG = "CREATE TABLE " + TABLE_TAG_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_NAME + " TEXT," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedText1 + " TEXT" +
            ");";

    private static final String CREATE_TABLE_VENDOR = "CREATE TABLE " + TABLE_VENDOR_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_NAME + " TEXT," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedText1 + " TEXT" +
            ");";

    private static final String CREATE_TABLE_VENDOR_CATEGORY = "CREATE TABLE " + TABLE_VENDOR_CATEGORY_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_STATE + " INTEGER," +
            TABLE_COLUMN_VENDOR + " INTEGER," +
            TABLE_COLUMN_CATEGORY + " INTEGER" +
            ");";

    private static final String CREATE_TABLE_RESERVED = "" +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedInteger2 + " INTEGER," +
            TABLE_COLUMN_reservedInteger3 + " INTEGER," +
            TABLE_COLUMN_reservedInteger4 + " INTEGER," +
            TABLE_COLUMN_reservedInteger5 + " INTEGER," +
            TABLE_COLUMN_reservedInteger6 + " INTEGER," +
            TABLE_COLUMN_reservedInteger7 + " INTEGER," +
            TABLE_COLUMN_reservedInteger8 + " INTEGER," +
            TABLE_COLUMN_reservedText1 + " TEXT," +
            TABLE_COLUMN_reservedText2 + " TEXT," +
            TABLE_COLUMN_reservedText3 + " TEXT," +
            TABLE_COLUMN_reservedText4 + " TEXT," +
            TABLE_COLUMN_reservedText5 + " TEXT," +
            TABLE_COLUMN_reservedText6 + " TEXT," +
            TABLE_COLUMN_reservedText7 + " TEXT," +
            TABLE_COLUMN_reservedText8 + " TEXT" +
            TABLE_COLUMN_reservedReal1 + " REAL," +
            TABLE_COLUMN_reservedBlob1 + " BLOB" +
            ");";

    public DBHelper(Context context, int version) {
        super(context, DATABASE_NAME, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ACCOUNT);
        db.execSQL(CREATE_TABLE_ACCOUNT_BALANCE);
        db.execSQL(CREATE_TABLE_CATEGORY);
        db.execSQL(CREATE_TABLE_TAG);
        db.execSQL(CREATE_TABLE_VENDOR);
        db.execSQL(CREATE_TABLE_VENDOR_CATEGORY);
        db.execSQL(CREATE_TABLE_TRANSACTION);
        db.execSQL("CREATE TABLE " + TABLE_reserved1 + CREATE_TABLE_RESERVED);
        db.execSQL("CREATE TABLE " + TABLE_reserved2 + CREATE_TABLE_RESERVED);
        db.execSQL("CREATE TABLE " + TABLE_reserved3 + CREATE_TABLE_RESERVED);
        db.execSQL("CREATE TABLE " + TABLE_reserved4 + CREATE_TABLE_RESERVED);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LLog.d(TAG, "upgrading Database from/to version: " + oldVersion + "/" + newVersion);
        if (oldVersion == 4 && newVersion == 5) {
            db.execSQL("DROP TABLE IF EXISTS LLog");

            /*
            db.execSQL(CREATE_TABLE_TRANSACTION);
            Cursor cur = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_LOG_NAME
                            + " WHERE State=? ORDER BY " + DBHelper.TABLE_LOG_COLUMN_TIMESTAMP + " ASC",
                    new String[]{"" + LTransaction.LOG_STATE_ACTIVE});
            if (cur != null && cur.getCount() > 0) {
                cur.moveToFirst();
                do  {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.TABLE_COLUMN_TYPE, cur.getInt(cur.getColumnIndexOrThrow(TABLE_LOG_COLUMN_TYPE)));
                    cv.put(DBHelper.TABLE_COLUMN_STATE, STATE_ACTIVE);
                    cv.put(DBHelper.TABLE_COLUMN_CATEGORY, cur.getLong(cur.getColumnIndexOrThrow(TABLE_COLUMN_CATEGORY)));
                    cv.put(DBHelper.TABLE_COLUMN_ACCOUNT, cur.getLong(cur.getColumnIndexOrThrow(TABLE_LOG_COLUMN_TO)));
                    cv.put(DBHelper.TABLE_COLUMN_AMOUNT, cur.getDouble(cur.getColumnIndexOrThrow(TABLE_LOG_COLUMN_VALUE)));
                    cv.put(DBHelper.TABLE_COLUMN_TIMESTAMP, cur.getLong(cur.getColumnIndexOrThrow(TABLE_LOG_COLUMN_TIMESTAMP)));
                    cv.put(DBHelper.TABLE_COLUMN_VENDOR, cur.getLong(cur.getColumnIndexOrThrow(TABLE_COLUMN_VENDOR)));

                    db.insert(TABLE_TRANSACTION_NAME, "", cv);
                } while(cur.moveToNext());
            }
            */
        }
    }
}
