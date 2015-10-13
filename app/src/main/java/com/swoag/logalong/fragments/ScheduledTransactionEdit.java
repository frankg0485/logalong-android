package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LScheduledTransaction;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LDollarAmountPicker;
import com.swoag.logalong.views.LSelectionDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ScheduledTransactionEdit implements View.OnClickListener, LSelectionDialog.OnSelectionDialogItf,
        DatePickerDialog.OnDateSetListener, LDollarAmountPicker.LDollarAmountPickerItf {
    private static final String TAG = ScheduledTransactionEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private boolean bCreate;
    private LScheduledTransaction scheduledItem;
    private LScheduledTransaction savedScheduledItem;
    private ScheduledTransitionEditItf callback;

    private View viewAmount, viewAccount, viewCategory, viewVendor, viewTag;
    private View viewDiscard, viewCancel, viewSave;
    private TextView amountTV, accountTV, categoryTV, vendorTV, tagTV;
    private TextView dateTV, weekMonthTV, intervalTV, countTV;

    private LSelectionDialog mSelectionDialog;
    private boolean firstTimeAmountPicker = true;

    public interface ScheduledTransitionEditItf {
        public static final int EXIT_DELETE = 10;
        public static final int EXIT_OK = 20;
        public static final int EXIT_CANCEL = 30;

        public void onScheduledTransactionEditExit(int action, boolean changed);
    }

    public ScheduledTransactionEdit(Activity activity, View rootView, LScheduledTransaction item, boolean bCreate,
                                    ScheduledTransitionEditItf callback) {
        this.activity = activity;
        this.rootView = rootView;
        this.scheduledItem = item;
        this.callback = callback;

        this.savedScheduledItem = new LScheduledTransaction(item);

        this.bCreate = bCreate;
        create();
    }

    private void updateItemDisplay() {
        intervalTV.setText("" + scheduledItem.getRepeatInterval());
        weekMonthTV.setText(activity.getString(savedScheduledItem.getRepeatUnit() == LScheduledTransaction.REPEAT_UNIT_WEEK ? R.string.week : R.string.month));
        countTV.setText(activity.getString(R.string.unlimited));

        LTransaction item = scheduledItem.getItem();

        dateTV.setText(new SimpleDateFormat("MMM d, yyy").format(item.getTimeStamp()));
        accountTV.setText(DBAccess.getAccountNameById(item.getAccount()));
        categoryTV.setText(DBAccess.getCategoryNameById(item.getCategory()));

        String tmp = DBAccess.getVendorNameById(item.getVendor());
        if (tmp.isEmpty()) {
            vendorTV.setText(activity.getString(R.string.unspecified_vendor));
        } else {
            vendorTV.setText(tmp);
        }

        tmp = DBAccess.getTagNameById(item.getTag());
        if (tmp.isEmpty()) {
            tagTV.setText(activity.getString(R.string.unspecified_tag));
        } else {
            tagTV.setText(tmp);
        }
    }

    private void create() {
        setViewListener(rootView, R.id.goback);
        viewSave = setViewListener(rootView, R.id.save);
        viewAmount = setViewListener(rootView, R.id.amountRow);
        viewAccount = setViewListener(rootView, R.id.accountRow);
        viewCategory = setViewListener(rootView, R.id.categoryRow);
        viewVendor = setViewListener(rootView, R.id.vendorRow);
        viewTag = setViewListener(rootView, R.id.tagRow);


        dateTV = (TextView) setViewListener(rootView, R.id.tvDate);

        intervalTV = (TextView) rootView.findViewById(R.id.repeatInterval);
        countTV = (TextView) rootView.findViewById(R.id.repeatCount);
        weekMonthTV = (TextView) rootView.findViewById(R.id.repeatWeekMonth);

        amountTV = (TextView) rootView.findViewById(R.id.tvAmount);
        accountTV = (TextView) rootView.findViewById(R.id.tvAccount);
        categoryTV = (TextView) rootView.findViewById(R.id.tvCategory);
        vendorTV = (TextView) rootView.findViewById(R.id.tvVendor);
        tagTV = (TextView) rootView.findViewById(R.id.tvTag);
        updateItemDisplay();

        clearInputString();
        String inputString = value2string(scheduledItem.getItem().getValue());
        if (inputString.isEmpty()) {
            amountTV.setText("0.0");
        } else {
            amountTV.setText(inputString);
            enableOk(true);
        }

        if (bCreate) {
            //LDollarAmountPicker picker = new LDollarAmountPicker(activity, item.getValue(), this);
            //picker.show();
        } else {
            viewDiscard = setViewListener(rootView, R.id.discard);
            viewDiscard.setVisibility(View.VISIBLE);
        }
    }

    private void destroy() {
        viewSave = null;

        intervalTV = null;
        countTV = null;
        weekMonthTV = null;

        viewAmount = null;
        viewAccount = null;
        viewCategory = null;
        viewVendor = null;
        viewTag = null;
        if (viewDiscard != null) {
            viewDiscard.setVisibility(View.GONE);
            viewDiscard = null;
        }

        accountTV = null;
        categoryTV = null;
        vendorTV = null;
        tagTV = null;
        dateTV = null;
        savedScheduledItem = null;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);
        dateTV.setText(new SimpleDateFormat("MMM d, yyy").format(calendar.getTimeInMillis()));
        scheduledItem.getItem().setTimeStamp(calendar.getTimeInMillis());
    }

    @Override
    public void onDollarAmountPickerExit(double value, boolean save) {
        if (save) {
            scheduledItem.getItem().setValue(value);
            String inputString = value2string(value);
            if (inputString.isEmpty()) {
                amountTV.setText("0.0");
                enableOk(false);
            } else {
                amountTV.setText(inputString);
                if (scheduledItem.getItem().getAccount() != 0) enableOk(true);
            }
        } else if (firstTimeAmountPicker) {
            if (bCreate) {
                callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_CANCEL, false);
                destroy();
            }
        }

        if (firstTimeAmountPicker) firstTimeAmountPicker = false;
    }

    @Override
    public void onClick(View v) {
        LTransaction item = scheduledItem.getItem();
        switch (v.getId()) {
            case R.id.tvDate:
                final Calendar c = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(activity, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                datePickerDialog.show();
                break;

            case R.id.amountRow:
                LDollarAmountPicker picker = new LDollarAmountPicker(activity, item.getValue(), this);
                picker.show();
                break;

            case R.id.accountRow:
                try {
                    ids[9] = R.string.select_account;
                    mSelectionDialog = new LSelectionDialog
                            (activity, this, ids,
                                    DBHelper.TABLE_ACCOUNT_NAME,
                                    DBHelper.TABLE_COLUMN_NAME, DBAccess.getAccountIndexById(item.getAccount()), DLG_ID_ACCOUNT);
                    mSelectionDialog.show();
                    mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                    LLog.w(TAG, "unable to open phone database");
                }
                break;
            case R.id.categoryRow:
                try {
                    ids[9] = R.string.select_category;
                    mSelectionDialog = new LSelectionDialog
                            (activity, this, ids,
                                    DBHelper.TABLE_CATEGORY_NAME,
                                    DBHelper.TABLE_COLUMN_NAME, DBAccess.getCategoryIndexById(item.getCategory()), DLG_ID_CATEGORY);
                    mSelectionDialog.show();
                    mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                    LLog.w(TAG, "unable to open phone database");
                }
                break;
            case R.id.vendorRow:
                try {
                    ids[9] = R.string.select_vendor;
                    mSelectionDialog = new LSelectionDialog
                            (activity, this, ids,
                                    DBHelper.TABLE_VENDOR_NAME,
                                    DBHelper.TABLE_COLUMN_NAME, DBAccess.getVendorIndexById(item.getVendor()), DLG_ID_VENDOR);
                    mSelectionDialog.show();
                    mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                    LLog.w(TAG, "unable to open phone database");
                }
                break;
            case R.id.tagRow:
                try {
                    ids[9] = R.string.select_tag;
                    mSelectionDialog = new LSelectionDialog
                            (activity, this, ids,
                                    DBHelper.TABLE_TAG_NAME,
                                    DBHelper.TABLE_COLUMN_NAME, DBAccess.getTagIndexById(item.getTag()), DLG_ID_TAG);
                    mSelectionDialog.show();
                    mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                    LLog.w(TAG, "unable to open phone database");
                }
                break;
            case R.id.discard:
                destroy();
                callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_DELETE, false);
                break;

            case R.id.goback:
                destroy();
                callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_CANCEL, false);
                break;

            case R.id.save:
                saveLog();
                break;

            default:
                break;
        }
    }

    public void dismiss() {
        destroy();
        callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_CANCEL, false);
    }

    @Override
    public Cursor onGetCursor(String table, String column) {
        if (table.contentEquals(DBHelper.TABLE_ACCOUNT_NAME))
            return DBAccount.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        else if (table.contentEquals(DBHelper.TABLE_CATEGORY_NAME))
            return DBCategory.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        else if (table.contentEquals(DBHelper.TABLE_VENDOR_NAME))
            return DBVendor.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        else if (table.contentEquals(DBHelper.TABLE_TAG_NAME))
            return DBTag.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        return null;
    }

    private static final int DLG_ID_ACCOUNT = 10;
    private static final int DLG_ID_CATEGORY = 20;
    private static final int DLG_ID_VENDOR = 30;
    private static final int DLG_ID_TAG = 40;

    @Override
    public void onSelectionDialogExit(int dlgId, long selectedId) {
        LTransaction item = scheduledItem.getItem();
        switch (dlgId) {
            case DLG_ID_ACCOUNT:
                item.setAccount(selectedId);
                if (item.getValue() != 0) {
                    enableOk(true);
                }
                break;
            case DLG_ID_CATEGORY:
                item.setCategory(selectedId);
                break;
            case DLG_ID_VENDOR:
                item.setVendor(selectedId);
                break;
            case DLG_ID_TAG:
                item.setTag(selectedId);
                break;
        }
        updateItemDisplay();
    }

    private int[] ids = new int[]{
            R.layout.selection_dialog,
            R.layout.selection_item,
            R.id.title,
            R.id.save,
            R.id.cancel,
            R.id.radioButton,
            R.id.name,
            R.id.list,
            R.id.searchText,
            R.string.select_account};

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(this);
        return view;
    }

    private void enableOk(boolean yes) {
        viewSave.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) viewSave, 1.0f);
        else LViewUtils.setAlpha((View) viewSave, 0.5f);
    }

    private void clearInputString() {
        amountTV.setText("0.0");
        enableOk(false);
    }

    private String value2string(double value) {
        String str = "";
        if (value != 0) {
            str = String.format("%.2f", value);
        }
        return str;
    }

    private void saveLog() {
        boolean changed = !scheduledItem.isEqual(savedScheduledItem);
        if (changed) scheduledItem.getItem().setTimeStampLast(System.currentTimeMillis());
        callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_OK, changed);
        destroy();
    }
}
