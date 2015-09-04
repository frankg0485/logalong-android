package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.DBHelper;

public class LVendorCategory {
    private static final String TAG = LVendorCategory.class.getSimpleName();

    int state;
    long vendor;
    long category;

    public LVendorCategory() {
        this.state = DBHelper.STATE_ACTIVE;
    }

    public LVendorCategory(long vendor, long category) {
        this.state = DBHelper.STATE_ACTIVE;
        this.vendor = vendor;
        this.category = category;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getVendor() {
        return vendor;
    }

    public void setVendor(long vendor) {
        this.vendor = vendor;
    }

    public long getCategory() {
        return category;
    }

    public void setCategory(long category) {
        this.category = category;
    }
}
