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
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAllBalances;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

public class LNewEntryDialog extends Dialog implements TextWatcher {
    private static final String TAG = LNewEntryDialog.class.getSimpleName();
    public static final int TYPE_ACCOUNT = 10;
    public static final int TYPE_CATEGORY = 20;
    public static final int TYPE_VENDOR = 30;
    public static final int TYPE_TAG = 40;

    LNewEntryDialogItf callback;
    private String title;
    private String hint;
    private EditText text;
    private Context context;
    private int type;
    private int id;
    private View okView;
    private View payeePayerView, accountShowBalanceView;
    private CheckBox checkBoxAttr1, checkBoxAttr2, checkBoxAccountShowBalance;
    private boolean isNameAvailable;
    private boolean attr1, attr2;
    private MyClickListener myClickListener;

    public interface LNewEntryDialogItf {
        // return FALSE to keep this dialog alive.
        public boolean onNewEntryDialogExit(int id, int type, boolean created, String name, boolean attr1, boolean attr2);
    }

    private boolean isEntryNameAvailable(String name) {
        return (name != null && !name.isEmpty());
    }

    private boolean onExit(int type, boolean created, String name, boolean attr1, boolean attr2) {
        if (created && (!name.isEmpty())) {
            LJournal journal;
            switch (type) {
                case TYPE_ACCOUNT:
                    long did = DBAccount.getIdByName(name);
                    if (did == 0) {
                        did = DBAccount.add(new LAccount(name));
                    }
                    LPreferences.setShowAccountBalance(did, attr1);
                    LAllBalances.getInstance(true);
                    break;

                case TYPE_CATEGORY:
                    if (DBCategory.getIdByName(name) == 0) {
                        LCategory category = new LCategory(name);
                        DBCategory.add(category);

                        journal = new LJournal();
                        journal.updateCategory(category);
                    }
                    break;

                case TYPE_VENDOR:
                    int vtype = LVendor.TYPE_PAYEE;
                    if (attr1 && attr2) vtype = LVendor.TYPE_PAYEE_PAYER;
                    else if (attr1) vtype = LVendor.TYPE_PAYEE;
                    else vtype = LVendor.TYPE_PAYER;
                    LVendor vendor;
                    if (DBVendor.getIdByName(name) == 0) {
                        vendor = new LVendor(name, vtype);
                        DBVendor.add(vendor);
                    } else {
                        vendor = DBVendor.getByName(name);
                        vendor.setType(vtype);
                    }

                    journal = new LJournal();
                    journal.updateVendor(vendor);
                    break;

                case TYPE_TAG:

                    LTag tag;
                    if (DBTag.getIdByName(name) == 0) {
                        tag = new LTag(name);
                        DBTag.add(tag);
                    } else {
                        tag = DBTag.getByName(name);
                    }

                    journal = new LJournal();
                    journal.updateTag(tag);
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
        if (isEntryNameAvailable(text.getText().toString().trim())) {
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
                    case R.id.closeDialog:
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
                }
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error: " + e.getMessage());
            }
            if (ret) dismiss();
        }
    }
}
