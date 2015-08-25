package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class LAccount {
    private static final String TAG = LAccount.class.getSimpleName();

    public static final int ACCOUNT_STATE_ACTIVE = 10;
    public static final int ACCOUNT_STATE_DELETED = 20;

    int state;
    String name;

    public LAccount() {
        this.state = ACCOUNT_STATE_ACTIVE;
        this.name = "";
    }

    public LAccount(int state, String name) {
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
