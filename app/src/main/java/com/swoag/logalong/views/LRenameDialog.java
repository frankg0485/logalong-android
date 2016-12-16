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
import com.swoag.logalong.utils.LOnClickListener;

import java.util.ArrayList;
import java.util.List;

public class LRenameDialog extends Dialog implements TextWatcher {
    private static final String TAG = LRenameDialog.class.getSimpleName();

    private LRenameDialogItf callback;
    private String oldname;
    private String title;
    private String hint;
    private EditText text;
    private Context context;
    private Object id;
    private MyClickListener myClickListener;

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

        findViewById(R.id.cancelDialog).setOnClickListener(myClickListener);
        findViewById(R.id.confirmDialog).setOnClickListener(myClickListener);
        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);

        ((TextView) findViewById(R.id.title)).setText(title);
        TextView from = (TextView) findViewById(R.id.oldname);
        from.setText(oldname);
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
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            try {
                InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(text.getWindowToken(), 0);
            } catch (Exception e) {
            }

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
