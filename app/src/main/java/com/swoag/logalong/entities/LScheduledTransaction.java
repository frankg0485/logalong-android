package com.swoag.logalong.entities;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import java.util.Calendar;

public class LScheduledTransaction extends LTransaction {
    private static final String TAG = LScheduledTransaction.class.getSimpleName();

    public static final int START_HOUR_OF_DAY = 2; //align to 2am so not to worry about DTS
    public static final int REPEAT_UNIT_WEEK = 10;
    public static final int REPEAT_UNIT_MONTH = 20;
    private static boolean TEST_SCAN_LOGIC = false;

    //private LTransaction item;
    private boolean enabled;
    private int repeatCount;
    private int repeatUnit;
    private int repeatInterval;
    private long nextTime; //next alarm time

    public LScheduledTransaction() {
        this.enabled = true;
        this.repeatCount = 0;
        this.repeatInterval = 1;
        this.repeatUnit = REPEAT_UNIT_MONTH;
        this.nextTime = 0;
    }

    public LScheduledTransaction(LScheduledTransaction sch) {
        super(sch);
        this.enabled = sch.isEnabled();
        this.repeatUnit = sch.getRepeatUnit();
        this.repeatInterval = sch.getRepeatInterval();
        this.repeatCount = sch.getRepeatCount();
        this.nextTime = sch.getNextTime();
    }

    public boolean isEqual(LScheduledTransaction sch) {
        return (this.enabled == sch.isEnabled() &&
                this.repeatCount == sch.getRepeatCount() &&
                this.repeatInterval == sch.getRepeatInterval() &&
                this.repeatUnit == sch.getRepeatUnit() &&
                this.nextTime == sch.getNextTime() &&
                super.isEqual(sch));
    }

    /*
    public void calculateNextTimeMs() {
        nextTimeMs();
    }
    */
    public void initNextTimeMs() {
        long baseTimeMs = getTimeStamp();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(baseTimeMs);
        calendar.set(Calendar.HOUR_OF_DAY, START_HOUR_OF_DAY);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        baseTimeMs = calendar.getTimeInMillis();

        long curTimeMs = System.currentTimeMillis();
        if (baseTimeMs > curTimeMs || (curTimeMs - baseTimeMs < (long) 24 * 3600 * 1000)) nextTime = baseTimeMs;
        else if (0 == repeatCount) {
            //reset to today
            calendar.setTimeInMillis(curTimeMs);
            calendar.set(Calendar.HOUR_OF_DAY, START_HOUR_OF_DAY);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            baseTimeMs = calendar.getTimeInMillis();

            setTimeStamp(baseTimeMs);
            nextTime = baseTimeMs;
        } else {
            nextTimeMs();
        }
    }

    /*
    private long getEndMs() {
        if (repeatCount == 0) return Long.MAX_VALUE;

        long ms = getTimeStamp();

        // always align time to 00:00:00 of the day
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ms);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        ms = calendar.getTimeInMillis();

        if (repeatUnit == REPEAT_UNIT_MONTH) {
            calendar.add(Calendar.MONTH, repeatInterval * repeatCount);
            ms = calendar.getTimeInMillis();
        } else {
            ms += (long) repeatInterval * 7 * 24 * 3600 * 1000 * repeatCount;
        }
        return ms;
    }*/

    public void scanNextTimeMs() {
        return;
        //TODO
        /*
        if (nextTime >= System.currentTimeMillis()) return;

        if (nextTime == 0) {
            //special case, this must be a schedule that is just imported.
            nextTimeMs();
            DBScheduledTransaction.update(this);
            return;
        }

        //we missed the alarm, let's check to populate the DB
        long baseTimeMs = nextTime;
        long endTimeMs = getEndMs();

        // always align time to 00:00:00 of the day
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(baseTimeMs);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        baseTimeMs = calendar.getTimeInMillis();

        while (baseTimeMs <= System.currentTimeMillis() && baseTimeMs < endTimeMs) {
            //check to update DB
            String ymd = "" + calendar.get(Calendar.YEAR) + (calendar.get(Calendar.MONTH) + 1) + calendar.get(Calendar.DAY_OF_MONTH);

            LTransaction transaction = DBTransaction.getByRid(getRid() + ymd);
            if (transaction == null) {
                transaction = new LTransaction(item);

                transaction.setTimeStampLast(LPreferences.getServerUtc());
                transaction.setRid(transaction.getRid() + ymd);
                transaction.setTimeStamp(baseTimeMs);

                DBTransaction.add(transaction, true, true);
                LLog.d(TAG, "creating missed schedule entry: " + transaction.getRid() + ymd);
            } else {
                //entry already exits, do nothing
            }

            if (repeatUnit == REPEAT_UNIT_MONTH) {
                calendar.add(Calendar.MONTH, repeatInterval);
                baseTimeMs = calendar.getTimeInMillis();
            } else {
                baseTimeMs += (long) repeatInterval * 7 * 24 * 3600 * 1000;
            }
        }

        nextTime = baseTimeMs;

        if (repeatCount > 0) {
            if (baseTimeMs >= endTimeMs) {
                setState(DBHelper.STATE_DISABLED);
                nextTime = getTimeStamp();
            }
        }
        DBScheduledTransaction.update(this);
        */
    }

    private void nextTimeMs() {
        long baseTimeMs = getTimeStamp();

        // always align time to 00:00:00 of the day
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(baseTimeMs);
        calendar.set(Calendar.HOUR_OF_DAY, START_HOUR_OF_DAY);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        baseTimeMs = calendar.getTimeInMillis();

        if (repeatInterval == 0) repeatInterval = 1; //JIC

        while (baseTimeMs <= System.currentTimeMillis()) {
            if (repeatUnit == REPEAT_UNIT_MONTH) {
                calendar.add(Calendar.MONTH, repeatInterval);
                baseTimeMs = calendar.getTimeInMillis();
            } else {
                baseTimeMs += (long) repeatInterval * 7 * 24 * 3600 * 1000;
            }
        }

        nextTime = baseTimeMs;
    }

    /*
    public void cancelAlarm() {
        LAlarm.cancelAlarm((int) getId());
    }

    public void setAlarm() {
        cancelAlarm();
        if (getState() == DBHelper.STATE_ACTIVE) {
            LLog.d(TAG, "alarm " + getId() + " set: " + (new Date(nextTime)));
            LAlarm.setAlarm((int) getId(), nextTime);
        }
    }*/

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public long getNextTime() {
        return nextTime;
    }

    public void setNextTime(long nextTime) {
        this.nextTime = nextTime;
    }
}
