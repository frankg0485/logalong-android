package com.swoag.logalong.receivers;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.swoag.logalong.LApp;
import com.swoag.logalong.MainService;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LScheduledTransaction;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.LAlarm;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class LAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = LAlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int action = intent.getIntExtra("action", 0);
        switch (action) {
            case LAlarm.ACTION_AUTO_RECONNECT:
                LLog.d(TAG, "auto reconnect at: " + (new Date()));
                MainService.start(LApp.ctx);
                break;

            case LAlarm.ACTION_SCHEDULE: {
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
                    calendar.setTimeInMillis(sch.getTimestamp());
                    String ymd = "" + calendar.get(Calendar.YEAR) + (calendar.get(Calendar.MONTH) + 1) + calendar.get(Calendar.DAY_OF_MONTH);

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
                    LLog.w(TAG, "schedule already happened? " + (new Date(sch.getTimestamp())) + " now: " + (new Date()));
                }

                // always check to schedule next alarm locally.
                sch.calculateNextTimeMs();
                sch.setAlarm();
                DBScheduledTransaction.update(sch);
            }
        }
    }
}
