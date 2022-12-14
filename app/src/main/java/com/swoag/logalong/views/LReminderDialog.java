package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LOnClickListener;

public class LReminderDialog extends Dialog {
    private static final String TAG = LReminderDialog.class.getSimpleName();

    private TextView text;
    private Context context;
    private String msg;
    private MyClickListener myClickListener;

    public LReminderDialog(Context context, String msg) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.msg = msg;
        myClickListener = new MyClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.reminder_dialog);

        TextView textView = (TextView) findViewById(R.id.msg);
        textView.setText(msg);
        findViewById(R.id.confirmDialog).setOnClickListener(myClickListener);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            if (v.getId() == R.id.confirmDialog) {
                dismiss();
            }
        }
    }
}
