package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LScheduledTransaction;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.fragments.ScheduledTransactionEdit;
import com.swoag.logalong.fragments.TransactionEdit;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBLoaderHelper;
import com.swoag.logalong.utils.DBProvider;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.TransactionSearchDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class ChartActivity extends LFragmentActivity implements
        DBLoaderHelper.DBLoaderHelperCallbacks, TransactionSearchDialog.TransactionSearchDialogItf {
    private static final String TAG = ChartActivity.class.getSimpleName();

    private static final int MAX_PIE_CHART_ITEMS = 12;
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
    private ImageView barPieIV, searchIV;
    private ImageView prevIV, nextIV;
    private ProgressBar progressBar;
    private BarChart barChart;
    private PieChart pieChart;
    private TextView entryDetailsTV;

    private DBLoaderHelper dbLoaderHelper;
    private boolean barChartDisplayed = false;
    private boolean pieChartDisplayed = false;
    private boolean loaderFinished = false;

    private class ChartData {
        int year;
        TreeMap<String, Double> expenseCatetories;
        double[] expenses;
        double[] incomes;
    }
    private HashMap<Integer, ChartData> chartDataHashMap;


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case DBLoaderHelper.LOADER_ALL_SUMMARY:
                new MyAsyncTask().execute(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onTransactionSearchDialogDismiss(boolean changed) {
        if (changed) {
            /*if (LPreferences.getSearchAllTime() && LPreferences.getSearchAll()) {
                LViewUtils.setAlpha(searchIV, 0.8f);
            } else {
                LViewUtils.setAlpha(searchIV, 1.0f);
            }*/

            AppPersistency.clearViewHistory();
            chartDataHashMap.clear();
            restartDbLoader();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.charts);

        myClickListener = new MyClickListener();
        findViewById(R.id.closeChart).setOnClickListener(myClickListener);

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);

        barChart = (BarChart)findViewById(R.id.barChart);
        barChart.setNoDataText("");
        pieChart = (PieChart)findViewById(R.id.pieChart);
        pieChart.setNoDataText("");
        entryDetailsTV = (TextView) findViewById(R.id.entryDetails);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        barPieIV = (ImageView)findViewById(R.id.barPieChart);
        barPieIV.setOnClickListener(myClickListener);

        searchIV = (ImageView) findViewById(R.id.search);
        searchIV.setOnClickListener(myClickListener);

        prevIV = (ImageView)findViewById(R.id.prev);
        nextIV = (ImageView)findViewById(R.id.next);
        //prevIV.setVisibility(View.INVISIBLE);
        //nextIV.setVisibility(View.INVISIBLE);
        prevIV.setOnClickListener(myClickListener);
        nextIV.setOnClickListener(myClickListener);

        dbLoaderHelper = new DBLoaderHelper(this, this);

        chartDataHashMap = new HashMap<Integer, ChartData>();
        restartDbLoader();
    }

    private void restartDbLoader() {
        ChartData chartData = chartDataHashMap.get(AppPersistency.viewTransactionYear);
        if (null == chartData) {
            loaderFinished = false;
            progressBar.setVisibility(View.VISIBLE);
            dbLoaderHelper.restart(getSupportLoaderManager(), DBLoaderHelper.LOADER_ALL_SUMMARY);
        } else {
            pieChartDisplayed = barChartDisplayed = false;
            showChart(chartData);
        }
    }

    private  void showPrevNext() {
        //nextIV.setVisibility((AppPersistency.viewTransactionYear < dbLoaderHelper.getEndYear())? View.VISIBLE : View.INVISIBLE);
        //prevIV.setVisibility((AppPersistency.viewTransactionYear > dbLoaderHelper.getStartYear())? View.VISIBLE : View.INVISIBLE);
        if (AppPersistency.viewTransactionYear < dbLoaderHelper.getStartYear() || AppPersistency.viewTransactionYear > dbLoaderHelper.getEndYear()) {
            AppPersistency.viewTransactionYear = dbLoaderHelper.getEndYear();
        }
        prevIV.setClickable(AppPersistency.viewTransactionYear > dbLoaderHelper.getStartYear());
        nextIV.setClickable(AppPersistency.viewTransactionYear < dbLoaderHelper.getEndYear());
    }

    private void showChart(ChartData chartData) {
        //viewFlipper.setAnimateFirstView(false);
        viewFlipper.setInAnimation(null);
        viewFlipper.setOutAnimation(null);
        //viewFlipper.setFlipInterval(0);
        if (AppPersistency.showPieChart) {
            barPieIV.setImageResource(R.drawable.chart_light);
            viewFlipper.setDisplayedChild(0);
            displayPieChart(chartData);
        } else {
            barPieIV.setImageResource(R.drawable.pie_chart_light);
            viewFlipper.setDisplayedChild(1);
            displayBarChart(chartData);
        }
        showPrevNext();
    }

    private void displayBarChart(ChartData chartData) {
        if (barChartDisplayed) return;
        barChartDisplayed = true;

        barChart.clear();
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
                if (((int) (months.length * percent) < 0) || (((int) (months.length * percent)) > 11))
                    return "";
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
        ArrayList<BarEntry> yVals2 = new ArrayList<BarEntry>();
        for (int ii = 0; ii < 12; ii++) {
            yVals1.add(new BarEntry(ii, (float)chartData.expenses[ii]));
            yVals2.add(new BarEntry(ii, (float)chartData.incomes[ii]));
        }

        BarDataSet dataSet1 = new BarDataSet(yVals1, "Expense");
        BarDataSet dataSet2 = new BarDataSet(yVals2, "Income - " + chartData.year);

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

    static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = -e1.getValue().compareTo(e2.getValue());
                        return res != 0 ? res : 1; // Special fix to preserve items with equal values
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    private List<PieEntry> pieEntries;
    private List<PieEntry> extraPieEntries;
    private int lastPieEntry;

    private void displayPieChart(ChartData chartData) {
        if (pieChartDisplayed) return;
        pieChartDisplayed = true;

        pieChart.clear();
        entryDetailsTV.setVisibility(View.GONE);

        pieChart.setCenterText("Expense - " + chartData.year);
        pieChart.setDrawSlicesUnderHole(true);
        pieChart.setUsePercentValues(true);

        pieEntries = new ArrayList<>();
        extraPieEntries = new ArrayList<>();
        lastPieEntry = chartData.expenseCatetories.size() - 1;
        if (chartData.expenseCatetories.size() <= MAX_PIE_CHART_ITEMS) {
            for (String key : chartData.expenseCatetories.keySet()) {
                pieEntries.add(new PieEntry(chartData.expenseCatetories.get(key).floatValue(), key));
            }
        } else {
            int count = 0;
            String lastGroup = null;
            double lastGroupValue = 0;

            List list = new ArrayList<String>();
            List groupList = new ArrayList<String>();
            for (Map.Entry<String, Double> entry : entriesSortedByValues(chartData.expenseCatetories)) {
                if (count < MAX_PIE_CHART_ITEMS - 1) {
                    list.add(entry.getKey());
                } else {
                    extraPieEntries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));

                    groupList.add(entry.getKey());
                    if (null == lastGroup) {
                        lastGroup = entry.getKey() + " ...";
                        lastPieEntry = count;
                    }
                    lastGroupValue += entry.getValue();
                }
                count++;
            }

            count = 0;
            for (String key : chartData.expenseCatetories.keySet()) {
                if (count < MAX_PIE_CHART_ITEMS - 1) {
                    if (list.contains(key)) {
                        pieEntries.add(new PieEntry(chartData.expenseCatetories.get(key).floatValue(), key));
                        count++;
                    }
                } else break;
            }
            pieEntries.add(new PieEntry((float) lastGroupValue, lastGroup));
        }

        PieDataSet set = new PieDataSet(pieEntries, "");
        set.setSliceSpace(2.0f);
        set.setSelectionShift(5.0f);
        set.setColors(colors);

        /*
        set.setValueLinePart1OffsetPercentage(80.f);
        set.setValueLinePart1Length(0.2f);
        set.setValueLinePart2Length(0.4f);
        //dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        */

        PieData pieData = new PieData(set);
        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextColor(Color.WHITE);
        pieData.setValueTextSize(10.0f);
        pieChart.setData(pieData);

        Legend legend = pieChart.getLegend();
        //legend.setPosition(Legend.LegendPosition.LEFT_OF_CHART); //deprecated
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setEnabled(true);

        Description description = new Description();
        description.setText("");
        pieChart.setDescription(description);

        pieChart.setOnChartValueSelectedListener(
                new OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(Entry e, Highlight h) {
                        PieEntry pe = (PieEntry)e;

                        int index = pieEntries.indexOf(pe);
                        entryDetailsTV.setTextColor(Color.WHITE);
                        entryDetailsTV.setBackgroundColor(colors[index]);

                        if (index == lastPieEntry && extraPieEntries.size() > 0) {
                            String str = "";
                            for (PieEntry entry : extraPieEntries) {
                                str += entry.getLabel() + " : $" + entry.getValue() + "\n";
                            }
                            entryDetailsTV.setText(str.trim());
                        } else {
                            entryDetailsTV.setText(pe.getLabel() + " : $" + pe.getValue());
                        }
                        entryDetailsTV.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onNothingSelected() {
                        entryDetailsTV.setVisibility(View.GONE);
                    }
                }

        );
        pieChart.invalidate();
    }

    private void enableBarPieChart() {
        AppPersistency.showPieChart = !AppPersistency.showPieChart;
        if (!AppPersistency.showPieChart) {
            barPieIV.setImageResource(R.drawable.pie_chart_light);
            displayBarChart(chartDataHashMap.get(AppPersistency.viewTransactionYear));

            viewFlipper.setInAnimation(this, R.anim.slide_in_left);
            viewFlipper.setOutAnimation(this, R.anim.slide_out_right);
            viewFlipper.showPrevious();
        } else {
            barPieIV.setImageResource(R.drawable.chart_light);
            displayPieChart(chartDataHashMap.get(AppPersistency.viewTransactionYear));

            viewFlipper.setInAnimation(this, R.anim.slide_in_right);
            viewFlipper.setOutAnimation(this, R.anim.slide_out_left);
            viewFlipper.showNext();
        }
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.closeChart:
                    finish();
                    break;

                case R.id.barPieChart:
                    if (loaderFinished) enableBarPieChart();
                    break;

                case R.id.search:
                    if (loaderFinished) {
                        TransactionSearchDialog dialog = new TransactionSearchDialog(ChartActivity.this, ChartActivity.this);
                        dialog.show();
                    }
                    break;

                case R.id.prev:
                    if (loaderFinished && AppPersistency.viewTransactionYear > dbLoaderHelper.getStartYear()) {
                        AppPersistency.viewTransactionYear--;
                        restartDbLoader();
                    }
                    break;
                case R.id.next:
                    if (loaderFinished && AppPersistency.viewTransactionYear < dbLoaderHelper.getEndYear()) {
                        AppPersistency.viewTransactionYear++;
                        restartDbLoader();
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private class MyAsyncTask extends AsyncTask<Cursor, Void, Boolean> {
        private TreeMap<String, Double> expenseCatetories = new TreeMap<String, Double>();
        private double[] expenses = new double[12];
        private double[] incomes = new double[12];
        private int year = AppPersistency.viewTransactionYear;

        @Override
        protected Boolean doInBackground(Cursor... params) {
            Cursor data = params[0];
            if (data == null) return false;

            //prepare chart data
            Set<Integer> accountIds = new HashSet<Integer>();
            int accountId, accountId2;
            data.moveToFirst();
            do {
                accountId = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT));
                if (accountId != 0) accountIds.add(accountId);

                accountId2 = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT2));
                if (accountId2 != 0) accountIds.add(accountId2);
            } while (data.moveToNext());

            int categoryId;
            double v = 0;
            long timeMs = 0;
            Calendar calendar = Calendar.getInstance();
            data.moveToFirst();
            do {
                v = data.getDouble(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                accountId = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT));
                accountId2 = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT2));
                categoryId = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CATEGORY));
                timeMs = data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP));
                calendar.setTimeInMillis(timeMs);

                int type = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                switch (type) {
                    case LTransaction.TRANSACTION_TYPE_TRANSFER:
                    case LTransaction.TRANSACTION_TYPE_TRANSFER_COPY:
                        if (accountIds.contains(accountId) && accountIds.contains(accountId2)) {
                            v = 0;
                        } else {
                            if (LTransaction.TRANSACTION_TYPE_TRANSFER == type) {
                                v = -v;
                            }
                        }
                        break;
                    case LTransaction.TRANSACTION_TYPE_EXPENSE:
                        v = -v;
                        break;
                    case LTransaction.TRANSACTION_TYPE_INCOME:
                        break;
                }

                if (v < 0) {
                    String category = DBCategory.getNameById(categoryId);
                    if (category == null || TextUtils.isEmpty(category)) {
                        category = "Unspecified";
                    } else {
                        category = category.split(":", -1)[0];
                    }

                    Double sum = expenseCatetories.get(category);
                    if (sum == null) {
                        expenseCatetories.put(category, -v);
                    } else {
                        sum += -v;
                        expenseCatetories.put(category, sum);
                    }

                    expenses[calendar.get(Calendar.MONTH)] += -v;
                } else {
                    incomes[calendar.get(Calendar.MONTH)] += v;
                }
            } while (data.moveToNext());

            if (year != calendar.get(Calendar.YEAR)) {
                LLog.w(TAG, "unexpected year code mismatch: " + year + " DB: " + calendar.get(Calendar.YEAR));
                AppPersistency.viewTransactionYear = year = calendar.get(Calendar.YEAR);
            };

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            ChartData chartData = new ChartData();
            chartData.year = year;
            chartData.expenseCatetories = expenseCatetories;
            chartData.expenses = expenses;
            chartData.incomes = incomes;
            chartDataHashMap.put(year, chartData);

            loaderFinished = true;
            progressBar.setVisibility(View.GONE);

            pieChartDisplayed = barChartDisplayed = false;
            showChart(chartData);
        }

        @Override
        protected void onPreExecute() {
        }
    }
}
