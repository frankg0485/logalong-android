package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.view.View;

public abstract class LOnClickListener implements View.OnClickListener {
    private static final long debounceMs = 500;
    private long lastMs;

    @Override
    public final void onClick(View v) {
        long now = System.currentTimeMillis();
        if (now - lastMs > debounceMs) onClicked(v);
        lastMs = now;
    }

    public abstract void onClicked(View v);
}
