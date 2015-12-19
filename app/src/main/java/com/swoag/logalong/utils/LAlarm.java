package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.swoag.logalong.LApp;
import com.swoag.logalong.receivers.LAlarmReceiver;

import java.util.Date;

public class LAlarm {
    private static final String TAG = LAlarm.class.getSimpleName();

    public static final String SCHEDULE_ID = "scheduleId";
    public static final int ACTION_SCHEDULE = 10;
    public static final int ACTION_AUTO_RECONNECT = 20;
    private static final AlarmManager alarmManager =
            (AlarmManager) LApp.ctx.getSystemService(Context.ALARM_SERVICE);

    public static void setAlarm(int scheduleId, long timems) {
        Intent alarmIntent = new Intent(LApp.ctx, LAlarmReceiver.class);
        alarmIntent.putExtra("action", ACTION_SCHEDULE);
        alarmIntent.putExtra(SCHEDULE_ID, scheduleId);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast
                (LApp.ctx, scheduleId, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
        LLog.d(TAG, "schedule alarm set to: " + (new Date(timems)));
        alarmManager.set(AlarmManager.RTC_WAKEUP, timems, alarmPendingIntent);
    }

    public static void cancelAlarm(int scheduleId) {
        Intent alarmIntent = new Intent(LApp.ctx, LAlarmReceiver.class);
        alarmIntent.putExtra("action", ACTION_SCHEDULE);
        alarmIntent.putExtra(SCHEDULE_ID, scheduleId);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast
                (LApp.ctx, scheduleId, alarmIntent, PendingIntent.FLAG_ONE_SHOT);

        alarmManager.cancel(alarmPendingIntent);
    }

    public static void setAutoReconnectAlarm(long timems) {
        Intent alarmIntent = new Intent(LApp.ctx, LAlarmReceiver.class);
        alarmIntent.putExtra("action", ACTION_AUTO_RECONNECT);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast
                (LApp.ctx, -1, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
        LLog.d(TAG, "auto reconnect alarm set to: " + (new Date(timems)));
        alarmManager.set(AlarmManager.RTC_WAKEUP, timems, alarmPendingIntent);
    }

    public static void cancelAutoReconnectAlarm() {
        Intent alarmIntent = new Intent(LApp.ctx, LAlarmReceiver.class);
        alarmIntent.putExtra("action", ACTION_AUTO_RECONNECT);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast
                (LApp.ctx, -1, alarmIntent, PendingIntent.FLAG_ONE_SHOT);

        alarmManager.cancel(alarmPendingIntent);
    }
}
