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

import java.util.Calendar;
import java.util.UUID;

public class LAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int action = intent.getIntExtra("action", 0);
        switch (action) {
            case LAlarm.ACTION_AUTO_RECONNECT:
                MainService.start(LApp.ctx);
                break;

            case LAlarm.ACTION_SCHEDULE: {
                int scheduleId = intent.getIntExtra(LAlarm.SCHEDULE_ID, 0);

                LScheduledTransaction sch = DBScheduledTransaction.getById(scheduleId);
                if (sch.getItem().getState() != DBHelper.STATE_ACTIVE) {
                    //this schedule has been disabled
                    return;
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(sch.getTimestamp());
                String ymd = "" + calendar.get(Calendar.YEAR) + (calendar.get(Calendar.MONTH) + 1) + calendar.get(Calendar.DAY_OF_MONTH);

                LTransaction item = DBTransaction.getByRid(sch.getItem().getRid() + ymd);
                if (item == null) {
                    item = new LTransaction(sch.getItem());

                    item.setTimeStampLast(System.currentTimeMillis());
                    item.setRid(item.getRid() + ymd);
                    item.setTimeStamp(sch.getTimestamp());

                    DBTransaction.add(item);
                } else {
                    //this is the case where other party has already had alarm triggered and created the DB entry
                    long saveId = item.getId();

                    //copy over current schedule settings, in case *current* schedule has update
                    item.copy(sch.getItem());

                    //restore the following fields after the copy
                    item.setId(saveId);
                    item.setTimeStampLast(System.currentTimeMillis());
                    item.setRid(item.getRid() + ymd);
                    item.setTimeStamp(sch.getTimestamp());

                    DBTransaction.update(item);
                }
                LJournal journal = new LJournal();
                journal.updateItem(item);

                sch.calculateNextTimeMs(System.currentTimeMillis());
                sch.setAlarm();
                DBScheduledTransaction.update(sch);
            }
        }
    }
}
