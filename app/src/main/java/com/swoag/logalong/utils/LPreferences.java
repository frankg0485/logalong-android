package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccountShareRequest;

import java.util.Calendar;

public class LPreferences {
    //private static final String LAST_DB_RESTORE_DATE = "LastDbRd";
    private static final String CACHE_READ_POINTER = "CacheRdPtr";
    private static final String CACHE_WRITE_POINTER = "CacheWrPtr";
    private static final String CACHE_LENGTH = "CacheLength";
    private static final String DB_SYNCED = "DbSynced";
    private static final String FIRST_TRANSACTION_TIMESTAMP = "FirstDbTrans";
    private static final String LAST_TRANSACTION_TIMESTAMP = "LastDbTrans";
    private static final String LOGIN_ERROR = "LoginError";
    private static final String ONE_TIME_INIT = "OneTimeInit";
    private static final String QUERY_ORDER = "QueryOrder";
    private static final String SEARCH_ALLTIME = "SearchAllTime";
    private static final String SEARCH_ALLTIME_FROM = "SearchAllTimeFrom";
    private static final String SEARCH_ALLTIME_TO = "SearchAllTimeTo";
    private static final String SEARCH_ALL = "SearchAll";
    private static final String SEARCH_ACCOUNTS = "SearchAccounts";
    private static final String SEARCH_CATEGORIES = "SearchCategories";
    private static final String SEARCH_VENDORS = "SearchVendors";
    private static final String SEARCH_TAGS = "SearchTags";
    private static final String SEARCH_FILTER_BY_EDIT_TIME = "SearchFilterByEditTime";
    private static final String SEARCH_FILTER_BY_VALUE = "SearchFilterByValue";
    private static final String SEARCH_VALUE = "SearchValue";
    private static final String SHARE_ACCOUNT_REQUEST = "ShareAcntRqst";
    private static final String SHARED_PREF_NAME = "LogAlong";
    private static final String SHOW_ACCOUNT_BALANCE = "ShowAB";
    private static final String SERVER_MSG_BROADCAST = "SrvMsgBroadcast";
    private static final String USER_ID = "MyUserId";
    private static final String USER_PASS = "MyUserPass";
    private static final String DEVICE_ID = "MyDeviceId";
    private static final String USER_NAME = "MyUserName";
    private static final String UTC_DELTA = "UtcDelta";

    /*
    public static String getLastDbRestoreDate() {
        return getPreference(LApp.ctx, LAST_DB_RESTORE_DATE, "N/A");
    }

    public static void setLastDbRestoreDate(String date) {
        savePreference(LApp.ctx, LAST_DB_RESTORE_DATE, date);
    }
    */

    public static boolean getDbSynced() {
        return getPreference(LApp.ctx, DB_SYNCED, false);
    }

    public static void setDbSynced(boolean synced) {
        savePreference(LApp.ctx, DB_SYNCED, synced);
    }

    public static boolean getLoginError() {
        return getPreference(LApp.ctx, LOGIN_ERROR, false);
    }

    public static void setLoginError(boolean error) {
        savePreference(LApp.ctx, LOGIN_ERROR, error);
    }

    public static long getCacheReadPointer() {
        return getPreference(LApp.ctx, CACHE_READ_POINTER, 0L);
    }

    public static void setCacheReadPointer(long ptr) {
        savePreference(LApp.ctx, CACHE_READ_POINTER, ptr);
    }

    public static long getCacheWritePointer() {
        return getPreference(LApp.ctx, CACHE_WRITE_POINTER, 0L);
    }

    public static void setCacheWritePointer(long ptr) {
        savePreference(LApp.ctx, CACHE_WRITE_POINTER, ptr);
    }

    public static int getCacheLength() {
        return getPreference(LApp.ctx, CACHE_LENGTH, 0);
    }

    public static void setCacheLength(int length) {
        savePreference(LApp.ctx, CACHE_LENGTH, length);
    }

    public static long getFirstDbTransactionTimestamp() {
        return getPreference(LApp.ctx, FIRST_TRANSACTION_TIMESTAMP, Long.MAX_VALUE);
    }

    public static void setFirstDbTransactionTimestamp(long timestamp) {
        if (timestamp < getFirstDbTransactionTimestamp()) {
            savePreference(LApp.ctx, FIRST_TRANSACTION_TIMESTAMP, timestamp);
        }
    }

    public static long getLastDbTransactionTimestamp() {
        return getPreference(LApp.ctx, LAST_TRANSACTION_TIMESTAMP, 0L);
    }

    public static void setLastDbTransactionTimestamp(long timestamp) {
        if (timestamp > getLastDbTransactionTimestamp()) {
            savePreference(LApp.ctx, LAST_TRANSACTION_TIMESTAMP, timestamp);
        }
    }

    public static String getDeviceId() {
        return getPreference(LApp.ctx, DEVICE_ID, "");
    }

    public static void setDeviceId(String deviceId) {
        savePreference(LApp.ctx, DEVICE_ID, deviceId);
    }

