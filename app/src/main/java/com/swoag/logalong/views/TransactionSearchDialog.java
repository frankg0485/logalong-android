package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

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
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;

public class TransactionSearchDialog extends Dialog implements
        DialogInterface.OnDismissListener, DatePickerDialog.OnDateSetListener {

    private Context context;
    private View filterCheckView, filterView, timeCheckView, timeView;
    private TextView accountV, categoryV, vendorV, tagV, fromV, toV, filterByV;
    private TransactionSearchDialogItf callback;
    private MyClickListener myClickListener;
    private CheckBox checkBox, checkBoxTime;
    private boolean showAll, allTime, showAllOrig, allTimeOrig;

    private LMultiSelectionDialog.MultiSelectionDialogItf accountSelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf categorySelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf vendorSelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf tagSelectionDlgItf;

    public interface TransactionSearchDialogItf {
        public void onTransactionSearchDialogDismiss(boolean changed);
    }

    public TransactionSearchDialog(Context parent, TransactionSearchDialogItf callback) {
        super(parent, android.R.style.Theme_Translucent_NoTitleBar);
        this.context = parent;
        this.callback = callback;
        myClickListener = new MyClickListener();

        allTimeOrig = LPreferences.getSearchAllTime();
        showAllOrig = LPreferences.getSearchAll();

        accountSelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return DBAccount.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            }

            @Override
            public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections, boolean allSelected) {
                if (selections != null) {
                    boolean ok = false;
                    long[] accounts = null;
                    if (!allSelected && selections.size() > 0) {
                        accounts = new long[selections.size()];
                        int ii = 0;
                        for (Long ll : selections) {
                            LAccount account = DBAccount.getById(ll);
                            if ((account != null) && (account.getState() == DBHelper.STATE_ACTIVE)) {
                                accounts[ii++] = ll;
                                ok = true;
                            }
                        }
                    }
                    LPreferences.setSearchAccounts(ok? accounts : null);
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
                    boolean ok = false;
                    long[] categories = null;
                    if (!allSelected && selections.size() > 0) {
                        categories = new long[selections.size()];
                        int ii = 0;
                        for (Long ll : selections) {
                            LCategory category = DBCategory.getById(ll);
                            if ((category != null) && (category.getState() == DBHelper.STATE_ACTIVE)) {
                                categories[ii++] = ll;
                                ok = true;
                            }
                        }
                    }
                    LPreferences.setSearchCategories(ok? categories : null);
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
                    boolean ok = false;
                    long[] vendors = null;
                    if (!allSelected && selections.size() > 0) {
                        vendors = new long[selections.size()];
                        int ii = 0;
                        for (Long ll : selections) {
                            LVendor vendor = DBVendor.getById(ll);
                            if ((vendor != null) && (vendor.getState() == DBHelper.STATE_ACTIVE)) {
                                vendors[ii++] = ll;
                                ok = true;
                            }
                        }
                    }
                    LPreferences.setSearchVendors(ok? vendors : null);
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
                    boolean ok = false;
                    long[] tags = null;
                    if (!allSelected && selections.size() > 0) {
                        tags = new long[selections.size()];
                        int ii = 0;
                        for (Long ll : selections) {
                            LTag tag = DBTag.getById(ll);
                            if ((tag != null) && (tag.getState() == DBHelper.STATE_ACTIVE)) {
                                tags[ii++] = ll;
                                ok = true;
                            }
                        }
                    }
                    LPreferences.setSearchTags(ok? tags : null);
                }
                displayTags();
            }
        };
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (LPreferences.getSearchAllTimeFrom() > LPreferences.getSearchAllTimeTo()) {
            LPreferences.setSearchAllTimeFrom(0);
            LPreferences.setSearchAllTimeTo(0);
        }

        callback.onTransactionSearchDialogDismiss(!(allTimeOrig && showAllOrig && allTime && showAll));
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
        findViewById(R.id.filterBy).setOnClickListener(myClickListener);

        filterView = findViewById(R.id.customFilter);
        accountV = (TextView) findViewById(R.id.selectedAccounts);
        categoryV = (TextView) findViewById(R.id.selectedCategories);
        vendorV = (TextView) findViewById(R.id.selectedPayers);
        tagV = (TextView) findViewById(R.id.selectedTags);

        timeView = findViewById(R.id.customTime);
        fromV = (TextView) findViewById(R.id.fromTime);
        toV = (TextView) findViewById(R.id.toTime);
        filterByV = (TextView) findViewById(R.id.filterByValue);

        filterCheckView = findViewById(R.id.checkboxView);
        filterCheckView.setOnClickListener(myClickListener);
        checkBox = (CheckBox) findViewById(R.id.checkboxAll);
        showAll = LPreferences.getSearchAll();

        timeCheckView = findViewById(R.id.checkboxViewTime);
        timeCheckView.setOnClickListener(myClickListener);
        checkBoxTime = (CheckBox) findViewById(R.id.checkboxAllTime);
        allTime = LPreferences.getSearchAllTime();

        displayUpdateFilter(showAll);

        if (LPreferences.getSearchAllTimeFrom() > LPreferences.getSearchAllTimeTo()) {
            LPreferences.setSearchAllTimeFrom(0);
            LPreferences.setSearchAllTimeTo(0);
        }
        displayUpdateTime(allTime);
        displayFilterBy();

        this.setOnDismissListener(this);
    }

    private void displayAccounts() {
        long vals[] = LPreferences.getSearchAccounts();
        String str = "";
        boolean ok = false;
        if (null != vals) {
            for (int ii = 0; ii < vals.length; ii++) {
                LAccount account = DBAccount.getById(vals[ii]);
                if ((account != null) && (account.getState() == DBHelper.STATE_ACTIVE)) {
                    str += DBAccount.getNameById(vals[ii]) + ", ";
                    ok = true;
                }
            }
        }
        if (ok) {
            str = str.replaceAll(", $", "");
            accountV.setText(str);
        } else {
            LPreferences.setSearchAccounts(null);
            accountV.setText(context.getString(R.string.all));
        }
    }


    private void displayCategories() {
        long vals[] = LPreferences.getSearchCategories();
        String str = "";
        boolean ok = false;
        if (null != vals) {
            for (int ii = 0; ii < vals.length; ii++) {
                LCategory category = DBCategory.getById(vals[ii]);
                if ((category != null) && (category.getState() == DBHelper.STATE_ACTIVE)) {
                    str += DBCategory.getNameById(vals[ii]) + ", ";
                    ok = true;
                }
            }
        }
        if (ok) {
            str = str.replaceAll(", $", "");
            categoryV.setText(str);
        } else {
            LPreferences.setSearchCategories(null);
            categoryV.setText(context.getString(R.string.all));
        }
    }

    private void displayVendors() {
        long vals[] = LPreferences.getSearchVendors();
        String str = "";
        boolean ok = false;
        if (null != vals) {
            for (int ii = 0; ii < vals.length; ii++) {
                LVendor vendor = DBVendor.getById(vals[ii]);
                if ((vendor != null) && (vendor.getState() == DBHelper.STATE_ACTIVE)) {
                    str += DBVendor.getNameById(vals[ii]) + ", ";
                    ok = true;
                }
            }
        }
        if (ok) {
            str = str.replaceAll(", $", "");
            vendorV.setText(str);
        } else {
            LPreferences.setSearchVendors(null);
            vendorV.setText(context.getString(R.string.all));
        }
    }

    private void displayTags() {
        long vals[] = LPreferences.getSearchTags();
        String str = "";
        boolean ok = false;
        if (null != vals) {
            for (int ii = 0; ii < vals.length; ii++) {
                LTag tag = DBTag.getById(vals[ii]);
                if ((tag != null) && (tag.getState() == DBHelper.STATE_ACTIVE)) {
                    str += DBTag.getNameById(vals[ii]) + ", ";
                    ok = true;
                }
            }
        }
        if (ok) {
            str = str.replaceAll(", $", "");
            tagV.setText(str);
        } else {
            LPreferences.setSearchTags(null);
            tagV.setText(context.getString(R.string.all));
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

    private void displayFilterBy() {
        filterByV.setText(context.getString
                (LPreferences.getSearchFilterByEditTIme() ? R.string.edit_time : R.string.record_time));
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
                        for (long ll : accounts) {
                            selectedIds.add(ll);
                        }
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

                case R.id.filterBy:
                    LPreferences.setSearchFilterByEditTime(!LPreferences.getSearchFilterByEditTIme());
                    displayFilterBy();
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
        calendar.clear();
        calendar.set(year, monthOfYear, dayOfMonth);
        TextView tv = fromV;

        if (setFromTime) {
            LPreferences.setSearchAllTimeFrom(calendar.getTimeInMillis());
        } else {
            tv = toV;
            LPreferences.setSearchAllTimeTo(calendar.getTimeInMillis() + (long) 24 * 3600 * 1000 - 1);
        }

        tv.setText(new SimpleDateFormat("MMM d, yyy").format(calendar.getTimeInMillis()));
    }
}
