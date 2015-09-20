package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.SharedPreferences;

import com.swoag.logalong.LApp;

public class LPreferences {
    private static final String SHARED_PREF_NAME = "LogAlong";
    private static final String USER_NAME = "UserName";
    private static final String USER_ID = "UserId";

    public static int getUserId() {
        return getPreference(LApp.ctx, USER_ID, (int) 0);
    }

    public static void setUserId(int userId) {
        savePreference(LApp.ctx, USER_ID, userId);
    }

    public static String getUserName() {
        return getPreference(LApp.ctx, USER_NAME, "");
    }

    public static void setUserName(String userName) {
        savePreference(LApp.ctx, USER_NAME, userName);
    }

    public static String getShareUserName (int userId) {
        return getPreference(LApp.ctx, USER_NAME + "." + userId, "");
    }

    public static void setShareUserName (int userId, String name) {
        savePreference(LApp.ctx, USER_NAME + "." + userId, name);
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
