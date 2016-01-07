package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.LPreferences;

import java.util.UUID;

public class LCategory {
    private static final String TAG = LCategory.class.getSimpleName();

    public static final int CATEGORY_STATE_ACTIVE = 10;
    public static final int CATEGORY_STATE_DELETED = 20;

    private long id;
    private int state;
    private String name;
    private String rid;
    private long timeStampLast;

    private void init() {
        this.state = CATEGORY_STATE_ACTIVE;
        this.timeStampLast = LPreferences.getServerUtc();
        this.rid = UUID.randomUUID().toString();
        this.name = "";
    }

    public LCategory() {
        init();
    }

    public LCategory(String name) {
        init();
        this.name = name;
    }

    public LCategory(String name, String rid) {
        init();
        this.name = name;
        this.rid = rid;
    }

    public LCategory(int state, String name) {
        init();
        this.state = state;
        this.name = name;
    }

    public LCategory(String name, String rid, long timeStampLast) {
        init();
        this.name = name;
        this.rid = rid;
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

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public long getTimeStampLast() {
        return timeStampLast;
    }

    public void setTimeStampLast(long timeStampLast) {
        this.timeStampLast = timeStampLast;
    }
}
