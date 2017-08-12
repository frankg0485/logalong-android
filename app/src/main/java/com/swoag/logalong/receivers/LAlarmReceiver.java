package com.swoag.logalong.receivers;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.swoag.logalong.LApp;
import com.swoag.logalong.MainService;
import com.swoag.logalong.utils.LAlarm;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;

import java.util.Date;

public class LAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = LAlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int action = intent.getIntExtra("action", 0);
        switch (action) {
            case LAlarm.ACTION_AUTO_RECONNECT:
                if (!TextUtils.isEmpty(LPreferences.getUserId())) {
                    LLog.d(TAG, "auto reconnect at: " + (new Date()));
                    MainService.start(LApp.ctx);
                }
                break;

            case LAlarm.ACTION_SCHEDULE: {
                //TODO
                /*
                int scheduleId = intent.getIntExtra(LAlarm.SCHEDULE_ID, 0);

                LScheduledTransaction sch = DBScheduledTransaction.getById(scheduleId);
                if (sch == null) {
                    LLog.w(TAG, "schedule no longer valid: " + scheduleId);
                    return;
                }

                if (sch.getItem().getState() != DBHelper.STATE_ACTIVE) {
                    //this schedule has been disabled
                    return;
                }

                if (sch.getTimestamp() <= System.currentTimeMillis()) {
                    // only update DB record if this is *our* alarm: meaning it must have been scheduled in the past
                    Calendar calendar = Calendar.getInstance();
                    // always use GMT timezone y/m/d to avoid duplicated schedule entry.
                    calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
                    calendar.setTimeInMillis(sch.getTimestamp());
                    String ymd = "" + calendar.get(Calendar.YEAR) + (calendar.get(Calendar.MONTH) + 1) + calendar.get
                    (Calendar.DAY_OF_MONTH);

                    LTransaction item = DBTransaction.getByRid(sch.getItem().getRid() + ymd);
                    if (item == null) {
                        item = new LTransaction(sch.getItem());

                        item.setTimeStampLast(LPreferences.getServerUtc());
                        item.setRid(item.getRid() + ymd);
                        item.setTimeStamp(sch.getTimestamp());

                        DBTransaction.add(item, true, true);

                    } else {
                        //this is the case where other party has already had alarm triggered and created the DB entry
                        long saveId = item.getId();

                        //copy over current schedule settings, in case *current* schedule has update
                        item.copy(sch.getItem());

                        //restore the following fields after the copy
                        item.setId(saveId);
                        item.setTimeStampLast(LPreferences.getServerUtc());
                        item.setRid(item.getRid() + ymd);
                        item.setTimeStamp(sch.getTimestamp());

                        DBTransaction.update(item, true);
                    }
                } else {
                    LLog.w(TAG, "schedule already happened? " + (new Date(sch.getTimestamp())) + " now: " + (new Date
                    ()));
                }

                // always check to schedule next alarm locally.
                sch.calculateNextTimeMs();
                sch.setAlarm();
                DBScheduledTransaction.update(sch);
                */
            }
        }

    }
}
