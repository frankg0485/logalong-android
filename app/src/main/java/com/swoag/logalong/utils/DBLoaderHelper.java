package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;

public class DBLoaderHelper implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = DBLoaderHelper.class.getSimpleName();

    public static final int LOADER_ALL_SUMMARY = 20;

    private Context context;
    private DBLoaderHelperCallbacks callbacks;
    private long startMs, endMs;
    private String selections = "";
    private ArrayList<String> selectionArgs = new ArrayList<String>();

    public interface DBLoaderHelperCallbacks {
        public void onLoadFinished(Loader<Cursor> loader, Cursor data);
        public void onLoaderReset(Loader<Cursor> loader);
    }

    public DBLoaderHelper (Context context, DBLoaderHelperCallbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
    }

    private long getMs(int year, int month) {
        Calendar now = Calendar.getInstance();
        now.clear();
        now.set(year, month, 1);
        return now.getTimeInMillis();
    }

    private void initStartEndMs () {
        if (LPreferences.getSearchAllTime()) {
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

        initStartEndMs();
        resetSelections();

        switch (id) {
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
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        callbacks.onLoadFinished(loader, data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        callbacks.onLoaderReset(loader);
    }
}
