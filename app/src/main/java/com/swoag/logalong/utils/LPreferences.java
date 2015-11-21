package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.SharedPreferences;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccountShareRequest;

public class LPreferences {
    //private static final String LAST_DB_RESTORE_DATE = "LastDbRd";
    private static final String FIRST_TRANSACTION_TIMESTAMP = "FirstDbTrans";
    private static final String LAST_TRANSACTION_TIMESTAMP = "LastDbTrans";
    private static final String SHARE_ACCOUNT_REQUEST = "ShareAcntRqst";
    private static final String SHARED_PREF_NAME = "LogAlong";
    private static final String SHOW_ACCOUNT_BALANCE = "ShowAB";
    private static final String USER_NAME = "UserName";
    private static final String USER_PASS = "UserPass";
    private static final String USER_ID = "UserId";
    private static final String USER_FULL_NAME = "UserFullName";

    /*
    public static String getLastDbRestoreDate() {
        return getPreference(LApp.ctx, LAST_DB_RESTORE_DATE, "N/A");
    }

    public static void setLastDbRestoreDate(String date) {
        savePreference(LApp.ctx, LAST_DB_RESTORE_DATE, date);
    }
    */

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

    public static int getUserId() {
        return getPreference(LApp.ctx, USER_ID, (int) 0);
    }

    public static void setUserId(int userId) {
        savePreference(LApp.ctx, USER_ID, userId);
    }

    /*
    public static String getUserPass() {
        return getPreference(LApp.ctx, USER_PASS, "");
    }

    public static void setUserPass(String userPass) {
        savePreference(LApp.ctx, USER_PASS, userPass);
    }
    */

    public static String getUserName() {
        return getPreference(LApp.ctx, USER_NAME, "");
    }

    public static void setUserName(String userName) {
        savePreference(LApp.ctx, USER_NAME, userName);
    }

    public static String getUserFullName() {
        return getPreference(LApp.ctx, USER_FULL_NAME, "");
    }

    public static void setUserFullName(String userName) {
        savePreference(LApp.ctx, USER_FULL_NAME, userName);
    }

    public static String getShareUserName(int userId) {
        return getPreference(LApp.ctx, USER_NAME + "." + userId, "");
    }

    public static void setShareUserName(int userId, String name) {
        savePreference(LApp.ctx, USER_NAME + "." + userId, name);
    }

    public static String getShareUserFullName(int userId) {
        return getPreference(LApp.ctx, USER_FULL_NAME + "." + userId, "");
    }

    public static void setShareUserFullName(int userId, String name) {
        savePreference(LApp.ctx, USER_FULL_NAME + "." + userId, name);
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
        savePreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + share + ".accountUuid", request.getAccountUuid());
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
                        && (getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".accountUuid", "").contentEquals(request.getAccountUuid()))) {
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
                        getPreference(LApp.ctx, SHARE_ACCOUNT_REQUEST + ii + ".accountUuid", "")
                );
                break;
            }
        }

        return request;
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

    public static boolean getPreference(Context context, String key, boolean defaultValue) {
        try {
            SharedPreferences settings = getPreferences(context);
            return settings.getBoolean(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getPreference(Context context, String key, int defaultValue) {
        SharedPreferences settings = getPreferences(context);
        try {
            return settings.getInt(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void savePreference(Context context, String key, String value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(key, value);
        editor.commit();
    }

    public static void savePreference(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(key, value);
        editor.commit();
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

    private static SharedPreferences.Editor getEditor(Context context) {
        return getPreferences(context).edit();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }
}
