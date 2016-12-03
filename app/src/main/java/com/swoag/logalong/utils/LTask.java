package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.annotation.TargetApi;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;

public class LTask {
    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // API 11
    public static void start(AsyncTask asyncTask, Cursor... params) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            asyncTask.execute(params);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // API 11
    public static void start(AsyncTask asyncTask, Integer... params) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            asyncTask.execute(params);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // API 11
    public static void start(AsyncTask asyncTask, Long... params) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            asyncTask.execute(params);
    }
}
