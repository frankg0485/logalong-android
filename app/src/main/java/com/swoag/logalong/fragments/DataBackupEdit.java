package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.CountDownTimer;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBPorter;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LTask;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LWarnDialog;

public class DataBackupEdit implements LWarnDialog.LWarnDialogItf {
    private static final String TAG = DataBackupEdit.class.getSimpleName();
    private static final int BACKUP = 10;
    private static final int RESTORE = 20;

    private Activity activity;
    private View rootView;
    private DataBackupEditItf callback;
    private ProgressBar backupProgressBar, restoreProgressBar;
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
        View saveV = setViewListener(rootView, R.id.add);
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

        backupProgressBar = (ProgressBar) rootView.findViewById(R.id.backupProgress);
        restoreProgressBar = (ProgressBar) rootView.findViewById(R.id.restoreProgress);

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
                    if (restoreProgressBar.getVisibility() != View.VISIBLE && backupProgressBar.getVisibility() != View.VISIBLE) {
                        hideErrorMsg();
                        backupProgressBar.setVisibility(View.VISIBLE);
                        LTask.start(new MyTask(), BACKUP);
                    }
                    break;

                case R.id.restoreView:
                    if (restoreProgressBar.getVisibility() != View.VISIBLE && backupProgressBar.getVisibility() != View.VISIBLE) {
                        hideErrorMsg();
                        LWarnDialog warnDialog = new LWarnDialog(activity, null, DataBackupEdit.this,
                                activity.getString(R.string.restore_db),
                                activity.getString(R.string.warning_restore_db),
                                activity.getString(R.string.restore_now),
                                true);
                        warnDialog.show();
                    }
                    break;

                case R.id.add:
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
        if (restoreProgressBar.getVisibility() != View.VISIBLE && backupProgressBar.getVisibility() != View.VISIBLE) {
            myClickListener.disableEnable(false);
            this.callback.onDataBackupEditExit();
            destroy();
        }
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

    @Override
    public void onWarnDialogExit(Object obj, boolean confirm, boolean ok) {
        if (confirm && ok) {
            restoreProgressBar.setVisibility(View.VISIBLE);
            LTask.start(new MyTask(), RESTORE);
        }
    }

    private class MyTask extends AsyncTask<Integer, Void, Boolean> {
        private int task;

        @Override
        protected Boolean doInBackground(Integer... params) {
            Boolean ret = false;
            task = params[0];
            switch (task) {
                case BACKUP:
                    ret = DBPorter.exportDb(DBHelper.DB_VERSION);
                    break;
                case RESTORE:
                    ret = DBPorter.importDb(DBHelper.DB_VERSION);
                    break;
            }
            /*
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
            }
            */
            return ret;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            switch (task) {
                case BACKUP:
                    if (result) {
                        backupDateTV.setText(DBPorter.getExportDate());
                        displayErrorMsg(activity.getString(R.string.hint_data_backup_to_sd_card_done));
                    } else {
                        displayErrorMsg(activity.getString(R.string.hint_data_backup_to_sd_card_error));
                    }
                    backupProgressBar.setVisibility(View.INVISIBLE);
                    break;

                case RESTORE:
                    if (result) {
                        //restoreDateTV.setText(DBPorter.getImportDate());
                        displayErrorMsg(activity.getString(R.string.hint_data_restore_from_sd_card_done));
                    } else {
                        displayErrorMsg(activity.getString(R.string.hint_data_restore_from_sd_card_error));
                    }
                    restoreProgressBar.setVisibility(View.INVISIBLE);
                    break;
            }
        }

        @Override
        protected void onPreExecute() {
        }
    }

}

