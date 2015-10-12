package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.swoag.logalong.LApp;
import com.swoag.logalong.receivers.LAlarmReceiver;

public class LAlarm {
    public static final String SCHEDULE_ID = "scheduleId";
    private static final AlarmManager alarmManager =
            (AlarmManager) LApp.ctx.getSystemService(Context.ALARM_SERVICE);

    public static void setAlarm(int scheduleId, long timems) {
        Intent alarmIntent = new Intent(LApp.ctx, LAlarmReceiver.class);
        alarmIntent.putExtra(SCHEDULE_ID, scheduleId);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast
                (LApp.ctx, scheduleId, alarmIntent, PendingIntent.FLAG_ONE_SHOT);

        alarmManager.set(AlarmManager.RTC_WAKEUP, timems, alarmPendingIntent);
    }

    public static void cancelAlarm(int scheduleId) {
        Intent alarmIntent = new Intent(LApp.ctx, LAlarmReceiver.class);
        alarmIntent.putExtra(SCHEDULE_ID, scheduleId);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast
                (LApp.ctx, scheduleId, alarmIntent, PendingIntent.FLAG_ONE_SHOT);

        alarmManager.cancel(alarmPendingIntent);
    }
}
