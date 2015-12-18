package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.view.View;

import java.util.HashMap;

public abstract class LOnClickListener implements View.OnClickListener {
    private static final long debounceMs = 300;
    private HashMap<View, Long> hashMap = new HashMap<View, Long>();
    private boolean enabled = true;

    public void disableEnable(boolean b) {
        enabled = b;
    }

    @Override
    public final void onClick(View v) {
        if (!enabled) return;

        long now = System.currentTimeMillis();
        Long val = hashMap.get(v);
        long lastMs = (val != null) ? val.longValue() : 0;

        if (now - lastMs > debounceMs) onClicked(v);
        hashMap.put(v, now);
    }

    public abstract void onClicked(View v);
}