    public static String getUserPass() {
        return getPreference(LApp.ctx, USER_PASS, "");
    }

    public static void setUserPass(String userPass) {
        savePreference(LApp.ctx, USER_PASS, userPass);
    }

    public static String getUserName() {
        return getPreference(LApp.ctx, USER_NAME, "");
    }

    public static void setUserName(String userName) {
        savePreference(LApp.ctx, USER_NAME, userName);
    }

    public static String getUserId() {
        return getPreference(LApp.ctx, USER_ID, "");
    }

    public static void setUserId(String userId) {
        savePreference(LApp.ctx, USER_ID, userId);
    }

    public static String getShareUserName(int userId) {
        return getPreference(LApp.ctx, USER_NAME + "." + userId, "");
    }

    public static void setShareUserName(int userId, String name) {
        savePreference(LApp.ctx, USER_NAME + "." + userId, name);
    }

    public static String getShareUserFullName(int userId) {
        //return getPreference(LApp.ctx, USER_FULL_NAME + "." + userId, "");
        return "";
    }

    public static void setShareUserFullName(int userId, String name) {
        //savePreference(LApp.ctx, USER_FULL_NAME + "." + userId, name);
    }

    public static boolean getShowAccountBalance(long id) {
        return getPreference(LApp.ctx, SHOW_ACCOUNT_BALANCE + "." + id, true);
    }

    public static void setShowAccountBalance(long id, boolean show) {
        savePreference(LApp.ctx, SHOW_ACCOUNT_BALANCE + "." + id, show);
    }

    private static int getEmptyAccountShareRequest() {
        int shares = getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ".total", 0);

