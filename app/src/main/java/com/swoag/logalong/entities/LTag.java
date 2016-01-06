package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import java.util.UUID;

public class LTag {
    private static final String TAG = LTag.class.getSimpleName();

    public static final int TAG_STATE_ACTIVE = 10;
    public static final int TAG_STATE_DELETED = 20;

    private long id;
    private int state;
    private String name;
    private String rid;
    private long timeStampLast;

    private void init() {
        this.state = TAG_STATE_ACTIVE;
        this.timeStampLast = System.currentTimeMillis();
        this.rid = UUID.randomUUID().toString();
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

    public LTag(String name, String rid) {
        init();
        this.name = name;
        this.rid = rid;
    }

    public LTag(String name, String rid, long timeStampLast) {
        init();
        this.name = name;
        this.rid = rid;
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
