package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LScheduledTransaction;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

import java.util.Calendar;

public class ScheduledTransactionEdit implements TransactionEdit.TransitionEditItf, View.OnClickListener {
    private static final String TAG = ScheduledTransactionEdit.class.getSimpleName();

    private Activity activity;
    private View rootView, repeatIntervalView, enableView, dateView;
    private ViewGroup allItemsViewGroup;
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

        allItemsViewGroup = (ViewGroup)rootView.findViewById(R.id.allItems);
        rootView.findViewById(R.id.repeatInterval).setOnClickListener(this);
        rootView.findViewById(R.id.repeatIntervalH).setOnClickListener(this);
        rootView.findViewById(R.id.repeatWeekMonth).setOnClickListener(this);
        rootView.findViewById(R.id.repeatCount).setOnClickListener(this);
        rootView.findViewById(R.id.repeatCountH).setOnClickListener(this);

        dateView = rootView.findViewById(R.id.tvDate);
        repeatIntervalView = rootView.findViewById(R.id.repeatRow);
        enableView = rootView.findViewById(R.id.enable);
        enableView.setOnClickListener(this);

        this.savedScheduledItem = new LScheduledTransaction(item);
        this.bCreate = bCreate;

        this.transactionEdit = new TransactionEdit(activity, rootView, item, bCreate, true, true, this);
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
            countTV.setText(activity.getString(R.string.disabled));
        } else if (scheduledItem.getRepeatCount() == 1) {
            countTV.setText(activity.getString(R.string.unlimited));
        } else if (scheduledItem.getRepeatCount() == 2) {
            countTV.setText("1 " + activity.getString(R.string.count_time));
        } else {
            countTV.setText(scheduledItem.getRepeatCount() + " " + activity.getString(R.string.count_times));
        }
        if ((scheduledItem.getRepeatCount() == 0)) {
            LViewUtils.setAlpha(repeatIntervalView, 0.5f);
            LViewUtils.disableEnableControls(false, (ViewGroup)repeatIntervalView);
        } else {
            LViewUtils.setAlpha(repeatIntervalView, 1.0f);
            LViewUtils.disableEnableControls(true, (ViewGroup)repeatIntervalView);
        }
    }

    private void create() {
        intervalTV = (TextView) rootView.findViewById(R.id.repeatInterval);
        countTV = (TextView) rootView.findViewById(R.id.repeatCount);
        weekMonthTV = (TextView) rootView.findViewById(R.id.repeatWeekMonth);

        updateItemDisplay();
        scheduleEnableDisplay();
    }

    private void destroy() {
        intervalTV = null;
        countTV = null;
        weekMonthTV = null;

        savedScheduledItem = null;
    }

    private void scheduleEnableDisplay()
    {
        if (scheduledItem.isEnabled()) {
            LViewUtils.setAlpha(enableView, 0.5f);
            LViewUtils.disableEnableControls(true, allItemsViewGroup);
            LViewUtils.setAlpha(allItemsViewGroup, 1.0f);

            dateView.setClickable(true);
            LViewUtils.setAlpha(dateView, 1.0f);

            //transactionEdit.updateOkDisplay();
        } else {
            LViewUtils.setAlpha(enableView, 1.0f);
            LViewUtils.disableEnableControls(false, allItemsViewGroup);
            LViewUtils.setAlpha(allItemsViewGroup, 0.6f);

            dateView.setClickable(false);
            LViewUtils.setAlpha(dateView, 0.6f);

            //transactionEdit.enableOk(false);
        }
    }

    @Override
    public void onClick(View v) {
        if (clickDisabled) return;

        switch (v.getId()) {
            case R.id.enable:
                scheduledItem.setEnabled(!scheduledItem.isEnabled());
                if (scheduledItem.isEnabled()) {
                    //reset to today if timestamp was past
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.set(Calendar.HOUR_OF_DAY, LScheduledTransaction.START_HOUR_OF_DAY);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    if (scheduledItem.getTimeStamp() < calendar.getTimeInMillis()) {
                        scheduledItem.setTimeStamp(calendar.getTimeInMillis());
                    }

                    scheduledItem.initNextTimeMs();
                    transactionEdit.updateDateDisplay();
                }
                scheduleEnableDisplay();
                break;
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
        if (changed) scheduledItem.setTimeStampLast(LPreferences.getServerUtc());
        clickDisabled = true;
        callback.onScheduledTransactionEditExit(ScheduledTransitionEditItf.EXIT_OK, changed);
        destroy();
    }
}
