package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class LTag {
    private static final String TAG = LTag.class.getSimpleName();

    public static final int TAG_STATE_ACTIVE = 10;
    public static final int TAG_STATE_DELETED = 20;

    int state;
    String name;

    public LTag() {
        this.state = TAG_STATE_ACTIVE;
        this.name = "";
    }

    public LTag(String name) {
        this.state = TAG_STATE_ACTIVE;
        this.name = name;
    }

    public LTag(int state, String name) {
        this.state = state;
        this.name = name;
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
}
