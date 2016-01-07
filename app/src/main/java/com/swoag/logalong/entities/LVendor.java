package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.LPreferences;

import java.util.UUID;

public class LVendor {
    private static final String TAG = LVendor.class.getSimpleName();

    public static final int TYPE_PAYEE = 10;
    public static final int TYPE_PAYER = 20;
    public static final int TYPE_PAYEE_PAYER = 30;

    private long id;
    private int state;
    private int type;
    private String name;
    private String rid;
    private long timeStampLast;

    private void init() {
        this.state = DBHelper.STATE_ACTIVE;
        this.type = TYPE_PAYEE;
        this.timeStampLast = LPreferences.getServerUtc();
        this.rid = UUID.randomUUID().toString();
        this.name = "";
    }

    public LVendor() {
        init();
    }

    public LVendor(String name) {
        init();
        this.name = name;
    }

    public LVendor(String name, int type) {
        init();
        this.name = name;
        this.type = type;
    }

    public LVendor(String name, String rid, int type) {
        init();
        this.name = name;
        this.type = type;
        this.rid = rid;
    }

    public LVendor(String name, int type, int state) {
        init();
        this.state = state;
        this.type = type;
        this.name = name;
    }

    public LVendor(String name, int type, String rid, long timeStampLast) {
        init();
        this.name = name;
        this.type = type;
        this.rid = rid;
        this.timeStampLast = timeStampLast;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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