        int ii = 0;
        for (ii = 0; ii < shares; ii++) {
            if (getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".state", 0) == 0) return ii;
        }
        savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ".total", shares + 1);
        return ii;
    }

    public static void addAccountShareRequest(LAccountShareRequest request) {
        int share = getEmptyAccountShareRequest();
        savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + share + ".accountName", request.getAccountName());
        savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + share + ".accountGid", request.getAccountGid());
        savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + share + ".shareAccountGid", request.getShareAccountGid());
        savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + share + ".userId", request.getUserId());
        savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + share + ".userName", request.getUserName());
        savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + share + ".userFullName", request.getUserFullName());
        savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + share + ".state", 1);
    }

    public static void deleteAccountShareRequest(LAccountShareRequest request) {
        int shares = getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ".total", 0);

        for (int ii = 0; ii < shares; ii++) {
            if (getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".state", 0) == 1) {
                if ((getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".userId", 0) == request.getUserId())
                        && (getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".userName", "").contentEquals(request.getUserName()))
                        && (getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".userFullName", "").contentEquals(request.getUserFullName()))
                        && (getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".accountName", "").contentEquals(request.getAccountName()))
                        && (getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".shareAccountGid", 0) == request.getShareAccountGid())
                        && (getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".accountGid", 0) == request.getAccountGid())) {
                    savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".state", 0);
                }
            }
        }
    }

    public static LAccountShareRequest getAccountShareRequest() {
        LAccountShareRequest request = null;
        int shares = getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ".total", 0);

        for (int ii = 0; ii < shares; ii++) {
            if (getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".state", 0) == 1) {
                request = new LAccountShareRequest(
                        getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".userId", 0),
                        getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".userName", ""),
                        getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".userFullName", ""),
                        getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".accountName", ""),
                        getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".accountGid", 0),
                        getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".shareAccountGid", 0)
                );
                break;
            }
        }

        return request;
    }

    public static boolean getOneTimeInit() {
        return getPreference(LApp.ctx, ONE_TIME_INIT, false);
    }

    public static void setOneTimeInit(Boolean yes) {
        savePreference(LApp.ctx, ONE_TIME_INIT, yes);
    }

    public static boolean getSearchAllTime() {
        return getPreference(LApp.ctx, SEARCH_ALLTIME, true);
    }

    public static void setSearchAllTime(boolean all) {
        savePreference(LApp.ctx, SEARCH_ALLTIME, all);
    }

    private static long defaultSearchAllTime(boolean from) {
        Calendar calendar = Calendar.getInstance();
        if (from) calendar.add(Calendar.MONTH, -1);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return calendar.getTimeInMillis();
    }

    public static long getSearchAllTimeFrom() {
        return getPreference(LApp.ctx, SEARCH_ALLTIME_FROM, defaultSearchAllTime(true));
    }

    public static void setSearchAllTimeFrom(long from) {
        if (from == 0) from = defaultSearchAllTime(true);
        savePreference(LApp.ctx, SEARCH_ALLTIME_FROM, from);
    }

    public static long getSearchAllTimeTo() {
        return getPreference(LApp.ctx, SEARCH_ALLTIME_TO, defaultSearchAllTime(false));
    }

    public static void setSearchAllTimeTo(long to) {
        if (to == 0) to = defaultSearchAllTime(false);
        savePreference(LApp.ctx, SEARCH_ALLTIME_TO, to);
    }

    private static long[] getLongArray(String key) {
        String str = getPreference(LApp.ctx, key, "");
        if (TextUtils.isEmpty(str)) return null;

        String[] ss = str.split(",");
        if (ss.length > 0) {
            long[] vals = new long[ss.length];
            for (int ii = 0; ii < ss.length; ii++) {
                vals[ii] = Long.parseLong(ss[ii]);
            }
            return vals;
        }
        return null;
    }

    private static void setLongArray(String key, long[] vals) {
        if (null == vals) {
            savePreference(LApp.ctx, key, "");
            return;
        }

        String str = "";
        for (int ii = 0; ii < vals.length - 1; ii++) {
            str += String.valueOf(vals[ii]) + ",";
        }
        str += String.valueOf(vals[vals.length - 1]);
        savePreference(LApp.ctx, key, str);
    }

    public static boolean getQueryOrderAscend() {
        return getPreference(LApp.ctx, QUERY_ORDER, true);
    }

    public static void setQueryOrderAscend(boolean order) {
        savePreference(LApp.ctx, QUERY_ORDER, order);
    }

    public static boolean getSearchAll() {
        return getPreference(LApp.ctx, SEARCH_ALL, true);
    }

    public static void setSearchAll(boolean all) {
        savePreference(LApp.ctx, SEARCH_ALL, all);
    }

    public static long[] getSearchAccounts() {
        return getLongArray(SEARCH_ACCOUNTS);
    }

    public static void setSearchAccounts(long[] accounts) {
        setLongArray(SEARCH_ACCOUNTS, accounts);
    }

    public static long[] getSearchCategories() {
        return getLongArray(SEARCH_CATEGORIES);
    }

    public static void setSearchCategories(long[] categories) {
        setLongArray(SEARCH_CATEGORIES, categories);
    }

    public static long[] getSearchVendors() {
        return getLongArray(SEARCH_VENDORS);
    }

    public static void setSearchVendors(long[] vendors) {
        setLongArray(SEARCH_VENDORS, vendors);
    }

    public static long[] getSearchTags() {
        return getLongArray(SEARCH_TAGS);
    }

    public static void setSearchTags(long[] tags) {
        setLongArray(SEARCH_TAGS, tags);
    }

    public static String getServerMsg() {
        return getPreference(LApp.ctx, SERVER_MSG_BROADCAST, "");
    }

    public static void setServerMsg(String msg) {
        savePreference(LApp.ctx, SERVER_MSG_BROADCAST, msg);
    }

    public static boolean getSearchFilterByEditTIme() {
        return getPreference(LApp.ctx, SEARCH_FILTER_BY_EDIT_TIME, false);
    }

    public static void setSearchFilterByEditTime(boolean yes) {
        savePreference(LApp.ctx, SEARCH_FILTER_BY_EDIT_TIME, yes);
    }

    public static boolean getSearchFilterByValue() {
        return getPreference(LApp.ctx, SEARCH_FILTER_BY_VALUE, false);
    }

    public static void setSearchFilterByValue(boolean yes) {
        savePreference(LApp.ctx, SEARCH_FILTER_BY_VALUE, yes);
    }

    public static float getSearchValue() {
        return getPreference(LApp.ctx, SEARCH_VALUE, 0f);
    }

    public static void setSearchValue(float value) {
        savePreference(LApp.ctx, SEARCH_VALUE, value);
    }

    public static long getUtcDelta() {
        return getPreference(LApp.ctx, UTC_DELTA, 0L);
    }

    public static void setUtcDelta(long delta) {
        savePreference(LApp.ctx, UTC_DELTA, delta);
    }

    public static long getServerUtc() {
        return System.currentTimeMillis() + LPreferences.getUtcDelta() * 1000;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    public static String getPreference(Context context, String key, String defaultValue) {
        try {
            SharedPreferences settings = getPreferences(context);
            return settings.getString(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void savePreference(Context context, String key, String value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(key, value);
        editor.commit();
    }

    public static boolean getPreference(Context context, String key, boolean defaultValue) {
        try {
            SharedPreferences settings = getPreferences(context);
            return settings.getBoolean(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void savePreference(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static int getPreference(Context context, String key, int defaultValue) {
        SharedPreferences settings = getPreferences(context);
        try {
            return settings.getInt(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void savePreference(Context context, String key, int value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(key, value);
        editor.commit();
    }

    public static long getPreference(Context context, String key, long defaultValue) {
        try {
            SharedPreferences settings = getPreferences(context);
            return settings.getLong(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void savePreference(Context context, String key, long value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putLong(key, value);
        editor.commit();
    }

    public static float getPreference(Context context, String key, float defaultValue) {
        SharedPreferences settings = getPreferences(context);
        try {
            return settings.getFloat(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void savePreference(Context context, String key, float value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putFloat(key, value);
        editor.commit();
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        return getPreferences(context).edit();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }
}
