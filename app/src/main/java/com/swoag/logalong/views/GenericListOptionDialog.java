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

import org.w3c.dom.Text;

import java.util.Dictionary;

public class GenericListOptionDialog extends Dialog implements
        View.OnClickListener, DialogInterface.OnDismissListener {

    private Object context;
    private String title;
    private int listId;
    private GenericListOptionDialogItf callback;
    private CheckBox checkBoxPayee, checkBoxPayer;
    private View payeePayerView;
    private boolean attr1, attr2;

    public interface GenericListOptionDialogItf {
        public void onGenericListOptionDialogDismiss(final Object context, boolean attr1, boolean attr2);
        public boolean onGenericListOptionDialogExit(final Object context, int viewId);
    }

    public GenericListOptionDialog(Context parent, final Object context, String title, int listId,
                                   GenericListOptionDialogItf callback, boolean attr1, boolean attr2) {
        super(parent, android.R.style.Theme_Translucent_NoTitleBar);
        this.context = context;
        this.title = title;
        this.callback = callback;
        this.listId = listId;
        this.attr1 = attr1;
        this.attr2 = attr2;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        callback.onGenericListOptionDialogDismiss(context, checkBoxPayee.isChecked(), checkBoxPayer.isChecked());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.generic_list_option_dialog);

        TextView tv = (TextView) findViewById(R.id.title);
        tv.setText(title);

        findViewById(R.id.closeDialog).setOnClickListener(this);
        findViewById(R.id.remove).setOnClickListener(this);
        findViewById(R.id.rename).setOnClickListener(this);

        payeePayerView = findViewById(R.id.payeePayerType);
        checkBoxPayee = (CheckBox) payeePayerView.findViewById(R.id.checkBoxPayee);
        checkBoxPayer = (CheckBox) payeePayerView.findViewById(R.id.checkBoxPayer);

        switch (listId) {
            case R.id.vendors:
                View view = findViewById(R.id.associated_categories);
                view.setVisibility(View.VISIBLE);
                view.setOnClickListener(this);

                payeePayerView.setVisibility(View.VISIBLE);
                checkBoxPayee.setOnClickListener(this);
                checkBoxPayer.setOnClickListener(this);
                checkBoxPayee.setChecked(attr1);
                checkBoxPayer.setChecked(attr2);
                break;
        }
        this.setOnDismissListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.closeDialog:
                break;

            case R.id.checkBoxPayer:
            case R.id.checkBoxPayee:
                if ((!checkBoxPayee.isChecked()) && (!checkBoxPayer.isChecked())) {
                    checkBoxPayee.setChecked(true);
                }
                return;

            default:
                if (callback.onGenericListOptionDialogExit(context, v.getId()))
                    return;
                break;
        }
        dismiss();
    }
}
