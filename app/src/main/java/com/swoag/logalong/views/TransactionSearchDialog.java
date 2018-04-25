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
import com.swoag.logalong.entities.LSearch;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

public class TransactionSearchDialog extends Dialog implements
        DialogInterface.OnDismissListener, DatePickerDialog.OnDateSetListener, LDollarAmountPickerView
        .LDollarAmountPickerViewItf {

    private Context context;
    private View filterCheckView, filterView, timeCheckView, timeView, valueCheckView, valueView;
    private TextView accountV, categoryV, vendorV, tagV, typeV, fromV, toV, fromValueV, toValueV, filterByV;
    private TransactionSearchDialogItf callback;
    private MyClickListener myClickListener;
    private CheckBox checkBox, checkBoxTime, checkboxByValue, checkBoxAccounts, checkBoxCategories, checkBoxPayers,
            checkBoxTags, checkBoxTypes;
    private LSearch searchOrig;
    private LSearch search;
    private View rootView, pickerV;
    private LDollarAmountPickerView picker;

    private LMultiSelectionDialog.MultiSelectionDialogItf accountSelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf categorySelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf vendorSelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf tagSelectionDlgItf;
    private LMultiSelectionDialog.MultiSelectionDialogItf typeSelectionDlgItf;

    public interface TransactionSearchDialogItf {
        public void onTransactionSearchDialogDismiss(boolean changed);
    }

    public TransactionSearchDialog(Context parent, TransactionSearchDialogItf callback) {
        super(parent, android.R.style.Theme_Translucent_NoTitleBar);
        this.context = parent;
        this.callback = callback;
        myClickListener = new MyClickListener();

        searchOrig = LPreferences.getSearchControls();
        search = new LSearch(searchOrig);

        accountSelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return DBAccount.getInstance().getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            }

            @Override
            public ArrayList<String> onMultiSelectionGetStrings() {
                return null;
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
                            LAccount account = DBAccount.getInstance().getById(ll);
                            if ((account != null) && (account.getState() == DBHelper.STATE_ACTIVE)) {
                                accounts[ii++] = ll;
                                ok = true;
                            }
                        }
                    }
                    search.setAccounts(ok ? accounts : null);
                    LPreferences.setSearchControls(search);
                }
                displayAccounts();
            }
        };

        categorySelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return DBCategory.getInstance().getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            }

            @Override
            public ArrayList<String> onMultiSelectionGetStrings() {
                return null;
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
                            LCategory category = DBCategory.getInstance().getById(ll);
                            if ((category != null) && (category.getState() == DBHelper.STATE_ACTIVE)) {
                                categories[ii++] = ll;
                                ok = true;
                            }
                        }
                    }
                    search.setCategories(ok ? categories : null);
                    LPreferences.setSearchControls(search);
                }
                displayCategories();
            }
        };

        vendorSelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return DBVendor.getInstance().getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            }

            @Override
            public ArrayList<String> onMultiSelectionGetStrings() {
                return null;
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
                            LVendor vendor = DBVendor.getInstance().getById(ll);
                            if ((vendor != null) && (vendor.getState() == DBHelper.STATE_ACTIVE)) {
                                vendors[ii++] = ll;
                                ok = true;
                            }
                        }
                    }
                    search.setVendors(ok ? vendors : null);
                    LPreferences.setSearchControls(search);
                }
                displayVendors();
            }
        };

        tagSelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return DBTag.getInstance().getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            }

            @Override
            public ArrayList<String> onMultiSelectionGetStrings() {
                return null;
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
                            LTag tag = DBTag.getInstance().getById(ll);
                            if ((tag != null) && (tag.getState() == DBHelper.STATE_ACTIVE)) {
                                tags[ii++] = ll;
                                ok = true;
                            }
                        }
                    }
                    search.setTags(ok ? tags : null);
                    LPreferences.setSearchControls(search);
                }
                displayTags();
            }
        };

        typeSelectionDlgItf = new LMultiSelectionDialog.MultiSelectionDialogItf() {
            @Override
            public Cursor onMultiSelectionGetCursor(String column) {
                return null;
            }

            @Override
            public ArrayList<String> onMultiSelectionGetStrings() {
                ArrayList<String> list = new ArrayList<>();
                list.add(context.getString(R.string.expense));
                list.add(context.getString(R.string.income));
                list.add(context.getString(R.string.transfer));
                return list;
            }

            @Override
            public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections, boolean allSelected) {
                if (selections != null) {
                    boolean ok = false;
                    long[] types = null;
                    if (!allSelected && selections.size() > 0) {
                        types = new long[selections.size()];
                        int ii = 0;
                        for (Long ll : selections) {
                            types[ii++] = ll;
                            ok = true;
                        }
                    }
                    search.setTypes(ok ? types : null);
                    LPreferences.setSearchControls(search);
                }
                displayTypes();
            }
        };
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (search.getTimeFrom() > search.getTimeTo()) {
            long time = search.getTimeFrom();
            search.setTimeFrom(search.getTimeTo());
            search.setTimeTo(time);
            LPreferences.setSearchControls(search);
        }
        if (search.getValueFrom() > search.getValueTo() && search.getValueTo() != 0) {
            float value = search.getValueFrom();
            search.setValueFrom(search.getValueTo());
            search.setValueTo(value);
            LPreferences.setSearchControls(search);
        }

        if ((search.isbAccounts() && search.getAccounts() != null) ||
                (search.isbCategories() && search.getCategories() != null) ||
                (search.isbVendors() && search.getVendors() != null) ||
                (search.isbTags() && search.getTags() != null) ||
                (search.isbTypes() && search.getTypes() != null)) {
        } else {
            search.setbShowAll(true);
            LPreferences.setSearchControls(search);
        }

        if (search.getValueFrom() < 0.01 && search.getValueTo() < 0.01 ) {
            search.setbAllValue(true);
            LPreferences.setSearchControls(search);
        }

        // for time and filter, treat it as change as long as it was/is not all default values.
        boolean changed = !search.isEqual(searchOrig);
        callback.onTransactionSearchDialogDismiss(changed);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.transaction_search_dialog);
        rootView = findViewById(android.R.id.content);

        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);
        findViewById(R.id.selectedAccounts).setOnClickListener(myClickListener);
        findViewById(R.id.selectedCategories).setOnClickListener(myClickListener);
        findViewById(R.id.selectedPayers).setOnClickListener(myClickListener);
        findViewById(R.id.selectedTags).setOnClickListener(myClickListener);
        findViewById(R.id.selectedTypes).setOnClickListener(myClickListener);

        checkBoxAccounts = (CheckBox) findViewById(R.id.checkboxAccounts);
        checkBoxAccounts.setOnClickListener(myClickListener);
        checkBoxCategories = (CheckBox) findViewById(R.id.checkboxCategories);
        checkBoxCategories.setOnClickListener(myClickListener);
        checkBoxPayers = (CheckBox) findViewById(R.id.checkboxPayers);
        checkBoxPayers.setOnClickListener(myClickListener);
        checkBoxTags = (CheckBox) findViewById(R.id.checkboxTags);
        checkBoxTags.setOnClickListener(myClickListener);
        checkBoxTypes = (CheckBox) findViewById(R.id.checkboxTypes);
        checkBoxTypes.setOnClickListener(myClickListener);

        findViewById(R.id.fromTime).setOnClickListener(myClickListener);
        findViewById(R.id.toTime).setOnClickListener(myClickListener);
        findViewById(R.id.filterBy).setOnClickListener(myClickListener);

        filterView = findViewById(R.id.customFilter);
        accountV = (TextView) findViewById(R.id.selectedAccounts);
        categoryV = (TextView) findViewById(R.id.selectedCategories);
        vendorV = (TextView) findViewById(R.id.selectedPayers);
        tagV = (TextView) findViewById(R.id.selectedTags);
        typeV = (TextView) findViewById(R.id.selectedTypes);

        timeView = findViewById(R.id.customTime);
        fromV = (TextView) findViewById(R.id.fromTime);
        toV = (TextView) findViewById(R.id.toTime);
        filterByV = (TextView) findViewById(R.id.filterByValue);

        valueView = findViewById(R.id.customValue);
        fromValueV = (TextView) findViewById(R.id.fromValue);
        toValueV = (TextView) findViewById(R.id.toValue);

        filterCheckView = findViewById(R.id.checkboxView);
        filterCheckView.setOnClickListener(myClickListener);
        checkBox = (CheckBox) findViewById(R.id.checkboxAll);

        timeCheckView = findViewById(R.id.checkboxViewTime);
        timeCheckView.setOnClickListener(myClickListener);

        checkBoxTime = (CheckBox) findViewById(R.id.checkboxAllTime);

        valueCheckView = findViewById(R.id.checkboxByValueView);
        valueCheckView.setOnClickListener(myClickListener);
        checkboxByValue = (CheckBox) findViewById(R.id.checkboxByValue);
        fromValueV.setOnClickListener(myClickListener);
        toValueV.setOnClickListener(myClickListener);
        pickerV = rootView.findViewById(R.id.picker);

        displayUpdateFilter(search.isbShowAll());
        displayUpdateTime(search.isbAllTime());
        displayFilterBy();
        displayByValue();

        this.setOnDismissListener(this);
    }

    private void displayItem(boolean enable, CheckBox checkbox, View view) {
        checkbox.setChecked(enable);
        if (enable) {
            LViewUtils.setAlpha(view, 1.0f);
            view.setEnabled(true);
        } else {
            LViewUtils.setAlpha(view, 0.4f);
            view.setEnabled(false);
        }
    }

    private void displayAccounts() {
        displayItem(search.isbAccounts(), checkBoxAccounts, accountV);

        long vals[] = search.getAccounts();
        String str = "";
        boolean ok = false;
        if (null != vals) {
            DBAccount dbAccount = DBAccount.getInstance();
            for (int ii = 0; ii < vals.length; ii++) {
                LAccount account = dbAccount.getById(vals[ii]);
                if ((account != null) && (account.getState() == DBHelper.STATE_ACTIVE)) {
                    str += dbAccount.getNameById(vals[ii]) + ", ";
                    ok = true;
                }
            }
        }
        if (ok) {
            str = str.replaceAll(", $", "");
            accountV.setText(str);
        } else {
            search.setAccounts(null);
            LPreferences.setSearchControls(search);
            accountV.setText(context.getString(R.string.all));
        }
    }

    private void displayCategories() {
        displayItem(search.isbCategories(), checkBoxCategories, categoryV);

        long vals[] = search.getCategories();
        String str = "";
        boolean ok = false;
        if (null != vals) {
            for (int ii = 0; ii < vals.length; ii++) {
                LCategory category = DBCategory.getInstance().getById(vals[ii]);
                if ((category != null) && (category.getState() == DBHelper.STATE_ACTIVE)) {
                    str += DBCategory.getInstance().getNameById(vals[ii]) + ", ";
                    ok = true;
                }
            }
        }
        if (ok) {
            str = str.replaceAll(", $", "");
            categoryV.setText(str);
        } else {
            search.setCategories(null);
            LPreferences.setSearchControls(search);
            categoryV.setText(context.getString(R.string.all));
        }
    }

    private void displayVendors() {
        displayItem(search.isbVendors(), checkBoxPayers, vendorV);

        long vals[] = search.getVendors();
        String str = "";
        boolean ok = false;
        if (null != vals) {
            for (int ii = 0; ii < vals.length; ii++) {
                DBVendor dbVendor = DBVendor.getInstance();
                LVendor vendor = dbVendor.getById(vals[ii]);
                if ((vendor != null) && (vendor.getState() == DBHelper.STATE_ACTIVE)) {
                    str += dbVendor.getNameById(vals[ii]) + ", ";
                    ok = true;
                }
            }
        }
        if (ok) {
            str = str.replaceAll(", $", "");
            vendorV.setText(str);
        } else {
            search.setVendors(null);
            LPreferences.setSearchControls(search);
            vendorV.setText(context.getString(R.string.all));
        }
    }

    private void displayTags() {
        displayItem(search.isbTags(), checkBoxTags, tagV);

        long vals[] = search.getTags();
        String str = "";
        boolean ok = false;
        if (null != vals) {
            for (int ii = 0; ii < vals.length; ii++) {
                DBTag dbTag = DBTag.getInstance();
                LTag tag = dbTag.getById(vals[ii]);
                if ((tag != null) && (tag.getState() == DBHelper.STATE_ACTIVE)) {
                    str += dbTag.getNameById(vals[ii]) + ", ";
                    ok = true;
                }
            }
        }
        if (ok) {
            str = str.replaceAll(", $", "");
            tagV.setText(str);
        } else {
            search.setTags(null);
            LPreferences.setSearchControls(search);
            tagV.setText(context.getString(R.string.all));
        }
    }

    private void displayTypes() {
        displayItem(search.isbTypes(), checkBoxTypes, typeV);

        long vals[] = search.getTypes();
        String str = "";
        boolean ok = false;
        if (null != vals) {
            for (int ii = 0; ii < vals.length; ii++) {
                String name;
                if (vals[ii] == 0) name = context.getString(R.string.expense);
                else if (vals[ii] == 1) name = context.getString(R.string.income);
                else name = context.getString(R.string.transfer);
                str += name + ", ";
                ok = true;
            }
        }
        if (ok) {
            str = str.replaceAll(", $", "");
            typeV.setText(str);
        } else {
            search.setTypes(null);
            LPreferences.setSearchControls(search);
            typeV.setText(context.getString(R.string.all));
        }
    }

    private void displayUpdateFilter(boolean all) {
        checkBox.setChecked(all);
        //LViewUtils.disableEnableControls(!all, (ViewGroup) filterView);
        if (all) {
            filterView.setVisibility(View.GONE);
            LViewUtils.setAlpha(filterCheckView, 0.8f);
        } else {
            filterView.setVisibility(View.VISIBLE);
            LViewUtils.setAlpha(filterCheckView, 0.6f);
        }
        displayAccounts();
        displayCategories();
        displayVendors();
        displayTags();
        displayTypes();
    }

    private void displayUpdateTime(boolean all) {
        checkBoxTime.setChecked(all);
        LViewUtils.disableEnableControls(!all, (ViewGroup) timeView);
        if (all) {
            timeView.setVisibility(View.GONE);
            LViewUtils.setAlpha(timeCheckView, 0.8f);
        } else {
            timeView.setVisibility(View.VISIBLE);
            LViewUtils.setAlpha(timeCheckView, 0.6f);
        }
        fromV.setText(new SimpleDateFormat("MMM d, yyy").format(search.getTimeFrom()));
        toV.setText(new SimpleDateFormat("MMM d, yyy").format(search.getTimeTo()));
    }

    private void displayFilterBy() {
        filterByV.setText(context.getString
                (search.isbByEditTime() ? R.string.edit_time : R.string.record_time));
    }

    private void displayByValue() {
        checkboxByValue.setChecked(search.isbAllValue());

        if (search.isbAllValue()) {
            LViewUtils.setAlpha(valueCheckView, 0.8f);
            valueView.setVisibility(View.GONE);
        } else {
            LViewUtils.setAlpha(valueCheckView, 0.6f);
            valueView.setVisibility(View.VISIBLE);
        }

        if (search.getValueFrom() <= 0) {
            fromValueV.setText("---");
        } else {
            fromValueV.setText(String.format("%.2f", (float) search.getValueFrom()));
        }
        if (search.getValueTo() <= 0) {
            toValueV.setText("---");
        } else {
            toValueV.setText(String.format("%.2f", (float) search.getValueTo()));
        }
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
                    search.setbShowAll(!search.isbShowAll());
                    LPreferences.setSearchControls(search);
                    displayUpdateFilter(search.isbShowAll());
                    return;

                case R.id.checkboxViewTime:
                    search.setbAllTime(!search.isbAllTime());
                    LPreferences.setSearchControls(search);
                    displayUpdateTime(search.isbAllTime());
                    return;

                case R.id.checkboxByValueView:
                    search.setbAllValue(!search.isbAllValue());
                    LPreferences.setSearchControls(search);
                    displayByValue();
                    return;

                case R.id.fromValue:
                    showValuePicker(true);
                    return;

                case R.id.toValue:
                    showValuePicker(false);
                    return;

                case R.id.checkboxAccounts:
                    search.setbAccounts(!search.isbAccounts());
                    LPreferences.setSearchControls(search);
                    displayAccounts();
                    return;

                case R.id.selectedAccounts:
                    ids[9] = R.string.select_accounts;

                    long[] accounts = search.getAccounts();
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

                case R.id.checkboxCategories:
                    search.setbCategories(!search.isbCategories());
                    LPreferences.setSearchControls(search);
                    displayCategories();
                    return;

                case R.id.selectedCategories:
                    ids[9] = R.string.select_categories;

                    long[] categories = search.getCategories();
                    selectedIds.clear();
                    if (categories != null) {
                        for (long ll : categories) selectedIds.add(ll);
                    }

                    dialog = new LMultiSelectionDialog
                            (context, context, selectedIds, categorySelectionDlgItf, ids, columns);
                    dialog.show();
                    return;

                case R.id.checkboxPayers:
                    search.setbVendors(!search.isbVendors());
                    LPreferences.setSearchControls(search);
                    displayVendors();
                    return;

                case R.id.selectedPayers:
                    ids[9] = R.string.select_vendors;

                    long[] vendors = search.getVendors();
                    selectedIds.clear();
                    if (vendors != null) {
                        for (long ll : vendors) selectedIds.add(ll);
                    }

                    dialog = new LMultiSelectionDialog
                            (context, context, selectedIds, vendorSelectionDlgItf, ids, columns);
                    dialog.show();
                    return;

                case R.id.checkboxTags:
                    search.setbTags(!search.isbTags());
                    LPreferences.setSearchControls(search);
                    displayTags();
                    return;

                case R.id.selectedTags:
                    ids[9] = R.string.select_tags;

                    long[] tags = search.getTags();
                    selectedIds.clear();
                    if (tags != null) {
                        for (long ll : tags) selectedIds.add(ll);
                    }

                    dialog = new LMultiSelectionDialog
                            (context, context, selectedIds, tagSelectionDlgItf, ids, columns);
                    dialog.show();
                    return;

                case R.id.checkboxTypes:
                    search.setbTypes(!search.isbTypes());
                    LPreferences.setSearchControls(search);
                    displayTypes();
                    return;

                case R.id.selectedTypes:
                    ids[9] = R.string.select_types;

                    long[] types = search.getTypes();
                    selectedIds.clear();
                    if (types != null) {
                        for (long ll : types) selectedIds.add(ll);
                    }

                    dialog = new LMultiSelectionDialog
                            (context, context, selectedIds, typeSelectionDlgItf, ids, columns);
                    dialog.show();
                    return;

                case R.id.fromTime:
                    showDatePicker(true);
                    return;

                case R.id.toTime:
                    showDatePicker(false);
                    return;

                case R.id.filterBy:
                    search.setbByEditTime(!search.isbByEditTime());
                    LPreferences.setSearchControls(search);
                    displayFilterBy();
                    return;

                case R.id.closeDialog:
                default:
                    break;
            }
            dismiss();
        }
    }

    private boolean setFromTime;

    private void showDatePicker(boolean from) {
        setFromTime = from;

        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(from ? search.getTimeFrom() : search.getTimeTo());

        DatePickerDialog datePickerDialog = new DatePickerDialog(context, android.R.style
                .Theme_Holo_Light_Dialog_NoActionBar,
                TransactionSearchDialog.this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar
                .DAY_OF_MONTH));
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
            search.setTimeFrom(calendar.getTimeInMillis());
        } else {
            tv = toV;
            search.setTimeTo(calendar.getTimeInMillis() + (long) 24 * 3600 * 1000 - 1);
        }
        LPreferences.setSearchControls(search);
        tv.setText(new SimpleDateFormat("MMM d, yyy").format(calendar.getTimeInMillis()));
    }

    private boolean setFromValue;

    private void showValuePicker(boolean from) {
        setFromValue = from;

        if (picker != null) picker.onDestroy();
        float value = from ? search.getValueFrom() : search.getValueTo();
        picker = new LDollarAmountPickerView(rootView, value,
                LDollarAmountPickerView.VALUE_COLOR_NEUTRAL, true, TransactionSearchDialog.this);
        pickerV.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDollarAmountPickerExit(double value, boolean save) {
        if (save) {
            if (value < 0.01) value = 0;

            if (setFromValue) {
                search.setValueFrom((float) value);
            } else {
                search.setValueTo((float) value);
            }
            LPreferences.setSearchControls(search);
            displayByValue();
        }
        pickerV.setVisibility(View.GONE);
    }
}
