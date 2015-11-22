package com.swoag.logalong.receivers;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.swoag.logalong.LApp;
import com.swoag.logalong.MainService;

public class BootIntentReceiver extends BroadcastReceiver {

    private static final String TAG = BootIntentReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                MainService.start(LApp.ctx);
            }
        } catch (RuntimeException e) {
        }
    }
}
