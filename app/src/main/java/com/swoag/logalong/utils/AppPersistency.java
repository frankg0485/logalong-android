package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class AppPersistency {
    public static final int TRANSACTION_FILTER_BY_ACCOUNT = 10;
    public static final int TRANSACTION_FILTER_BY_CATEGORY = 20;
    public static final int TRANSACTION_FILTER_BY_TAG = 30;
    public static final int TRANSACTION_FILTER_BY_VENDOR = 40;
    public static final int TRANSACTION_FILTER_ALL = 50;

    public static boolean transactionChanged = false;
    public static int viewTransactionYear = -1;
    public static int viewTransactionMonth = -1;
    public static int viewTransactionFilter = TRANSACTION_FILTER_ALL;
}
