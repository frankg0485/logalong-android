package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import java.util.UUID;

public class LTag {
    private static final String TAG = LTag.class.getSimpleName();

    public static final int TAG_STATE_ACTIVE = 10;
    public static final int TAG_STATE_DELETED = 20;

    long id;
    int state;
    String name;
    UUID rid;
    long timeStampLast;

    private void init() {
        this.state = TAG_STATE_ACTIVE;
        this.timeStampLast = System.currentTimeMillis();
        this.rid = UUID.randomUUID();
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

    public LTag(String name, UUID rid) {
        init();
        this.name = name;
        this.rid = rid;
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

    public UUID getRid() {
        return rid;
    }

    public void setRid(UUID rid) {
        this.rid = rid;
    }

    public long getTimeStampLast() {
        return timeStampLast;
    }

    public void setTimeStampLast(long timeStampLast) {
        this.timeStampLast = timeStampLast;
    }
}
