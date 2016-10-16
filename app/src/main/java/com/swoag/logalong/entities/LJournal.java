package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LBuffer;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LStorage;
import com.swoag.logalong.utils.LViewUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

public class LJournal {
    private static final String TAG = LJournal.class.getSimpleName();

    private static final int MAX_JOURNAL_DATA_BYTES = 1024;

    public static final int JOURNAL_STATE_ACTIVE = 10;
    public static final int JOURNAL_STATE_DELETED = 20;

    private static final int ACTION_SHARE_ITEM = 9;
    private static final int ACTION_UPDATE_ITEM = 10;
    private static final int ACTION_UPDATE_SCHEDULED_ITEM = 15;
    private static final int ACTION_UPDATE_ACCOUNT = 20;
    private static final int ACTION_UPDATE_CATEGORY = 30;
    private static final int ACTION_UPDATE_VENDOR = 40;
    private static final int ACTION_UPDATE_TAG = 50;
    private static final int ACTION_UPDATE_VENDOR_CATEGORY = 60;
    private static final int ACTION_SHARE_ACCOUNT = 70;

    private static final short JRQST_SHARE_ACCOUNT = 0x0100;
    private static final short JRQST_UNSHARE_ACCOUNT = 0x0101;
    private static final short JRQST_CONFIRM_ACCOUNT_SHARE = 0x0102;
    private static final short JRQST_SHARE_TRANSITION_RECORD = 0x0103;

    private int state;
    private int userId;
    private String record;
    private LBuffer data;

    private void init() {
        this.state = JOURNAL_STATE_ACTIVE;
        this.record = "";
        this.data = new LBuffer(MAX_JOURNAL_DATA_BYTES);
    }

    public LJournal() {
        init();
    }

    /*
    public static void flush() {
        Cursor cursor = DBAccess.getAllActiveJournalCursor();
        if (cursor != null && cursor.getCount() > 0) {
            LLog.d(TAG, "journal count: " + cursor.getCount());
            cursor.moveToFirst();
            do {
                int userId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TO_USER));
                String rec = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RECORD));
                long id = cursor.getLong(0);
                LProtocol.ui.postJournal(userId, id + ":" + rec);
                break;
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    public static void deleteById (long journalId) {
        DBAccess.deleteJournalById(journalId);
    }

    private void post(int userId) {
        this.userId = userId;
        long id = DBAccess.addJournal(this);
        LProtocol.ui.postJournal(userId, id + ":" + this.record);
    }
    */

    private static long lastFlushMs;
    private static long lastFlushId;

    public static void flush() {
        LStorage.Entry entry = LStorage.getInstance().get();
        if (entry != null) {
            if (lastFlushId == entry.id && (System.currentTimeMillis() - lastFlushMs < 15000)) {
                //so not to keep flushing the same journal over and over
                LLog.w(TAG, "journal flush request ignored: " + entry.id + " lastFlushMs: "
                        + lastFlushMs + " delta: " + (lastFlushMs - System.currentTimeMillis()));
            } else {
                lastFlushId = entry.id;
                lastFlushMs = System.currentTimeMillis();
                LLog.d(TAG, "post journal: " + entry.id);

                LProtocol.ui.postJournal(entry.userId, entry.id, entry.data);
            }
        }
    }

    public static void deleteById(int journalId) {
        //LLog.d(TAG, "release journal: " + journalId);
        LStorage.getInstance().release(journalId);
    }

    private void post(int userId) {
        LStorage.Entry entry = new LStorage.Entry();
        entry.userId = userId;
        try {
            //entry.data = Arrays.copyOf(this.data.getBuf(), this.data.getLen());
            entry.data = new byte[this.data.getLen()];
            System.arraycopy(this.data.getBuf(), 0, entry.data, 0, this.data.getLen());
        } catch(Exception e) {
            LLog.e(TAG, "unexpected record: " + e.getMessage());
        }

        //automatically flush the very first journal, so we don't always wait for polling handler to time out.
        //this is not thread safe, unlikely but possible that the following may happen,
        // 1. postNow = FALSE (since there's pending journal)
        // 2. other thread prempts, and release journal, do a flush(), and nothing happens, since
        //    there's no more journal available
        // 3. this thread comes back, create the new journal, but will not post the journal immediately
        //remedy: when this happens the journal will be posted when polling handler times out in main service thread.
        boolean postNow = LStorage.getInstance().getCacheLength() <= 0;
        LStorage.getInstance().put(entry);
        if (postNow) flush();
    }

    public static String transactionItemString(LTransaction item) {
        LCategory category = DBCategory.getById(item.getCategory());
        LVendor vendor = DBVendor.getById(item.getVendor());
        LTag tag = DBTag.getById(item.getTag());
        String rid = item.getRid();

        int type = item.getType();
        //if (type == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
        //    type = LTransaction.TRANSACTION_TYPE_TRANSFER;
        //}

        String str = DBHelper.TABLE_COLUMN_STATE + "=" + item.getState() + ","
                + DBHelper.TABLE_COLUMN_TYPE + "=" + type + ","
                + DBHelper.TABLE_COLUMN_AMOUNT + "=" + item.getValue() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP + "=" + item.getTimeStamp() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + item.getTimeStampLast() + ","
                + DBHelper.TABLE_COLUMN_MADEBY + "=" + item.getBy() + ",";

        if (category != null && (!TextUtils.isEmpty(category.getName()))) {
            str += DBHelper.TABLE_COLUMN_CATEGORY + "=" + category.getName()
                    + ";" + category.getRid()
                    + ";" + category.getTimeStampLast()
                    + ",";
        }
        if (vendor != null && (!TextUtils.isEmpty(vendor.getName()))) {
            str += DBHelper.TABLE_COLUMN_VENDOR + "=" + vendor.getName()
                    + ";" + vendor.getRid()
                    + ";" + vendor.getTimeStampLast()
                    + ";" + vendor.getType()
                    + ",";
        }
        if (tag != null && (!TextUtils.isEmpty(tag.getName()))) {
            str += DBHelper.TABLE_COLUMN_TAG + "=" + tag.getName()
                    + ";" + tag.getRid()
                    + ";" + tag.getTimeStampLast()
                    + ",";
        }

        if (!TextUtils.isEmpty(item.getNote())) {
            str += DBHelper.TABLE_COLUMN_NOTE + "=" + item.getNote() + ",";
        }

        /*
        if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
            LAccount account2 = DBAccount.getById(item.getAccount2());
            str += DBHelper.TABLE_COLUMN_ACCOUNT2 + "=" + account2.getName()
                    + ";" + account2.getRid()
                    + ";" + account2.getTimeStampLast()
                    + ",";
        } else if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
            //always transmit the original TRANSFER, not the copy.
            LAccount account2 = DBAccount.getById(item.getAccount2());
            str += DBHelper.TABLE_COLUMN_ACCOUNT2 + "=" + account.getName()
                    + ";" + account.getRid()
                    + ";" + account.getTimeStampLast()
                    + ",";
            account = account2;
            rid = item.getRid().substring(0, item.getRid().length() - 1);
        }
        */
        str += DBHelper.TABLE_COLUMN_RID + "=" + rid;
        //str += DBHelper.TABLE_COLUMN_RID + "=" + rid + ",";
        //str += DBHelper.TABLE_COLUMN_ACCOUNT + "=" + account.getName()
        //        + ";" + account.getRid()
        //        + ";" + account.getTimeStampLast();

        return str;
    }

