package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class LItem {
    private static final String TAG = LItem.class.getSimpleName();

    public static final int LOG_TYPE_EXPENSE = 10;
    public static final int LOG_TYPE_INCOME = 20;
    public static final int LOG_TYPE_TRNASACTION = 30;

    double value;
    int type;
    int category;
    int from;
    int to;
    int by;
    int tag;
    int vendor;
    String note;
    long timeStamp;

    public LItem() {
        this.timeStamp = System.currentTimeMillis();
        this.note = "";
    }

    public LItem(LItem item) {
        this.timeStamp = item.timeStamp;
        this.value = item.value;
        this.type = item.type;
        this.category = item.category;
        this.from = item.from;
        this.to = item.to;
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
                this.from == item.from &&
                this.to == item.to &&
                this.by == item.by &&
                this.tag == item.tag &&
                this.vendor == item.vendor &&
                this.note == item.note);
    }

    public LItem(double value, int type, int category, int vendor, int tag,
                 int from, int to, int by, String note) {
        this.timeStamp = System.currentTimeMillis();
        this.value = value;
        this.type = type;
        this.category = category;
        this.from = from;
        this.to = to;
        this.by = by;
        this.note = note;
        this.tag = tag;
        this.vendor = vendor;
    }

    public LItem(double value, int type, int category,
                 int from, int to, int by, String note, long timeStamp) {
        this.value = value;
        this.type = type;
        this.category = category;
        this.from = from;
        this.to = to;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
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

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getVendor() {
        return vendor;
    }

    public void setVendor(int vendor) {
        this.vendor = vendor;
    }
}
