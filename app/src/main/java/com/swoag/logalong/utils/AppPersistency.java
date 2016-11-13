package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Parcelable;

import java.util.HashMap;

public class AppPersistency {
    public static final int TRANSACTION_FILTER_BY_ACCOUNT = 10;
    public static final int TRANSACTION_FILTER_BY_CATEGORY = 20;
    public static final int TRANSACTION_FILTER_BY_TAG = 30;
    public static final int TRANSACTION_FILTER_BY_VENDOR = 40;
    public static final int TRANSACTION_FILTER_ALL = 50;

    public static final int TRANSACTION_TIME_MONTHLY = 10;
    public static final int TRANSACTION_TIME_QUARTERLY = 20;
    public static final int TRANSACTION_TIME_ANNUALLY = 30;
    public static final int TRANSACTION_TIME_ALL = 40;

    public static boolean transactionChanged = false;
    public static int viewTransactionYear = -1;
    public static int viewTransactionMonth = -1;
    public static int viewTransactionQuarter = -1;

    public static int viewTransactionFilter = TRANSACTION_FILTER_ALL;
    public static int viewTransactionTime = TRANSACTION_TIME_MONTHLY;

    public static boolean profileSet = false;

    private static HashMap<Integer, Parcelable> viewHistory = new HashMap<Integer, Parcelable>();
    private static int viewLevel;
    public static Parcelable getViewHistory(int level) {
        if (viewHistory.containsKey(level)) {
            return viewHistory.get(level);
        } else {
            return null;
        }
    }
    public static void setViewHistory(int level, Parcelable history) {
        viewHistory.put(level, history);
    }
    public static int getViewLevel() {
        return viewLevel;
    }
    public static void setViewLevel( int level) {
        viewLevel = level;
    }
    public static void clearViewHistory() {
        viewHistory.clear();
        viewLevel = 1000;
    }

    public static boolean showPieChart = true;
}
