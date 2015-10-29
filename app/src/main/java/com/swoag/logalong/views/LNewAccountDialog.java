package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;

import java.util.ArrayList;
import java.util.List;

public class LNewAccountDialog extends Dialog implements TextWatcher {
    private static final String TAG = LNewAccountDialog.class.getSimpleName();

    LNewAccountDialogItf callback;
    private String title;
    private String hint;
    private EditText text;
    private Context context;
    private int id;
    private View okView;
    private View payeePayerView, accountShowBalanceView;
    private CheckBox checkBoxAttr1, checkBoxAttr2, checkBoxAccountShowBalance;
    private boolean isNameAvailable;
    private boolean attr1, attr2;
    private MyClickListener myClickListener;

    public interface LNewAccountDialogItf {
        // return FALSE to keep this dialog alive.
        public boolean onNewAccountDialogExit(int id, boolean created, String name, boolean attr1, boolean attr2);

        public boolean isNewAccountNameAvailable(String name);
    }

    public LNewAccountDialog(Context context, int id, LNewAccountDialogItf callback,
                             String title, String hint, boolean attr1, boolean attr2) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.callback = callback;
        this.title = title;
        this.hint = hint;
        this.id = id;
        this.attr1 = attr1;
        this.attr2 = attr2;
        myClickListener = new MyClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.new_account_dialog);

        accountShowBalanceView = findViewById(R.id.accountShowBalanceView);
        checkBoxAccountShowBalance = (CheckBox) accountShowBalanceView.findViewById(R.id.checkboxAccountShowBalance);

        payeePayerView = findViewById(R.id.payeePayerView);
        checkBoxAttr1 = (CheckBox) payeePayerView.findViewById(R.id.checkBoxPayee);
        checkBoxAttr2 = (CheckBox) payeePayerView.findViewById(R.id.checkBoxPayer);
        if (id == R.id.vendors) {
            payeePayerView.setVisibility(View.VISIBLE);
            checkBoxAttr1.setOnClickListener(myClickListener);
            checkBoxAttr2.setOnClickListener(myClickListener);

            checkBoxAttr1.setChecked(attr1);
            checkBoxAttr2.setChecked(attr2);
        } else {
            payeePayerView.setVisibility(View.GONE);
        }

        if (id == R.id.accounts) {
            accountShowBalanceView.setVisibility(View.VISIBLE);
            checkBoxAccountShowBalance.setOnClickListener(myClickListener);
            checkBoxAccountShowBalance.setChecked(attr1);
        } else {
            accountShowBalanceView.setVisibility(View.GONE);
        }

        findViewById(R.id.cancelDialog).setOnClickListener(myClickListener);
        okView = findViewById(R.id.confirmDialog);
        okView.setOnClickListener(myClickListener);

        isNameAvailable = false;
        okView.setClickable(false);
        LViewUtils.setAlpha(okView, 0.5f);

        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);

        ((TextView) findViewById(R.id.title)).setText(title);
        text = (EditText) findViewById(R.id.newname);
        text.setHint((hint != null) ? hint : "");
        text.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (callback.isNewAccountNameAvailable(text.getText().toString().trim())) {
            if (!isNameAvailable) {
                isNameAvailable = true;
                okView.setClickable(true);
                LViewUtils.setAlpha(okView, 1.0f);
            }
        } else {
            if (isNameAvailable) {
                isNameAvailable = false;
                okView.setClickable(false);
                LViewUtils.setAlpha(okView, 0.5f);
            }
        }
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            try {
                InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(text.getWindowToken(), 0);
            } catch (Exception e) {
            }
            boolean ret = true;
            try {
                switch (v.getId()) {
                    case R.id.confirmDialog:
                        String name = text.getText().toString();
                        name = name.trim();

                        if (name.length() <= 0) {
                            ret = callback.onNewAccountDialogExit(id, false, null, false, false);
                        } else {
                            if (id == R.id.accounts) {
                                attr1 = checkBoxAccountShowBalance.isChecked();
                            } else {
                                attr1 = checkBoxAttr1.isChecked();
                            }
                            attr2 = checkBoxAttr2.isChecked();
                            ret = callback.onNewAccountDialogExit(id, true, name, attr1, attr2);
                        }
                        break;
                    case R.id.cancelDialog:
                    case R.id.closeDialog:
                        ret = callback.onNewAccountDialogExit(id, false, null, false, false);
                        break;

                    case R.id.checkBoxPayee:
                    case R.id.checkBoxPayer:
                        ret = false;
                        if ((!checkBoxAttr1.isChecked()) && (!checkBoxAttr2.isChecked())) {
                            checkBoxAttr1.setChecked(true);
                        }
                        break;
                    case R.id.checkboxAccountShowBalance:
                        ret = false;
                        break;
                }
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error: " + e.getMessage());
            }
            if (ret) dismiss();
        }
    }
}
