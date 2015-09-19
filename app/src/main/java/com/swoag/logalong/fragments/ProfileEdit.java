package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.views.LReminderDialog;
import com.swoag.logalong.views.LRenameDialog;

public class ProfileEdit implements View.OnClickListener,
        LRenameDialog.LRenameDialogItf {
    private static final String TAG = ProfileEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private TextView userNameTV;
    private String userName;

    public ProfileEdit(Activity activity, View rootView) {
        this.activity = activity;
        this.rootView = rootView;

        create();
    }

    private void create() {
        userName = LPreferences.getUserName();
        //TODO: validate userName and request one if empty
        userNameTV = (TextView) setViewListener(rootView, R.id.userName);
        userNameTV.setText(userName);
    }

    private void destroy() {
        userNameTV = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.userName:
                LRenameDialog dialog = new LRenameDialog(activity, null, this, activity.getString(R.string.change_user_name),
                        userName, activity.getString(R.string.new_user_name));
                dialog.show();
                break;
        }
    }

    public void dismiss() {
        destroy();
    }

    @Override
    public void onRenameDialogExit(Object id, boolean renamed, String name) {
        if (renamed) {
            userNameTV.setText(name);
            userName = name;
            LPreferences.setUserName(userName);
        }
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(this);
        return view;
    }
}