    public boolean shareItem(int userId, int accountGid, LTransaction item) {
        data.putShortAutoInc(JRQST_SHARE_TRANSITION_RECORD);
        data.putIntAutoInc(accountGid);

        try {
            byte[] rec = transactionItemString(item).getBytes("UTF-8");
            data.putShortAutoInc((short) rec.length);
            data.appendAutoInc(rec);
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
        data.setLen(data.getBufOffset());
        post(userId);
        return true;
    }

    private boolean post_item_update(LTransaction item) {
        LAccount account = DBAccount.getById(item.getAccount());
        if (!account.isShareConfirmed()) return false;

        data.putShortAutoInc(JRQST_SHARE_TRANSITION_RECORD);
        data.putIntAutoInc(account.getGid());

        record += transactionItemString(item);
         try {
            byte[] rec = record.getBytes("UTF-8");
            data.putShortAutoInc((short) rec.length);
            data.appendAutoInc(rec);
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
        data.setLen(data.getBufOffset());

        ArrayList<Integer> ids = account.getShareIds();
        ArrayList<Integer> states = account.getShareStates();
        for (int ii = 0; ii < states.size(); ii++) {
            if (states.get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED_SYNCED) {
                post(ids.get(ii));
            }
        }
        return true;
    }

    public boolean updateItem(LTransaction item, int oldState) {
        record = DBHelper.TABLE_COLUMN_STATE + "old=" + oldState + ",";
        return post_item_update(item);
    }

    private void item_diff(LTransaction item, LTransaction oldItem) {
        if (oldItem.getValue() != item.getValue()) {
            record += DBHelper.TABLE_COLUMN_AMOUNT + "old=" + oldItem.getValue() + ",";
        }

        if (oldItem.getType() != item.getType()) {
            record += DBHelper.TABLE_COLUMN_TYPE + "old=" + oldItem.getType() + ",";
        }
        if (oldItem.getCategory() != item.getCategory()) {
            LCategory category = DBCategory.getById(oldItem.getCategory());
            if (category != null && (!TextUtils.isEmpty(category.getName()))) {
                record += DBHelper.TABLE_COLUMN_CATEGORY + "old=" + category.getName()
                        + ";" + category.getRid()
                        + ";" + category.getTimeStampLast()
                        + ",";
            }
        }
        if (oldItem.getVendor() != item.getVendor()) {
            LVendor vendor = DBVendor.getById(oldItem.getVendor());
            if (vendor != null && (!TextUtils.isEmpty(vendor.getName()))) {
                record += DBHelper.TABLE_COLUMN_VENDOR + "old=" + vendor.getName()
                        + ";" + vendor.getRid()
                        + ";" + vendor.getTimeStampLast()
                        + ";" + vendor.getType()
                        + ",";
            }
        }
        if (oldItem.getTag() != item.getTag()) {
            LTag tag = DBTag.getById(oldItem.getTag());
            if (tag != null && (!TextUtils.isEmpty(tag.getName()))) {
                record += DBHelper.TABLE_COLUMN_TAG + "old=" + tag.getName()
                        + ";" + tag.getRid()
                        + ";" + tag.getTimeStampLast()
                        + ",";
            }
        }
        if (oldItem.getTimeStamp() != item.getTimeStamp()) {
            record += DBHelper.TABLE_COLUMN_TIMESTAMP + "old=" + oldItem.getTimeStamp() + ",";
        }

        if (!oldItem.getNote().contentEquals(item.getNote())) {
            if (!TextUtils.isEmpty(oldItem.getNote())) {
                record += DBHelper.TABLE_COLUMN_NOTE + "old=" + oldItem.getNote() + ",";
            } else {
                record += DBHelper.TABLE_COLUMN_NOTE + "old=_,";
            }
        }
    }

    public boolean updateItem(LTransaction item, LTransaction oldItem) {
        record = "";
        item_diff(item, oldItem);
        return post_item_update(item);
    }

    public boolean updateItem(LTransaction item) {
        record = "";
        return post_item_update(item);
    }

    private boolean post_schedule_item_update(LScheduledTransaction sch) {
        LTransaction item = sch.getItem();
        LAccount account = DBAccount.getById(item.getAccount());
        if ((null == account) || (!account.isShareConfirmed())) return false;

        record += transactionItemString(item) + ","
                + DBHelper.TABLE_COLUMN_REPEAT_COUNT + "=" + sch.getRepeatCount() + ","
                + DBHelper.TABLE_COLUMN_REPEAT_INTERVAL + "=" + sch.getRepeatInterval() + ","
                + DBHelper.TABLE_COLUMN_REPEAT_UNIT + "=" + sch.getRepeatUnit() + ","
                + DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP + "=" + sch.getTimestamp();

        ArrayList<Integer> ids = account.getShareIds();
        ArrayList<Integer> states = account.getShareStates();
        for (int ii = 0; ii < states.size(); ii++) {
            if (states.get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED) {
                post(ids.get(ii));
            }
        }
        return true;
    }

    public boolean updateScheduledItem(LScheduledTransaction sch, int oldState) {
        record = ACTION_UPDATE_SCHEDULED_ITEM + ":" + DBHelper.TABLE_COLUMN_STATE + "old=" + oldState + ",";
        return post_schedule_item_update(sch);
    }

    public boolean updateScheduledItem(LScheduledTransaction sch, LScheduledTransaction oldSch) {
        record = ACTION_UPDATE_SCHEDULED_ITEM + ":";
        if (oldSch.getRepeatCount() != sch.getRepeatCount()) {
            record += DBHelper.TABLE_COLUMN_REPEAT_COUNT + "old=" + oldSch.getRepeatCount() + ",";
        }
        if (oldSch.getRepeatUnit() != sch.getRepeatUnit()) {
            record += DBHelper.TABLE_COLUMN_REPEAT_UNIT + "old=" + oldSch.getRepeatUnit() + ",";
        }
        if (oldSch.getRepeatInterval() != sch.getRepeatInterval()) {
            record += DBHelper.TABLE_COLUMN_REPEAT_INTERVAL + "old=" + oldSch.getRepeatInterval() + ",";
        }
        if (oldSch.getTimestamp() != sch.getTimestamp()) {
            record += DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP + "old=" + oldSch.getTimestamp() + ",";
        }
        item_diff(sch.getItem(), oldSch.getItem());
        return post_schedule_item_update(sch);
    }

    public boolean updateScheduledItem(LScheduledTransaction sch) {
        record = ACTION_UPDATE_SCHEDULED_ITEM + ":";
        return post_schedule_item_update(sch);
    }

    private boolean post_account_update(LAccount account) {
        record += DBHelper.TABLE_COLUMN_STATE + "=" + account.getState() + ","
                + DBHelper.TABLE_COLUMN_NAME + "=" + account.getName() + ","
                + DBHelper.TABLE_COLUMN_RID + "=" + account.getRid() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + account.getTimeStampLast();

        ArrayList<Integer> ids = account.getShareIds();
        ArrayList<Integer> states = account.getShareStates();
        for (int ii = 0; ii < states.size(); ii++) {
            if (states.get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED) {
                post(ids.get(ii));
            }
        }
        return true;
    }

    public boolean updateAccount(LAccount account, String oldName) {
        if (!account.isShareConfirmed()) return false;
        record = ACTION_UPDATE_ACCOUNT + ":" + DBHelper.TABLE_COLUMN_NAME + "old=" + oldName + ",";
        return post_account_update(account);
    }

    public boolean updateAccount(LAccount account, int oldState) {
        if (!account.isShareConfirmed()) return false;

        record = ACTION_UPDATE_ACCOUNT + ":" + DBHelper.TABLE_COLUMN_STATE + "old=" + oldState + ",";
        return post_account_update(account);
    }

    public boolean unshareAccount(int userId, int accountId, int accountGid, String accountName) {
        data.putShortAutoInc(JRQST_UNSHARE_ACCOUNT);
        data.putIntAutoInc(accountId);
        data.putIntAutoInc(accountGid);
        try {
            byte[] name = accountName.getBytes("UTF-8");
            data.putByteAutoInc((byte) name.length);
            data.appendAutoInc(name);
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
        data.setLen(data.getBufOffset());
        post(userId);
        return true;
    }

    public boolean shareAccount(int userId, int accountId, int accountGid, String accountName) {
        data.putShortAutoInc(JRQST_SHARE_ACCOUNT);
        data.putIntAutoInc(accountId);
        data.putIntAutoInc(accountGid);
        try {
            byte[] name = accountName.getBytes("UTF-8");
            data.putByteAutoInc((byte) name.length);
            data.appendAutoInc(name);
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
        data.setLen(data.getBufOffset());
        post(userId);
        return true;
    }

    public boolean confirmAccountShare(boolean confirmed, int accountGid, int shareWithUserId, int shareWithAccountGid) {
        data.putShortAutoInc(JRQST_CONFIRM_ACCOUNT_SHARE);
        data.putByteAutoInc((byte) (confirmed ? 1 : 0));
        data.putIntAutoInc(accountGid);
        data.putIntAutoInc(shareWithUserId);
        data.putIntAutoInc(shareWithAccountGid);
        data.setLen(data.getBufOffset());
        post(LPreferences.getUserId());
        LLog.d(TAG, "confirm account: " + accountGid + " <--> " + shareWithAccountGid + " with user: "
                + shareWithUserId + " confirmed: " + confirmed);
        return true;
    }

    public boolean updateAccountShare(LAccount account, HashSet<Integer> userIds) {
        record = ACTION_UPDATE_ACCOUNT + ":" + DBHelper.TABLE_COLUMN_SHARE + "=" + account.getShareIdsString() + ","
                + DBHelper.TABLE_COLUMN_STATE + "=" + account.getState() + ","
                + DBHelper.TABLE_COLUMN_NAME + "=" + account.getName() + ","
                + DBHelper.TABLE_COLUMN_RID + "=" + account.getRid() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=0";

        for (int id : userIds) {
            post(id);
        }
        return true;
    }

    private boolean post_category_update(LCategory category) {
        HashSet<Integer> users = DBAccess.getAllAccountsConfirmedShareUser();
        if (users.size() < 1) return false;

        LLog.d(TAG, "updating category: " + category.getName() + " UUID: " + category.getRid());
        record += DBHelper.TABLE_COLUMN_STATE + "=" + category.getState() + ","
                + DBHelper.TABLE_COLUMN_NAME + "=" + category.getName() + ","
                + DBHelper.TABLE_COLUMN_RID + "=" + category.getRid() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + category.getTimeStampLast();

        for (int user : users) post(user);
        return true;
    }

    public boolean updateCategory(LCategory category, String oldName) {
        record = ACTION_UPDATE_CATEGORY + ":" + DBHelper.TABLE_COLUMN_NAME + "old=" + oldName + ",";
        return post_category_update(category);
    }

    public boolean updateCategory(LCategory category, int oldState) {
        record = ACTION_UPDATE_CATEGORY + ":" + DBHelper.TABLE_COLUMN_STATE + "old=" + oldState + ",";
        return post_category_update(category);
    }

    public boolean updateCategory(LCategory category) {
        record = ACTION_UPDATE_CATEGORY + ":";
        return post_category_update(category);
    }

    private boolean post_vendor_update(LVendor vendor) {
        HashSet<Integer> users = DBAccess.getAllAccountsConfirmedShareUser();
        if (users.size() < 1) return false;

        record += DBHelper.TABLE_COLUMN_STATE + "=" + vendor.getState() + ","
                + DBHelper.TABLE_COLUMN_TYPE + "=" + vendor.getType() + ","
                + DBHelper.TABLE_COLUMN_NAME + "=" + vendor.getName() + ","
                + DBHelper.TABLE_COLUMN_RID + "=" + vendor.getRid() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + vendor.getTimeStampLast();

        for (int user : users) post(user);
        return true;

    }

    public boolean updateVendor(LVendor vendor, String oldName) {
        record = ACTION_UPDATE_VENDOR + ":" + DBHelper.TABLE_COLUMN_NAME + "old=" + oldName + ",";
        return post_vendor_update(vendor);
    }

    public boolean updateVendor(LVendor vendor, int oldState) {
        record = ACTION_UPDATE_VENDOR + ":" + DBHelper.TABLE_COLUMN_STATE + "old=" + oldState + ",";
        return post_vendor_update(vendor);
    }

    public boolean updateVendor(LVendor vendor) {
        record = ACTION_UPDATE_VENDOR + ":";
        return post_vendor_update(vendor);
    }

    private boolean post_tag_update(LTag tag) {
        HashSet<Integer> users = DBAccess.getAllAccountsConfirmedShareUser();
        if (users.size() < 1) return false;

        record += DBHelper.TABLE_COLUMN_STATE + "=" + tag.getState() + ","
                + DBHelper.TABLE_COLUMN_NAME + "=" + tag.getName() + ","
                + DBHelper.TABLE_COLUMN_RID + "=" + tag.getRid() + ","
                + DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "=" + tag.getTimeStampLast();

        for (int user : users) post(user);
        return true;
    }

    public boolean updateTag(LTag tag, String oldName) {
        record = ACTION_UPDATE_TAG + ":" + DBHelper.TABLE_COLUMN_NAME + "old=" + oldName + ",";
        return post_tag_update(tag);
    }

    public boolean updateTag(LTag tag, int oldState) {
        record = ACTION_UPDATE_TAG + ":" + DBHelper.TABLE_COLUMN_STATE + "old=" + oldState + ",";
        return post_tag_update(tag);
    }

    public boolean updateTag(LTag tag) {
        record = ACTION_UPDATE_TAG + ":";
        return post_tag_update(tag);
    }

    public boolean updateVendorCategory(boolean add, String vendorRid, String category) {
        HashSet<Integer> users = DBAccess.getAllAccountsConfirmedShareUser();
        if (users.size() < 1) return false;

        record = ACTION_UPDATE_VENDOR_CATEGORY + ":"
                + DBHelper.TABLE_COLUMN_STATE + "=" + (add ? DBHelper.STATE_ACTIVE : DBHelper.STATE_DELETED) + ","
                + DBHelper.TABLE_COLUMN_RID + ".vendor=" + vendorRid + ","
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
    private static class OldRecord {
        boolean oldState;
        boolean oldCategory;
        boolean oldVendor;
        boolean oldTag;
        boolean oldType;
        boolean oldNote;
        boolean oldAmount;
        boolean oldTimestamp;

        int state;
        String categoryName;
        String vendorName;
        String tagName;

        int type;
        String note;
        double amount;
        long timestamp;

        public OldRecord() {
            oldState = false;
            oldAmount = false;
            oldCategory = false;
            oldVendor = false;
            oldTag = false;
            oldType = false;
            oldNote = false;
            oldTimestamp = false;
        }
    }

    private static LTransaction parseItemFromReceivedRecord(String receivedRecord, OldRecord oldRecord) {
        LTransaction item = new LTransaction();

        String[] splitRecords = receivedRecord.split(",", -1);
        String rid = "";
        String note = "";
        double amount = 0;
        int type = LTransaction.TRANSACTION_TYPE_EXPENSE;
        int madeBy = 0;
        int state = DBHelper.STATE_ACTIVE;
        long timestamp = 0, timestampLast = 0, categoryId = 0, vendorId = 0, tagId = 0;

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
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_CATEGORY)) {
                String[] sss = ss[1].split(";");

                LCategory category1 = DBCategory.getByName(sss[0]);
                if (null == category1) {
                    LLog.d(TAG, "adding category: " + sss[0] + " UUID: " + sss[1]);
                    categoryId = DBCategory.add(new LCategory(sss[0], sss[1]));
                } else {
                    boolean update = false;
                    if (sss[1].compareTo(category1.getRid()) > 0) {
                        LLog.d(TAG, "updating category: " + sss[0] + " UUID to : " + sss[1] + " from: " + category1.getRid());
                        category1.setRid(sss[1]);
                        update = true;
                    }
                    if (Long.parseLong(sss[2]) >= category1.getTimeStampLast()) {
                        category1.setTimeStampLast(Long.parseLong(sss[2]));
                        update = true;
                    }
                    if (update) DBCategory.update(category1);
                    categoryId = category1.getId();
                }
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_VENDOR)) {
                String[] sss = ss[1].split(";");

                LVendor vendor1 = DBVendor.getByName(sss[0]);
                if (null == vendor1) {
                    vendorId = DBVendor.add(new LVendor(sss[0], sss[1], Integer.valueOf(sss[3])));
                } else {
                    boolean update = false;
                    if (sss[1].compareTo(vendor1.getRid()) > 0) {
                        vendor1.setRid(sss[1]);
                        update = true;
                    }
                    if (Long.valueOf(sss[2]) >= vendor1.getTimeStampLast()) {
                        vendor1.setType(Integer.valueOf(sss[3]));
                        vendor1.setTimeStampLast(Long.valueOf(sss[2]));
                        update = true;
                    }
                    if (update) DBVendor.update(vendor1);
                    vendorId = vendor1.getId();
                }
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TAG)) {
                String[] sss = ss[1].split(";");

                LTag tag1 = DBTag.getByName(sss[0]);
                if (null == tag1) {
                    tagId = DBTag.add(new LTag(sss[0], sss[1]));
                } else {
                    boolean update = false;
                    if (sss[1].compareTo(tag1.getRid()) > 0) {
                        tag1.setRid(sss[1]);
                        update = true;
                    }
                    if (Long.parseLong(sss[2]) >= tag1.getTimeStampLast()) {
                        tag1.setTimeStampLast(Long.parseLong(sss[2]));
                        update = true;
                    }
                    if (update) DBTag.update(tag1);
                    tagId = tag1.getId();
                }
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NOTE)) {
                note = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE + "old")) {
                oldRecord.oldState = true;
                oldRecord.state = Integer.parseInt(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TYPE + "old")) {
                oldRecord.oldType = true;
                oldRecord.type = Integer.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_AMOUNT + "old")) {
                oldRecord.oldAmount = true;
                oldRecord.amount = Double.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP + "old")) {
                oldRecord.oldTimestamp = true;
                oldRecord.timestamp = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NOTE + "old")) {
                oldRecord.oldNote = true;
                oldRecord.note = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_CATEGORY + "old")) {
                oldRecord.oldCategory = true;
                String[] sss = ss[1].split(";");
                oldRecord.categoryName = sss[0];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_VENDOR + "old")) {
                oldRecord.oldVendor = true;
                String[] sss = ss[1].split(";");
                oldRecord.vendorName = sss[0];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TAG + "old")) {
                oldRecord.oldTag = true;
                String[] sss = ss[1].split(";");
                oldRecord.tagName = sss[0];
            }
        }

        item.setState(state);
        item.setValue(amount);
        item.setType(type);
        item.setCategory(categoryId);
        item.setVendor(vendorId);
        item.setTag(tagId);
        item.setTimeStamp(timestamp);
        item.setTimeStampLast(timestampLast);
        item.setNote(note);
        item.setRid(rid);
        item.setBy(madeBy);

        return item;
    }

    //return 0: has old value, but no conflict
    //       1: has old value, has conflict
    //       2. no old value
    private static int itemHasConflict(LTransaction item, OldRecord old) {
        if ((old.oldState && old.state != item.getState())
                || (old.oldAmount && old.amount != item.getValue())
                || (old.oldTimestamp && old.timestamp != item.getTimeStamp())
                || (old.oldType && old.type != item.getType())
                || (old.oldNote && (!old.note.contentEquals(item.getNote()))))
            return 1;

        if (old.oldCategory) {
            LCategory category = DBCategory.getById(item.getCategory());
            if (!old.categoryName.equalsIgnoreCase(category.getName())) {
                LLog.w(TAG, "category conflict: " + old.categoryName + " : " + category.getName());
                return 1;
            }
        }

        if (old.oldVendor) {
            LVendor vendor = DBVendor.getById(item.getVendor());
            if (!old.vendorName.equalsIgnoreCase(vendor.getName())) {
                LLog.w(TAG, "vendor conflict: " + old.vendorName + " : " + vendor.getName());
                return 1;
            }

        }

        if (old.oldTag) {
            LTag tag = DBTag.getById(item.getTag());
            if (!old.tagName.equalsIgnoreCase(tag.getName())) {
                LLog.w(TAG, "tag conflict: " + old.tagName + " : " + tag.getName());
                return 1;
            }
        }

        return ((!old.oldState)
                && (!old.oldAmount)
                && (!old.oldTimestamp)
                && (!old.oldType)
                && (!old.oldNote)
                && (!old.oldCategory)
                && (!old.oldVendor)
                && (!old.oldTag)) ? 2 : 0;
    }

    public static void updateItemFromReceivedRecord(int accountGid, String receivedRecord) {
        LAccount account = DBAccount.getByGid(accountGid);
        if (account == null) {
            LLog.w(TAG, "unexpected, account no longer available: " + account);
            return;
        }

        OldRecord oldRecord = new OldRecord();
        LTransaction receivedItem = parseItemFromReceivedRecord(receivedRecord, oldRecord);
        receivedItem.setAccount(account.getId());

        LTransaction item = DBTransaction.getByRid(receivedItem.getRid());
        if (item != null) {
            int conflict = itemHasConflict(item, oldRecord);
            LLog.d(TAG, "update item conflict? " + conflict);
            if (conflict == 0) {
                if (oldRecord.oldState) item.setState(receivedItem.getState());
                if (oldRecord.oldAmount) item.setValue(receivedItem.getValue());
                if (oldRecord.oldType) item.setType(receivedItem.getType());
                if (oldRecord.oldCategory) item.setCategory(receivedItem.getCategory());
                if (oldRecord.oldVendor) item.setVendor(receivedItem.getVendor());
                if (oldRecord.oldTag) item.setTag(receivedItem.getTag());
                if (oldRecord.oldNote) item.setNote(receivedItem.getNote());
                if (oldRecord.oldTimestamp) item.setTimeStamp(receivedItem.getTimeStamp());

                item.setBy(receivedItem.getBy());
                if (item.getTimeStampLast() <= receivedItem.getTimeStampLast())
                    item.setTimeStampLast(receivedItem.getTimeStampLast());

                LLog.d(TAG, "no conflict: update item, amount: " + item.getValue());
                DBTransaction.update(item);
            } else if (item.getTimeStampLast() <= receivedItem.getTimeStampLast()) {
                item.setState(receivedItem.getState());
                item.setValue(receivedItem.getValue());
                item.setType(receivedItem.getType());
                item.setCategory(receivedItem.getCategory());
                item.setVendor(receivedItem.getVendor());
                item.setTag(receivedItem.getTag());
                item.setNote(receivedItem.getNote());
                item.setTimeStamp(receivedItem.getTimeStamp());
                item.setTimeStampLast(receivedItem.getTimeStampLast());
                item.setBy(receivedItem.getBy());

                LLog.d(TAG, "override: update item, amount: " + item.getValue());
                DBTransaction.update(item);
            } else {
                LLog.w(TAG, "account: " + account.getName() + " received conflict record amount: " + item.getValue());
            }
        } else {
            DBTransaction.add(new LTransaction(receivedItem.getRid(),
                    receivedItem.getValue(),
                    receivedItem.getType(),
                    receivedItem.getCategory(),
                    receivedItem.getVendor(),
                    receivedItem.getTag(),
                    receivedItem.getAccount(),
                    0,
                    receivedItem.getBy(),
                    receivedItem.getTimeStamp(),
                    receivedItem.getTimeStampLast(),
                    receivedItem.getNote()));
        }
    }

    public static void updateItemFromReceivedRecord(String receivedRecord) {
        OldRecord oldRecord = new OldRecord();
        LTransaction receivedItem = parseItemFromReceivedRecord(receivedRecord, oldRecord);

        LTransaction item = DBTransaction.getByRid(receivedItem.getRid());
        if (item != null) {
            int conflict = itemHasConflict(item, oldRecord);
            LLog.d(TAG, "update item conflict? " + conflict);
            if (conflict == 0) {
                if (oldRecord.oldState) item.setState(receivedItem.getState());
                if (oldRecord.oldAmount) item.setValue(receivedItem.getValue());
                if (oldRecord.oldType) item.setType(receivedItem.getType());
                if (oldRecord.oldCategory) item.setCategory(receivedItem.getCategory());
                if (oldRecord.oldCategory) item.setVendor(receivedItem.getVendor());
                if (oldRecord.oldTag) item.setTag(receivedItem.getTag());
                if (oldRecord.oldNote) item.setNote(receivedItem.getNote());
                if (oldRecord.oldTimestamp) item.setTimeStamp(receivedItem.getTimeStamp());

                item.setBy(receivedItem.getBy());
                if (item.getTimeStampLast() <= receivedItem.getTimeStampLast())
                    item.setTimeStampLast(receivedItem.getTimeStampLast());

                LLog.d(TAG, "no conflict: update item, amount: " + item.getValue());
                DBTransaction.update(item);
            } else if (item.getTimeStampLast() <= receivedItem.getTimeStampLast()) {
                item.setState(receivedItem.getState());
                item.setValue(receivedItem.getValue());
                item.setType(receivedItem.getType());
                item.setAccount(receivedItem.getAccount());
                item.setAccount2(receivedItem.getAccount2());
                item.setCategory(receivedItem.getCategory());
                item.setVendor(receivedItem.getVendor());
                item.setTag(receivedItem.getTag());
                item.setBy(receivedItem.getBy());
                item.setTimeStamp(receivedItem.getTimeStamp());
                if (item.getTimeStampLast() <= receivedItem.getTimeStampLast())
                    item.setTimeStampLast(receivedItem.getTimeStampLast());
                item.setNote(receivedItem.getNote());
                LLog.d(TAG, "override: update item, amount: " + item.getValue());
                DBTransaction.update(item);
            } else {
                LLog.w(TAG, "conflicts: " + conflict + " received journal ignored amount: " + item.getValue());
                /*LLog.w(TAG, "conflicts: received journal ignored due to local edit: "
                        + (new Date(item.getTimeStampLast()) + ":"
                        + (new Date(receivedItem.getTimeStampLast())))
                        + " last: " + item.getTimeStampLast()
                        + " new: " + receivedItem.getTimeStampLast());
                        */
            }
        } else {
            DBTransaction.add(new LTransaction(receivedItem.getRid(),
                    receivedItem.getValue(),
                    receivedItem.getType(),
                    receivedItem.getCategory(),
                    receivedItem.getVendor(),
                    receivedItem.getTag(),
                    receivedItem.getAccount(),
                    receivedItem.getAccount2(),
                    receivedItem.getBy(),
                    receivedItem.getTimeStamp(),
                    receivedItem.getTimeStampLast(),
                    receivedItem.getNote()));
        }
    }

    private static void updateScheduledItemFromReceivedRecord(String receivedRecord) {
        OldRecord oldRecord = new OldRecord();
        LTransaction receivedItem = parseItemFromReceivedRecord(receivedRecord, oldRecord);

        String[] splitRecords = receivedRecord.split(",", -1);
        long timestamp = 0;
        int repeatUnit = LScheduledTransaction.REPEAT_UNIT_MONTH;
        int repeatInterval = 1;
        int repeatCount = 0;
        boolean oldTimestampFound = false;
        boolean oldIntervalFound = false;
        boolean oldUnitFound = false;
        boolean oldCountFound = false;
        long oldTimestamp = 0;
        int oldInterval = 0;
        int oldUnit = 0;
        int oldCount = 0;

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
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_REPEAT_COUNT + "old")) {
                oldCountFound = true;
                oldCount = Integer.parseInt(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_REPEAT_INTERVAL + "old")) {
                oldIntervalFound = true;
                oldInterval = Integer.parseInt(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_REPEAT_UNIT + "old")) {
                oldUnitFound = true;
                oldUnit = Integer.parseInt(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP + "old")) {
                oldTimestampFound = true;
                oldTimestamp = Long.parseLong(ss[1]);
            }
        }

        LScheduledTransaction sch = DBScheduledTransaction.getByRid(receivedItem.getRid());
        if (sch != null) {
            LTransaction item = sch.getItem();
            int conflict = itemHasConflict(item, oldRecord);
            if (conflict != 1) {
                if ((oldCountFound && oldCount != repeatCount)
                        || (oldIntervalFound && oldInterval != repeatInterval)
                        || (oldUnitFound && oldUnit != repeatUnit)
                        || (oldTimestampFound && oldTimestamp != timestamp)) {
                    conflict = 1;
                } else {
                    conflict = ((!oldCountFound) && (!oldIntervalFound) && (!oldUnitFound) && (!oldTimestampFound)) ? 2 : 0;
                }
            }

            if (conflict == 0) {
                if (oldRecord.oldState) item.setState(receivedItem.getState());
                if (oldRecord.oldAmount) item.setValue(receivedItem.getValue());
                if (oldRecord.oldType) item.setType(receivedItem.getType());
                if (oldRecord.oldCategory) item.setCategory(receivedItem.getCategory());
                if (oldRecord.oldCategory) item.setVendor(receivedItem.getVendor());
                if (oldRecord.oldTag) item.setTag(receivedItem.getTag());
                if (oldRecord.oldNote) item.setNote(receivedItem.getNote());
                if (oldRecord.oldTimestamp) item.setTimeStamp(receivedItem.getTimeStamp());
                item.setBy(receivedItem.getBy());
                if (item.getTimeStampLast() <= receivedItem.getTimeStampLast())
                    item.setTimeStampLast(receivedItem.getTimeStampLast());

                if (oldCountFound) sch.setRepeatCount(repeatCount);
                if (oldIntervalFound) sch.setRepeatInterval(repeatInterval);
                if (oldUnitFound) sch.setRepeatUnit(repeatUnit);
                if (oldTimestampFound) sch.setTimestamp(timestamp);
                DBScheduledTransaction.update(sch);
            } else if (item.getTimeStampLast() <= receivedItem.getTimeStampLast()) {
                item.setState(receivedItem.getState());
                item.setValue(receivedItem.getValue());
                item.setType(receivedItem.getType());
                item.setAccount(receivedItem.getAccount());
                item.setAccount2(receivedItem.getAccount2());
                item.setCategory(receivedItem.getCategory());
                item.setVendor(receivedItem.getVendor());
                item.setTag(receivedItem.getTag());
                item.setBy(receivedItem.getBy());
                item.setTimeStamp(receivedItem.getTimeStamp());
                if (item.getTimeStampLast() <= receivedItem.getTimeStampLast())
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

    private static void updateAccountFromReceivedRecord(String receivedRecord) {
        String[] splitRecords = receivedRecord.split(",", -1);
        String rid = "";
        int state = DBHelper.STATE_ACTIVE;
        long timestampLast = 0;
        String name = "";
        boolean oldNameFound = false;
        String oldName = "";
        boolean oldStateFound = false;
        int oldState = DBHelper.STATE_ACTIVE;
        boolean stateFound = false;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
                stateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE + "old")) {
                oldState = Integer.parseInt(ss[1]);
                oldStateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) {
                timestampLast = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME)) {
                name = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME + "old")) {
                oldName = ss[1];
                oldNameFound = true;
            }
        }

        if (!TextUtils.isEmpty(rid)) {
            LAccount account = DBAccount.getByRid(rid);
            if (account == null) {
                //account can only be update *after* it has been shared, and UUID can not change after
                //share is settled
                LLog.w(TAG, "account removed? " + name + " : " + rid);
            } else {
                boolean conflict = true;
                if (oldNameFound || oldStateFound) {
                    conflict = false;
                    if ((oldStateFound && oldState != account.getState())
                            || (oldNameFound && !oldName.contentEquals(account.getName())))
                        conflict = true;
                }

                if (!conflict) {
                    if (oldStateFound) {
                        account.setState(state);
                        if (account.getState() == DBHelper.STATE_DELETED) {
                            DBTransaction.deleteByAccount(account.getId());
                            DBScheduledTransaction.deleteByAccount(account.getId());
                        }
                    }
                    if (oldNameFound) account.setName(name);

                    if (account.getTimeStampLast() < timestampLast)
                        account.setTimeStampLast(timestampLast);
                    DBAccount.update(account);
                } else if (account.getTimeStampLast() <= timestampLast) {
                    if (!TextUtils.isEmpty(name)) account.setName(name);
                    if (stateFound) account.setState(state);

                    if (account.getState() == DBHelper.STATE_DELETED) {
                        DBTransaction.deleteByAccount(account.getId());
                        DBScheduledTransaction.deleteByAccount(account.getId());
                    }

                    if (account.getTimeStampLast() < timestampLast)
                        account.setTimeStampLast(timestampLast);
                    DBAccount.update(account);
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
        int oldState = DBHelper.STATE_ACTIVE;
        boolean oldStateFound = false;
        String oldName = "";
        boolean oldNameFound = false;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
                stateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE + "old")) {
                oldState = Integer.parseInt(ss[1]);
                oldStateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) {
                timestampLast = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME)) {
                name = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME + "old")) {
                oldName = ss[1];
                oldNameFound = true;
            }
        }

        if (!TextUtils.isEmpty(rid)) {
            LCategory category = DBCategory.getByRid(rid);
            boolean resetRid = false;
            if (category == null) {
                //if category with given RID could not be found, it means,
                //- this is a brand new category shared from peer, or
                //- this is a rename to an existing category that has yet to be shared
                //- shared category RID has been updated without our knowledge
                //either way, before creating a new entry, let's query to make sure the name is unique.
                LLog.d(TAG, "unable to find category with given ID, but named category exist: '" + name + "' with id: " + rid);
                category = DBCategory.getByName(name);
                if (category == null && oldNameFound) {
                    category = DBCategory.getByName(oldName);
                }
                resetRid = true;
            }
            if (category == null) {
                LLog.d(TAG, "unable to find category '" + name + "' with id: " + rid);
                category = new LCategory(name, rid, timestampLast);
                DBCategory.add(category);
            } else {
                boolean conflict = true;
                if (oldNameFound || oldStateFound) {
                    conflict = false;
                    if ((oldStateFound && oldState != category.getState())
                            || (oldNameFound && !oldName.contentEquals(category.getName())))
                        conflict = true;
                }

                if (resetRid && (rid.compareTo(category.getRid()) > 0)) {
                    LLog.d(TAG, "updating category: " + name + " UUID to : " + rid + " from: " + category.getRid());
                    category.setRid(rid);
                    conflict = false;
                }
                LLog.d(TAG, "updating category '" + name + "' with id: " + rid + " : " + conflict);
                if (!conflict) {
                    if (oldStateFound) category.setState(state);
                    if (oldNameFound) category.setName(name);
                    if (category.getTimeStampLast() < timestampLast)
                        category.setTimeStampLast(timestampLast);
                    LLog.d(TAG, "no conflict: updating category '" + name);
                    DBCategory.update(category);
                } else if (category.getTimeStampLast() <= timestampLast) {
                    if (!TextUtils.isEmpty(name)) category.setName(name);
                    if (stateFound) category.setState(state);
                    if (category.getTimeStampLast() < timestampLast)
                        category.setTimeStampLast(timestampLast);
                    LLog.d(TAG, "override: updating category '" + name);
                    DBCategory.update(category);
                }
            }
        }
    }

    public static void updateVendorFromReceivedRecord(String receivedRecord) {
        String[] splitRecords = receivedRecord.split(",", -1);
        String rid = "";
        int state = DBHelper.STATE_ACTIVE;
        int type = LVendor.TYPE_PAYEE;
        long timestampLast = 0;
        String name = "";
        boolean stateFound = false;
        boolean typeFound = false;
        int oldState = DBHelper.STATE_ACTIVE;
        boolean oldStateFound = false;
        String oldName = "";
        boolean oldNameFound = false;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
                stateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE + "old")) {
                oldState = Integer.parseInt(ss[1]);
                oldStateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) {
                timestampLast = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME)) {
                name = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME + "old")) {
                oldName = ss[1];
                oldNameFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TYPE)) {
                type = Integer.valueOf(ss[1]);
                typeFound = true;
            }
        }

        if (!TextUtils.isEmpty(rid)) {
            boolean resetRid = false;
            LVendor vendor = DBVendor.getByRid(rid);
            if (vendor == null) {
                vendor = DBVendor.getByName(name);
                if (vendor == null && oldNameFound) {
                    vendor = DBVendor.getByName(oldName);
                }
                resetRid = true;
            }
            if (vendor == null) {
                vendor = new LVendor(name, type, rid, timestampLast);
                DBVendor.add(vendor);
            } else {
                boolean conflict = true;
                if (oldNameFound || oldStateFound) {
                    conflict = false;
                    if ((oldStateFound && oldState != vendor.getState())
                            || (oldNameFound && !oldName.contentEquals(vendor.getName())))
                        conflict = true;
                }

                if (resetRid && (rid.compareTo(vendor.getRid()) > 0)) {
                    LLog.d(TAG, "updating vendor: " + name + " UUID to : " + rid + " from: " + vendor.getRid());
                    vendor.setRid(rid);
                    conflict = false;
                }

                if (!conflict) {
                    if (oldStateFound) vendor.setState(state);
                    if (oldNameFound) vendor.setName(name);
                    //TODO: fix 'oldTypeFound' support
                    if (typeFound) vendor.setType(type);
                    if (vendor.getTimeStampLast() < timestampLast)
                        vendor.setTimeStampLast(timestampLast);
                    DBVendor.update(vendor);
                } else if (vendor.getTimeStampLast() <= timestampLast) {
                    if (!TextUtils.isEmpty(name)) vendor.setName(name);
                    if (stateFound) vendor.setState(state);
                    if (typeFound) vendor.setType(type);
                    if (vendor.getTimeStampLast() < timestampLast)
                        vendor.setTimeStampLast(timestampLast);
                    DBVendor.update(vendor);
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
        int oldState = DBHelper.STATE_ACTIVE;
        boolean oldStateFound = false;
        String oldName = "";
        boolean oldNameFound = false;

        for (String str : splitRecords) {
            String[] ss = str.split("=", -1);
            if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE)) {
                state = Integer.parseInt(ss[1]);
                stateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_STATE + "old")) {
                oldState = Integer.parseInt(ss[1]);
                oldStateFound = true;
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) {
                timestampLast = Long.valueOf(ss[1]);
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_RID)) {
                rid = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME)) {
                name = ss[1];
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME + "old")) {
                oldName = ss[1];
                oldNameFound = true;
            }
        }

        if (!TextUtils.isEmpty(rid)) {
            LTag tag = DBTag.getByRid(rid);
            boolean resetRid = false;
            if (tag == null) {
                tag = DBTag.getByName(name);
                if (tag == null && oldNameFound) {
                    tag = DBTag.getByName(oldName);
                }
                resetRid = true;
            }
            if (tag == null) {
                tag = new LTag(name, rid, timestampLast);
                DBTag.add(tag);
            } else {
                boolean conflict = true;
                if (oldNameFound || oldStateFound) {
                    conflict = false;
                    if ((oldStateFound && oldState != tag.getState())
                            || (oldNameFound && !oldName.contentEquals(tag.getName())))
                        conflict = true;
                }

                if (resetRid && (rid.compareTo(tag.getRid()) > 0)) {
                    LLog.d(TAG, "updating tag: " + name + " UUID to : " + rid + " from: " + tag.getRid());
                    tag.setRid(rid);
                    conflict = false;
                }

                if (!conflict) {
                    if (oldStateFound) tag.setState(state);
                    if (oldNameFound) tag.setName(name);
                    if (tag.getTimeStampLast() < timestampLast)
                        tag.setTimeStampLast(timestampLast);
                    DBTag.update(tag);
                } else if (tag.getTimeStampLast() <= timestampLast) {
                    if (!TextUtils.isEmpty(name)) tag.setName(name);
                    if (stateFound) tag.setState(state);
                    if (tag.getTimeStampLast() < timestampLast)
                        tag.setTimeStampLast(timestampLast);
                    DBTag.update(tag);
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

        if ((!TextUtils.isEmpty(vendorRid)) && (!TextUtils.isEmpty(categoryRid))) {
            long vendor = DBVendor.getIdByRid(vendorRid);
            long category = DBCategory.getIdByRid(categoryRid);
            DBVendor.updateCategory(vendor, category, state == DBHelper.STATE_ACTIVE);
        }
    }

    private static void shareAccountFromReceivedRecord(String receivedRecord) {
        LLog.d(TAG, receivedRecord);
        //TODO:
    }

    public static void receive(String recvRecord) {
        String[] ss = recvRecord.split(":", 3);
        int action = Integer.parseInt(ss[1]);
        switch (action) {
            case ACTION_SHARE_ITEM:
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

            case ACTION_SHARE_ACCOUNT:
                shareAccountFromReceivedRecord(ss[2]);
                break;
        }
    }

    //////////////////////////////////////////////////////////////////////////
    public static void pushAllAccountRecords(int userId, LAccount account) {
        new MyTask().execute(new MyParams(userId, account));
    }

    private static boolean do_pushAllAccountRecords(int userId, LAccount account) {
        if (null == account) return false;
        if (account.getGid() == 0) {
            LLog.w(TAG, "unexpected: account GID not set");
        }

        Cursor cursor = DBTransaction.getCursorByAccount(account.getId());
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            LJournal journal = new LJournal();

            do {
                LTransaction item = new LTransaction();
                DBTransaction.getValues(cursor, item);
                journal.shareItem(userId, account.getGid(), item);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        cursor = DBScheduledTransaction.getCursorByAccount(account.getId());
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            LJournal journal = new LJournal();

            do {
                LScheduledTransaction sitem = new LScheduledTransaction();
                DBScheduledTransaction.getValues(cursor, sitem);
                journal.updateScheduledItem(sitem);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return true;
    }

    private static class MyParams {
        int userId;
        LAccount account;

        MyParams(int userId, LAccount account) {
            this.userId = userId;
            this.account = account;
        }
    }

    private static class MyTask extends AsyncTask<MyParams, Void, Boolean> {
        @Override
        protected Boolean doInBackground(MyParams... params) {
            MyParams p = params[0];
            return do_pushAllAccountRecords(p.userId, p.account);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
            }
        }
    }
}