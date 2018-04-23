package com.swoag.logalong.fragments;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.MainService;
import com.swoag.logalong.R;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.CountDownTimer;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LChangePassDialog;
import com.swoag.logalong.views.LUpdateProfileDialog;

public class ProfileEdit implements LChangePassDialog.LChangePassDialogItf, LUpdateProfileDialog
        .LUpdateProfileDialogItf,
        LBroadcastReceiver.BroadcastReceiverListener {
    private static final String TAG = ProfileEdit.class.getSimpleName();

    private static final int MAX_USER_ID_LEN = 16;
    private static final int MAX_USER_NAME_LEN = 32;
    public static final int MAX_USER_PASS_LEN = 32;
    //public static final int MAX_USER_NUMBER_LEN = 10;
    private Activity activity;
    private View parentView, rootView, userNameV, userPassV, /*userNumberV, userNumberHintV,*/
            saveV, changePassV,
            showPassV, /*userIdOkV, userPassOkV,*/ newUserV;
    //private View checkUserIdAvailabilityV;
    private View dummyV;
    //private ProgressBar checkUserIdAvailabilityProgressBar;
    private CheckBox checkboxShowPass;
    //private RadioButton newUserBtn, loginUserBtn;
    private CheckBox loginUserBtn;
    private EditText userIdTV, userNameTV, userPassTV/*, userNumberTV*/;
    private TextWatcher userIdTextWatcher, userNameTextWatcher, userPassTextWatcher/*, userNumberTextWatcher*/;
    private String oldUserId, oldUserName, oldUserPass/*, oldUserNumber*/;
    private String userId, userName, userPass/*, userNumber*/;
    private ProfileEditItf callback;
    private TextView errorMsgV;
    private CountDownTimer countDownTimer;
    private MyClickListener myClickListener;
    private BroadcastReceiver broadcastReceiver;
    private boolean userIdPresent = false;

    public interface ProfileEditItf {
        public void onProfileEditExit();
    }

    public ProfileEdit(Activity activity, View rootView, ProfileEditItf callback) {
        this.activity = activity;
        this.parentView = rootView;
        this.rootView = parentView.findViewById(R.id.profileSettings);
        this.callback = callback;
        myClickListener = new MyClickListener();
        create();
    }

    private void create() {
        userIdTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String txt = userIdTV.getText().toString();
                userId = txt.replaceAll("\\s+", "");
                userId = userId.replaceAll("[^A-Za-z0-9.]", "");
                while (userId.length() > MAX_USER_ID_LEN) {
                    userId = userId.substring(0, MAX_USER_ID_LEN);
                }
                if (!userId.contentEquals(txt)) {
                    userIdTV.setText(userId);
                    userIdTV.setSelection(userId.length());
                }
                hideMsg();
                setUserIdDisplayControls();
                setupSaveButton();
            }
        };

        userNameTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String txt = userNameTV.getText().toString();
                userName = txt.replaceAll("^\\s+", "");
                while (userName.length() > MAX_USER_NAME_LEN) {
                    userName = userName.substring(0, MAX_USER_NAME_LEN);
                }
                if (!userName.contentEquals(txt)) {
                    userNameTV.setText(userName);
                    userNameTV.setSelection(userName.length());
                }
                setupSaveButton();
            }
        };

        userPassTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String txt = userPassTV.getText().toString();
                userPass = txt.trim();
                while (userPass.length() > MAX_USER_PASS_LEN) {
                    userPass = userPass.substring(0, MAX_USER_PASS_LEN);
                }
                if (!userPass.contentEquals(txt)) {
                    userPassTV.setText(userPass);
                    userPassTV.setSelection(userPass.length());
                }
                //if (userPass.length() > 3) {
                //    LViewUtils.setAlpha(userPassOkV, 1.0f);
                //} else {
                //    LViewUtils.setAlpha(userPassOkV, 0.3f);
                //}
                setupSaveButton();
            }
        };

        /*
        userNumberTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String txt = userNumberTV.getText().toString();
                userNumber = txt.replaceAll("^\\s+", "");
                while (userNumber.length() > MAX_USER_NUMBER_LEN) {
                    userNumber = userNumber.substring(0, MAX_USER_NUMBER_LEN);
                }
                if (!userNumber.contentEquals(txt)) {
                    userNumberTV.setText(userNumber);
                    userNumberTV.setSelection(userNumber.length());
                }
                setupSaveButton();
            }
        };
        */

        errorMsgV = (TextView) setViewListener(rootView, R.id.errorMsg);
        hideMsg();
        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_UI_UPDATE_USER_PROFILE,
                LBroadcastReceiver.ACTION_GET_USER_BY_NAME}, this);

        newUserV = rootView.findViewById(R.id.createUserView);
        //newUserBtn = (RadioButton) setViewListener(rootView, R.id.createUser);
        loginUserBtn = (CheckBox) setViewListener(rootView, R.id.loginUser);
        dummyV = setViewListener(rootView, R.id.dummy);

        //checkUserIdAvailabilityV = setViewListener(rootView, R.id.checkUserIdAvailability);
        //checkUserIdAvailabilityProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        //checkUserIdAvailabilityProgressBar.setVisibility(View.GONE);

        //userIdOkV = setViewListener(rootView, R.id.userIdOk);
        //userPassOkV = setViewListener(rootView, R.id.userPassOk);

        saveV = setViewListener(parentView, R.id.add);
        LViewUtils.setAlpha(saveV, 0.5f);
        saveV.setEnabled(false);

        userNameV = setViewListener(rootView, R.id.nameView);
        userPassV = setViewListener(rootView, R.id.passView);
        changePassV = setViewListener(rootView, R.id.changePassOption);
        //userNumberV = setViewListener(rootView, R.id.userNumberView);
        //userNumberHintV = setViewListener(rootView, R.id.userNumberViewHint);

        checkboxShowPass = (CheckBox) rootView.findViewById(R.id.showPass);
        checkboxShowPass.setClickable(false);
        showPassV = setViewListener(rootView, R.id.showPassView);

        userIdTV = (EditText) setViewListener(rootView, R.id.userId);

        userNameTV = (EditText) setViewListener(rootView, R.id.userName);
        userNameTV.addTextChangedListener(userNameTextWatcher);

        userPassTV = (EditText) setViewListener(rootView, R.id.userPass);
        userPassTV.addTextChangedListener(userPassTextWatcher);

        //userNumberTV = (EditText) setViewListener(rootView, R.id.userNumber);
        //userNumberTV.addTextChangedListener(userNumberTextWatcher);

        //userIdTV.setTextScaleX(1.0f);
        //userIdTV.setTextSize(18);
        //userIdTV.setTypeface(Typeface.MONOSPACE);

        oldUserId = userId = LPreferences.getUserId();
        oldUserName = userName = LPreferences.getUserName();
        oldUserPass = userPass = LPreferences.getUserPass();
        //oldUserNumber = userNumber = LPreferences.getUserNumber();

        if ((!TextUtils.isEmpty(userId)) && (!TextUtils.isEmpty(userPass))) {
            enableDisplayForLoggedInUser();
        } else {
            userIdTV.addTextChangedListener(userIdTextWatcher);

            userPassV.setVisibility(View.VISIBLE);
            showPassV.setVisibility(View.VISIBLE);
            changePassV.setVisibility(View.GONE);

            //userIdOkV.setVisibility(View.VISIBLE);
            //userPassOkV.setVisibility(View.VISIBLE);
            //LViewUtils.setAlpha(userIdOkV, 0.3f);
            //LViewUtils.setAlpha(userPassOkV, 0.3f);

            newUserV.setVisibility(View.VISIBLE);
            //checkUserIdAvailabilityV.setVisibility(View.VISIBLE);
            //checkUserIdAvailabilityV.setEnabled(false);
            userNameV.setVisibility(View.VISIBLE);
            //userNumberV.setVisibility(View.VISIBLE);
            //userNumberHintV.setVisibility(View.VISIBLE);

            userIdPresent = false;
        }
        //newUserBtn.setChecked(true);
        loginUserBtn.setChecked(false);

        userIdTV.setText(userId);
        userPassTV.setText(userPass);
        userNameTV.setText(userName);
        //userNumberTV.setText(userNumber);

        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        hideIME();
    }

    private void enableDisplayForLoggedInUser() {
        userPassV.setVisibility(View.GONE);
        showPassV.setVisibility(View.GONE);
        changePassV.setVisibility(View.VISIBLE);
        //userIdOkV.setVisibility(View.GONE);
        //userPassOkV.setVisibility(View.GONE);
        newUserV.setVisibility(View.GONE);
        userNameV.setVisibility(View.VISIBLE);
        //userNumberV.setVisibility(View.GONE);
        //userNumberHintV.setVisibility(View.GONE);
        userIdTV.setEnabled(false);
        LViewUtils.setAlpha(userIdTV, 0.6f);
        userIdPresent = true;
    }

    private void setUserIdDisplayControls() {
        /*
        checkUserIdAvailabilityV.setVisibility((newUserBtn.isChecked() && (!userIdPresent)) ? View.VISIBLE : View
                .INVISIBLE);
        if (userId.length() > 1) {
            if (newUserBtn.isChecked()) {
                LViewUtils.setAlpha(userIdOkV, 0.5f);
                LViewUtils.setAlpha(checkUserIdAvailabilityV, 1.0f);
                checkUserIdAvailabilityV.setEnabled(true);
            } else {
                LViewUtils.setAlpha(userIdOkV, 1.0f);
            }
        } else {
            LViewUtils.setAlpha(userIdOkV, 0.3f);
            LViewUtils.setAlpha(checkUserIdAvailabilityV, 0.3f);
            checkUserIdAvailabilityV.setEnabled(false);
        }
        */
    }

    private void setupSaveButton() {
        boolean enable = false;
        if ((userId.length() > 1) && (userPass.length() > 3)) {
            if (loginUserBtn.isChecked()) {
                enable = true;
            } else {
                if (/*(userNumber.length() > 2) && */(userName.length() > 1) &&
                        ((!oldUserId.contentEquals(userId)) || (!oldUserPass.contentEquals(userPass))
                                || (!oldUserName.contentEquals(userName))/* || (!oldUserNumber.contentEquals
                                (userNumber))*/)) {
                    enable = true;
                }
            }
        }

        if (enable) {
            LViewUtils.setAlpha(saveV, 1.0f);
            saveV.setEnabled(true);
        } else {
            LViewUtils.setAlpha(saveV, 0.5f);
            saveV.setEnabled(false);
        }
    }

    private void destroy() {
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }

        checkUserIdAvailabilityStop();
        if (userNameTV != null) {
            userNameTV.removeTextChangedListener(userNameTextWatcher);
            userNameTextWatcher = null;
            userNameTV = null;
        }
        /*
        if (userNumberTV != null) {
            userNumberTV.removeTextChangedListener(userNumberTextWatcher);
            userNumberTextWatcher = null;
            userNumberTV = null;
        }
        */
        if (userIdTV != null) {
            userIdTV.removeTextChangedListener(userIdTextWatcher);
            userIdTextWatcher = null;
            userIdTV = null;
        }
        if (userPassTV != null) {
            userPassTV.removeTextChangedListener(userPassTextWatcher);
            userPassTextWatcher = null;
            userPassTV = null;
        }
        saveV = null;
    }

    private void requestInputFocus(EditText etv) {
        etv.setCursorVisible(true);
        try {
            if (etv.requestFocus()) {
                InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context
                        .INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(etv, 0);
            }
        } catch (Exception e) {
        }
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                //case R.id.createUser:
                case R.id.loginUser:
                    hideMsg();
                    setupSaveButton();
                    setUserIdDisplayControls();
                    userNameV.setVisibility(loginUserBtn.isChecked() ? View.GONE : View.VISIBLE);
                    //userNumberV.setVisibility(newUserBtn.isChecked() ? View.VISIBLE : View.GONE);
                    //userNumberHintV.setVisibility(newUserBtn.isChecked() ? View.VISIBLE : View.GONE);
                    break;
                case R.id.userId:
                    if (!userIdPresent) requestInputFocus(userIdTV);
                    break;
                case R.id.userName:
                    requestInputFocus(userNameTV);
                    break;
                //case R.id.userNumber:
                //    requestInputFocus(userNumberTV);
                //    break;
                case R.id.userPass:
                    requestInputFocus(userPassTV);
                    break;

                case R.id.showPassView:
                    checkboxShowPass.setChecked(!checkboxShowPass.isChecked());
                    if (checkboxShowPass.isChecked()) {
                        userPassTV.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    } else {
                        userPassTV.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    break;

                case R.id.changePassOption:
                    LChangePassDialog changePassDialog = new LChangePassDialog(activity, ProfileEdit.this);
                    changePassDialog.show();
                    break;

                case R.id.errorMsg:
                //case R.id.userIdOk:
                //case R.id.userPassOk:
                case R.id.dummy:
                    //case R.id.userNumberView:
                    //case R.id.userNumberViewHint:
                case R.id.nameView:
                case R.id.passView:
                    hideIME();
                    break;

                //case R.id.checkUserIdAvailability:
                //    checkUserIdAvailabilityStart();
                //    break;
                case R.id.add: //SAVE
                    int action;
                    if ((!TextUtils.isEmpty(LPreferences.getUserId())) && (!TextUtils.isEmpty(LPreferences
                            .getUserPass()))) {
                        action = LUpdateProfileDialog.UPDATE_USER;
                    } else if (!loginUserBtn.isChecked()) {
                        action = LUpdateProfileDialog.NEW_USER;
                    } else {
                        action = LUpdateProfileDialog.LOGIN_USER;
                    }
                    LUpdateProfileDialog updateProfileDialog = new LUpdateProfileDialog(activity, ProfileEdit.this,
                            action, userId, userPass, userName);
                    updateProfileDialog.setCancelable(false);
                    updateProfileDialog.show();
                    break;
            }
        }
    }

    public void dismiss() {
        myClickListener.disableEnable(false);
        destroy();
        this.callback.onProfileEditExit();
    }

    private void hideMsg() {
        errorMsgV.setVisibility(View.GONE);
    }

    private void displayMsg(boolean error, String msg) {
        //checkUserIdAvailabilityProgressBar.setVisibility(View.GONE);
        errorMsgV.setTextColor(error ? activity.getResources().getColor(R.color.red_text_color) :
                activity.getResources().getColor(R.color.base_text_color));
        errorMsgV.setText(msg);
        errorMsgV.setVisibility(View.VISIBLE);
    }

    private void disableEnableAllControls(boolean enable) {
        userIdTV.setEnabled(enable);
        userPassTV.setEnabled(enable);
        userNameTV.setEnabled(enable);
        //userNumberTV.setEnabled(enable);
        //newUserBtn.setEnabled(enable);
        loginUserBtn.setEnabled(enable);
        showPassV.setEnabled(enable);
        saveV.setEnabled(enable);
    }

    private void checkUserIdAvailabilityStart() {
        countDownTimer = new CountDownTimer(16000, 1000) {
            private boolean checkOnce = true;

            @Override
            public void onTick(long millisUntilFinished) {
                if (LAppServer.getInstance().UiIsConnected()) {
                    if (checkOnce) checkUserIdAvailability();
                    checkOnce = false;
                }
            }

            @Override
            public void onFinish() {
                displayMsg(true, activity.getString(R.string.warning_unable_to_connect));
                //checkUserIdAvailabilityV.setVisibility(View.VISIBLE);

                disableEnableAllControls(true);

                LViewUtils.setAlpha(saveV, 0.5f);
                saveV.setEnabled(false);
            }
        }.start();
        //checkUserIdAvailabilityProgressBar.setVisibility(View.VISIBLE);
        //checkUserIdAvailabilityV.setVisibility(View.GONE);
        hideIME();
        hideMsg();
        disableEnableAllControls(false);

        if (!LAppServer.getInstance().UiIsConnected()) {
            LAppServer.getInstance().connect();
            MainService.start(activity);
        }
    }

    private void checkUserIdAvailabilityStop() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        disableEnableAllControls(true);
    }

    private void checkUserIdAvailability() {
        LAppServer.getInstance().UiGetUserByName(userId);
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

    private void hideIME() {
        try {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(userIdTV.getWindowToken(), 0);
            inputManager.hideSoftInputFromWindow(userNameTV.getWindowToken(), 0);
            //inputManager.hideSoftInputFromWindow(userNumberTV.getWindowToken(), 0);
            inputManager.hideSoftInputFromWindow(userPassTV.getWindowToken(), 0);

            userIdTV.setCursorVisible(false);
            userNameTV.setCursorVisible(false);
            userPassTV.setCursorVisible(false);
            //userNumberTV.setCursorVisible(false);
        } catch (Exception e) {
        }
    }

    @Override
    public void onChangePassDialogExit(boolean changed) {
        if (changed) {
            userPassV.setVisibility(View.VISIBLE);
            showPassV.setVisibility(View.VISIBLE);
            changePassV.setVisibility(View.INVISIBLE);
            checkboxShowPass.setChecked(true);
            userPassTV.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            userPassTV.setText(LPreferences.getUserPass());

            //userNumberV.setVisibility(View.VISIBLE);
            //userNumberHintV.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onUpdateProfileDialogExit(boolean success) {
        if (success) {
            userName = LPreferences.getUserName(); //user full name may get updated on first signin
            userNameTV.setText(userName);

            //userNumber = LPreferences.getUserNumber();
            //userNumberTV.setText(userNumber);
            enableDisplayForLoggedInUser();
            hideMsg();
        }
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_GET_USER_BY_NAME:
                disableEnableAllControls(true);
                setupSaveButton();
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                if (!loginUserBtn.isChecked()) {
                    if (ret != LProtocol.RSPS_OK) {
                        displayMsg(false, activity.getResources().getString(R.string.note_user_id_available));
                    } else {
                        LViewUtils.setAlpha(saveV, 0.5f);
                        saveV.setEnabled(false);
                        displayMsg(true, activity.getResources().getString(R.string.warning_user_id_taken));
                    }
                }
                break;
            case LBroadcastReceiver.ACTION_UI_UPDATE_USER_PROFILE:
                oldUserName = userName = LPreferences.getUserName();
                userNameTV.setText(userName);

                //oldUserNumber = userNumber = LPreferences.getUserNumber();
                //userNumberTV.setText(userNumber);
                break;
        }
    }
}
