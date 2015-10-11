package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ViewFlipper;

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.fragments.ScheduledTransactionEdit;

public class ScheduleActivity extends Activity implements View.OnClickListener,
        ScheduledTransactionEdit.TransitionEditItf {
    private ViewFlipper viewFlipper;
    private ScheduledTransactionEdit edit;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scheduled_transactions);

        rootView = findViewById(R.id.scheduleEdit);
        findViewById(R.id.goback).setOnClickListener(this);
        findViewById(R.id.add).setOnClickListener(this);

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.goback:
                finish();
                break;

            case R.id.add:
                addNewSchedule();
                break;
        }
    }

    @Override
    public void onTransactionEditExit(int action, boolean changed) {
        viewFlipper.setInAnimation(ScheduleActivity.this, R.anim.slide_in_left);
        viewFlipper.setOutAnimation(ScheduleActivity.this, R.anim.slide_out_right);
        viewFlipper.showPrevious();
    }

    private void addNewSchedule() {
        LTransaction item = new LTransaction();
        edit = new ScheduledTransactionEdit(this, rootView, item, true, this);

        viewFlipper.setInAnimation(this, R.anim.slide_in_right);
        viewFlipper.setOutAnimation(this, R.anim.slide_out_left);
        viewFlipper.showNext();
    }
}
