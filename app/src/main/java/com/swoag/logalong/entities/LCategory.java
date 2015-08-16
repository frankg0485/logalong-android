package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class LCategory {
    private static final String TAG = LCategory.class.getSimpleName();

    public static final int CATEGORY_STATE_ACTIVE = 10;
    public static final int CATEGORY_STATE_DELETED = 20;

    int state;
    String name;

    public LCategory() {
        this.state = CATEGORY_STATE_ACTIVE;
        this.name = "";
    }

    public LCategory(int state, String name) {
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
