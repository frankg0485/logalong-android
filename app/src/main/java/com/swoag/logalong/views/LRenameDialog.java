package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;

import java.util.ArrayList;
import java.util.List;

public class LRenameDialog extends Dialog implements TextWatcher {
    private static final String TAG = LRenameDialog.class.getSimpleName();

    private LRenameDialogItf callback;
    private View catchallV;
    private String oldname;
    private String title;
    private String hint;
    private EditText text;
    private Context context;
    private Object id;
    private MyClickListener myClickListener;
    private boolean isNameAvailable;
    private View okView;

    public interface LRenameDialogItf {
        public void onRenameDialogExit(Object id, boolean renamed, String name);
    }

    public LRenameDialog(Context context, Object id, LRenameDialogItf callback,
                         String title, String oldname, String hint) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.callback = callback;
        this.title = title;
        this.oldname = oldname;
        this.hint = hint;
        this.id = id;
        myClickListener = new MyClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rename_dialog);

        okView = findViewById(R.id.confirmDialog);
        findViewById(R.id.cancelDialog).setOnClickListener(myClickListener);
        okView.setOnClickListener(myClickListener);
        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);

        ((TextView) findViewById(R.id.title)).setText(title);
        TextView from = (TextView) findViewById(R.id.oldname);
        from.setText(oldname);
        text = (EditText) findViewById(R.id.newname);
        text.setHint((hint != null) ? hint : "");
        text.addTextChangedListener(this);

        catchallV = findViewById(R.id.catchAll);
        catchallV.setOnClickListener(myClickListener);

        isNameAvailable = false;
        okView.setEnabled(false);
        LViewUtils.setAlpha(okView, 0.5f);
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

        if (str2.length() > LNewEntryDialog.MAX_ENTRY_NAME_LEN) {
            str2 = str2.substring(0, LNewEntryDialog.MAX_ENTRY_NAME_LEN);
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

    private boolean isEntryNameAvailable(String name) {
        boolean ret = false;
        if (name != null && (!TextUtils.isEmpty(name))) {
            return true;
        }
        return ret;
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

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            hideIME();
            if (v.getId() == R.id.catchAll) return;

            try {
                List<String> msg = new ArrayList<String>();
                if (v.getId() == R.id.confirmDialog) {
                    String name = text.getText().toString();
                    name = name.trim();

                    if (name.length() <= 0) {
                        callback.onRenameDialogExit(id, false, null);
                    } else {
                        callback.onRenameDialogExit(id, true, name);
                    }
                } else {
                    callback.onRenameDialogExit(id, false, null);
                }
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error: " + e.getMessage());
            }
            dismiss();
        }
    }
}
