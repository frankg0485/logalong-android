package com.swoag.logalong.views;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.LApp;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;

public class LNewEntryDialog extends Dialog implements TextWatcher {
    private static final String TAG = LNewEntryDialog.class.getSimpleName();
    public static final int TYPE_ACCOUNT = 10;
    public static final int TYPE_CATEGORY = 20;
    public static final int TYPE_VENDOR = 30;
    public static final int TYPE_TAG = 40;
    public static final int MAX_ENTRY_NAME_LEN = 30;

    LNewEntryDialogItf callback;
    private String title;
    private String hint;
    private EditText text;
    private Context context;
    private int type;
    private int id;
    private View okView;
    private View catchallV;
    private View payeePayerView, accountShowBalanceView;
    private CheckBox checkBoxAttr1, checkBoxAttr2, checkBoxAccountShowBalance;
    private boolean isNameAvailable;
    private boolean attr1, attr2;
    private MyClickListener myClickListener;

    public interface LNewEntryDialogItf {
        // return FALSE to keep this dialog alive.
        public boolean onNewEntryDialogExit(int id, int type, boolean created, String name, boolean attr1, boolean
                attr2);
    }

    private boolean isEntryNameAvailable(String name) {
        boolean ret = false;
        if (name != null && (!TextUtils.isEmpty(name))) {
            switch (type) {
                case TYPE_ACCOUNT:
                    ret = (null == DBAccount.getInstance().getByName(name));
                    break;
                case TYPE_CATEGORY:
                    ret = (null == DBCategory.getInstance().getByName(name));
                    break;
                case TYPE_TAG:
                    ret = (null == DBTag.getInstance().getByName(name));
                    break;
                case TYPE_VENDOR:
                    ret = (null == DBVendor.getInstance().getByName(name));
                    break;
            }
        }
        return ret;
    }

    private boolean onExit(int type, boolean created, String name, boolean attr1, boolean attr2) {
        if (created && (!TextUtils.isEmpty(name))) {
            LJournal journal = new LJournal();

            switch (type) {
                case TYPE_ACCOUNT:
                    DBAccount dbAccount = DBAccount.getInstance();
                    if (null == dbAccount.getByName(name)) {
                        LAccount account = new LAccount(name);
                        account.setShowBalance(attr1);
                        dbAccount.add(account);
                        journal.addAccount(account.getId());
                    } else {
                        LLog.w(TAG, "account already exists");
                    }
                    break;

                case TYPE_CATEGORY:
                    DBCategory dbCategory = DBCategory.getInstance();
                    if (null == dbCategory.getByName(name)) {
                        LCategory category = new LCategory(name);
                        dbCategory.add(category);
                        journal.addCategory(category.getId());
                    }
                    break;

                case TYPE_VENDOR:
                    int vtype = LVendor.TYPE_PAYEE;
                    if (attr1 && attr2) vtype = LVendor.TYPE_PAYEE_PAYER;
                    else if (attr1) vtype = LVendor.TYPE_PAYEE;
                    else vtype = LVendor.TYPE_PAYER;
                    DBVendor dbVendor = DBVendor.getInstance();
                    if (null == dbVendor.getByName(name)) {
                        LVendor vendor = new LVendor(name, vtype);
                        dbVendor.add(vendor);
                        journal.addVendor(vendor.getId());
                    }
                    break;

                case TYPE_TAG:
                    DBTag dbTag = DBTag.getInstance();
                    if (null == dbTag.getByName(name)) {
                        LTag tag = new LTag(name);
                        dbTag.add(tag);
                        journal.addTag(tag.getId());
                    }
                    break;

                default:
                    break;
            }
        }
        return callback.onNewEntryDialogExit(id, type, created, name, attr1, attr2);
    }

