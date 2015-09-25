package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.DBHelper;

public class LTransaction {
    private static final String TAG = LTransaction.class.getSimpleName();

    public static final int TRANSACTION_TYPE_EXPENSE = 10;
    public static final int TRANSACTION_TYPE_INCOME = 20;
    public static final int TRANSACTION_TYPE_TRANSFER = 30;

    double value;
    int type;
    int by;
    int state;
    long id;
    long category;
    long account;
    long tag;
    long vendor;
    long timeStamp;

    String note;

    public LTransaction() {
        this.timeStamp = System.currentTimeMillis();
        this.value = 0;
        this.note = "";
        this.by = 0;
        this.id = 0;
        this.state = DBHelper.STATE_ACTIVE;
    }

    public LTransaction(LTransaction item) {
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

    public boolean isEqual(LTransaction item) {
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

    public LTransaction(double value, int type, long category, long vendor, long tag,
                        long account, long timeStamp) {
        this.state = DBHelper.STATE_ACTIVE;

        this.value = value;
        this.type = type;
        this.category = category;
        this.account = account;
        this.tag = tag;
        this.vendor = vendor;
        this.timeStamp = timeStamp;
        this.note = "";
        this.by = 0;
    }

    public LTransaction(double value, int type, long category, long vendor, long tag,
                        long account, int by, String note) {
        this.timeStamp = System.currentTimeMillis();
        this.state = DBHelper.STATE_ACTIVE;

        this.value = value;
        this.type = type;
        this.category = category;
        this.account = account;
        this.by = by;
        this.note = note;
        this.tag = tag;
        this.vendor = vendor;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCategory() {
        return category;
    }

    public void setCategory(long category) {
        this.category = category;
    }

    public long getAccount() {
        return account;
    }

    public void setAccount(long account) {
        this.account = account;
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

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getBy() {
        return by;
    }

    public void setBy(int by) {
        this.by = by;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
