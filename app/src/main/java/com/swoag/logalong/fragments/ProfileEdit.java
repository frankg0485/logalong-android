package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.CountDownTimer;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LReminderDialog;
import com.swoag.logalong.views.LRenameDialog;

import org.w3c.dom.Text;

public class ProfileEdit implements LBroadcastReceiver.BroadcastReceiverListener {
    private static final String TAG = ProfileEdit.class.getSimpleName();

    private static final int MAX_USER_NAME_LEN = 16;
    //private static final int MAX_USER_PASS_LEN = 16;
    private Activity activity;
    private View rootView, saveV;
    //private CheckBox checkboxShowPass;
    private EditText userNameTV;
    private TextView userIdTV;
    private TextWatcher userTextWatcher;
    //private EditText userPassTV;
    private String oldUserId;
    private String oldUserName;
    //private String userPass;
    private ProfileEditItf callback;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private TextView errorMsgV;

    private BroadcastReceiver broadcastReceiver;
    private MyClickListener myClickListener;

    public interface ProfileEditItf {
        public void onProfileEditExit();
    }

    public ProfileEdit(Activity activity, View rootView, ProfileEditItf callback) {
        this.activity = activity;
        this.rootView = rootView;
        this.callback = callback;
        myClickListener = new MyClickListener();
        create();
    }

    private void create() {
        userTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Object x = userNameTV.getText();

                String txt = userNameTV.getText().toString();
                //String str = txt.replaceAll("\\s+", "");
                String str = txt.replaceAll("^\\s+", "");
                str = str.replaceAll("[^ A-Za-z0-9]", "");
                while (str.length() > MAX_USER_NAME_LEN) {
                    str = str.substring(0, MAX_USER_NAME_LEN);
                }
                if (!str.contentEquals(txt)) {
                    userNameTV.setText(str);
                    userNameTV.setSelection(str.length());
                }

                if (str.isEmpty() || str.length() < 2) {
                    LViewUtils.setAlpha(saveV, 0.5f);
                    saveV.setEnabled(false);
                } else {
                    LViewUtils.setAlpha(saveV, 1.0f);
                    saveV.setEnabled(true);
                }
            }
        };

        oldUserId = LPreferences.getUserName();

        userIdTV = (TextView) rootView.findViewById(R.id.userId);
        userIdTV.setTextScaleX(1.0f);
        userIdTV.setTextSize(18);
        userIdTV.setTypeface(Typeface.MONOSPACE);
        userIdTV.setText(oldUserId);

        oldUserName = LPreferences.getUserFullName();
        //userPass = LPreferences.getUserPass();
        //checkboxShowPass = (CheckBox) rootView.findViewById(R.id.showPass);
        saveV = setViewListener(rootView, R.id.add);

        if (oldUserName.isEmpty()) {
            LViewUtils.setAlpha(saveV, 0.5f);
            saveV.setEnabled(false);
        } else {
            LViewUtils.setAlpha(saveV, 1.0f);
            saveV.setEnabled(true);
        }

        /*if (userPass.isEmpty()) {
            checkboxShowPass.setVisibility(View.VISIBLE);
        } else {
            checkboxShowPass.setVisibility(View.GONE);
        }*/

        userNameTV = (EditText) setViewListener(rootView, R.id.userName);
        userNameTV.addTextChangedListener(userTextWatcher);
        /*userPassTV = (EditText) setViewListener(rootView, R.id.userPass);
        userPassTV.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String txt = userPassTV.getText().toString();
                String str = txt.trim();
                while (str.length() > MAX_USER_PASS_LEN) {
                    str = str.substring(0, MAX_USER_PASS_LEN);
                }
                if (!str.contentEquals(txt)) {
                    userPassTV.setText(str);
                    userPassTV.setSelection(str.length());
                }
            }
        });*/

        userNameTV.setText(oldUserName);
        //userPassTV.setText(userPass);
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        errorMsgV = (TextView) rootView.findViewById(R.id.errorMsg);
        hideErrorMsg();
        hideIME();

        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_USER_PROFILE_UPDATED}, this);
    }

    private void destroy() {
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        if (userNameTV != null) {
            userNameTV.removeTextChangedListener(userTextWatcher);
            userTextWatcher = null;
            userNameTV = null;
        }

        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
        saveV = null;
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_USER_PROFILE_UPDATED:
                if (countDownTimer != null) countDownTimer.cancel();
                progressBar.setVisibility(View.GONE);
                if (ret == LProtocol.RSPS_OK) {
                    if (oldUserId.isEmpty()) {
                        userIdTV.setText(LPreferences.getUserName());
                    } else if (this.callback != null) {
                        dismiss();
                        break;
                    }
                } else {
                    displayErrorMsg(activity.getString(R.string.warning_unable_to_update_profile));
                    restoreOldProfile();
                }
                break;
        }
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.userName:
                    userNameTV.setCursorVisible(true);
                    try {
                        if (userNameTV.requestFocus()) {
                            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            keyboard.showSoftInput(userNameTV, 0);
                        }
                    } catch (Exception e) {
                    }
                    break;
            /*case R.id.userPass:
                userPassTV.setCursorVisible(true);
                try {
                    if (userPassTV.requestFocus()) {
                        InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        keyboard.showSoftInput(userPassTV, 0);
                    }
                } catch (Exception e) {
                }
                break;

            case R.id.showPass:
                checkboxShowPass.setChecked(!checkboxShowPass.isChecked());
                if (checkboxShowPass.isChecked()) {
                    userPassTV.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    userPassTV.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                break;*/

                case R.id.add:
                    countDownTimer = new CountDownTimer(15000, 15000) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            displayErrorMsg(activity.getString(R.string.warning_get_share_user_time_out));
                            restoreOldProfile();
                            progressBar.setVisibility(View.GONE);
                        }
                    }.start();

                    progressBar.setVisibility(View.VISIBLE);

                    LPreferences.setUserFullName(userNameTV.getText().toString().trim());
                    //LPreferences.setUserPass(userPassTV.getText().toString().trim());

                    if (LPreferences.getUserName().isEmpty()) {
                        LProtocol.ui.requestUserName();
                        //profile is automatically updated when username is first-time generated.
                    } else {
                        LProtocol.ui.updateUserProfile();
                    }
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

    private void restoreOldProfile() {
        LPreferences.setUserFullName(oldUserName);
    }

    public void dismiss() {
        this.callback.onProfileEditExit();
        destroy();
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

    private void hideIME() {
        try {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(userNameTV.getWindowToken(), 0);
            //inputManager.hideSoftInputFromWindow(userPassTV.getWindowToken(), 0);
            userNameTV.setCursorVisible(false);
            //userPassTV.setCursorVisible(false);
        } catch (Exception e) {
        }
    }
}

