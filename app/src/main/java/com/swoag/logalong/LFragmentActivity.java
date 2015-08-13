package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.support.v4.app.FragmentActivity;

public class LFragmentActivity extends FragmentActivity {
    //private static final String TAG = LFragmentActivity.class.getSimpleName();
    public static boolean upRunning;

    @Override
    protected void onResume() {
        super.onResume();
        upRunning = true;
    }
    @Override
    protected void onPause() {
        super.onPause();
        upRunning = false;
    }
}
