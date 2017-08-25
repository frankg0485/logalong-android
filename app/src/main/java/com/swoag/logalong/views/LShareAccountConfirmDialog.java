package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountShareRequest;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.CountDownTimer;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LOnClickListener;

public class LShareAccountConfirmDialog extends Dialog implements
        LBroadcastReceiver.BroadcastReceiverListener, DialogInterface.OnDismissListener {
    private static final String TAG = LShareAccountConfirmDialog.class.getSimpleName();
    public Context context;

    private LAccountShareRequest request;
    private LShareAccountConfirmDialog.LShareAccountConfirmDialogItf callback;

    private TextView errorMsgV;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private BroadcastReceiver broadcastReceiver;
    private MyClickListener myClickListener;
    private LAccount account;

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

        //broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
        //        LBroadcastReceiver.ACTION_GET_SHARE_USER_BY_NAME}, this);

        findViewById(R.id.save).setOnClickListener(myClickListener);
        findViewById(R.id.cancel).setOnClickListener(myClickListener);

        ((TextView) findViewById(R.id.request)).setText(request.getAccountName() + " : " + request.getUserFullName()
                + " (" + request.getUserName() + ")");
        errorMsgV = (TextView) findViewById(R.id.errorMsg);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_GET_USER_BY_NAME:
                if (countDownTimer != null) countDownTimer.cancel();
                if (ret == LProtocol.RSPS_OK) {

                } else {
                    displayErrorMsg(LShareAccountConfirmDialog.this.getContext().getString(R.string.warning_unable_to_find_share_user));
                }

                progressBar.setVisibility(View.GONE);
                break;
        }
    }

    /*
    private void do_add_share_user(String name) {
        hideErrorMsg();
        progressBar.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(15000, 15000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                displayErrorMsg(LShareAccountConfirmDialog.this.getContext().getString(R.string.warning_get_share_user_time_out));
                progressBar.setVisibility(View.GONE);
            }
        }.start();
        LProtocol.ui.getShareUserByName(name);
    }
    */

    private void hideErrorMsg() {
        errorMsgV.setVisibility(View.GONE);
    }

    private void displayErrorMsg(String msg) {
        errorMsgV.setText(msg);
        errorMsgV.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            leave(false);
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
        if (callback != null)
            callback.onShareAccountConfirmDialogExit(ok, request);
        dismiss();
    }
}
