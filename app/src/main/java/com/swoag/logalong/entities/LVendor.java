package com.swoag.logalong.entities;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

public class LVendor extends LDbBase {
    public static final int TYPE_PAYEE = 10;
    public static final int TYPE_PAYER = 20;
    public static final int TYPE_PAYEE_PAYER = 30;

    private int type;

    public LVendor() {
        super();
        this.type = TYPE_PAYEE;
    }

    public LVendor(String name) {
        super(name);
        this.type = TYPE_PAYEE;
    }

    public LVendor(String name, int type) {
        super(name);
        this.type = type;
    }

    public LVendor(String name, int type, int state) {
        super(state, name);
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
