package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class AppPersistency {
    public static final int TRANSACTION_FILTER_BY_ACCOUNT = 10;
    public static final int TRANSACTION_FILTER_BY_CATEGORY = 10;
    public static final int TRANSACTION_FILTER_BY_TAG = 10;
    public static final int TRANSACTION_FILTER_BY_VENDOR = 10;
    public static final int TRANSACTION_FILTER_ALL = 10;

    public static boolean transactionChanged = false;
    public static int viewTransactionYear = -1;
    public static int viewTransactionMonth = -1;
    public static int viewTransactionFilter = TRANSACTION_FILTER_ALL;
}
