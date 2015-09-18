package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.view.View;

public class ProfileEdit implements View.OnClickListener {
    private static final String TAG = ProfileEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;

    public ProfileEdit(Activity activity, View rootView) {
        this.activity = activity;
        this.rootView = rootView;
        create();
    }


    private void create() {
    }

    private void destroy() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }


    public void dismiss() {
        destroy();
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(this);
        return view;
    }
}

