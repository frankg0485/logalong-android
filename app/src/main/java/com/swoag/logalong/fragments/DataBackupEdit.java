package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.CountDownTimer;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBPorter;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LWarnDialog;

public class DataBackupEdit implements LWarnDialog.LWarnDialogItf {
    private static final String TAG = DataBackupEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private DataBackupEditItf callback;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private TextView errorMsgV, backupDateTV, restoreDateTV;

    private View restoreView;
    private BroadcastReceiver broadcastReceiver;
    private MyClickListener myClickListener;

    public interface DataBackupEditItf {
        public void onDataBackupEditExit();
    }

    public DataBackupEdit(Activity activity, View rootView, DataBackupEditItf callback) {
        this.activity = activity;
        this.rootView = rootView;
        this.callback = callback;
        myClickListener = new MyClickListener();
        create();
    }

    private void create() {
        setViewListener(rootView, R.id.backupView);
        restoreView = setViewListener(rootView, R.id.restoreView);
        View saveV = setViewListener(rootView, R.id.save);
        LViewUtils.setAlpha(saveV, 1.0f);
        saveV.setEnabled(true);

        backupDateTV = (TextView) rootView.findViewById(R.id.backupDate);
        backupDateTV.setText(DBPorter.getExportDate());
        if (DBPorter.getExportDate().contentEquals("N/A")) {
            restoreView.setEnabled(false);
            LViewUtils.setAlpha(restoreView, 0.5f);
        } else {
            restoreView.setEnabled(true);
            LViewUtils.setAlpha(restoreView, 1.0f);
        }
        //restoreDateTV = (TextView) rootView.findViewById(R.id.restoreDate);
        //restoreDateTV.setText(DBPorter.getImportDate());

        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        errorMsgV = (TextView) rootView.findViewById(R.id.errorMsg);
        hideErrorMsg();
    }

    private void destroy() {
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.backupView:
                    if (DBPorter.exportDb(DBHelper.DB_VERSION)) {
                        backupDateTV.setText(DBPorter.getExportDate());
                        displayErrorMsg(activity.getString(R.string.hint_data_backup_to_sd_card_done));
                    } else {
                        displayErrorMsg(activity.getString(R.string.hint_data_backup_to_sd_card_error));
                    }
                    break;

                case R.id.restoreView:
                    LWarnDialog warnDialog = new LWarnDialog(activity, null, DataBackupEdit.this,
                            activity.getString(R.string.restore_db),
                            activity.getString(R.string.warning_restore_db),
                            activity.getString(R.string.restore_now),
                            true);
                    warnDialog.show();
                    break;

                case R.id.save:
                    /*countDownTimer = new CountDownTimer(15000, 15000) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            displayErrorMsg(activity.getString(R.string.warning_get_share_user_time_out));
                            progressBar.setVisibility(View.GONE);
                        }
                    }.start();

                    progressBar.setVisibility(View.VISIBLE);
                    */
                    dismiss();
                    break;
            }
        }
    }

    private void hideErrorMsg() {
        errorMsgV.setVisibility(View.GONE);
    }

    private void displayErrorMsg(String msg) {
        errorMsgV.setText(msg);
        errorMsgV.setVisibility(View.VISIBLE);
    }

    public void dismiss() {
        this.callback.onDataBackupEditExit();
        destroy();
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

    @Override
    public void onWarnDialogExit(Object obj, boolean confirm, boolean ok) {
        if (confirm && ok) {
            if (DBPorter.importDb(DBHelper.DB_VERSION)) {
                //restoreDateTV.setText(DBPorter.getImportDate());
                displayErrorMsg(activity.getString(R.string.hint_data_restore_from_sd_card_done));
            } else {
                displayErrorMsg(activity.getString(R.string.hint_data_restore_from_sd_card_error));
            }
        }
    }
}

