package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.swoag.logalong.LApp;

import java.util.HashMap;

public class LBroadcastReceiver {
    public static final String EXTRA_RET_CODE = "ert";
    private static final String ACTION_BASE = "com.swoag.logalong.action.";
    public static final int ACTION_POLL_ACKED = 1;
    public static final int ACTION_POLL_IDLE = 2;
    public static final int ACTION_USER_CREATED = 4;
    public static final int ACTION_LOGIN = 5;
    public static final int ACTION_GET_SHARE_USER_BY_ID = 10;
    public static final int ACTION_GET_SHARE_USER_BY_NAME = 20;
    public static final int ACTION_SHARE_ACCOUNT_WITH_USER = 30;
    public static final int ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH = 40;
    public static final int ACTION_CONFIRMED_ACCOUNT_SHARE = 50;
    public static final int ACTION_SHARED_TRANSITION_RECORD = 60;
    public static final int ACTION_JOURNAL_POSTED = 70;
    public static final int ACTION_JOURNAL_RECEIVED = 71;
    public static final int ACTION_SHARE_ACCOUNT_USER_CHANGE = 80;
    public static final int ACTION_USER_PROFILE_UPDATED = 84;
    public static final int ACTION_NETWORK_CONNECTED = 90;

    private static LBroadcastReceiver instance;

    public static String action(int id) {
        return ACTION_BASE + id;
    }

    public static LBroadcastReceiver getInstance() {
        if (instance == null) {
            instance = new LBroadcastReceiver();
        }
        return instance;
    }

    public interface BroadcastReceiverListener {
        public void onBroadcastReceiverReceive(int action, int ret, Intent intent);
    }

    private LBroadcastReceiver() {
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        private BroadcastReceiverListener listener;

        private MyBroadcastReceiver(BroadcastReceiverListener listener) {
            this.listener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String str = intent.getAction();
            String ss[] = str.split("\\.");
            int action = Integer.parseInt(ss[ss.length - 1]);
            int ret = intent.getIntExtra(EXTRA_RET_CODE, (int) 0);
            listener.onBroadcastReceiverReceive(action, ret, intent);
        }
    }

    public BroadcastReceiver register(int action, BroadcastReceiverListener listener) {
        BroadcastReceiver broadcastReceiver = new MyBroadcastReceiver(listener);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_BASE + action);
        LocalBroadcastManager.getInstance(LApp.ctx).registerReceiver(broadcastReceiver, intentFilter);
        return broadcastReceiver;
    }

    public BroadcastReceiver register(int[] actions, BroadcastReceiverListener listener) {
        BroadcastReceiver broadcastReceiver = new MyBroadcastReceiver(listener);

        IntentFilter intentFilter = new IntentFilter();
        for (int act : actions) {
            intentFilter.addAction(ACTION_BASE + act);
        }
        LocalBroadcastManager.getInstance(LApp.ctx).registerReceiver(broadcastReceiver, intentFilter);
        return broadcastReceiver;
    }

    public void unregister(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(LApp.ctx).unregisterReceiver(receiver);
    }
}
