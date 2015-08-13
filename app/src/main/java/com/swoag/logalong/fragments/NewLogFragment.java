package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.R;

public class NewLogFragment extends LFragment implements View.OnClickListener
{
    private static final String TAG = NewLogFragment.class.getSimpleName();
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_accounts, container, false);
        return rootView;
    }

    @Override
    public void onDestroyView () {
        super.onDestroyView();
    }

    @Override
    public void onSelected(boolean selected) {
    }

    @Override
    public void onPause() {
        super.onPause();
    };

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default :
                break;
        }
    }
}
