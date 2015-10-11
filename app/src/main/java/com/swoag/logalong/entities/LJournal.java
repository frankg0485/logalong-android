package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.database.Cursor;

import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class LJournal {
    private static final String TAG = LJournal.class.getSimpleName();

    public static final int JOURNAL_STATE_ACTIVE = 10;
    public static final int JOURNAL_STATE_DELETED = 20;

    private static final int ACTION_UPDATE_ITEM = 10;
    private static final int ACTION_UPDATE_SCHEDULED_ITEM = 15;
    private static final int ACTION_UPDATE_ACCOUNT = 20;
    private static final int ACTION_UPDATE_CATEGORY = 30;
    private static final int ACTION_UPDATE_VENDOR = 40;
    private static final int ACTION_UPDATE_TAG = 50;
    private static final int ACTION_UPDATE_VENDOR_CATEGORY = 60;

    long id;
    int state;
    int userId;
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

    public static void flush() {
        Cursor cursor = DBAccess.getAllActiveJournalCursor();
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                int userId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TO_USER));
                String rec = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RECORD));
                long id = cursor.getLong(0);
                LProtocol.ui.postJournal(userId, id + ":" + rec);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void post(int userId) {
        this.userId = userId;
        this.id = DBAccess.addJournal(this);
        LProtocol.ui.postJournal(userId, this.id + ":" + this.record);
    }

    private String transactionItemString(LTransaction item) {
        LAccount account = DBAccess.getAccountById(item.getAccount());

        LCategory category = DBCategory.getById(item.getCategory());
        LVendor vendor = DBVendor.getById(item.getVendor());
        LTag tag = DBAccess.getTagById(item.getTag());

        String str = DBHelper.TABLE_COLUMN_STATE + "=" + item.getState() + ","
                + DBHelper.TABLE_COLUMN_TYPE + "=" + item.getType() + ","
                + DBHelper.TABLE_COLUMN_AMOUNT + "=" + item.getValue() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP + "=" + item.getTimeStamp() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + item.getTimeStampLast() + ","
                + DBHelper.TABLE_COLUMN_MADEBY + "=" + item.getBy() + ",";

        if (category != null && (!category.getName().isEmpty())) {
            str += DBHelper.TABLE_COLUMN_CATEGORY + "=" + category.getName() + ";" + category.getRid() + ";" + category.getTimeStampLast() + ",";
        }
        if (vendor != null && (!vendor.getName().isEmpty())) {
            str += DBHelper.TABLE_COLUMN_VENDOR + "=" + vendor.getName() + ";" + vendor.getRid() + ";" + vendor.getTimeStampLast() + ",";
        }
        if (tag != null && (!tag.getName().isEmpty())) {
            str += DBHelper.TABLE_COLUMN_TAG + "=" + tag.getName() + ";" + tag.getRid() + ";" + tag.getTimeStampLast() + ",";
        }

        if (!item.getNote().isEmpty()) {
            str += DBHelper.TABLE_COLUMN_NOTE + "=" + item.getNote() + ",";
        }

        str += DBHelper.TABLE_COLUMN_RID + "=" + item.getRid() + ",";
        str += DBHelper.TABLE_COLUMN_ACCOUNT + "=" + account.getName() + ";" + account.getRid() + ";" + account.getTimeStampLast();

        return str;
    }

    public boolean updateItem(LTransaction item) {
        LAccount account = DBAccess.getAccountById(item.getAccount());
        if (!account.isShared()) return false;

        record = ACTION_UPDATE_ITEM + ":" + transactionItemString(item);

        ArrayList<Integer> ids = account.getShareIds();
        ArrayList<Integer> states = account.getShareStates();
        for (int ii = 0; ii < states.size(); ii++) {
            if ((states.get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED) || (states.get(ii) == LAccount.ACCOUNT_SHARE_INVITED)) {
                post(ids.get(ii));
            }
        }
        return true;
    }

    public boolean updateScheduledItem(LScheduledTransaction sch) {
        LTransaction item = sch.getItem();
        LAccount account = DBAccess.getAccountById(item.getAccount());
        if ((null == account) || (!account.isShared())) return false;

        record = ACTION_UPDATE_SCHEDULED_ITEM + ":" + transactionItemString(item);
        record += ","
                + DBHelper.TABLE_COLUMN_REPEAT_COUNT + "=" + sch.getRepeatCount() + ","
                + DBHelper.TABLE_COLUMN_REPEAT_INTERVAL + "=" + sch.getRepeatInterval() + ","
                + DBHelper.TABLE_COLUMN_REPEAT_UNIT + "=" + sch.getRepeatUnit() + ","
                + DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP + "=" + sch.getTimestamp();

        ArrayList<Integer> ids = account.getShareIds();
        ArrayList<Integer> states = account.getShareStates();
        for (int ii = 0; ii < states.size(); ii++) {
            if ((states.get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED) || (states.get(ii) == LAccount.ACCOUNT_SHARE_INVITED)) {
                post(ids.get(ii));
            }
        }
        return true;
    }

    public boolean updateAccount(LAccount account) {
        if (!account.isShared()) return false;

        record = ACTION_UPDATE_ACCOUNT + ":"
                + DBHelper.TABLE_COLUMN_STATE + "=" + account.getState() + ","
                + DBHelper.TABLE_COLUMN_NAME + "=" + account.getName() + ","
                + DBHelper.TABLE_COLUMN_RID + "=" + account.getRid() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + account.getTimeStampLast();

        ArrayList<Integer> ids = account.getShareIds();
        ArrayList<Integer> states = account.getShareStates();
        for (int ii = 0; ii < states.size(); ii++) {
            if ((states.get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED) || (states.get(ii) == LAccount.ACCOUNT_SHARE_INVITED)) {
                post(ids.get(ii));
            }
        }
        return true;
    }

    public boolean updateCategory(LCategory category) {
        HashSet<Integer> users = DBAccess.getAllAccountsConfirmedShareUser();
        if (users.size() < 1) return false;

        record = ACTION_UPDATE_CATEGORY + ":"
                + DBHelper.TABLE_COLUMN_STATE + "=" + category.getState() + ","
                + DBHelper.TABLE_COLUMN_NAME + "=" + category.getName() + ","
                + DBHelper.TABLE_COLUMN_RID + "=" + category.getRid() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + category.getTimeStampLast();

        for (int user : users) post(user);
        return true;
    }

    public boolean updateVendor(LVendor vendor) {
        HashSet<Integer> users = DBAccess.getAllAccountsConfirmedShareUser();
        if (users.size() < 1) return false;

        record = ACTION_UPDATE_VENDOR + ":"
                + DBHelper.TABLE_COLUMN_STATE + "=" + vendor.getState() + ","
                + DBHelper.TABLE_COLUMN_NAME + "=" + vendor.getName() + ","
                + DBHelper.TABLE_COLUMN_RID + "=" + vendor.getRid() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + vendor.getTimeStampLast();

        for (int user : users) post(user);
        return true;
    }

    public boolean updateTag(LTag tag) {
        HashSet<Integer> users = DBAccess.getAllAccountsConfirmedShareUser();
        if (users.size() < 1) return false;

        record = ACTION_UPDATE_TAG + ":"
                + DBHelper.TABLE_COLUMN_STATE + "=" + tag.getState() + ","
                + DBHelper.TABLE_COLUMN_NAME + "=" + tag.getName() + ","
                + DBHelper.TABLE_COLUMN_RID + "=" + tag.getRid() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + tag.getTimeStampLast();

        for (int user : users) post(user);
        return true;
    }

    public boolean updateVendorCategory(boolean add, UUID vendor, UUID category) {
        HashSet<Integer> users = DBAccess.getAllAccountsConfirmedShareUser();
        if (users.size() < 1) return false;

        record = ACTION_UPDATE_VENDOR_CATEGORY + ":"
                + DBHelper.TABLE_COLUMN_STATE + "=" + (add ? DBHelper.STATE_ACTIVE : DBHelper.STATE_DELETED) + ","
                + DBHelper.TABLE_COLUMN_RID + ".vendor=" + vendor + ","
                + DBHelper.TABLE_COLUMN_RID + ".category=" + category;

        for (int user : users) post(user);
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static LTransaction parseItemFromReceivedRecord(String receivedRecord) {
        LTransaction item = new LTransaction();

        String[] splitRecords = receivedRecord.split(",", -1);
        String rid = "";
        String note = "";
        double amount = 0;
        int type = LTransaction.TRANSACTION_TYPE_EXPENSE;
        int madeBy = 0;
        int state = DBHelper.STATE_ACTIVE;
        long timestamp = 0, timestampLast = 0, accountId = 0, categoryId = 0, vendorId = 0, tagId = 0;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TYPE)) {
                type = Integer.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_MADEBY)) {
                madeBy = Integer.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_AMOUNT)) {
                amount = Double.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP)) {
                timestamp = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) {
                timestampLast = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_ACCOUNT)) {
                String[] sss = ss[1].split(";");

                LAccount account1 = DBAccess.getAccountByName(sss[0]);
                if (null == account1) {
                    accountId = DBAccess.addAccount(new LAccount(sss[0], UUID.fromString(sss[1])));
                } else {
                    if (Long.parseLong(sss[2]) > account1.getTimeStampLast()) {
                        account1.setRid(UUID.fromString(sss[1]));
                        DBAccess.updateAccount(account1);
                    }

                    accountId = account1.getId();
                }
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_CATEGORY)) {
                String[] sss = ss[1].split(";");

                LCategory category1 = DBAccess.getCategoryByName(sss[0]);
                if (null == category1) {
                    categoryId = DBAccess.addCategory(new LCategory(sss[0], UUID.fromString(sss[1])));
                } else {
                    if (Long.parseLong(sss[2]) > category1.getTimeStampLast()) {
                        category1.setRid(UUID.fromString(sss[1]));
                        DBAccess.updateCategory(category1);
                    }
                    categoryId = category1.getId();
                }
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_VENDOR)) {
                String[] sss = ss[1].split(";");

                LVendor vendor1 = DBAccess.getVendorByName(sss[0]);
                if (null == vendor1) {
                    vendorId = DBAccess.addVendor(new LVendor(sss[0], UUID.fromString(sss[1])));
                } else {
                    if (Long.parseLong(sss[2]) > vendor1.getTimeStampLast()) {
                        vendor1.setRid(UUID.fromString(sss[1]));
                        DBAccess.updateVendor(vendor1);
                    }

                    vendorId = vendor1.getId();
                }
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TAG)) {
                String[] sss = ss[1].split(";");

                LTag tag1 = DBAccess.getTagByName(sss[0]);
                if (null == tag1) {
                    tagId = DBAccess.addTag(new LTag(sss[0], UUID.fromString(sss[1])));
                } else {
                    if (Long.parseLong(sss[2]) > tag1.getTimeStampLast()) {
                        tag1.setRid(UUID.fromString(sss[1]));
                        DBAccess.updateTag(tag1);
                    }

                    tagId = tag1.getId();
                }
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NOTE)) {
                note = ss[1];
            }
        }

        item.setState(state);
        item.setValue(amount);
        item.setType(type);
        item.setAccount(accountId);
        item.setCategory(categoryId);
        item.setVendor(vendorId);
        item.setTag(tagId);
        item.setTimeStamp(timestamp);
        item.setTimeStampLast(timestampLast);
        item.setNote(note);
        item.setRid(UUID.fromString(rid));
        item.setBy(madeBy);

        return item;
    }

    public static void updateItemFromReceivedRecord(String receivedRecord) {
        LTransaction receivedItem = parseItemFromReceivedRecord(receivedRecord);

        LTransaction item = DBAccess.getItemByRid(receivedItem.getRid().toString());
        if (item != null) {
            if (item.getTimeStampLast() < receivedItem.getTimeStampLast()) {
                item.setState(receivedItem.getState());
                item.setValue(receivedItem.getValue());
                item.setType(receivedItem.getType());
                item.setAccount(receivedItem.getAccount());
                item.setCategory(receivedItem.getCategory());
                item.setVendor(receivedItem.getVendor());
                item.setTag(receivedItem.getTag());
                item.setTimeStamp(receivedItem.getTimeStamp());
                item.setTimeStampLast(receivedItem.getTimeStampLast());
                item.setNote(receivedItem.getNote());
                DBAccess.updateItem(item);
            }
        } else {
            DBAccess.addItem(new LTransaction(receivedItem.getRid().toString(),
                    receivedItem.getValue(),
                    receivedItem.getType(),
                    receivedItem.getCategory(),
                    receivedItem.getVendor(),
                    receivedItem.getTag(),
                    receivedItem.getAccount(),
                    receivedItem.getBy(),
                    receivedItem.getTimeStamp(),
                    receivedItem.getTimeStampLast(),
                    receivedItem.getNote()));
        }
    }

    public static void updateScheduledItemFromReceivedRecord(String receivedRecord) {
        LTransaction receivedItem = parseItemFromReceivedRecord(receivedRecord);

        String[] splitRecords = receivedRecord.split(",", -1);
        long timestamp = 0;
        int repeatUnit = LScheduledTransaction.REPEAT_UNIT_MONTH;
        int repeatInterval = 1;
        int repeatCount = 0;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_REPEAT_COUNT)) {
                repeatCount = Integer.parseInt(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_REPEAT_INTERVAL)) {
                repeatInterval = Integer.parseInt(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_REPEAT_UNIT)) {
                repeatUnit = Integer.parseInt(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP)) {
                timestamp = Long.parseLong(ss[1]);
            }
        }

        LScheduledTransaction sch = DBScheduledTransaction.getByRid(receivedItem.getRid());
        if (sch != null) {
            LTransaction item = sch.getItem();
            if (item.getTimeStampLast() < receivedItem.getTimeStampLast()) {
                item.setState(receivedItem.getState());
                item.setValue(receivedItem.getValue());
                item.setType(receivedItem.getType());
                item.setAccount(receivedItem.getAccount());
                item.setCategory(receivedItem.getCategory());
                item.setVendor(receivedItem.getVendor());
                item.setTag(receivedItem.getTag());
                item.setTimeStamp(receivedItem.getTimeStamp());
                item.setTimeStampLast(receivedItem.getTimeStampLast());
                item.setNote(receivedItem.getNote());

                sch.setTimestamp(timestamp);
                sch.setRepeatUnit(repeatUnit);
                sch.setRepeatInterval(repeatInterval);
                sch.setRepeatCount(repeatCount);
                DBScheduledTransaction.update(sch);
            }
        } else {
            DBScheduledTransaction.add(new LScheduledTransaction(repeatInterval, repeatUnit, repeatCount, timestamp, receivedItem));
        }
    }

    public static void updateAccountFromReceivedRecord(String receivedRecord) {
        String[] splitRecords = receivedRecord.split(",", -1);
        String rid = "";
        int state = DBHelper.STATE_ACTIVE;
        long timestampLast = 0;
        String name = "";
        boolean stateFound = false;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
                stateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) {
                timestampLast = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME)) {
                name = ss[1];
            }
        }

        if (!rid.isEmpty()) {
            LAccount account = DBAccess.getAccountByUuid(UUID.fromString(rid));
            if (account == null) {
                LLog.w(TAG, "account removed?");
            } else {
                if (account.getTimeStampLast() < timestampLast) {
                    if (!name.isEmpty()) account.setName(name);
                    if (stateFound) account.setState(state);
                    account.setTimeStampLast(timestampLast);
                    DBAccess.updateAccount(account);
                }
            }
        }
    }

    public static void updateCategoryFromReceivedRecord(String receivedRecord) {
        String[] splitRecords = receivedRecord.split(",", -1);
        String rid = "";
        int state = DBHelper.STATE_ACTIVE;
        long timestampLast = 0;
        String name = "";
        boolean stateFound = false;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
                stateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) {
                timestampLast = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME)) {
                name = ss[1];
            }
        }

        if (!rid.isEmpty()) {
            LCategory category = DBAccess.getCategoryByUuid(UUID.fromString(rid));
            if (category == null) {
                category = new LCategory(name, UUID.fromString(rid), timestampLast);
                DBAccess.addCategory(category);
            } else {
                if (category.getTimeStampLast() < timestampLast) {
                    if (!name.isEmpty()) category.setName(name);
                    if (stateFound) category.setState(state);
                    category.setTimeStampLast(timestampLast);
                    DBAccess.updateCategory(category);
                }
            }
        }
    }

    public static void updateVendorFromReceivedRecord(String receivedRecord) {
        String[] splitRecords = receivedRecord.split(",", -1);
        String rid = "";
        int state = DBHelper.STATE_ACTIVE;
        long timestampLast = 0;
        String name = "";
        boolean stateFound = false;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
                stateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) {
                timestampLast = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME)) {
                name = ss[1];
            }
        }

        if (!rid.isEmpty()) {
            LVendor vendor = DBAccess.getVendorByUuid(UUID.fromString(rid));
            if (vendor == null) {
                vendor = new LVendor(name, UUID.fromString(rid), timestampLast);
                DBAccess.addVendor(vendor);
            } else {
                if (vendor.getTimeStampLast() < timestampLast) {
                    if (!name.isEmpty()) vendor.setName(name);
                    if (stateFound) vendor.setState(state);
                    vendor.setTimeStampLast(timestampLast);
                    DBAccess.updateVendor(vendor);
                }
            }
        }
    }

    public static void updateTagFromReceivedRecord(String receivedRecord) {
        String[] splitRecords = receivedRecord.split(",", -1);
        String rid = "";
        int state = DBHelper.STATE_ACTIVE;
        long timestampLast = 0;
        String name = "";
        boolean stateFound = false;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
                stateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) {
                timestampLast = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME)) {
                name = ss[1];
            }
        }

        if (!rid.isEmpty()) {
            LTag tag = DBAccess.getTagByUuid(UUID.fromString(rid));
            if (tag == null) {
                tag = new LTag(name, UUID.fromString(rid), timestampLast);
                DBAccess.addTag(tag);
            } else {
                if (tag.getTimeStampLast() < timestampLast) {
                    if (!name.isEmpty()) tag.setName(name);
                    if (stateFound) tag.setState(state);
                    tag.setTimeStampLast(timestampLast);
                    DBAccess.updateTag(tag);
                }
            }
        }
    }

    private static void updateVendorCategoryFromReceivedRecord(String receivedRecord) {
        String[] splitRecords = receivedRecord.split(",", -1);
        String vendorRid = "", categoryRid = "";
        int state = DBHelper.STATE_ACTIVE;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID + ".vendor")) {
                vendorRid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID + ".category")) {
                categoryRid = ss[1];
            }
        }

        if ((!vendorRid.isEmpty()) && (!categoryRid.isEmpty())) {
            long vendor = DBVendor.getIdByRid(UUID.fromString(vendorRid));
            long category = DBCategory.getIdByRid(UUID.fromString(categoryRid));
            DBVendor.updateCategory(vendor, category, state == DBHelper.STATE_ACTIVE);
        }
    }

    public static void receive(String recvRecord) {
        String[] ss = recvRecord.split(":", 3);
        int action = Integer.parseInt(ss[1]);
        switch (action) {
            case ACTION_UPDATE_ITEM:
                updateItemFromReceivedRecord(ss[2]);
                break;

            case ACTION_UPDATE_SCHEDULED_ITEM:
                updateScheduledItemFromReceivedRecord(ss[2]);
                break;

            case ACTION_UPDATE_ACCOUNT:
                updateAccountFromReceivedRecord(ss[2]);
                break;

            case ACTION_UPDATE_CATEGORY:
                updateCategoryFromReceivedRecord(ss[2]);
                break;

            case ACTION_UPDATE_VENDOR:
                updateVendorFromReceivedRecord(ss[2]);
                break;

            case ACTION_UPDATE_TAG:
                updateTagFromReceivedRecord(ss[2]);
                break;

            case ACTION_UPDATE_VENDOR_CATEGORY:
                updateVendorCategoryFromReceivedRecord(ss[2]);
                break;
        }
    }
}
