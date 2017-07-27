package com.swoag.logalong.views;
/* Copyright (C) 2017 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.fragments.ProfileEdit;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

import java.util.ArrayList;
import java.util.List;

public class LChangePassDialog extends Dialog implements TextWatcher {
    private static final String TAG = LChangePassDialog.class.getSimpleName();

    private LChangePassDialogItf callback;
    private EditText userPassET;
    private CheckBox checkboxShowPass;
    private Context context;
    private Object id;
    private MyClickListener myClickListener;
    private String userPass = "";
    private TextView errorMsgV;
    private View okV;

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

        okV = findViewById(R.id.confirmDialog);
        findViewById(R.id.cancelDialog).setOnClickListener(myClickListener);
        okV.setOnClickListener(myClickListener);
        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);
        findViewById(R.id.dummy).setOnClickListener(myClickListener);

        userPassET = (EditText) findViewById(R.id.currentPass);
        userPassET.addTextChangedListener(this);
        checkboxShowPass = (CheckBox) findViewById(R.id.showPass);
        checkboxShowPass.setClickable(false);
        findViewById(R.id.showPassView).setOnClickListener(myClickListener);

        setupOkButton();
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

    private void hideIME() {
        try {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(userPassET.getWindowToken(), 0);
            userPassET.setCursorVisible(false);
        } catch (Exception e) {
        }
    }

    private void setupOkButton() {
        if (userPass.length() > 3) {
            LViewUtils.setAlpha(okV, 1.0f);
            okV.setEnabled(true);
        } else {
            LViewUtils.setAlpha(okV, 0.5f);
            okV.setEnabled(false);
        }
    }

    private void hideErrorMsg() {
        errorMsgV.setVisibility(View.GONE);
    }

    private void displayErrorMsg(String msg) {
        errorMsgV.setText(msg);
        errorMsgV.setVisibility(View.VISIBLE);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
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
                        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(userPassET.getWindowToken(), 0);
                    } catch (Exception e) {
                    }

                    try {
                        if (v.getId() == R.id.confirmDialog) {
                            if (userPass.length() <= 0) {
                                callback.onChangePassDialogExit(false);
                            } else {
                                if (userPass.contentEquals(LPreferences.getUserPass())) {
                                    callback.onChangePassDialogExit(true);
                                } else {
                                    dismissMe = false;
                                    displayErrorMsg(context.getString(R.string.warning_password_mismatch));
                                }
                            }
                        } else {
                            callback.onChangePassDialogExit(false);
                        }
                    } catch (Exception e) {
                        LLog.e(TAG, "unexpected error: " + e.getMessage());
                    }
                    if (dismissMe) dismiss();
            }
        }
    }
}
