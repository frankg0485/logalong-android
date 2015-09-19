package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LLog;

import java.util.ArrayList;
import java.util.List;

public class LRenameDialog extends Dialog implements
        android.view.View.OnClickListener {
    private static final String TAG = LRenameDialog.class.getSimpleName();

    LRenameDialogItf callback;
    private String oldname;
    private String title;
    private String hint;
    private EditText text;
    private Context context;
    private Object id;

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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rename_dialog);

        findViewById(R.id.cancelDialog).setOnClickListener(this);
        findViewById(R.id.confirmDialog).setOnClickListener(this);
        findViewById(R.id.closeDialog).setOnClickListener(this);

        ((TextView) findViewById(R.id.title)).setText(title);
        TextView from = (TextView) findViewById(R.id.oldname);
        from.setText(oldname);
        text = (EditText) findViewById(R.id.newname);
        text.setHint((hint != null) ? hint : "");
    }

    @Override
    public void onClick(View v) {
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
