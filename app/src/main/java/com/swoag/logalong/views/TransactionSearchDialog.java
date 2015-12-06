package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.fragments.TransactionEdit;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

public class TransactionSearchDialog extends Dialog implements
        DialogInterface.OnDismissListener, DatePickerDialog.OnDateSetListener {

    private Context context;
    private View filterCheckView, filterView, timeCheckView, timeView;
    private TextView accountV, categoryV, vendorV, tagV, fromV, toV;
    private TransactionSearchDialogItf callback;
    private MyClickListener myClickListener;
    private CheckBox checkBox, checkBoxTime;
    private boolean showAll, allTime;

    private LMultiSelectionDialog.MultiSelectionDialogItf accountSelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf categorySelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf vendorSelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf tagSelectionDlgItf;

    public interface TransactionSearchDialogItf {
        public void onTransactionSearchDialogDismiss();
    }

    public TransactionSearchDialog(Context parent, TransactionSearchDialogItf callback) {
        super(parent, android.R.style.Theme_Translucent_NoTitleBar);
        this.context = parent;
        this.callback = callback;
        myClickListener = new MyClickListener();

        accountSelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return DBAccount.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            }

            @Override
            public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections, boolean allSelected) {
                if (selections != null) {
                    long[] accounts = null;
                    if (!allSelected && selections.size() > 0) {
                        accounts = new long[selections.size()];
                        int ii = 0;
                        for (Long ll : selections) {
                            accounts[ii++] = ll;
                        }
                    }
                    LPreferences.setSearchAccounts(accounts);
                }
                displayAccounts();
            }
        };

        categorySelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return DBCategory.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            }

            @Override
            public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections, boolean allSelected) {
                if (selections != null) {
                    long[] categories = null;
                    if (!allSelected && selections.size() > 0) {
                        categories = new long[selections.size()];
                        int ii = 0;
                        for (Long ll : selections) {
                            categories[ii++] = ll;
                        }
                    }
                    LPreferences.setSearchCategories(categories);
                }
                displayCategories();
            }
        };

        vendorSelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return DBVendor.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            }

            @Override
            public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections, boolean allSelected) {
                if (selections != null) {
                    long[] vendors = null;
                    if (!allSelected && selections.size() > 0) {
                        vendors = new long[selections.size()];
                        int ii = 0;
                        for (Long ll : selections) {
                            vendors[ii++] = ll;
                        }
                    }
                    LPreferences.setSearchVendors(vendors);
                }
                displayVendors();
            }
        };

        tagSelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return DBTag.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            }

            @Override
            public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections, boolean allSelected) {
                if (selections != null) {
                    long[] tags = null;
                    if (!allSelected && selections.size() > 0) {
                        tags = new long[selections.size()];
                        int ii = 0;
                        for (Long ll : selections) {
                            tags[ii++] = ll;
                        }
                    }
                    LPreferences.setSearchTags(tags);
                }
                displayTags();
            }
        };
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (LPreferences.getSearchAllTimeFrom() >= LPreferences.getSearchAllTimeTo()) {
            LPreferences.setSearchAllTimeFrom(0);
            LPreferences.setSearchAllTimeTo(0);
        }

        callback.onTransactionSearchDialogDismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.transaction_search_dialog);

        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);
        findViewById(R.id.accounts).setOnClickListener(myClickListener);
        findViewById(R.id.categories).setOnClickListener(myClickListener);
        findViewById(R.id.vendors).setOnClickListener(myClickListener);
        findViewById(R.id.tags).setOnClickListener(myClickListener);

        findViewById(R.id.fromTime).setOnClickListener(myClickListener);
        findViewById(R.id.toTime).setOnClickListener(myClickListener);

        filterView = findViewById(R.id.customFilter);
        accountV = (TextView) findViewById(R.id.selectedAccounts);
        categoryV = (TextView) findViewById(R.id.selectedCategories);
        vendorV = (TextView) findViewById(R.id.selectedPayers);
        tagV = (TextView) findViewById(R.id.selectedTags);

        timeView = findViewById(R.id.customTime);
        fromV = (TextView) findViewById(R.id.fromTime);
        toV = (TextView) findViewById(R.id.toTime);

        filterCheckView = findViewById(R.id.checkboxView);
        filterCheckView.setOnClickListener(myClickListener);
        checkBox = (CheckBox) findViewById(R.id.checkboxAll);
        showAll = LPreferences.getSearchAll();

        timeCheckView = findViewById(R.id.checkboxViewTime);
        timeCheckView.setOnClickListener(myClickListener);
        checkBoxTime = (CheckBox) findViewById(R.id.checkboxAllTime);
        allTime = LPreferences.getSearchAllTime();

        displayUpdateFilter(showAll);

        if (LPreferences.getSearchAllTimeFrom() >= LPreferences.getSearchAllTimeTo()) {
            LPreferences.setSearchAllTimeFrom(0);
            LPreferences.setSearchAllTimeTo(0);
        }
        displayUpdateTime(allTime);

        this.setOnDismissListener(this);
    }

    private void displayAccounts() {
        long vals[] = LPreferences.getSearchAccounts();
        if (null == vals) {
            accountV.setText(context.getString(R.string.all));
        } else {
            String str = "";
            for (int ii = 0; ii < vals.length - 1; ii++) {
                str += DBAccount.getNameById(vals[ii]) + ", ";
            }
            str += DBAccount.getNameById(vals[vals.length - 1]);
            accountV.setText(str);
        }
    }

    private void displayCategories() {
        long vals[] = LPreferences.getSearchCategories();
        if (null == vals) {
            categoryV.setText(context.getString(R.string.all));
        } else {
            String str = "";
            for (int ii = 0; ii < vals.length - 1; ii++) {
                str += DBCategory.getNameById(vals[ii]) + ", ";
            }
            str += DBCategory.getNameById(vals[vals.length - 1]);
            categoryV.setText(str);
        }
    }

    private void displayVendors() {
        long vals[] = LPreferences.getSearchVendors();
        if (null == vals) {
            vendorV.setText(context.getString(R.string.all));
        } else {
            String str = "";
            for (int ii = 0; ii < vals.length - 1; ii++) {
                str += DBVendor.getNameById(vals[ii]) + ", ";
            }
            str += DBVendor.getNameById(vals[vals.length - 1]);
            vendorV.setText(str);
        }
    }

    private void displayTags() {
        long vals[] = LPreferences.getSearchTags();
        if (null == vals) {
            tagV.setText(context.getString(R.string.all));
        } else {
            String str = "";
            for (int ii = 0; ii < vals.length - 1; ii++) {
                str += DBTag.getNameById(vals[ii]) + ", ";
            }
            str += DBTag.getNameById(vals[vals.length - 1]);
            tagV.setText(str);
        }
    }

    private void displayUpdateFilter(boolean all) {
        checkBox.setChecked(all);
        LViewUtils.disableEnableControls(!all, (ViewGroup) filterView);
        if (all) {
            //LViewUtils.setAlpha(filterView, 0.8f);
            filterView.setVisibility(View.GONE);
            LViewUtils.setAlpha(filterCheckView, 1.0f);
        } else {
            //LViewUtils.setAlpha(filterView, 1.0f);
            filterView.setVisibility(View.VISIBLE);
            LViewUtils.setAlpha(filterCheckView, 0.8f);
        }
        displayAccounts();
        displayCategories();
        displayVendors();
        displayTags();
    }

    private void displayUpdateTime(boolean all) {
        checkBoxTime.setChecked(all);
        LViewUtils.disableEnableControls(!all, (ViewGroup) timeView);
        if (all) {
            //LViewUtils.setAlpha(timeView, 0.8f);
            timeView.setVisibility(View.GONE);
            LViewUtils.setAlpha(timeCheckView, 1.0f);
        } else {
            //LViewUtils.setAlpha(timeView, 1.0f);
            timeView.setVisibility(View.VISIBLE);
            LViewUtils.setAlpha(timeCheckView, 0.8f);
        }
        fromV.setText(new SimpleDateFormat("MMM d, yyy").format(LPreferences.getSearchAllTimeFrom()));
        toV.setText(new SimpleDateFormat("MMM d, yyy").format(LPreferences.getSearchAllTimeTo()));
    }

    private class MyClickListener extends LOnClickListener {
        int[] ids = new int[]{
                R.layout.multi_selection_dialog,
                R.layout.multi_selection_item,
                R.id.title,
                R.id.save,
                R.id.cancel,
                R.id.selectall,
                R.id.checkBox1,
                R.id.name,
                R.id.list,
                0};
        String[] columns = new String[]{
                DBHelper.TABLE_COLUMN_NAME
        };
        HashSet<Long> selectedIds = new HashSet<Long>();
        LMultiSelectionDialog dialog;

        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.checkboxView:
                    showAll = !showAll;
                    LPreferences.setSearchAll(showAll);
                    displayUpdateFilter(showAll);
                    return;

                case R.id.checkboxViewTime:
                    allTime = !allTime;
                    LPreferences.setSearchAllTime(allTime);
                    displayUpdateTime(allTime);
                    return;

                case R.id.accounts:
                    ids[9] = R.string.select_accounts;

                    long[] accounts = LPreferences.getSearchAccounts();
                    selectedIds.clear();
                    if (accounts != null) {
                        for (long ll : accounts) selectedIds.add(ll);
                    }

                    dialog = new LMultiSelectionDialog
                            (context, context, selectedIds, accountSelectionDlgItf, ids, columns);
                    dialog.show();
                    return;

                case R.id.categories:
                    ids[9] = R.string.select_categories;

                    long[] categories = LPreferences.getSearchCategories();
                    selectedIds.clear();
                    if (categories != null) {
                        for (long ll : categories) selectedIds.add(ll);
                    }

                    dialog = new LMultiSelectionDialog
                            (context, context, selectedIds, categorySelectionDlgItf, ids, columns);
                    dialog.show();
                    return;

                case R.id.vendors:
                    ids[9] = R.string.select_vendors;

                    long[] vendors = LPreferences.getSearchVendors();
                    selectedIds.clear();
                    if (vendors != null) {
                        for (long ll : vendors) selectedIds.add(ll);
                    }

                    dialog = new LMultiSelectionDialog
                            (context, context, selectedIds, vendorSelectionDlgItf, ids, columns);
                    dialog.show();
                    return;

                case R.id.tags:
                    ids[9] = R.string.select_tags;

                    long[] tags = LPreferences.getSearchTags();
                    selectedIds.clear();
                    if (tags != null) {
                        for (long ll : tags) selectedIds.add(ll);
                    }

                    dialog = new LMultiSelectionDialog
                            (context, context, selectedIds, tagSelectionDlgItf, ids, columns);
                    dialog.show();
                    return;

                case R.id.fromTime:
                    showDatePicker(true);
                    return;

                case R.id.toTime:
                    showDatePicker(false);
                    return;

                case R.id.closeDialog:
                default:
                    break;
            }
            dismiss();
        }
    }

    boolean setFromTime;

    private void showDatePicker(boolean from) {
        setFromTime = from;

        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(from ? LPreferences.getSearchAllTimeFrom() : LPreferences.getSearchAllTimeTo());

        DatePickerDialog datePickerDialog = new DatePickerDialog(context, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                TransactionSearchDialog.this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);
        TextView tv = fromV;

        if (setFromTime) {
            LPreferences.setSearchAllTimeFrom(calendar.getTimeInMillis());
        } else {
            tv = toV;
            LPreferences.setSearchAllTimeTo(calendar.getTimeInMillis());
        }

        tv.setText(new SimpleDateFormat("MMM d, yyy").format(calendar.getTimeInMillis()));
    }
}