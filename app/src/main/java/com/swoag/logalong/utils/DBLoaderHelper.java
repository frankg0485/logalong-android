package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;

public class DBLoaderHelper implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = DBLoaderHelper.class.getSimpleName();

    private static final  int LOADER_INIT_RANGE = 1;
    public static final int LOADER_ALL_SUMMARY = 20;

    private Context context;
    private DBLoaderHelperCallbacks callbacks;
    private long startMs, endMs, allStartMs, allEndMs;
    private int allStartYear, allEndYear, allStartMonth, allEndMonth;

    private String selections = "";
    private ArrayList<String> selectionArgs = new ArrayList<String>();
    private LoaderManager manager;
    private int loadId;

    public interface DBLoaderHelperCallbacks {
        public void onLoadFinished(Loader<Cursor> loader, Cursor data);
        public void onLoaderReset(Loader<Cursor> loader);
    }

    public DBLoaderHelper (Context context, DBLoaderHelperCallbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
    }

    public boolean restart(LoaderManager manager, int id) {
        this.manager = manager;
        manager.restartLoader(LOADER_INIT_RANGE, null, this);
        loadId = id;
        return true;
    }

    public int getStartYear() {
        return allStartYear;
    }

    public int getEndYear() {
        return allEndYear;
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

    private void initStartEndMs () {
        if (LPreferences.getSearchAllTime()) {
            if (AppPersistency.viewTransactionYear < allStartYear || AppPersistency.viewTransactionYear > allEndYear) {
                AppPersistency.viewTransactionYear = allEndYear;
            }
            startMs = getMs(AppPersistency.viewTransactionYear, 0);
            endMs = getMs(AppPersistency.viewTransactionYear + 1, 0);
        } else {
            startMs = LPreferences.getSearchAllTimeFrom();
            endMs = LPreferences.getSearchAllTimeTo();
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

    private void resetSelections() {
        if (LPreferences.getSearchAll()) {
            selections = "";
        } else {
            selections = "";
            selectionArgs.clear();

            boolean and = false;
            long[] ids = LPreferences.getSearchAccounts();
            if (ids != null) {
                setSelections(ids, DBHelper.TABLE_COLUMN_ACCOUNT);
                and = true;
            }

            ids = LPreferences.getSearchCategories();
            if (ids != null) {
                if (and) selections += " AND ";
                setSelections(ids, DBHelper.TABLE_COLUMN_CATEGORY);
                and = true;
            }

            ids = LPreferences.getSearchVendors();
            if (ids != null) {
                if (and) selections += " AND ";
                setSelections(ids, DBHelper.TABLE_COLUMN_VENDOR);
                and = true;
            }

            ids = LPreferences.getSearchTags();
            if (ids != null) {
                if (and) selections += " AND ";
                setSelections(ids, DBHelper.TABLE_COLUMN_TAG);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String s, ds, sort;
        String[] sa, dsa;
        Uri uri;

        resetSelections();

        switch (id) {
            case LOADER_INIT_RANGE:
                long start, end;
                if (LPreferences.getSearchAllTime()) {
                    start = 0;
                    end = Long.MAX_VALUE;
                } else {
                    start = LPreferences.getSearchAllTimeFrom();
                    end = LPreferences.getSearchAllTimeTo();
                }

                uri = DBProvider.URI_TRANSACTIONS;
                if ((!LPreferences.getSearchAllTime()) && LPreferences.getSearchFilterByEditTIme()) {
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
                uri = DBProvider.URI_TRANSACTIONS;

                if ((!LPreferences.getSearchAllTime()) && LPreferences.getSearchFilterByEditTIme()) {
                    s = ds = DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + ">=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "<?";
                    sa = dsa = new String[]{"" + DBHelper.STATE_ACTIVE, "" + startMs, "" + endMs};
                    sort = DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + " ASC";
                } else {
                    s = ds = DBHelper.TABLE_COLUMN_STATE + "=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + ">=? AND "
                            + DBHelper.TABLE_COLUMN_TIMESTAMP + "<?";
                    sa = dsa = new String[]{"" + DBHelper.STATE_ACTIVE, "" + startMs, "" + endMs};
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

            default:
                // An invalid id was passed in
                break;
        }
        return null;
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
            manager.restartLoader(loadId, null, this);
        } else {
            callbacks.onLoadFinished(loader, data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        callbacks.onLoaderReset(loader);
    }
}
