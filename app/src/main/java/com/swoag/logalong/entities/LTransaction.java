package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.DBHelper;

import java.util.UUID;

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
    long timeStampLast;
    UUID rid;
    String note;

    private void init() {
        this.timeStampLast = this.timeStamp = System.currentTimeMillis();
        this.value = 0;
        this.type = TRANSACTION_TYPE_EXPENSE;
        this.by = 0;
        this.state = DBHelper.STATE_ACTIVE;
        this.id = 0;
        this.category = 0;
        this.account = 0;
        this.tag = 0;
        this.vendor = 0;
        this.rid = UUID.randomUUID();

        this.note = "";
    }

    public LTransaction() {
        init();
    }

    public LTransaction(LTransaction item) {
        this.timeStamp = item.timeStamp;
        this.timeStampLast = item.timeStampLast;
        this.value = item.value;
        this.type = item.type;
        this.by = item.by;
        this.state = item.state;
        this.id = item.id;
        this.category = item.category;
        this.account = item.account;
        this.tag = item.tag;
        this.vendor = item.vendor;
        this.note = item.note;
    }

    public boolean isEqual(LTransaction item) {
        return (this.timeStamp == item.timeStamp &&
                this.value == item.value &&
                this.type == item.type &&
                this.by == item.by &&
                this.category == item.category &&
                this.account == item.account &&
                this.tag == item.tag &&
                this.vendor == item.vendor &&
                this.note.contentEquals(item.note));
    }

    public LTransaction(double value, int type, long category, long vendor, long tag,
                        long account, long timeStamp, String note) {
        init();
        this.value = value;
        this.type = type;
        this.category = category;
        this.vendor = vendor;
        this.tag = tag;
        this.account = account;
        this.timeStamp = timeStamp;
        this.note = note;
    }

    public LTransaction(String rid, double value, int type, long category, long vendor, long tag,
                        long account, int by, long timeStamp, long timeStampLast, String note) {
        init();
        this.rid = UUID.fromString(rid);
        this.value = value;
        this.type = type;
        this.category = category;
        this.vendor = vendor;
        this.tag = tag;
        this.account = account;
        this.by = by;
        this.timeStamp = timeStamp;
        this.timeStampLast = timeStampLast;
        this.note = note;
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

    public long getTimeStampLast() {
        return timeStampLast;
    }

    public void setTimeStampLast(long timeStampLast) {
        this.timeStampLast = timeStampLast;
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

    public UUID getRid() {
        return rid;
    }

    public void setRid(UUID rid) {
        this.rid = rid;
    }
}
