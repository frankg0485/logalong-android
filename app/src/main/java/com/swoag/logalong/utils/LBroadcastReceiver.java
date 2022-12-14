package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.swoag.logalong.LApp;

public class LBroadcastReceiver {
    private static final String TAG = LBroadcastReceiver.class.getSimpleName();
    public static final String EXTRA_RET_CODE = "ret";
    private static final String ACTION_BASE = "com.swoag.logalong.action.";

    public static final int ACTION_USER_CREATED = 4;
    public static final int ACTION_CONNECTED_TO_SERVER = 10;
    public static final int ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH = 40;

    public static final int ACTION_NETWORK_CONNECTED = 50;
    public static final int ACTION_NETWORK_DISCONNECTED = 51;
    public static final int ACTION_GET_USER_BY_NAME = 52;
    public static final int ACTION_CREATE_USER = 54;
    public static final int ACTION_SIGN_IN = 56;
    public static final int ACTION_LOG_IN = 58;
    public static final int ACTION_UPDATE_USER_PROFILE = 60;
    public static final int ACTION_POST_JOURNAL = 62;
    public static final int ACTION_POLL = 63;
    public static final int ACTION_POLL_ACK = 64;

    public static final int ACTION_NEW_JOURNAL_AVAILABLE = 300;

    public static final int ACTION_UI_UPDATE_USER_PROFILE = 500;
    public static final int ACTION_UI_UPDATE_ACCOUNT = 501;
    public static final int ACTION_UI_UPDATE_CATEGORY = 502;
    public static final int ACTION_UI_UPDATE_TAG = 503;
    public static final int ACTION_UI_UPDATE_VENDOR = 504;
    public static final int ACTION_UI_SHARE_ACCOUNT = 505;
    public static final int ACTION_UI_NET_IDLE = 510;
    public static final int ACTION_UI_NET_BUSY = 512;
    public static final int ACTION_UI_RESET_PASSWORD = 515;

    public static final int ACTION_REQUESTED_TO_SET_ACCOUNT_GID = 100;
    public static final int ACTION_REQUESTED_TO_UPDATE_ACCOUNT_SHARE = 101;
    public static final int ACTION_REQUESTED_TO_UPDATE_ACCOUNT_INFO = 102;
    public static final int ACTION_REQUESTED_TO_UPDATE_SHARE_USER_PROFILE = 103;
    public static final int ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORD = 113;
    public static final int ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORDS = 114;
    public static final int ACTION_REQUESTED_TO_SHARE_TRANSITION_CATEGORY = 115;
    public static final int ACTION_REQUESTED_TO_SHARE_TRANSITION_PAYER = 116;
    public static final int ACTION_REQUESTED_TO_SHARE_TRANSITION_TAG = 117;
    public static final int ACTION_REQUESTED_TO_SHARE_PAYER_CATEGORY = 118;
    public static final int ACTION_REQUESTED_TO_SHARE_SCHEDULE = 119;

    public static final int ACTION_PUSH_NOTIFICATION = 7777;;
    public static final int ACTION_SERVER_BROADCAST_MSG_RECEIVED = 1000;
    public static final int ACTION_UNKNOWN_MSG = 9999;

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
            try {
                String str = intent.getAction();
                String ss[] = str.split("\\.");
                int action = Integer.parseInt(ss[ss.length - 1]);
                int ret = intent.getIntExtra(EXTRA_RET_CODE, (int) 0);
                listener.onBroadcastReceiverReceive(action, ret, intent);
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error, broadcast receiver failed: " + e.getMessage());
            }
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

