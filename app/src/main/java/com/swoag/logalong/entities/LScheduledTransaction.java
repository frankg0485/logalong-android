package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.LAlarm;
import com.swoag.logalong.utils.LLog;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class LScheduledTransaction {
    private static final String TAG = LScheduledTransaction.class.getSimpleName();

    public static final int REPEAT_UNIT_WEEK = 10;
    public static final int REPEAT_UNIT_MONTH = 20;

    LTransaction item;
    int repeatCount;
    int repeatUnit;
    int repeatInterval;
    long timestamp; //next alarm time

    private void init() {
        this.item = new LTransaction();
        this.repeatCount = 0;
        this.repeatInterval = 1;
        this.repeatUnit = REPEAT_UNIT_MONTH;
        this.timestamp = 0;
    }

    public LScheduledTransaction() {
        init();
    }

    public LScheduledTransaction(LTransaction item) {
        init();
        this.item = item;
    }

    public LScheduledTransaction(int repeatInterval, int repeatUnit, int repeatCount, long timestamp, LTransaction item) {
        this.repeatInterval = repeatInterval;
        this.repeatUnit = repeatUnit;
        this.repeatCount = repeatCount;
        this.timestamp = timestamp;
        this.item = item;
    }

    public LScheduledTransaction(LScheduledTransaction sch) {
        this.repeatUnit = sch.getRepeatUnit();
        this.repeatInterval = sch.getRepeatInterval();
        this.repeatCount = sch.getRepeatCount();
        this.timestamp = sch.getTimestamp();

        this.item = new LTransaction(sch.getItem());
    }

    public boolean isEqual(LScheduledTransaction sch) {
        return (this.repeatCount == sch.getRepeatCount() &&
                this.repeatInterval == sch.getRepeatInterval() &&
                this.repeatUnit == sch.getRepeatUnit() &&
                this.timestamp == sch.getTimestamp() &&
                this.item.isEqual(sch.getItem()));
    }

    public void calculateNextTimeMs() {
        long baseTimeMs = item.getTimeStamp();
        int count = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(baseTimeMs);
        while (baseTimeMs <= System.currentTimeMillis()) {
            if (repeatUnit == REPEAT_UNIT_MONTH) {
                calendar.add(Calendar.MONTH, repeatInterval);
                baseTimeMs = calendar.getTimeInMillis();
            } else {
                baseTimeMs += (long) repeatInterval * 7 * 24 * 3600 * 1000;
                calendar.setTimeInMillis(baseTimeMs);
            }
            count++;
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        timestamp = calendar.getTimeInMillis();

        if (repeatCount == 0) return;

        if (repeatCount < count) {
            item.setState(DBHelper.STATE_DISABLED);
            timestamp = item.getTimeStamp();
        }
    }

    public void cancelAlarm() {
        LAlarm.cancelAlarm((int) item.getId());
    }

    public void setAlarm() {
        cancelAlarm();
        if (item.getState() == DBHelper.STATE_ACTIVE) {
            LLog.d(TAG, "alarm " + item.getId() + " set: " + (new Date(timestamp)));
            LAlarm.setAlarm((int) item.getId(), timestamp);
        }
    }

    public LTransaction getItem() {
        return item;
    }

    public void setItem(LTransaction item) {
        this.item = item;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public int getRepeatUnit() {
        return repeatUnit;
    }

    public void setRepeatUnit(int repeatUnit) {
        this.repeatUnit = repeatUnit;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
