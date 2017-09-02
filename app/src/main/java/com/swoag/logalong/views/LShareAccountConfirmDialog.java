package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccountShareRequest;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

public class LShareAccountConfirmDialog extends Dialog implements DialogInterface.OnDismissListener {
    private static final String TAG = LShareAccountConfirmDialog.class.getSimpleName();
    public Context context;

    private LAccountShareRequest request;
    private LShareAccountConfirmDialog.LShareAccountConfirmDialogItf callback;
    private MyClickListener myClickListener;
    private Handler handler;

    private void init(Context context, LAccountShareRequest request,
                      LShareAccountConfirmDialog.LShareAccountConfirmDialogItf callback) {
        this.context = context;
        this.callback = callback;
        this.request = request;
        myClickListener = new MyClickListener();
    }

    public interface LShareAccountConfirmDialogItf {
        public void onShareAccountConfirmDialogExit(boolean ok, LAccountShareRequest request);
    }

    public LShareAccountConfirmDialog(Context context, LAccountShareRequest request,
                                      LShareAccountConfirmDialog.LShareAccountConfirmDialogItf callback) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        init(context, request, callback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.account_share_confirm_dialog);
        handler = new Handler();

        findViewById(R.id.save).setOnClickListener(myClickListener);
        findViewById(R.id.cancel).setOnClickListener(myClickListener);

        ((TextView) findViewById(R.id.request)).setText(request.getAccountName() + " : " + request.getUserFullName()
                + " (" + request.getUserName() + ")");

        LViewUtils.smoothFade(findViewById(android.R.id.content), false, 1000);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        ((Activity) context).onUserInteraction();

        try {
            if (Build.VERSION.SDK_INT >= 12) {
                return super.dispatchGenericMotionEvent(ev);
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        ((Activity) context).onUserInteraction();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        ((Activity) context).onUserInteraction();
        return super.dispatchTouchEvent(ev);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            int id = v.getId();

            switch (id) {
                case R.id.save:
                    leave(true);
                    break;
                case R.id.cancel:
                    leave(false);
                    break;
            }
        }
    }

    private void leave(boolean ok) {
        if (ok) {
            CheckBox checkBox = (CheckBox) findViewById(R.id.checkboxAccept);
            LPreferences.setShareAccept(request.getUserId(), checkBox.isChecked());
        }

        if (callback != null)
            callback.onShareAccountConfirmDialogExit(ok, request);

        LViewUtils.smoothFade(findViewById(android.R.id.content), true, 1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
                handler = null;
            }
        }, 1500);
    }
}
