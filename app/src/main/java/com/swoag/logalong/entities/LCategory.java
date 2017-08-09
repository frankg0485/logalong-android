package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.LPreferences;

public class LCategory {
    private static final String TAG = LCategory.class.getSimpleName();

    public static final int CATEGORY_STATE_ACTIVE = 10;
    public static final int CATEGORY_STATE_DELETED = 20;

    private long id;
    private int state;
    private String name;
    private int gid;
    private long timeStampLast;

    private void init() {
        this.state = CATEGORY_STATE_ACTIVE;
        this.timeStampLast = LPreferences.getServerUtc();
        this.gid = 0;
        this.name = "";
    }

    public LCategory() {
        init();
    }

    public LCategory(String name) {
        init();
        this.name = name;
    }

    public LCategory(String name, int gid) {
        init();
        this.name = name;
        this.gid = gid;
    }

    public LCategory(int state, String name) {
        init();
        this.state = state;
        this.name = name;
    }

    public LCategory(String name, int gid, long timeStampLast) {
        init();
        this.name = name;
        this.gid = gid;
        this.timeStampLast = timeStampLast;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public long getTimeStampLast() {
        return timeStampLast;
    }

    public void setTimeStampLast(long timeStampLast) {
        this.timeStampLast = timeStampLast;
    }
}
