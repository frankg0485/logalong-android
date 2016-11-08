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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
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

    private MyClickListener myClickListener;
    private ImageView barPieIV;
    private boolean barPie = false;

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

        myClickListener = new MyClickListener();

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        barPieIV = (ImageView)findViewById(R.id.barPieChart);
        barPieIV.setOnClickListener(myClickListener);

        displayPieChart();

        getSupportLoaderManager().restartLoader(LOADER_SUMMARY, null, this);
    }

    private void displayBarChart() {
        BarChart barChart = (BarChart)findViewById(R.id.barChart);

        barChart.getDescription().setEnabled(false);

        // scaling can now only be done on x- and y-axis separately
        barChart.setPinchZoom(false);
        //barChart.setTouchEnabled(false);
        barChart.setScaleXEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setHighlightPerTapEnabled(false);
        barChart.setHighlightPerDragEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        //xAxis.setLabelRotationAngle(15f);
        xAxis.setLabelCount(13, true);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            private String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                float percent = value / axis.mAxisRange;
                if (percent < 0f || value < 0f) return "";
                if (((int) (months.length * percent) < 0) || (((int) (months.length * percent)) > 11)) return "";
                return months[(int) (months.length * percent)];
            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        });

        barChart.getAxisLeft().setDrawGridLines(false);

        barChart.setDrawBarShadow(false);
        barChart.setDrawGridBackground(false);

        Legend l = barChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(true);

        ArrayList<BarEntry> yVals1 = new ArrayList<BarEntry>();
        for (int i = 0; i < 12; i++) {
            float mult = i + 100;
            float val = (float) (Math.random() * mult) + mult / 3;
            yVals1.add(new BarEntry(i, val));
        }
        ArrayList<BarEntry> yVals2 = new ArrayList<BarEntry>();
        for (int i = 0; i < 12; i++) {
            float mult = i + 100;
            float val = (float) (Math.random() * mult) + mult / 3;
            yVals2.add(new BarEntry(i, val));
        }


        BarDataSet dataSet1 = new BarDataSet(yVals1, "Expense");
        BarDataSet dataSet2 = new BarDataSet(yVals2, "Income");

        //dataSet1.setColors(colors);
        dataSet1.setColor(0xffcc0000);
        dataSet1.setDrawValues(false);

        //dataSet2.setColors(colors);
        dataSet2.setColor(0xff669900);
        dataSet2.setDrawValues(false);


        ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
        dataSets.add(dataSet1);
        dataSets.add(dataSet2);

        BarData barData = new BarData(dataSets);
        barChart.setData(barData);

        float groupSpace = 0.25f;
        float barSpace = 0f; // x2 DataSet
        float barWidth = 0.375f; // x2 DataSet
        // (barSpace + barWidth) * 2 + groupSpace = 1.00 -> interval per "group"
        barChart.getBarData().setBarWidth(barWidth);

        barChart.getXAxis().setAxisMinimum(0);
        barChart.getXAxis().setAxisMaximum(12);
        barChart.groupBars(0f, groupSpace, barSpace);

        barChart.invalidate();
    }

    private void displayPieChart() {
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
    }

    private void enableBarPieChart() {
        barPie = !barPie;

        if (barPie) {
            barPieIV.setImageResource(R.drawable.pie_chart_light);
            displayBarChart();

            viewFlipper.setInAnimation(this, R.anim.slide_in_left);
            viewFlipper.setOutAnimation(this, R.anim.slide_out_right);
            viewFlipper.showPrevious();
        } else {
            barPieIV.setImageResource(R.drawable.chart_light);

            viewFlipper.setInAnimation(this, R.anim.slide_in_right);
            viewFlipper.setOutAnimation(this, R.anim.slide_out_left);
            viewFlipper.showNext();
        }
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.barPieChart:
                    enableBarPieChart();
                    break;
                default:
                    break;
            }
        }
    }

}
