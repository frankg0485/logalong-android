package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBHelper;

import java.util.ArrayList;

public class LJournal {
    private static final String TAG = LJournal.class.getSimpleName();

    public static final int JOURNAL_STATE_ACTIVE = 10;
    public static final int JOURNAL_STATE_DELETED = 20;

    public static final int ACTION_ADD_ITEM = 1;

    long id;
    int state;
    String record;

    private void init() {
        this.state = JOURNAL_STATE_ACTIVE;
        this.record = "";
    }

    public LJournal() {
        init();
    }

    public LJournal(String record) {
        init();
        this.record = record;
    }

    public LJournal(int state, String record) {
        init();
        this.state = state;
        this.record = record;
    }

    private void post(int userId) {
        this.id = DBAccess.addJournal(this);
        LProtocol.ui.postJournal(userId, this.id + ":" + this.record);
    }

    public boolean addItem(LTransaction item) {
        LAccount account = DBAccess.getAccountById(item.getAccount());
        if (!account.isShared()) return false;

        LCategory category = DBAccess.getCategoryById(item.getCategory());
        LVendor vendor = DBAccess.getVendorById(item.getVendor());

        record = ACTION_ADD_ITEM + ":" + DBHelper.TABLE_COLUMN_TYPE + "=" + item.getType() + ","
                + DBHelper.TABLE_COLUMN_AMOUNT + "=" + item.getValue() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP + "=" + item.getTimeStamp() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + item.getTimeStampLast() + ","
                + DBHelper.TABLE_COLUMN_MADEBY + "=" + item.getBy() + ",";

        if (category != null && (!category.getName().isEmpty())) {
            record += DBHelper.TABLE_COLUMN_CATEGORY + "=" + category.getName() + ";" + category.getRid() + ";" + category.getTimeStampLast() + ",";
        }
        if (vendor != null && (!vendor.getName().isEmpty())) {
            record += DBHelper.TABLE_COLUMN_VENDOR + "=" + vendor.getName() + ";" + vendor.getRid() + ";" + vendor.getTimeStampLast() + ",";
        }

        record += DBHelper.TABLE_COLUMN_RID + "=" + item.getRid().toString() + ",";
        record += DBHelper.TABLE_COLUMN_ACCOUNT + "=" + account.getName() + ";" + account.getRid() + ";" + account.getTimeStampLast();

        ArrayList<Integer> ids = account.getShareIds();
        ArrayList<Integer> states = account.getShareStates();
        for (int ii = 0; ii < states.size(); ii++) {
            if ((states.get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED) || (states.get(ii) == LAccount.ACCOUNT_SHARE_INVITED)) {
                post(ids.get(ii));
            }
        }
        return true;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getRecord() {
        return record;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setRecord(String record) {
        this.record = record;
    }
}
