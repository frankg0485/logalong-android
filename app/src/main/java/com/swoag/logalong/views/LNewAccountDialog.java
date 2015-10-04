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
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LViewUtils;

import java.util.ArrayList;
import java.util.List;

public class LNewAccountDialog extends Dialog implements
        View.OnClickListener, TextWatcher {
    private static final String TAG = LNewAccountDialog.class.getSimpleName();

    LNewAccountDialogItf callback;
    private String title;
    private String hint;
    private EditText text;
    private Context context;
    private Object id;
    private View okView;
    private boolean isNameAvailable;

    public interface LNewAccountDialogItf {
        // return FALSE to keep this dialog alive.
        public boolean onNewAccountDialogExit(Object id, boolean created, String name);

        public boolean isNewAccountNameAvailable(String name);
    }

    public LNewAccountDialog(Context context, Object id, LNewAccountDialogItf callback,
                             String title, String hint) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.callback = callback;
        this.title = title;
        this.hint = hint;
        this.id = id;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.new_account_dialog);

        findViewById(R.id.cancelDialog).setOnClickListener(this);
        okView = findViewById(R.id.confirmDialog);
        okView.setOnClickListener(this);

        isNameAvailable = false;
        okView.setClickable(false);
        LViewUtils.setAlpha(okView, 0.5f);

        findViewById(R.id.closeDialog).setOnClickListener(this);

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

    @Override
    public void onClick(View v) {
        try {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(text.getWindowToken(), 0);
        } catch (Exception e) {
        }
        boolean ret = true;
        try {
            List<String> msg = new ArrayList<String>();
            if (v.getId() == R.id.confirmDialog) {
                String name = text.getText().toString();
                name = name.trim();

                if (name.length() <= 0) {
                    ret = callback.onNewAccountDialogExit(id, false, null);
                } else {
                    ret = callback.onNewAccountDialogExit(id, true, name);
                }
            } else {
                ret = callback.onNewAccountDialogExit(id, false, null);
            }
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
        if (ret) dismiss();
    }
}
