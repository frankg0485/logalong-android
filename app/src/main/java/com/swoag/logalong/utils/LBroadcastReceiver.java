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
    public static final String EXTRA_RET_CODE = "drt";
    public static final String ACTION_GET_SHARE_USER_BY_ID = "com.swoag.logalong.gsubi";
    public static final String ACTION_GET_SHARE_USER_BY_NAME = "com.swoag.logalong.gsubn";

    private BroadcastReceiver broadcastReceiver;
    private HashMap<String, BroadcastReceiverListener> listenerHashMap;

    private static LBroadcastReceiver instance;

    public static LBroadcastReceiver getInstance() {
        if (instance == null) {
            instance = new LBroadcastReceiver();
        }
        return instance;
    }

    public interface BroadcastReceiverListener {
        public void onBroadcastReceiverReceive(String action, int ret, Intent intent);
    }

    private LBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int ret = intent.getIntExtra(EXTRA_RET_CODE, (int) 0);
                listenerHashMap.get(action).onBroadcastReceiverReceive(action, ret, intent);
            }
        };
        listenerHashMap = new HashMap<String, BroadcastReceiverListener>();
    }

    public void register(String action, BroadcastReceiverListener listener) {
        listenerHashMap.put(action, listener);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(action);
        LocalBroadcastManager.getInstance(LApp.ctx).registerReceiver(broadcastReceiver, intentFilter);
    }
}
