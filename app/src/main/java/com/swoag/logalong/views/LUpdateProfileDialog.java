package com.swoag.logalong.views;
/* Copyright (C) 2017 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.swoag.logalong.MainService;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.CountDownTimer;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

import java.util.HashSet;

public class LUpdateProfileDialog extends Dialog implements LBroadcastReceiver.BroadcastReceiverListener {
    private static final String TAG = LUpdateProfileDialog.class.getSimpleName();

    public static final int NEW_USER = 10;
    public static final int LOGIN_USER = 20;
    public static final int UPDATE_USER = 30;

    private LUpdateProfileDialogItf callback;
    private Context context;
    private MyClickListener myClickListener;
    private TextView errorMsgV;
    private String userId, userPass, userName;
    private int action;
    private Button okBtn;
    private CountDownTimer countDownTimer;
    private ProgressBar progressBar;
    private TextView progressMsg, progressTxt, titleTV;
    private BroadcastReceiver broadcastReceiver;
    private boolean success;
    private boolean requestedOnce;

    public interface LUpdateProfileDialogItf {
        public void onUpdateProfileDialogExit(boolean success);
    }

    public LUpdateProfileDialog(Context context, LUpdateProfileDialogItf callback,
                                int action, String userId, String userPass, String userName) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.callback = callback;
        this.action = action;
        this.userId = userId;
        this.userPass = userPass;
        this.userName = userName;
        this.success = false;
        this.requestedOnce = false;

        myClickListener = new MyClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.update_profile_progress_dialog);

        errorMsgV = (TextView) findViewById(R.id.errorMsg);
        titleTV = (TextView) findViewById(R.id.title);
        String title = "";
        switch (action) {
            case UPDATE_USER:
                title = context.getResources().getString(R.string.updating_profile);
                break;
            case NEW_USER:
                title = context.getResources().getString(R.string.creating_user);
                break;
            case LOGIN_USER:
                title = context.getResources().getString(R.string.logging_in);
                break;
        }
        titleTV.setText(title);

        okBtn = (Button) findViewById(R.id.confirmDialog);
        okBtn.setOnClickListener(myClickListener);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressMsg = (TextView) findViewById(R.id.progressMsg);
        progressTxt = (TextView) findViewById(R.id.progressText);
        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_CREATE_USER,
                LBroadcastReceiver.ACTION_SIGN_IN,
                LBroadcastReceiver.ACTION_UPDATE_USER_PROFILE}, this);

        hideMsg();

        countDownTimer = new CountDownTimer(16000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                progressMsg.setText(millisUntilFinished / 1000 + "");
                if (LAppServer.getInstance().UiIsConnected()) createUpdateProfile();
            }

            @Override
            public void onFinish() {
                displayMsg(true, context.getString(R.string.warning_unable_to_connect));
            }
        }.start();

        if (!LAppServer.getInstance().UiIsConnected()) {
            LLog.d(TAG, "restarting service ...");
            LAppServer.getInstance().connect();
            MainService.start(context);
        }
    }

    private void hideMsg() {
        errorMsgV.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        progressMsg.setVisibility(View.VISIBLE);
        LViewUtils.setAlpha(okBtn, 0.5f);
        okBtn.setEnabled(false);
    }

    private void displayMsg(boolean error, String msg) {
        errorMsgV.setTextColor(error ? context.getResources().getColor(R.color.red_text_color) :
                context.getResources().getColor(R.color.base_black_text_color));
        errorMsgV.setText(msg);
        errorMsgV.setVisibility(View.VISIBLE);

        progressBar.setVisibility(View.GONE);
        progressMsg.setVisibility(View.GONE);
        if (error) {
            LViewUtils.setAlpha(okBtn, 1.0f);
            okBtn.setEnabled(true);
        }
    }


    private void createUpdateProfile() {
        if (requestedOnce) return;

        switch (action) {
            case NEW_USER:
                requestedOnce = true;
                LAppServer.getInstance().UiCreateUser(userId, userPass, userName);
                break;
            case LOGIN_USER:
                requestedOnce = true;
                LAppServer.getInstance().UiSignIn(userId, userPass);
                break;
            case UPDATE_USER:
                if (LAppServer.getInstance().UiIsLoggedIn()) {
                    requestedOnce = true;
                    LAppServer.getInstance().UiUpdateUserProfile(LPreferences.getUserId(), LPreferences.getUserPass(), userPass, userName);
                }
                break;
        }
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.confirmDialog:
                    if (callback != null) callback.onUpdateProfileDialogExit(success);
                    destroy();
            }
        }
    }

    private void destroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
        dismiss();
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        switch (action) {
            case LBroadcastReceiver.ACTION_CREATE_USER:
                if (ret == LProtocol.RSPS_OK) {
                    success = true;
                    displayMsg(false, context.getString(R.string.user_id) + ": " + userId +"\n"
                            + context.getString(R.string.password) + ": " + userPass + "\n\n"
                            + context.getString(R.string.synchronizing_database));

                    LPreferences.setUserPass(userPass);
                    LPreferences.setUserId(userId);
                    LPreferences.setUserName(userName);
                    LAppServer.getInstance().UiLogIn(userId, userPass);

                    pushLocalDb();
                } else {
                    displayMsg(true, context.getString(R.string.warning_unable_to_connect));
                }
                break;
            case LBroadcastReceiver.ACTION_SIGN_IN:
                switch (ret) {
                    case LProtocol.RSPS_OK:
                        success = true;
                        LPreferences.setUserPass(userPass);
                        LPreferences.setUserId(userId);
                        LPreferences.setUserName(intent.getStringExtra("userName"));

                        LAppServer.getInstance().UiLogIn(userId, userPass);
                        displayMsg(false, context.getString(R.string.synchronizing_database));

                        pushLocalDb();
                        break;
                    case LProtocol.RSPS_WRONG_PASSWORD:
                        displayMsg(true, context.getString(R.string.warning_password_mismatch));
                        break;
                    case LProtocol.RSPS_USER_NOT_FOUND:
                        displayMsg(true, context.getString(R.string.warning_user_id_invalid));
                        break;
                    default:
                        displayMsg(true, context.getString(R.string.warning_unable_to_connect));
                        break;
                }
                break;

            case LBroadcastReceiver.ACTION_UPDATE_USER_PROFILE:
                if (ret == LProtocol.RSPS_OK) {
                    LPreferences.setUserPass(userPass);
                    LPreferences.setUserName(userName);
                    if (callback != null) callback.onUpdateProfileDialogExit(true);
                    destroy();
                } else {
                    displayMsg(true, context.getString(R.string.warning_unable_to_connect));
                }
                break;
        }
    }


    private void pushLocalDb() {
        new MyTask().execute();
    }

    private class MyTask extends AsyncTask<Void, String, Boolean> {
        private boolean do_pushLocalDb() {
            LJournal journal = new LJournal();

            // send over all account/category/tag/vendor
            DBAccount dbAccount = DBAccount.getInstance();
            HashSet<Long> accountIds = dbAccount.getAllActiveIds();
            for (long id: accountIds) {
                LAccount account = dbAccount.getById(id);
                if (null != account) {
                    publishProgress(account.getName());
                    journal.addAccount(account.getId());
                }
            }

            DBCategory dbCategory = DBCategory.getInstance();
            HashSet<Long> catIds = dbCategory.getAllActiveIds();
            for (long id: catIds) {
                LCategory category = dbCategory.getInstance().getById(id);
                if (null != category) {
                    publishProgress(category.getName());
                    journal.addCategory(category.getId());
                }
            }

            DBVendor dbVendor = DBVendor.getInstance();
            HashSet<Long> vendorIds = dbVendor.getAllActiveIds();
            for (long id: vendorIds) {
                LVendor vendor = dbVendor.getById(id);
                if (null != vendor) {
                    publishProgress(vendor.getName());
                    journal.addVendor(vendor.getId());
                }
            }

            DBTag dbTag = DBTag.getInstance();
            HashSet<Long> tagIds = dbTag.getAllActiveIds();
            for (long id: tagIds) {
                LTag tag = dbTag.getById(id);
                if (null != tag) {
                    publishProgress(tag.getName());
                    journal.addTag(tag.getId());
                }
            }

            // get all accounts
            journal.getAllAccounts();
            journal.getAllCategories();
            journal.getAllTags();
            journal.getAllVendors();

            // send all records
            DBTransaction dbTransaction = DBTransaction.getInstance();
            Cursor cursor = dbTransaction.getAllCursor();
            if (cursor != null) {
                LTransaction transaction = new LTransaction();
                cursor.moveToFirst();
                do {
                    dbTransaction.getValues(cursor, transaction);
                    if (transaction.getType() != LTransaction.TRANSACTION_TYPE_TRANSFER_COPY)
                        journal.addRecord(transaction.getId());
                    LLog.d(TAG, "adding record: " + transaction.getId());
                    publishProgress(DBAccount.getInstance().getNameById(transaction.getAccount()) + " : " + transaction.getValue());
                } while (cursor.moveToNext());
                cursor.close();
            }

            journal.getAllRecords();
            journal.getAllSchedules();
            return true;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return do_pushLocalDb();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            progressTxt.setText(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                progressTxt.setText(context.getString(R.string.done));
                LViewUtils.setAlpha(okBtn, 1.0f);
                okBtn.setEnabled(true);
            }
        }
    }
}
