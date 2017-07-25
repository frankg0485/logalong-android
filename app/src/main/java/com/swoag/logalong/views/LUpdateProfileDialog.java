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

public class LUpdateProfileDialog extends Dialog implements LBroadcastReceiver.BroadcastReceiverListener {
    private static final String TAG = LUpdateProfileDialog.class.getSimpleName();

    private LUpdateProfileDialogItf callback;
    private Context context;
    private MyClickListener myClickListener;
    private TextView errorMsgV;
    private String userId, userPass, userName;
    private Button okBtn;
    private CountDownTimer countDownTimer;
    private ProgressBar progressBar;
    private TextView progressMsg;
    private BroadcastReceiver broadcastReceiver;

    public interface LUpdateProfileDialogItf {
        public void onUpdateProfileDialogExit(boolean changed);
    }

    public LUpdateProfileDialog(Context context, LUpdateProfileDialogItf callback,
                                String userId, String userPass, String userName) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.callback = callback;
        this.userId = userId;
        this.userPass = userPass;
        this.userName = userName;

        myClickListener = new MyClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.update_profile_progress_dialog);

        errorMsgV = (TextView) findViewById(R.id.errorMsg);

        okBtn = (Button) findViewById(R.id.confirmDialog);
        okBtn.setOnClickListener(myClickListener);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressMsg = (TextView)findViewById(R.id.progressMsg);
        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_USER_PROFILE_UPDATED}, this);

        countDownTimer = new CountDownTimer(16000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                progressMsg.setText(millisUntilFinished/1000 + "");
                if ((millisUntilFinished < 8000) && (millisUntilFinished > 6500)) {
                    if (LAppServer.getInstance().UiIsConnected()) createUpdateProfile();
                }
            }

            @Override
            public void onFinish() {
                displayMsg(true, context.getString(R.string.warning_get_share_user_time_out));
            }
        }.start();

        hideMsg();

        if (LAppServer.getInstance().UiIsConnected()) {
            createUpdateProfile();
        } else {
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
        if (TextUtils.isEmpty(LPreferences.getUserName())) {
            LAppServer.getInstance().UiRequestUserName();
            //profile is automatically updated when username is first-time generated.
        } else {
            if (LAppServer.getInstance().UiIsLoggedIn()) {
                LAppServer.getInstance().UiUpdateUserProfile();
            } else {
                MainService.start(context);
            }
        }
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.confirmDialog:
                    dismiss();
            }
        }
    }

    private void destroy() {
        if (countDownTimer != null) countDownTimer.cancel();

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

                    } else {
                        displayMsg(true, context.getString(R.string.warning_unable_to_create_update_profile));
                    }
                }
                break;
        }
    }
}
