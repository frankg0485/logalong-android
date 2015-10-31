package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class LWarnDialog extends Dialog implements Dialog.OnDismissListener {
    private static final String TAG = LWarnDialog.class.getSimpleName();

    LWarnDialogItf callback;
    private String title, msg, checkboxTitle;
    private boolean showCheckbox;
    private Context context;
    private View okView, confirmView;
    private CheckBox checkBoxConfirm;
    private MyClickListener myClickListener;
    private Object obj;

    boolean confirmed = false;
    boolean ok = false;

    public interface LWarnDialogItf {
        public void onWarnDialogExit(Object obj, boolean confirm, boolean ok);
    }

    public LWarnDialog(Context context, Object obj, LWarnDialogItf callback,
                       String title, String msg, String checkboxTitle, boolean showCheckbox) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.callback = callback;
        this.title = title;
        this.checkboxTitle = checkboxTitle;
        this.msg = msg;
        this.obj = obj;
        this.showCheckbox = showCheckbox;
        myClickListener = new MyClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.warn_dialog);

        confirmView = findViewById(R.id.confirmView);
        if (showCheckbox) {
            confirmView.setVisibility(View.VISIBLE);
            confirmView.setOnClickListener(myClickListener);
            checkBoxConfirm = (CheckBox) findViewById(R.id.checkboxConfirm);
            checkBoxConfirm.setText(checkboxTitle);
            checkBoxConfirm.setChecked(false);
        } else {
            confirmView.setVisibility(View.GONE);
        }

        findViewById(R.id.cancelDialog).setOnClickListener(myClickListener);
        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);

        okView = findViewById(R.id.confirmDialog);
        okView.setOnClickListener(myClickListener);

        if (showCheckbox) {
            okView.setClickable(false);
            LViewUtils.setAlpha(okView, 0.5f);
        }

        ((TextView) findViewById(R.id.title)).setText(title);
        ((TextView) findViewById(R.id.msg)).setText(msg);

        setOnDismissListener(this);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            boolean ret = true;
            try {
                switch (v.getId()) {
                    case R.id.confirmDialog:
                        ok = true;
                        break;
                    case R.id.cancelDialog:
                    case R.id.closeDialog:
                        break;

                    case R.id.confirmView:
                        confirmed = !confirmed;
                        checkBoxConfirm.setChecked(confirmed);
                        if (confirmed) {
                            okView.setClickable(true);
                            LViewUtils.setAlpha(okView, 1.0f);
                        } else {
                            okView.setClickable(false);
                            LViewUtils.setAlpha(okView, 0.5f);
                        }
                        ret = false;
                        break;
                }
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error: " + e.getMessage());
            }
            if (ret) dismiss();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        callback.onWarnDialogExit(obj, confirmed, ok);
    }
}
