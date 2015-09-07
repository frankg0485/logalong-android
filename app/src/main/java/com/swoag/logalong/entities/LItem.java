package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.DBHelper;

public class LItem {
    private static final String TAG = LItem.class.getSimpleName();

    public static final int LOG_TYPE_EXPENSE = 10;
    public static final int LOG_TYPE_INCOME = 20;
    public static final int LOG_TYPE_TRNASACTION = 30;

    double value;
    int type;
    int state;
    long id;
    long category;
    long account;
    long by;
    long tag;
    long vendor;
    String note;
    long timeStamp;

    public LItem() {
        this.timeStamp = System.currentTimeMillis();
        this.value = 0;
        this.note = "";
        this.id = 0;
        this.state = DBHelper.STATE_ACTIVE;
    }

    public LItem(LItem item) {
        this.timeStamp = item.timeStamp;
        this.value = item.value;
        this.type = item.type;
        this.category = item.category;
        this.account = item.account;
        this.by = item.by;
        this.tag = item.tag;
        this.vendor = item.vendor;
        this.note = item.note;
    }

    public boolean isEqual(LItem item) {
        return (this.timeStamp == item.timeStamp &&
                this.value == item.value &&
                this.type == item.type &&
                this.category == item.category &&
                this.account == item.account &&
                this.by == item.by &&
                this.tag == item.tag &&
                this.vendor == item.vendor &&
                this.note == item.note);
    }

    public LItem(double value, int type, int category, int vendor, int tag,
                 int account, int by, String note) {
        this.timeStamp = System.currentTimeMillis();
        this.value = value;
        this.type = type;
        this.category = category;
        this.account = account;
        this.by = by;
        this.note = note;
        this.tag = tag;
        this.vendor = vendor;
    }

    public LItem(double value, int type, int category,
                 int account, int by, String note, long timeStamp) {
        this.value = value;
        this.type = type;
        this.category = category;
        this.account = account;
        this.by = by;
        this.note = note;
        this.timeStamp = timeStamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getCategory() {
        return category;
    }

    public void setCategory(long category) {
        this.category = category;
    }

    public long getBy() {
        return by;
    }

    public void setBy(long by) {
        this.by = by;
    }

    public long getTag() {
        return tag;
    }

    public void setTag(long tag) {
        this.tag = tag;
    }

    public long getVendor() {
        return vendor;
    }

    public void setVendor(long vendor) {
        this.vendor = vendor;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccount() {
        return account;
    }

    public void setAccount(long account) {
        this.account = account;
    }
}
