package com.swoag.logalong.entities;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

public class LCategory extends LDbBase {
    private long pid;

    public LCategory() {
        super();
        this.pid = 0;
    }

    public LCategory(String name) {
        super(name);
        this.pid = 0;
    }

    public LCategory(int state, String name) {
        super(state, name);
        this.pid = 0;
    }

    public LCategory(String name, long gid) {
        super(name, gid);
        this.pid = 0;
    }

    public LCategory(String name, long gid, long timeStampLast) {
        super(name, gid, timeStampLast);
        this.pid = 0;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }
}
