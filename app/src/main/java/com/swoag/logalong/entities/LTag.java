package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.LPreferences;

public class LTag {
    private static final String TAG = LTag.class.getSimpleName();

    public static final int TAG_STATE_ACTIVE = 10;
    public static final int TAG_STATE_DELETED = 20;

    private long id;
    private int state;
    private String name;
    private int gid;
    private long timeStampLast;

    private void init() {
        this.state = TAG_STATE_ACTIVE;
        this.timeStampLast = LPreferences.getServerUtc();
        this.gid = 0;
        this.name = "";
    }

    public LTag() {
        init();
    }

    public LTag(String name) {
        init();
        this.name = name;
    }

    public LTag(int state, String name) {
        init();
        this.state = state;
        this.name = name;
    }

    public LTag(String name, int gid) {
        init();
        this.name = name;
        this.gid = gid;
    }

    public LTag(String name, int gid, long timeStampLast) {
        init();
        this.name = name;
        this.gid = gid;
        this.timeStampLast = timeStampLast;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
