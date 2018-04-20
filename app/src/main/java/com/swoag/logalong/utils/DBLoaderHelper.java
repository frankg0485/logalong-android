package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2018 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import com.swoag.logalong.entities.LSearch;
import com.swoag.logalong.entities.LTransaction;

import java.util.ArrayList;
import java.util.Calendar;

public class DBLoaderHelper implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = DBLoaderHelper.class.getSimpleName();

    public static final int LOADER_INIT_RANGE = 10;
    public static final int LOADER_ALL_SUMMARY = 20;
    public static final int LOADER_ALL_ACCOUNTS = 30;
    public static final int LOADER_ALL_ACCOUNT_BALANCES = 40;
    public static final int LOADER_TRANSACTION_FILTER_BY_ACCOUNT = 50;
    public static final int LOADER_TRANSACTION_FILTER_BY_CATEGORY = 60;
    public static final int LOADER_TRANSACTION_FILTER_BY_TAG = 70;
    public static final int LOADER_TRANSACTION_FILTER_BY_VENDOR = 80;
    public static final int LOADER_TRANSACTION_FILTER_ALL = 90;

    private boolean annualModeOnly;
    private Context context;
    private DBLoaderHelperCallbacks callbacks;
    private long startMs, endMs, allStartMs, allEndMs;
    private int allStartYear, allEndYear, allStartMonth, allEndMonth;

    private String selections = "";
    private ArrayList<String> selectionArgs = new ArrayList<String>();

    public interface DBLoaderHelperCallbacks {
        public void onLoadFinished(Loader<Cursor> loader, Cursor data);

        public void onLoaderReset(Loader<Cursor> loader);
    }

    public DBLoaderHelper(Context context, DBLoaderHelperCallbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
        this.annualModeOnly = false;
    }

    //annualModeOnly: loader that is used in chart
    public DBLoaderHelper(Context context, DBLoaderHelperCallbacks callbacks, boolean annualModeOnly) {
        this.context = context;
        this.callbacks = callbacks;
        this.annualModeOnly = annualModeOnly;
    }

    public boolean restart(LoaderManager manager, int id) {
        manager.restartLoader(id, null, this);
        return true;
    }

    public long getAllStartMs() {
        return allStartMs;
    }

    public long getAllEndMs() {
        return allEndMs;
    }

    public int getAllStartYear() {
        return allStartYear;
    }

    public int getAllStartMonth() {
        return allStartMonth;
    }

    public int getAllEndYear() {
        return allEndYear;
    }

    public int getAllEndMonth() {
        return allEndMonth;
    }

    private long getMs(int year, int month) {
        Calendar now = Calendar.getInstance();
        now.clear();
        now.set(year, month, 1);
        return now.getTimeInMillis();
    }

    private long resetMs(long ms, boolean nextMonth) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(ms);
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        if (nextMonth) {
            if (month == 11) {
                month = 0;
                year++;
            } else month++;
        }
        return getMs(year, month);
    }

    //TODO: cleanup redundant logic within this function.
    private boolean validateYearMonth() {
        long ym = getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth);

        if (ym < allStartMs || ym >= allEndMs) {
            //first remedy: honor user selected year, set to valid month of the year
            if (AppPersistency.viewTransactionYear == allStartYear) {
                AppPersistency.viewTransactionMonth = 11;
            } else if (AppPersistency.viewTransactionYear == allEndYear) {
                AppPersistency.viewTransactionMonth = 0;
            }
        }

        ym = getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth);
        if (ym < allStartMs || ym >= allEndMs) {
            //next try: current year month
            Calendar calendar = Calendar.getInstance();
            AppPersistency.viewTransactionYear = calendar.get(Calendar.YEAR);
            AppPersistency.viewTransactionMonth = calendar.get(Calendar.MONTH);
        } else return true;

        ym = getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth);
        if (ym < allStartMs || ym >= allEndMs) {
            //last resort: last valid year/month of current DB
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(allEndMs - 1);
            AppPersistency.viewTransactionYear = calendar.get(Calendar.YEAR);
            AppPersistency.viewTransactionMonth = calendar.get(Calendar.MONTH);
        }
        return false;
    }

    public void initStartEndMs() {
        LSearch search = LPreferences.getSearchControls();
        if (annualModeOnly) {
            //TODO: if this code is ever called, allStartYear allEndYear may not have been initialized
            LLog.w(TAG, "BUG: uninitialized start/end year");
            if (search.isbAllTime()) {
                if (AppPersistency.viewTransactionYear < allStartYear || AppPersistency.viewTransactionYear > allEndYear) {
                    AppPersistency.viewTransactionYear = allEndYear;
                }
                startMs = getMs(AppPersistency.viewTransactionYear, 0);
                endMs = getMs(AppPersistency.viewTransactionYear + 1, 0);
            } else {
                startMs = search.getTimeFrom();
                endMs = search.getTimeTo();
            }
        } else {
            validateYearMonth();

            if (search.isbAllTime()) {
                switch (AppPersistency.viewTransactionTime) {
                    case AppPersistency.TRANSACTION_TIME_ALL:
                        startMs = 0;
                        endMs = Long.MAX_VALUE;
                        break;
                    case AppPersistency.TRANSACTION_TIME_MONTHLY:
                        startMs = getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth);
                        endMs = getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth + 1);
                        break;
                    case AppPersistency.TRANSACTION_TIME_QUARTERLY:
                        startMs = getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionQuarter * 3);
                        endMs = getMs(AppPersistency.viewTransactionYear, (AppPersistency.viewTransactionQuarter + 1) * 3);
                        break;

                    case AppPersistency.TRANSACTION_TIME_ANNUALLY:
                        startMs = getMs(AppPersistency.viewTransactionYear, 0);
                        endMs = getMs(AppPersistency.viewTransactionYear + 1, 0);
                        break;
                }
            } else {
                startMs = search.getTimeFrom();
                endMs = search.getTimeTo();
                //LLog.d(TAG, "start: " + startMs + "@" + (new Date(startMs) + " end: " + endMs + "@" + (new Date(endMs))));
            }
        }
    }

    private void setSelections(long[] ids, String column) {
        selections += "(";
        for (int ii = 0; ii < ids.length - 1; ii++) {
            selections += column + "=? OR ";
            selectionArgs.add("" + ids[ii]);
        }
        selections += column + "=?)";
        selectionArgs.add("" + ids[ids.length - 1]);
    }

    private long transactionIndex2Type( long idx) {
        switch ((int)idx) {
            case 0: return (long)LTransaction.TRANSACTION_TYPE_EXPENSE;
            case 1: return (long)LTransaction.TRANSACTION_TYPE_INCOME;
        }
        return (long) LTransaction.TRANSACTION_TYPE_TRANSFER;
    }

    private void resetSelections() {
        LSearch search = LPreferences.getSearchControls();
        if (search.isbShowAll() && (search.isbAllValue() ||
                (search.getValueFrom() == 0 && search.getValueTo() == 0))) {
            selections = "";
        } else {
            selections = "";
            selectionArgs.clear();

            boolean and = false;
            long[] ids;

            if (!(search.isbAllValue() ||
                    (search.getValueFrom() == 0 && search.getValueTo() == 0))) {
                float startValue, endValue;
                startValue = search.getValueFrom();
                endValue = search.getValueTo();
                if (endValue == 0) {
                    endValue = Float.MAX_VALUE;
                }
                and = true;

                selections = "(" + DBHelper.TABLE_COLUMN_AMOUNT + ">=? AND ";
                selections += DBHelper.TABLE_COLUMN_AMOUNT + "<=?)";
                selectionArgs.add("" + startValue);
                selectionArgs.add("" + endValue);
            }

            if (!search.isbShowAll()) {
                if (search.isbAccounts()) {
                    ids = search.getAccounts();
                    if (ids != null) {
                        if (and) selections += " AND ";
                        setSelections(ids, DBHelper.TABLE_COLUMN_ACCOUNT);
                        and = true;
                    }
                }

                if (search.isbCategories()) {
                    ids = search.getCategories();
                    if (ids != null) {
                        if (and) selections += " AND ";
                        setSelections(ids, DBHelper.TABLE_COLUMN_CATEGORY);
                        and = true;
                    }
                }

                if (search.isbVendors()) {
                    ids = search.getVendors();
                    if (ids != null) {
                        if (and) selections += " AND ";
                        setSelections(ids, DBHelper.TABLE_COLUMN_VENDOR);
                        and = true;
                    }
                }

                if (search.isbTags()) {
                    ids = search.getTags();
                    if (ids != null) {
                        if (and) selections += " AND ";
                        setSelections(ids, DBHelper.TABLE_COLUMN_TAG);
                    }
                }

                if (search.isbTypes()) {
                    ids = search.getTypes();
                    if (ids != null) {
                        if (and) selections += " AND ";

                        int length = ids.length;
                        for (int ii = 0; ii < ids.length; ii++) {
                            if (LTransaction.TRANSACTION_TYPE_TRANSFER == transactionIndex2Type(ids[ii])) {
                                length++;
                                break;
                            }
                        }
                        long[] ids2 = new long[length];
                        int jj = 0;
                        for (int ii = 0; ii < ids.length; ii++) {
                            ids2[jj] = transactionIndex2Type(ids[ii]);
                            if (ids2[jj] == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                                ids2[jj + 1] = LTransaction.TRANSACTION_TYPE_TRANSFER_COPY;
                                jj++;
                            }
                            jj++;
                        }
                        setSelections(ids2, DBHelper.TABLE_COLUMN_TYPE);
                    }
                }
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        LSearch search = LPreferences.getSearchControls();
        if (id == LOADER_ALL_ACCOUNT_BALANCES) {
            return new CursorLoader(
                    context,
                    DBProvider.URI_ACCOUNT_BALANCES,
                    null,
                    DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE}, null);
        } else if (id == LOADER_ALL_ACCOUNTS) {
            return new CursorLoader(
                    context,
                    DBProvider.URI_ACCOUNTS,
                    null,
                    DBHelper.TABLE_COLUMN_STATE + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE},
                    DBHelper.TABLE_COLUMN_NAME + " ASC");
        }

        /*if (search.isbAllValue()) {
            return new CursorLoader(
                    context,
                    DBProvider.URI_TRANSACTIONS,
                    null,
                    DBHelper.TABLE_COLUMN_STATE + "=? AND " + DBHelper.TABLE_COLUMN_AMOUNT + "=?",
                    new String[]{"" + DBHelper.STATE_ACTIVE, "" + search.getValueFrom()},
                    DBHelper.TABLE_COLUMN_TIMESTAMP + (LPreferences.getQueryOrderAscend() ? " ASC" : " DESC"));
        }*/

        String s, ds, sort;
        String[] sa, dsa;
        Uri uri;
        String projection = "a._id,"
                + "a." + DBHelper.TABLE_COLUMN_AMOUNT + ","
                + "a." + DBHelper.TABLE_COLUMN_CATEGORY + ","
                + "a." + DBHelper.TABLE_COLUMN_ACCOUNT + ","
                + "a." + DBHelper.TABLE_COLUMN_ACCOUNT2 + ","
                + "a." + DBHelper.TABLE_COLUMN_TAG + ","
                + "a." + DBHelper.TABLE_COLUMN_VENDOR + ","
                + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + ","
                /*
                + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + ","
                */
                + "a." + DBHelper.TABLE_COLUMN_TYPE + ","
                /*
                + "a." + DBHelper.TABLE_COLUMN_STATE + ","
                + "a." + DBHelper.TABLE_COLUMN_MADEBY + ","
                */
                + "a." + DBHelper.TABLE_COLUMN_IRID + ","
                + "a." + DBHelper.TABLE_COLUMN_NOTE + ","
                + "b." + DBHelper.TABLE_COLUMN_NAME;

        resetSelections();

        switch (id) {
            case LOADER_INIT_RANGE:
                long start, end;
                if (search.isbAllTime()) {
                    start = 0;
                    end = Long.MAX_VALUE;
                } else {
                    start = search.getTimeFrom();
                    end = search.getTimeTo();
                }

                uri = DBProvider.URI_TRANSACTIONS;
                if ((!search.isbAllTime()) && search.isbByEditTime()) {
                    s = ds = DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + ">=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "<?";
                    sa = dsa = new String[]{"" + DBHelper.STATE_ACTIVE, "" + start, "" + end};
                    sort = DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + " ASC";
                } else {
                    s = ds = DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + ">=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + "<?";
                    sa = dsa = new String[]{"" + DBHelper.STATE_ACTIVE, "" + start, "" + end};
                    sort = DBHelper.TABLE_COLUMN_TIMESTAMP + " ASC";
                }


                if (!TextUtils.isEmpty(selections)) {
                    s = selections + " AND " + ds;
                    ArrayList<String> tmp = new ArrayList<String>(selectionArgs);
                    for (int ii = 0; ii < dsa.length; ii++) {
                        tmp.add(dsa[ii]);
                    }
                    sa = tmp.toArray(new String[tmp.size()]);
                }
                return new CursorLoader(
                        context,
                        uri,
                        null,
                        s,
                        sa,
                        sort
                );

            case LOADER_ALL_SUMMARY:
            case LOADER_TRANSACTION_FILTER_ALL:
                uri = DBProvider.URI_TRANSACTIONS;

                if ((!search.isbAllTime()) && search.isbByEditTime()) {
                    s = ds = DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + ">=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "<?";
                    sa = dsa = new String[]{"" + DBHelper.STATE_ACTIVE, "" + startMs, "" + endMs};
                    sort = DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + (LPreferences.getQueryOrderAscend() ? " ASC" : " DESC");
                } else {
                    s = ds = DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + ">=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + "<?";
                    sa = dsa = new String[]{"" + DBHelper.STATE_ACTIVE, "" + startMs, "" + endMs};
                    sort = DBHelper.TABLE_COLUMN_TIMESTAMP + (LPreferences.getQueryOrderAscend() ? " ASC" : " DESC");
                }

                if (!TextUtils.isEmpty(selections)) {
                    s = selections + " AND " + ds;
                    ArrayList<String> tmp = new ArrayList<String>(selectionArgs);
                    for (int ii = 0; ii < dsa.length; ii++) {
                        tmp.add(dsa[ii]);
                    }
                    sa = tmp.toArray(new String[tmp.size()]);
                }

                return new CursorLoader(
                        context,
                        uri,
                        null,
                        s,
                        sa,
                        sort
                );

            case LOADER_TRANSACTION_FILTER_BY_ACCOUNT:
                uri = DBProvider.URI_TRANSACTIONS_ACCOUNT;
                break;
            case LOADER_TRANSACTION_FILTER_BY_CATEGORY:
                uri = DBProvider.URI_TRANSACTIONS_CATEGORY;
                break;
            case LOADER_TRANSACTION_FILTER_BY_TAG:
                uri = DBProvider.URI_TRANSACTIONS_TAG;
                break;
            case LOADER_TRANSACTION_FILTER_BY_VENDOR:
                uri = DBProvider.URI_TRANSACTIONS_VENDOR;
                break;
            default:
                // An invalid id was passed in
                LLog.w(TAG, "invalid db loader ID: " + id);
                return null;
        }

        if ((!search.isbAllTime()) && search.isbByEditTime()) {
            s = ds = "a." + DBHelper.TABLE_COLUMN_STATE + "=? AND "
                    + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + ">=? AND "
                    + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "<?";
            sa = dsa = new String[]{"" + DBHelper.STATE_ACTIVE, "" + startMs, "" + endMs};
            sort = "b." + DBHelper.TABLE_COLUMN_NAME + " ASC, " + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + (LPreferences.getQueryOrderAscend() ? " ASC" : " DESC");
        } else {
            s = ds = "a." + DBHelper.TABLE_COLUMN_STATE + "=? AND "
                    + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + ">=? AND "
                    + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + "<?";
            sa = dsa = new String[]{"" + DBHelper.STATE_ACTIVE, "" + startMs, "" + endMs};
            sort = "b." + DBHelper.TABLE_COLUMN_NAME + " ASC, " + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + (LPreferences.getQueryOrderAscend() ? " ASC" : " DESC");
        }

        if (!TextUtils.isEmpty(selections)) {
            s = selections + " AND " + ds;
            ArrayList<String> tmp = new ArrayList<String>(selectionArgs);
            for (int ii = 0; ii < dsa.length; ii++) {
                tmp.add(dsa[ii]);
            }
            sa = tmp.toArray(new String[tmp.size()]);
        }

        return new CursorLoader(
                context,
                uri,
                new String[]{projection},
                s,
                sa,
                sort
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (LOADER_INIT_RANGE == loader.getId()) {
            Calendar calendar = Calendar.getInstance();

            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                allStartMs = resetMs(data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)), false);
                data.moveToLast();
                allEndMs = resetMs(data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)), true);

                calendar.setTimeInMillis(allStartMs);
                allStartYear = calendar.get(Calendar.YEAR);
                allStartMonth = calendar.get(Calendar.MONTH);
                calendar.setTimeInMillis(allEndMs - 1);
                allEndYear = calendar.get(Calendar.YEAR);
                allEndMonth = calendar.get(Calendar.MONTH);
            } else {
                allStartMs = allEndMs = calendar.getTimeInMillis();
                allStartYear = allEndYear = calendar.get(Calendar.YEAR);
                allStartMonth = allEndMonth = calendar.get(Calendar.MONTH);
            }
            //even if there's no data, we'll still start client loader anyway, so client gets its callback
            initStartEndMs();
        }
        callbacks.onLoadFinished(loader, data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        callbacks.onLoaderReset(loader);
    }
}
