package com.swoag.logalong.views;
/* Copyright (C) 2017 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.swoag.logalong.MainService;
import com.swoag.logalong.R;
import com.swoag.logalong.fragments.ProfileEdit;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.CountDownTimer;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

import org.w3c.dom.Text;

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
    private TextView progressMsg, titleTV;
    private BroadcastReceiver broadcastReceiver;
    private boolean success;
    private boolean requestedOnce;

    public interface LUpdateProfileDialogItf {
        public void onUpdateProfileDialogExit(boolean changed);
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
        titleTV = (TextView)findViewById(R.id.title);
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
        progressMsg = (TextView)findViewById(R.id.progressMsg);
        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_USER_PROFILE_UPDATED}, this);

        hideMsg();

        countDownTimer = new CountDownTimer(16000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                progressMsg.setText(millisUntilFinished/1000 + "");
                if (LAppServer.getInstance().UiIsConnected()) createUpdateProfile();
            }

            @Override
            public void onFinish() {
                displayMsg(true, context.getString(R.string.warning_get_share_user_time_out));
            }
        }.start();

        if (!LAppServer.getInstance().UiIsConnected()) {
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
        errorMsgV.setTextColor(error? context.getResources().getColor(R.color.red_text_color):
                context.getResources().getColor(R.color.base_black_text_color));
        errorMsgV.setText(msg);
        errorMsgV.setVisibility(View.VISIBLE);

        progressBar.setVisibility(View.GONE);
        progressMsg.setVisibility(View.GONE);
        LViewUtils.setAlpha(okBtn, 1.0f);
        okBtn.setEnabled(true);
    }


    private void createUpdateProfile() {
        if (requestedOnce) return;
        requestedOnce = true;

        LAppServer.getInstance().UiUpdateUserProfile(userId, userPass, userName);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.confirmDialog:
                    if(callback != null) callback.onUpdateProfileDialogExit(success);
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
        switch (action) {
            case LBroadcastReceiver.ACTION_USER_PROFILE_UPDATED:
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    if (ret == LProtocol.RSPS_OK) {
                        //switch (intent.getIntExtra("status", 0)) {
                        //    case
                        //}
                    } else {
                        displayMsg(true, context.getString(R.string.warning_unable_to_create_update_profile));
                    }
                }
                break;
        }
    }
}