    public static String getActionName(int action) {
        switch (action) {
            case ACTION_USER_CREATED:
                return "USER_CREATED";
            case ACTION_CONNECTED_TO_SERVER:
                return "CONNECTED_TO_SERVER";
            case ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH:
                return "SHARE_ACCOUNT_WITH";

            case ACTION_NETWORK_CONNECTED:
                return "NETWORK_CONNECTED";
            case ACTION_NETWORK_DISCONNECTED:
                return "NETWORK_DISCONNECTED";
            case ACTION_GET_USER_BY_NAME:
                return "GET_USER_BY_NAME";
            case ACTION_CREATE_USER:
                return "CREATE_USER";
            case ACTION_SIGN_IN:
                return "SIGN_IN";
            case ACTION_LOG_IN:
                return "LOG_IN";
            case ACTION_UPDATE_USER_PROFILE:
                return "UPDATE_USER_PROFILE";
            case ACTION_POST_JOURNAL:
                return "POST_JOURNAL";
            case ACTION_POLL:
                return "POLL";
            case ACTION_POLL_ACK:
                return "POLL_ACK";

            case ACTION_NEW_JOURNAL_AVAILABLE:
                return "NEW_JOURNAL_AVAILABLE";

            case ACTION_UI_UPDATE_USER_PROFILE:
                return "UI_UPDATE_PROFILE";
            case ACTION_UI_UPDATE_ACCOUNT:
                return "UI_UPDATE_USER_ACCOUNT";
            case ACTION_UI_UPDATE_CATEGORY:
                return "UI_UPDATE_CATEGORY";
            case ACTION_UI_UPDATE_TAG:
                return "UI_UPDATE_TAG";
            case ACTION_UI_UPDATE_VENDOR:
                return "UI_UPDATE_VENDOR";
            case ACTION_UI_SHARE_ACCOUNT:
                return "UI_SHARE_ACCOUNT";
            case ACTION_UI_NET_IDLE:
                return "UI_NET_IDLE";
            case ACTION_UI_NET_BUSY:
                return "UI_NET_BUSY";
            case ACTION_UI_RESET_PASSWORD:
                return "UI_RESET_PASSWORD";

            case ACTION_REQUESTED_TO_SET_ACCOUNT_GID:
                return "REQUESTED_TO_SET_ACCOUNT_GID";
            case ACTION_REQUESTED_TO_UPDATE_ACCOUNT_SHARE:
                return "REQUESTED_TO_UPDATE_ACCOUNT_SHARE";
            case ACTION_REQUESTED_TO_UPDATE_ACCOUNT_INFO:
                return "REQUESTED_TO_UPDATE_ACCOUNT_INFO";
            case ACTION_REQUESTED_TO_UPDATE_SHARE_USER_PROFILE:
                return "REQUESTED_TO_UPDATE_SHARE_USER_PROFILE";
            case ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORD:
                return "REQUESTED_TO_SHARE_TRANSITION_RECORD";
            case ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORDS:
                return "REQUESTED_TO_SHARE_TRANSITION_RECORDS";
            case ACTION_REQUESTED_TO_SHARE_TRANSITION_CATEGORY:
                return "REQUESTED_TO_SHARE_TRANSITION_CATEGORY";
            case ACTION_REQUESTED_TO_SHARE_TRANSITION_PAYER:
                return "REQUESTED_TO_SHARE_TRANSITION_PAYER";
            case ACTION_REQUESTED_TO_SHARE_TRANSITION_TAG:
                return "REQUESTED_TO_SHARE_TRANSITION_TAG";
            case ACTION_REQUESTED_TO_SHARE_PAYER_CATEGORY:
                return "REQUESTED_TO_SHARE_PAYER_CATEGORY";
            case ACTION_REQUESTED_TO_SHARE_SCHEDULE:
                return "REQUESTED_TO_SHARE_SCHEDULE";

            case ACTION_PUSH_NOTIFICATION:
                return "PUSH_NOTIFICATION";
            case ACTION_SERVER_BROADCAST_MSG_RECEIVED:
                return "SERVER_BROADCAST_MSG_RECEIVED";
            case ACTION_UNKNOWN_MSG:
                return "UNKNOWN_MSG";
            default:
                return "Unknown Message";
        }
    }
}
