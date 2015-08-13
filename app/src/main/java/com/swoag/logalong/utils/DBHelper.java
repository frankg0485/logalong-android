package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    //private static final String TAG = DBHelper.class.getSimpleName();

    public static final String DATABASE_NAME = "YaventDB";
    public static final int DB_VERSION = 1;

    public static final String TABLE_reserved1 = "TabReserved1";
    public static final String TABLE_reserved2 = "TabReserved2";
    public static final String TABLE_reserved3 = "TabReserved3";
    public static final String TABLE_reserved4 = "TabReserved4";
    public static final String TABLE_COLUMN_reservedLong1 = "ReservedLong1";
    public static final String TABLE_COLUMN_reservedLong2 = "ReservedLong2";
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

    public static final String TABLE_CONTACTS_NAME = "YContacts";
    public static final String TABLE_CONTACTS_COLUMN_ID = "Cid";
    public static final String TABLE_CONTACTS_COLUMN_STATUS = "Status";
    public static final String TABLE_CONTACTS_COLUMN_REAL_NAME = "RealName";
    public static final String TABLE_CONTACTS_COLUMN_NAME = "Name";
    public static final String TABLE_CONTACTS_COLUMN_NUMBER = "Number";
    public static final String TABLE_CONTACTS_COLUMN_EMAIL = "Email";
    public static final String TABLE_CONTACTS_COLUMN_ICON = "Icon";

    public static final String TABLE_YAVENTS_NAME = "Yavents";
    public static final String TABLE_YAVENTS_COLUMN_FLAGS = "Flags";
    public static final String TABLE_YAVENTS_COLUMN_TYPE = "Type";
    public static final String TABLE_YAVENTS_COLUMN_GROUP_ID = "GroupId";
    public static final String TABLE_YAVENTS_COLUMN_CONTACTS = "Contacts";
    public static final String TABLE_YAVENTS_COLUMN_NAME = "Name";
    public static final String TABLE_YAVENTS_COLUMN_DRAFT_MSG = "DrafgMsg";

    public static final String TABLE_YAVENTS_LOG_NAME = "YaventsLog";
    public static final String TABLE_YAVENTS_LOG_COLUMN_YAVENT_ID = "YaventId";
    public static final String TABLE_YAVENTS_LOG_COLUMN_TYPE = "Type";
    public static final String TABLE_YAVENTS_LOG_COLUMN_STATE = "State";
    public static final String TABLE_YAVENTS_LOG_COLUMN_TIMES = "TimeS";
    public static final String TABLE_YAVENTS_LOG_COLUMN_FROM = "FromUserId";
    public static final String TABLE_YAVENTS_LOG_COLUMN_MSG = "Msg";
    public static final String TABLE_YAVENTS_LOG_COLUMN_CACHE_IDX = "CacheIdx";

    public static final String TABLE_DISCOVER_NAME = "YDiscover";

    public static final String TABLE_QUICKIES_NAME = "YQuickies";
    public static final String TABLE_QUICKIES_COLUMN_TYPE = "Type";
    public static final String TABLE_QUICKIES_COLUMN_MSG = "Msg";

    private static final String CREATE_TABLE_CONTACTS = "CREATE TABLE " + TABLE_CONTACTS_NAME +
            "( _id integer primary key autoincrement," +
            TABLE_CONTACTS_COLUMN_ID + " INTEGER," +
            TABLE_CONTACTS_COLUMN_STATUS + " INTEGER," +
            TABLE_CONTACTS_COLUMN_NUMBER + " TEXT, " +
            TABLE_CONTACTS_COLUMN_REAL_NAME + " TEXT," +
            TABLE_CONTACTS_COLUMN_NAME + " TEXT," +
            TABLE_CONTACTS_COLUMN_EMAIL + " TEXT," +
            TABLE_CONTACTS_COLUMN_ICON + " BLOB," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedInteger2 + " INTEGER," +
            TABLE_COLUMN_reservedInteger3 + " INTEGER," +
            TABLE_COLUMN_reservedInteger4 + " INTEGER," +
            TABLE_COLUMN_reservedLong1 + " LONG," +
            TABLE_COLUMN_reservedLong2 + " LONG," +
            TABLE_COLUMN_reservedText1 + " TEXT," +
            TABLE_COLUMN_reservedText2 + " TEXT," +
            TABLE_COLUMN_reservedText3 + " TEXT," +
            TABLE_COLUMN_reservedText4 + " TEXT" +
            ");" ;

    private static final String CREATE_TABLE_QUICKIES = "CREATE TABLE " + TABLE_QUICKIES_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_QUICKIES_COLUMN_TYPE + " INTEGER," +
            TABLE_QUICKIES_COLUMN_MSG + " TEXT" +
            ");" ;

    private static final String CREATE_TABLE_DISCOVER = "CREATE TABLE " + TABLE_DISCOVER_NAME +
            "( _id integer primary key autoincrement," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedInteger2 + " INTEGER," +
            TABLE_COLUMN_reservedInteger3 + " INTEGER," +
            TABLE_COLUMN_reservedInteger4 + " INTEGER," +
            TABLE_COLUMN_reservedLong1 + " LONG," +
            TABLE_COLUMN_reservedLong2 + " LONG," +
            TABLE_COLUMN_reservedText1 + " TEXT," +
            TABLE_COLUMN_reservedText2 + " TEXT," +
            TABLE_COLUMN_reservedText3 + " TEXT," +
            TABLE_COLUMN_reservedText4 + " TEXT" +
            ");" ;

    private static final String CREATE_TABLE_YAVENTS = "CREATE TABLE " + TABLE_YAVENTS_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_YAVENTS_COLUMN_TYPE + " INTEGER," +
            TABLE_YAVENTS_COLUMN_FLAGS + " INTEGER," +
            TABLE_YAVENTS_COLUMN_GROUP_ID + " INTEGER," +
            TABLE_YAVENTS_COLUMN_CONTACTS + " TEXT," +
            TABLE_YAVENTS_COLUMN_NAME + " TEXT," +
            TABLE_YAVENTS_COLUMN_DRAFT_MSG + " TEXT," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedInteger2 + " INTEGER," +
            TABLE_COLUMN_reservedInteger3 + " INTEGER," +
            TABLE_COLUMN_reservedInteger4 + " INTEGER," +
            TABLE_COLUMN_reservedLong1 + " LONG," +
            TABLE_COLUMN_reservedLong2 + " LONG," +
            TABLE_COLUMN_reservedText1 + " TEXT," +
            TABLE_COLUMN_reservedText2 + " TEXT," +
            TABLE_COLUMN_reservedText3 + " TEXT," +
            TABLE_COLUMN_reservedText4 + " TEXT" +
            ");";

    private static final String CREATE_TABLE_YAVENTS_LOG = "CREATE TABLE " + TABLE_YAVENTS_LOG_NAME +
            "( _id integer primary key autoincrement ," +
            TABLE_YAVENTS_LOG_COLUMN_YAVENT_ID + " INTEGER," +
            TABLE_YAVENTS_LOG_COLUMN_TYPE + " INTEGER," +
            TABLE_YAVENTS_LOG_COLUMN_STATE + " INTEGER," +
            TABLE_YAVENTS_LOG_COLUMN_FROM + " INTEGER," +
            TABLE_YAVENTS_LOG_COLUMN_TIMES + " INTEGER," +
            TABLE_YAVENTS_LOG_COLUMN_MSG + " TEXT," +
            TABLE_YAVENTS_LOG_COLUMN_CACHE_IDX + " INTEGER," +
            TABLE_COLUMN_reservedInteger1 + " INTEGER," +
            TABLE_COLUMN_reservedInteger2 + " INTEGER," +
            TABLE_COLUMN_reservedInteger3 + " INTEGER," +
            TABLE_COLUMN_reservedInteger4 + " INTEGER," +
            TABLE_COLUMN_reservedLong1 + " LONG," +
            TABLE_COLUMN_reservedLong2 + " LONG," +
            TABLE_COLUMN_reservedText1 + " TEXT," +
            TABLE_COLUMN_reservedText2 + " TEXT," +
            TABLE_COLUMN_reservedText3 + " TEXT," +
            TABLE_COLUMN_reservedText4 + " TEXT" +
            ");";

    private static final String CREATE_TABLE_RESERVED = "" +
            "( _id integer primary key autoincrement ," +
            TABLE_COLUMN_reservedLong1 + " LONG," +
            TABLE_COLUMN_reservedLong2 + " LONG," +
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
            ");";

    public DBHelper(Context context, int version) {
        super(context, DATABASE_NAME, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CONTACTS);
        db.execSQL(CREATE_TABLE_YAVENTS);
        db.execSQL(CREATE_TABLE_YAVENTS_LOG);
        db.execSQL(CREATE_TABLE_QUICKIES);
        db.execSQL(CREATE_TABLE_DISCOVER);
        db.execSQL("CREATE TABLE " +  TABLE_reserved1 + CREATE_TABLE_RESERVED);
        db.execSQL("CREATE TABLE " +  TABLE_reserved2 + CREATE_TABLE_RESERVED);
        db.execSQL("CREATE TABLE " +  TABLE_reserved3 + CREATE_TABLE_RESERVED);
        db.execSQL("CREATE TABLE " +  TABLE_reserved4 + CREATE_TABLE_RESERVED);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //YLog.d(TAG, "upgrading Database from/to version: " + oldVersion + "/" + newVersion);
    }
}
