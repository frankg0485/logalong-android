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
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
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

public class LChangePassDialog extends Dialog implements TextWatcher, LBroadcastReceiver.BroadcastReceiverListener {
    private static final String TAG = LChangePassDialog.class.getSimpleName();

    private LChangePassDialogItf callback;
    private EditText userPassET;
    private CheckBox checkboxShowPass;
    private Context context;
    private View resetV, showPassV;
    private Object id;
    private MyClickListener myClickListener;
    private String userPass = "";
    private TextView errorMsgV, titleTV;
    private Button okBTN;
    private boolean isResetPass;

    private static final int RESET_PASS_INIT = 10;
    private static final int RESET_PASS_ERROR = 20;
    private static final int RESET_PASS_DONE = 30;
    private int resetPassState;
    private CountDownTimer countDownTimer;
    private ProgressBar progressBar;
    private BroadcastReceiver broadcastReceiver;

    public interface LChangePassDialogItf {
        public void onChangePassDialogExit(boolean changed);
    }

    public LChangePassDialog(Context context, LChangePassDialogItf callback) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.callback = callback;
        myClickListener = new MyClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.change_pass_dialog);

        errorMsgV = (TextView) findViewById(R.id.errorMsg);
        errorMsgV.setOnClickListener(myClickListener);
        hideErrorMsg();

        okBTN = (Button) findViewById(R.id.confirmDialog);
        findViewById(R.id.cancelDialog).setOnClickListener(myClickListener);
        okBTN.setOnClickListener(myClickListener);
        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);
        findViewById(R.id.dummy).setOnClickListener(myClickListener);

        userPassET = (EditText) findViewById(R.id.currentPass);
        userPassET.addTextChangedListener(this);
        checkboxShowPass = (CheckBox) findViewById(R.id.showPass);
        checkboxShowPass.setClickable(false);
        findViewById(R.id.showPassView).setOnClickListener(myClickListener);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        titleTV = (TextView) findViewById(R.id.title);
        showPassV = findViewById(R.id.showPassView);
        resetV = findViewById(R.id.resetPass);
        resetV.setOnClickListener(myClickListener);
        isResetPass = false;

        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_SIGN_IN,
                LBroadcastReceiver.ACTION_UI_RESET_PASSWORD}, this);

        setupOkButton();

        if (!LAppServer.getInstance().UiIsConnected()) {
            LLog.d(TAG, "restarting service ...");
            LAppServer.getInstance().connect();
            MainService.start(context);
        }
    }


    @Override
    public void afterTextChanged(Editable s) {
        String txt = userPassET.getText().toString();
        userPass = txt.trim();
        while (userPass.length() > ProfileEdit.MAX_USER_PASS_LEN) {
            userPass = userPass.substring(0, ProfileEdit.MAX_USER_PASS_LEN);
        }
        if (!userPass.contentEquals(txt)) {
            userPassET.setText(userPass);
            userPassET.setSelection(userPass.length());
        }
        hideErrorMsg();
        setupOkButton();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isResetPass) {
                return true;
            } else {
                destroy();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_SIGN_IN:
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer = null;
                    progressBar.setVisibility(View.GONE);
                }

                LViewUtils.setAlpha(okBTN, 1.0f);
                okBTN.setEnabled(true);

                userPassET.setEnabled(true);
                userPassET.setCursorVisible(true);

                switch (ret) {
                    case LProtocol.RSPS_OK:
                        LPreferences.setUserPass(userPass);
                        callback.onChangePassDialogExit(true);
                        destroy();
                        break;
                    case LProtocol.RSPS_WRONG_PASSWORD:
                        displayMsg(true, context.getString(R.string.warning_password_mismatch));
                        resetV.setVisibility(View.VISIBLE);
                        break;
                    case LProtocol.RSPS_USER_NOT_FOUND:
                        displayMsg(true, context.getString(R.string.warning_user_id_invalid));
                        break;
                    default:
                        displayMsg(true, context.getString(R.string.warning_unable_to_connect));
                        break;
                }
                break;

            case LBroadcastReceiver.ACTION_UI_RESET_PASSWORD:
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer = null;
                    progressBar.setVisibility(View.GONE);
                }

                if (ret == LProtocol.RSPS_OK) {
                    okBTN.setText(context.getString(android.R.string.ok));
                    displayMsg(false, context.getString(R.string.note_reset_password_email));

                    resetPassState = RESET_PASS_DONE;
                } else {
                    resetPassState = RESET_PASS_ERROR;
                    userPassET.setEnabled(true);
                    userPassET.setCursorVisible(true);
                    displayErrorMsg(context.getString(R.string.warning_unable_to_connect));
                }

                LViewUtils.setAlpha(okBTN, 1.0f);
                okBTN.setEnabled(true);
                break;
        }
    }

    private void hideIME() {
        try {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(userPassET.getWindowToken(), 0);
            userPassET.setCursorVisible(false);
        } catch (Exception e) {
        }
    }

    private void setupOkButton() {
        if (userPass.length() > 3) {
            if (isResetPass) {
                if (!TextUtils.isEmpty(userPassET.getText()) && android.util.Patterns.EMAIL_ADDRESS
                        .matcher(userPassET.getText()).matches()) {
                    LViewUtils.setAlpha(okBTN, 1.0f);
                    okBTN.setEnabled(true);
                    return;
                }
            } else {
                LViewUtils.setAlpha(okBTN, 1.0f);
                okBTN.setEnabled(true);
                return;
            }
        }
        LViewUtils.setAlpha(okBTN, 0.5f);
        okBTN.setEnabled(false);
    }

    private void hideErrorMsg() {
        errorMsgV.setVisibility(View.GONE);
    }

    private void displayMsg(boolean error, String msg) {
        errorMsgV.setTextColor(error ? context.getResources().getColor(R.color.red_text_color) :
                context.getResources().getColor(R.color.base_text_color));
        errorMsgV.setText(msg);
        errorMsgV.setVisibility(View.VISIBLE);
    }

    private void displayErrorMsg(String msg) {
        displayMsg(true, msg);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.resetPass:
                    isResetPass = true;
                    showPassV.setVisibility(View.GONE);
                    resetV.setVisibility(View.INVISIBLE);
                    userPassET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    userPassET.setText("");
                    userPassET.setHint(context.getString(R.string.hint_enter_your_email_here));

                    titleTV.setText(context.getString(R.string.reset_password));
                    okBTN.setText(context.getString(R.string.reset));

                    resetPassState = RESET_PASS_INIT;
                    break;
                case R.id.dummy:
                case R.id.errorMsg:
                    hideIME();
                    break;

                case R.id.showPassView:
                    checkboxShowPass.setChecked(!checkboxShowPass.isChecked());
                    if (checkboxShowPass.isChecked()) {
                        userPassET.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    } else {
                        userPassET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    break;

                default:
                    boolean dismissMe = true;
                    try {
                        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context
                                .INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(userPassET.getWindowToken(), 0);
                    } catch (Exception e) {
                    }

                    try {
                        if (v.getId() == R.id.confirmDialog) {
                            if (isResetPass) {
                                dismissMe = doResetPass();
                                if (dismissMe) callback.onChangePassDialogExit(false);
                            } else {
                                if (userPass.length() <= 0) {
                                    callback.onChangePassDialogExit(false);
                                } else {
                                    dismissMe = false;
                                    startTimer();
                                    LAppServer.getInstance().UiSignIn(LPreferences.getUserId(), userPass);
                                }
                            }
                        } else {
                            callback.onChangePassDialogExit(false);
                        }
                    } catch (Exception e) {
                        LLog.e(TAG, "unexpected error: " + e.getMessage());
                    }
                    if (dismissMe) destroy();
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

    private void startTimer() {
        countDownTimer = new CountDownTimer(16000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                displayErrorMsg(context.getString(R.string.warning_unable_to_connect));
                progressBar.setVisibility(View.GONE);
                LViewUtils.setAlpha(okBTN, 1.0f);
                okBTN.setEnabled(true);

                userPassET.setEnabled(true);
                userPassET.setCursorVisible(true);

                if (isResetPass)
                    resetPassState = RESET_PASS_ERROR;
            }
        }.start();

        hideErrorMsg();
        LViewUtils.setAlpha(okBTN, 0.5f);
        okBTN.setEnabled(false);

        userPassET.setEnabled(false);
        userPassET.setCursorVisible(false);

        progressBar.setVisibility(View.VISIBLE);
    }

    private boolean doResetPass() {
        switch (resetPassState) {
            case RESET_PASS_INIT:
            case RESET_PASS_ERROR:
                startTimer();
                LAppServer.getInstance().UiResetPassword(LPreferences.getUserId(), userPassET.getText().toString());
                break;

            case RESET_PASS_DONE:
                return true;
        }
        return false;
    }
}
