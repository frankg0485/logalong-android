package com.swoag.logalong.entities;
/* Copyright (C) 2017 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.LPreferences;

public class LDbBase {
    protected int state;
    protected long id;
    protected long gid;
    protected long timeStampLast;
    protected String name;

    public LDbBase() {
        this.state = DBHelper.STATE_ACTIVE;
        this.id = 0;
        this.gid = 0;
        this.timeStampLast = LPreferences.getServerUtc();
        this.name = "";
    }

    public LDbBase(String name) {
        this();
        this.name = name;
    }

    public LDbBase(int state, String name) {
        this();
        this.state = state;
        this.name = name;
    }

    public LDbBase(String name, long gid) {
        this();
        this.name = name;
        this.gid = gid;
    }

    public LDbBase(String name, long gid, long timeStampLast) {
        this();
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }

    public long getTimeStampLast() {
        return timeStampLast;
    }

    public void setTimeStampLast(long timeStampLast) {
        this.timeStampLast = timeStampLast;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
