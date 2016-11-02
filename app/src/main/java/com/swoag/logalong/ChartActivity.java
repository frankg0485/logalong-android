package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LScheduledTransaction;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.fragments.ScheduledTransactionEdit;
import com.swoag.logalong.fragments.TransactionEdit;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBProvider;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ChartActivity extends LFragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_SUMMARY = 20;
    private ViewFlipper viewFlipper;
    private static int[] colors = new int[] {
            0xffcc0000,
            0xffff8a00,
            0xff669900,
            0xff9933cc,
            0xff0099cc,

            0xffe21d1d,
            0xffffa00e,
            0xff7caf00,
            0xffac59d6,
            0xff16a5d7,

            0xfff83a3a,
            0xffffb61c,
            0xff92c500,
            0xffc182e0,
            0xff2cb1e1,

            0xffff7979,
            0xffffd060,
            0xffb6db49,
            0xffcf9fe7,
            0xff6dcaec
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;
        switch (id) {
            case LOADER_SUMMARY:
                uri = DBProvider.URI_SCHEDULED_TRANSACTIONS;
                return new CursorLoader(this,
                        uri,
                        null,
                        DBHelper.TABLE_COLUMN_STATE + "=? OR " + DBHelper.TABLE_COLUMN_STATE + "=?",
                        new String[]{"" + DBHelper.STATE_ACTIVE, "" + DBHelper.STATE_DISABLED},
                        DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP + " ASC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_SUMMARY:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_SUMMARY:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.charts);

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        PieChart pieChart = (PieChart)findViewById(R.id.pieChart);

        pieChart.setCenterText("Category - 2016");
        pieChart.setDrawSlicesUnderHole(true);
        pieChart.setUsePercentValues(true);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(34.5f, "Fuel"));
        entries.add(new PieEntry(123.0f, "Food"));
        entries.add(new PieEntry(223.0f, "Kids"));
        entries.add(new PieEntry(23.0f, "Other"));
        entries.add(new PieEntry(29.0f, "Travel"));
        entries.add(new PieEntry(59.0f, "Entertainment"));

        entries.add(new PieEntry(34.5f, "Fuel"));
        entries.add(new PieEntry(123.0f, "Food"));
        entries.add(new PieEntry(223.0f, "Kids"));
        entries.add(new PieEntry(23.0f, "Other"));
        entries.add(new PieEntry(29.0f, "Travel"));
        entries.add(new PieEntry(59.0f, "Entertainment"));

        PieDataSet set = new PieDataSet(entries, "");
        set.setSliceSpace(2.0f);
        set.setSelectionShift(5.0f);

        //set.setColors(ColorTemplate.COLORFUL_COLORS);
        //set.setColors(ColorTemplate.JOYFUL_COLORS);
        //set.setColors(ColorTemplate.LIBERTY_COLORS);
        //set.setColors(ColorTemplate.VORDIPLOM_COLORS);
        //set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setColors(colors);

        PieData pieData = new PieData(set);
        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextColor(Color.WHITE);
        pieData.setValueTextSize(10.0f);
        pieChart.setData(pieData);

        Legend legend = pieChart.getLegend();
        legend.setPosition(Legend.LegendPosition.LEFT_OF_CHART);

        Description description = new Description();
        description.setText("");
        pieChart.setDescription(description);
        pieChart.invalidate();

        getSupportLoaderManager().restartLoader(LOADER_SUMMARY, null, this);
    }
}
