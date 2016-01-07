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
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LDollarAmountPicker;
import com.swoag.logalong.views.LSelectionDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ScheduledTransactionEdit implements TransactionEdit.TransitionEditItf, View.OnClickListener {
    private static final String TAG = ScheduledTransactionEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private boolean bCreate;

    private TransactionEdit transactionEdit;
    private LScheduledTransaction scheduledItem;
    private LScheduledTransaction savedScheduledItem;
    private ScheduledTransitionEditItf callback;

    private TextView weekMonthTV, intervalTV, countTV;
    private boolean clickDisabled = false;

    public interface ScheduledTransitionEditItf {
        public static final int EXIT_DELETE = 10;
        public static final int EXIT_OK = 20;
        public static final int EXIT_CANCEL = 30;

        public void onScheduledTransactionEditExit(int action, boolean changed);
    }

    @Override
    public void onTransactionEditExit(int action, boolean changed) {
        switch (action) {
            case TransactionEdit.TransitionEditItf.EXIT_DELETE:
                clickDisabled = true;
                callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_DELETE, false);
                destroy();
                break;
            case TransactionEdit.TransitionEditItf.EXIT_CANCEL:
                clickDisabled = true;
                callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_CANCEL, false);
                destroy();
                break;
            case TransactionEdit.TransitionEditItf.EXIT_OK:
                saveLog();
                break;
        }
    }

    public ScheduledTransactionEdit(Activity activity, View rootView, LScheduledTransaction item, boolean bCreate,
                                    ScheduledTransitionEditItf callback) {
        this.activity = activity;
        this.rootView = rootView;
        this.scheduledItem = item;
        this.callback = callback;
        clickDisabled = false;

        rootView.findViewById(R.id.repeatInterval).setOnClickListener(this);
        rootView.findViewById(R.id.repeatIntervalH).setOnClickListener(this);
        rootView.findViewById(R.id.repeatWeekMonth).setOnClickListener(this);
        rootView.findViewById(R.id.repeatCount).setOnClickListener(this);
        rootView.findViewById(R.id.repeatCountH).setOnClickListener(this);

        this.savedScheduledItem = new LScheduledTransaction(item);
        this.bCreate = bCreate;

        this.transactionEdit = new TransactionEdit(activity, rootView, item.getItem(), bCreate, true, this);
        create();
    }

    private void updateItemDisplay() {
        intervalTV.setText("" + scheduledItem.getRepeatInterval());
        if (scheduledItem.getRepeatInterval() > 1) {
            weekMonthTV.setText(activity.getString(scheduledItem.getRepeatUnit() == LScheduledTransaction.REPEAT_UNIT_WEEK ? R.string.weeks : R.string.months));
        } else {
            weekMonthTV.setText(activity.getString(scheduledItem.getRepeatUnit() == LScheduledTransaction.REPEAT_UNIT_WEEK ? R.string.week : R.string.month));
        }
        if (scheduledItem.getRepeatCount() == 0) {
            countTV.setText(activity.getString(R.string.unlimited));
        } else if (scheduledItem.getRepeatCount() == 1) {
            countTV.setText("1 " + activity.getString(R.string.count_time));
        } else {
            countTV.setText(scheduledItem.getRepeatCount() + " " + activity.getString(R.string.count_times));
        }
    }

    private void create() {
        intervalTV = (TextView) rootView.findViewById(R.id.repeatInterval);
        countTV = (TextView) rootView.findViewById(R.id.repeatCount);
        weekMonthTV = (TextView) rootView.findViewById(R.id.repeatWeekMonth);

        updateItemDisplay();
    }

    private void destroy() {
        intervalTV = null;
        countTV = null;
        weekMonthTV = null;

        savedScheduledItem = null;
    }

    @Override
    public void onClick(View v) {
        if (clickDisabled) return;

        switch (v.getId()) {
            case R.id.repeatInterval:
            case R.id.repeatIntervalH:
                int interval = scheduledItem.getRepeatInterval();
                interval++;
                if (interval > 12) interval = 1;
                scheduledItem.setRepeatInterval(interval);
                break;
            case R.id.repeatWeekMonth:
                if (scheduledItem.getRepeatUnit() == LScheduledTransaction.REPEAT_UNIT_WEEK)
                    scheduledItem.setRepeatUnit(LScheduledTransaction.REPEAT_UNIT_MONTH);
                else
                    scheduledItem.setRepeatUnit(LScheduledTransaction.REPEAT_UNIT_WEEK);
                break;
            case R.id.repeatCount:
            case R.id.repeatCountH:
                int count = scheduledItem.getRepeatCount();
                count++;
                if (count > 10) count = 0;
                scheduledItem.setRepeatCount(count);
                break;
        }
        updateItemDisplay();
    }

    public void dismiss() {
        clickDisabled = true;
        callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_CANCEL, false);
        destroy();
    }


    private void saveLog() {
        boolean changed = !scheduledItem.isEqual(savedScheduledItem);
        if (changed) scheduledItem.getItem().setTimeStampLast(LPreferences.getServerUtc());
        clickDisabled = true;
        callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_OK, changed);
        destroy();
    }
}
