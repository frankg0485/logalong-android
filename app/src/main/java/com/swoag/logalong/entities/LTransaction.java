package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.R;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.LPreferences;

public class LTransaction {
    private static final String TAG = LTransaction.class.getSimpleName();

    public static final int TRANSACTION_TYPE_EXPENSE = 10;
    public static final int TRANSACTION_TYPE_INCOME = 20;
    public static final int TRANSACTION_TYPE_TRANSFER = 30;
    public static final int TRANSACTION_TYPE_TRANSFER_COPY = 31;

    public static int getTypeStringId(int type) {
        switch (type) {
            case TRANSACTION_TYPE_EXPENSE:
                break;
            case TRANSACTION_TYPE_INCOME:
                return R.string.income;
            case TRANSACTION_TYPE_TRANSFER:
                return R.string.transfer;
        }
        return R.string.expense;
    }

    private double value;
    private int type;
    private int by;
    private int state;
    private long id;
    private long category;
    private long account;
    private long account2;
    private long tag;
    private long vendor;
    private long timeStamp;
    private long timeStampLast;
    private long gid;
    private String note;

    private void init() {
        this.timeStampLast = LPreferences.getServerUtc();
        this.timeStamp = System.currentTimeMillis();
        this.value = 0;
        this.type = TRANSACTION_TYPE_EXPENSE;
        this.by = 0;
        this.state = DBHelper.STATE_ACTIVE;
        this.id = 0;
        this.category = 0;
        this.account = 0;
        this.account2 = 0;
        this.tag = 0;
        this.vendor = 0;
        this.gid = 0;

        this.note = "";
    }

    public LTransaction() {
        init();
    }

    public void copy(LTransaction item) {
        this.timeStamp = item.timeStamp;
        this.timeStampLast = item.timeStampLast;
        this.value = item.value;
        this.type = item.type;
        this.by = item.by;
        this.state = item.state;
        this.id = item.id;
        this.category = item.category;
        this.account = item.account;
        this.account2 = item.account2;
        this.tag = item.tag;
        this.vendor = item.vendor;
        this.note = item.note;
        this.gid = item.gid;
    }

    public LTransaction(LTransaction item) {
        copy(item);
    }

    public boolean isEqual(LTransaction item) {
        return (this.timeStamp == item.timeStamp &&
                this.value == item.value &&
                this.type == item.type &&
                this.by == item.by &&
                this.category == item.category &&
                this.account == item.account &&
                this.account2 == item.account2 &&
                this.tag == item.tag &&
                this.vendor == item.vendor &&
                this.note.contentEquals(item.note));
    }

    public boolean isPrimaryAccountEqual(LTransaction item) {
        return (this.timeStamp == item.timeStamp &&
                this.value == item.value &&
                this.type == item.type &&
                this.by == item.by &&
                this.category == item.category &&
                this.account == item.account &&
                /*this.account2 == item.account2 &&*/
                this.tag == item.tag &&
                this.vendor == item.vendor &&
                this.note.contentEquals(item.note));
    }

    public LTransaction(double value, int type, long category, long vendor, long tag,
                        long account, long account2, long timeStamp, String note) {
        init();
        this.value = value;
        this.type = type;
        this.category = category;
        this.vendor = vendor;
        this.tag = tag;
        this.account = account;
        this.account2 = account2;
        this.timeStamp = timeStamp;
        this.note = note;
    }

    public LTransaction(long gid, double value, int type, long category, long vendor, long tag,
                        long account, long account2, int by, long timeStamp, long timeStampLast, String note) {
        init();
        this.gid = gid;
        this.value = value;
        this.type = type;
        this.category = category;
        this.vendor = vendor;
        this.tag = tag;
        this.account = account;
        this.account2 = account2;
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

    public long getAccount2() {
        return account2;
    }

    public void setAccount2(long account2) {
        this.account2 = account2;
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

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }
}