    public LNewEntryDialog(Context context, int id, int type, LNewEntryDialogItf callback,
                           String title, String hint, boolean attr1, boolean attr2) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.callback = callback;
        this.title = title;
        this.hint = hint;
        this.type = type;
        this.id = id;
        this.attr1 = attr1;
        this.attr2 = attr2;
        myClickListener = new MyClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.new_entry_dialog);

        accountShowBalanceView = findViewById(R.id.accountShowBalanceView);
        checkBoxAccountShowBalance = (CheckBox) accountShowBalanceView.findViewById(R.id.checkboxAccountShowBalance);

        payeePayerView = findViewById(R.id.payeePayerView);
        checkBoxAttr1 = (CheckBox) payeePayerView.findViewById(R.id.checkBoxPayee);
        checkBoxAttr2 = (CheckBox) payeePayerView.findViewById(R.id.checkBoxPayer);
        if (type == TYPE_VENDOR) {
            payeePayerView.setVisibility(View.VISIBLE);
            checkBoxAttr1.setOnClickListener(myClickListener);
            checkBoxAttr2.setOnClickListener(myClickListener);

            checkBoxAttr1.setChecked(attr1);
            checkBoxAttr2.setChecked(attr2);
        } else {
            payeePayerView.setVisibility(View.GONE);
        }

        if (type == TYPE_ACCOUNT) {
            accountShowBalanceView.setVisibility(View.VISIBLE);
            checkBoxAccountShowBalance.setOnClickListener(myClickListener);
            checkBoxAccountShowBalance.setChecked(attr1);
        } else {
            accountShowBalanceView.setVisibility(View.GONE);
        }

        findViewById(R.id.cancelDialog).setOnClickListener(myClickListener);

        okView = findViewById(R.id.confirmDialog);
        okView.setOnClickListener(myClickListener);
        catchallV = findViewById(R.id.catchAll);
        catchallV.setOnClickListener(myClickListener);

        isNameAvailable = false;
        okView.setEnabled(false);
        LViewUtils.setAlpha(okView, 0.5f);

        ((TextView) findViewById(R.id.title)).setText(title);
        text = (EditText) findViewById(R.id.newname);
        text.setHint((hint != null) ? hint : (type == TYPE_CATEGORY) ?
                LApp.ctx.getResources().getString(R.string.hint_primary_category_sub_category) : "");
        text.addTextChangedListener(this);

        showIME();


        qqqqq
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String str = text.getText().toString().trim();
        String str2 = str.replaceAll(",", "");

        if (str2.length() > MAX_ENTRY_NAME_LEN) {
            str2 = str2.substring(0, MAX_ENTRY_NAME_LEN);
            text.setText(str2);
            text.setSelection(str2.length());
        } else if (!str.contentEquals(str2)) {
            text.setText(str2);
            text.setSelection(str2.length());
        }

        if (isEntryNameAvailable(str2)) {
            if (!isNameAvailable) {
                isNameAvailable = true;
                okView.setEnabled(true);
                LViewUtils.setAlpha(okView, 1.0f);
            }
        } else {
            if (isNameAvailable) {
                isNameAvailable = false;
                okView.setEnabled(false);
                LViewUtils.setAlpha(okView, 0.5f);
            }
        }
    }

    private void hideIME() {
        try {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(text.getWindowToken(), 0);
            text.setCursorVisible(false);
        } catch (Exception e) {
        }
    }

    private void showIME() {
        try {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            text.setCursorVisible(true);
        } catch (Exception e) {
        }
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            hideIME();
            boolean ret = true;
            try {
                switch (v.getId()) {
                    case R.id.confirmDialog:
                        String name = text.getText().toString();
                        name = name.trim();

                        if (name.length() <= 0) {
                            ret = onExit(type, false, null, false, false);
                        } else {
                            if (type == TYPE_ACCOUNT) {
                                attr1 = checkBoxAccountShowBalance.isChecked();
                            } else {
                                attr1 = checkBoxAttr1.isChecked();
                            }
                            attr2 = checkBoxAttr2.isChecked();
                            ret = onExit(type, true, name, attr1, attr2);
                        }
                        break;
                    case R.id.cancelDialog:
                        ret = onExit(type, false, null, false, false);
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
                    case R.id.catchAll:
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
