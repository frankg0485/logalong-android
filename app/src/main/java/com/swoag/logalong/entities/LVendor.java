package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import java.util.UUID;

public class LVendor {
    private static final String TAG = LVendor.class.getSimpleName();

    public static final int VENDOR_STATE_ACTIVE = 10;
    public static final int VENDOR_STATE_DELETED = 20;

    long id;
    int state;
    String name;
    UUID rid;
    long timeStampLast;

    private void init() {
        this.state = VENDOR_STATE_ACTIVE;
        this.timeStampLast = System.currentTimeMillis();
        this.rid = UUID.randomUUID();
        this.name = "";
    }

    public LVendor() {
        init();
    }

    public LVendor(String name) {
        init();
        this.name = name;
    }

    public LVendor(String name, UUID rid) {
        init();
        this.name = name;
        this.rid = rid;
    }

    public LVendor(int state, String name) {
        init();
        this.state = state;
        this.name = name;
    }

    public LVendor(String name, UUID rid, long timeStampLast) {
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
