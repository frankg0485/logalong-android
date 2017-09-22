package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;

public class GenericListOptionDialog extends Dialog implements
        DialogInterface.OnDismissListener {

    private Object context;
    private String title;
    private int listId;
    private GenericListOptionDialogItf callback;
    private CheckBox checkBoxPayee, checkBoxPayer, checkBoxAccountShowBalance;
    private View payeePayerView, accountShowBalanceView;
    private boolean attr1, attr2, allowDelete;
    private MyClickListener myClickListener;

    public interface GenericListOptionDialogItf {
        public void onGenericListOptionDialogDismiss(final Object context, boolean attr1, boolean attr2);

        public boolean onGenericListOptionDialogExit(final Object context, int viewId);
    }

    public GenericListOptionDialog(Context parent, final Object context, String title, int listId,
                                   GenericListOptionDialogItf callback, boolean attr1, boolean attr2, boolean
                                           allowDelete) {
        super(parent, android.R.style.Theme_Translucent_NoTitleBar);
        this.context = context;
        this.title = title;
        this.callback = callback;
        this.listId = listId;
        this.attr1 = attr1;
        this.attr2 = attr2;
        this.allowDelete = allowDelete;
        myClickListener = new MyClickListener();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        switch (listId) {
            case R.id.vendors:
                callback.onGenericListOptionDialogDismiss(context, checkBoxPayee.isChecked(), checkBoxPayer.isChecked
                        ());
                break;
            case R.id.accounts:
                callback.onGenericListOptionDialogDismiss(context, checkBoxAccountShowBalance.isChecked(), false);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.generic_list_option_dialog);

        TextView tv = (TextView) findViewById(R.id.title);
        tv.setText(title);

        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);
        View v = findViewById(R.id.remove);
        if (allowDelete)
            v.setOnClickListener(myClickListener);
        else {
            //LViewUtils.setAlpha(v, 0.90f);
            //v.setEnabled(false);
            v.setVisibility(View.GONE);
        }
        findViewById(R.id.rename).setOnClickListener(myClickListener);

        payeePayerView = findViewById(R.id.payeePayerType);
        checkBoxPayee = (CheckBox) payeePayerView.findViewById(R.id.checkBoxPayee);
        checkBoxPayer = (CheckBox) payeePayerView.findViewById(R.id.checkBoxPayer);

        accountShowBalanceView = findViewById(R.id.accountShowBalanceView);
        checkBoxAccountShowBalance = (CheckBox) accountShowBalanceView.findViewById(R.id.checkboxAccountShowBalance);

        switch (listId) {
            case R.id.vendors:
                View view = findViewById(R.id.associated_categories);
                view.setVisibility(View.VISIBLE);
                view.setOnClickListener(myClickListener);

                payeePayerView.setVisibility(View.VISIBLE);
                checkBoxPayee.setOnClickListener(myClickListener);
                checkBoxPayer.setOnClickListener(myClickListener);
                checkBoxPayee.setChecked(attr1);
                checkBoxPayer.setChecked(attr2);
                break;
            case R.id.accounts:
                accountShowBalanceView.setVisibility(View.VISIBLE);
                checkBoxAccountShowBalance.setOnClickListener(myClickListener);
                checkBoxAccountShowBalance.setChecked(attr1);
                break;
        }
        this.setOnDismissListener(this);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.closeDialog:
                    break;

                case R.id.checkBoxPayer:
                case R.id.checkBoxPayee:
                    if ((!checkBoxPayee.isChecked()) && (!checkBoxPayer.isChecked())) {
                        checkBoxPayee.setChecked(true);
                    }
                    return;
                case R.id.checkboxAccountShowBalance:
                    return;

                default:
                    //findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                    if (callback.onGenericListOptionDialogExit(context, v.getId())) {
                        View vv = findViewById(R.id.catchAll);
                        LViewUtils.smoothFade(vv, true, 1000);
                        return;
                    }
                    break;
            }
            //findViewById(R.id.progressBar).setVisibility(View.GONE);
            dismiss();
        }
    }
}
