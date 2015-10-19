package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.views.LReminderDialog;
import com.swoag.logalong.views.LRenameDialog;

public class ProfileEdit implements View.OnClickListener {
    private static final String TAG = ProfileEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private EditText userNameTV;
    private EditText userPassTV;
    private String userName;

    public ProfileEdit(Activity activity, View rootView) {
        this.activity = activity;
        this.rootView = rootView;

        create();
    }

    private void create() {
        userName = LPreferences.getUserName();
        userNameTV = (EditText) setViewListener(rootView, R.id.userName);
        userPassTV = (EditText) setViewListener(rootView, R.id.userPass);
        userNameTV.setText(userName);
        userPassTV.setText("12345678");
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    private void destroy() {
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        userNameTV = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.userName:
                break;
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

