package com.swoag.logalong.entities;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */


import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.swoag.logalong.LApp;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LBuffer;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LStorage;

public class LJournal {
    private static final String TAG = LJournal.class.getSimpleName();

    private static final int MAX_JOURNAL_DATA_BYTES = 1024 * 64;
    private LBuffer data;

    public LJournal() {
        this.data = new LBuffer(MAX_JOURNAL_DATA_BYTES);
    }

    private static long lastFlushMs;
    private static long lastFlushId;

    private abstract class GenericJournalFlushAction<D> {
        abstract D getById(long id);

        abstract D getByIdAll(long id);

        abstract boolean isGidAssigned(D d);

        abstract short getRequestCode();

        abstract boolean add_data(LBuffer jdata, D d);

        private boolean do_add(LBuffer jdata, D d) {
            jdata.clear();
            jdata.putShortAutoInc(getRequestCode());
            jdata.putLongAutoInc(LDbBase.class.cast(d).getId());
            return add_data(jdata, d);
        }

        private boolean do_update(LBuffer jdata, D d) {
            jdata.clear();
            jdata.putShortAutoInc((short) (getRequestCode() + 1));
            jdata.putLongAutoInc(LDbBase.class.cast(d).getGid());
            return add_data(jdata, d);
        }

        private void do_delete(LBuffer jdata, D d) {
            jdata.clear();
            jdata.putShortAutoInc((short) (getRequestCode() + 2));
            jdata.putLongAutoInc(LDbBase.class.cast(d).getGid());
            jdata.setLen(jdata.getBufOffset());
        }

        public boolean add(LBuffer jdata, LBuffer ndata) {
            long id = jdata.getLongAutoInc();
            D d = getById(id);
            if (d == null) {
                //ok to ignore add request if entry has been deleted afterwards
                LLog.w(TAG, "unable to find record with id: " + id);
                removeEntry = true;
            } else if (isGidAssigned(d)) {
                // ok to ignore add request if entry has GID assigned already
                removeEntry = true;
            } else {
                if (!do_add(ndata, d)) {
                    LLog.w(TAG, "unable to add record with id: " + id);
                    removeEntry = true;
                } else
                    newEntry = true;
            }
            return true;
        }

        public boolean update(LBuffer jdata, LBuffer ndata) {
            long id = jdata.getLongAutoInc();
            D d = getById(id);
            if (d == null) {
                //ok to ignore update request if entry has been deleted afterwards
                LLog.w(TAG, "unable to find record with id: " + id);
                removeEntry = true;
            } else {
                if (LDbBase.class.cast(d).getGid() == 0) {
                    // let service to retry later
                    if (++errorCount > MAX_ERROR_RETRIES) {
                        removeEntry = true;
                        LLog.e(TAG, "update " + LDbBase.class.cast(d).getName() + " GID not available");
                    } else
                        return false;
                } else {
                    do_update(ndata, d);
                    newEntry = true;
                }
            }
            return true;
        }

        public boolean delete(LBuffer jdata, LBuffer ndata) {
            long id = jdata.getLongAutoInc();
            D d = getByIdAll(id);
            if (d == null) {
                //ok to ignore delete request if entry has been deleted already
                LLog.w(TAG, "unable to find record with id: " + id);
                removeEntry = true;
            } else {
                if (LDbBase.class.cast(d).getGid() == 0) {
                    // let service to retry later
                    if (++errorCount > MAX_ERROR_RETRIES) {
                        removeEntry = true;
                        LLog.e(TAG, "delete " + LDbBase.class.cast(d).getName() + " GID not available");
                    } else
                        return false;
                } else {
                    do_delete(ndata, d);
                    newEntry = true;
                }
            }
            return true;
        }
    }

    private class RecordJournalFlushAction extends GenericJournalFlushAction<LTransactionDetails> {
        @Override
        LTransactionDetails getById(long id) {
            return DBTransaction.getInstance().getDetailsById(id);
        }

        @Override
        LTransactionDetails getByIdAll(long id) {
            return DBTransaction.getInstance().getDetailsByIdAll(id);
        }

        @Override
        boolean isGidAssigned(LTransactionDetails details) {
            return details.getGid() != 0;
        }

        @Override
        short getRequestCode() {
            return LProtocol.JRQST_ADD_RECORD;
        }

        @Override
        boolean add_data(LBuffer jdata, LTransactionDetails details) {
            if (LTransaction.TRANSACTION_TYPE_TRANSFER_COPY == details.getTransaction().getType()) return false;

            jdata.putLongAutoInc(details.getAccount().getGid());
            jdata.putLongAutoInc(details.getAccount2().getGid());
            jdata.putLongAutoInc(details.getCategory().getGid());
            jdata.putLongAutoInc(details.getTag().getGid());
            jdata.putLongAutoInc(details.getVendor().getGid());
            jdata.putByteAutoInc((byte) details.getTransaction().getType());
            jdata.putDoubleAutoInc(details.getTransaction().getValue());
            jdata.putLongAutoInc(details.getTransaction().getChangeBy());
            if (0 == details.getTransaction().getRid()) {
                //assign new record rid
                details.getTransaction().generateRid();
                DBTransaction.getInstance().updateColumnById(details.getId(), DBHelper.TABLE_COLUMN_IRID, details
                        .getTransaction().getRid());

                if (LTransaction.TRANSACTION_TYPE_TRANSFER == details.getTransaction().getType()) {
                    DBTransaction.getInstance().updateTransferCopyRid(details.getTransaction());
                }
            }
            jdata.putLongAutoInc(details.getTransaction().getRid());
            jdata.putLongAutoInc(details.getTransaction().getTimeStamp());
            jdata.putLongAutoInc(details.getTransaction().getTimeStampCreate());
            jdata.putLongAutoInc(details.getTransaction().getTimeStampLast());
            try {
                byte[] note = details.getTransaction().getNote().getBytes("UTF-8");
                jdata.putShortAutoInc((short) note.length);
                jdata.putBytesAutoInc(note);
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error when adding record " + e.getMessage());
                return false;
            }
            jdata.setLen(jdata.getBufOffset());
            return true;
        }
    }

    private class ScheduleJournalFlushAction extends GenericJournalFlushAction<LScheduledTransaction> {

        @Override
        LScheduledTransaction getById(long id) {
            return DBScheduledTransaction.getInstance().getById(id);
        }

        @Override
        LScheduledTransaction getByIdAll(long id) {
            return DBScheduledTransaction.getInstance().getByIdAll(id);
        }

        @Override
        boolean isGidAssigned(LScheduledTransaction lScheduledTransaction) {
            return lScheduledTransaction.getGid() != 0;
        }

        @Override
        short getRequestCode() {
            return LProtocol.JRQST_ADD_SCHEDULE;
        }

        @Override
        boolean add_data(LBuffer jdata, LScheduledTransaction lScheduledTransaction) {
            jdata.putLongAutoInc(DBAccount.getInstance().getGidById(lScheduledTransaction.getAccount()));
            jdata.putLongAutoInc(DBAccount.getInstance().getGidById(lScheduledTransaction.getAccount2()));
            jdata.putLongAutoInc(DBCategory.getInstance().getGidById(lScheduledTransaction.getCategory()));
            jdata.putLongAutoInc(DBTag.getInstance().getGidById(lScheduledTransaction.getTag()));
            jdata.putLongAutoInc(DBVendor.getInstance().getGidById(lScheduledTransaction.getVendor()));
            jdata.putByteAutoInc((byte) lScheduledTransaction.getType());
            jdata.putDoubleAutoInc(lScheduledTransaction.getValue());
            jdata.putLongAutoInc(lScheduledTransaction.getChangeBy());
            if (0 == lScheduledTransaction.getRid()) {
                lScheduledTransaction.generateRid();
                DBScheduledTransaction.getInstance().updateColumnById(lScheduledTransaction.getId(), DBHelper
                        .TABLE_COLUMN_IRID, lScheduledTransaction.getRid());
            }
            jdata.putLongAutoInc(lScheduledTransaction.getRid());
            jdata.putLongAutoInc(lScheduledTransaction.getTimeStamp());
            jdata.putLongAutoInc(lScheduledTransaction.getTimeStampCreate());
            jdata.putLongAutoInc(lScheduledTransaction.getTimeStampLast());
            try {
                byte[] note = lScheduledTransaction.getNote().getBytes("UTF-8");
                jdata.putShortAutoInc((short) note.length);
                jdata.putBytesAutoInc(note);
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error when adding schedule " + e.getMessage());
                return false;
            }

            jdata.putLongAutoInc(lScheduledTransaction.getNextTime());
            jdata.putByteAutoInc((byte) lScheduledTransaction.getRepeatInterval());
            jdata.putByteAutoInc((byte) lScheduledTransaction.getRepeatUnit());
            jdata.putByteAutoInc((byte) lScheduledTransaction.getRepeatCount());
            jdata.putByteAutoInc((byte) (lScheduledTransaction.isEnabled() ? 1 : 0));

            jdata.setLen(jdata.getBufOffset());
            return true;
        }
    }

    private class AccountJournalFlushAction extends GenericJournalFlushAction<LAccount> {
        @Override
        LAccount getById(long id) {
            return DBAccount.getInstance().getById(id);
        }

        @Override
        LAccount getByIdAll(long id) {
            return DBAccount.getInstance().getByIdAll(id);
        }

        @Override
        boolean isGidAssigned(LAccount account) {
            return account.getGid() != 0;
        }

        @Override
        short getRequestCode() {
            return LProtocol.JRQST_ADD_ACCOUNT;
        }

        @Override
        boolean add_data(LBuffer jdata, LAccount account) {
            try {
                byte[] name = account.getName().getBytes("UTF-8");
                jdata.putShortAutoInc((short) name.length);
                jdata.putBytesAutoInc(name);
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error when adding account: " + e.getMessage());
                return false;
            }
            jdata.setLen(jdata.getBufOffset());
            return true;
        }
    }

    private class CategoryJournalFlushAction extends GenericJournalFlushAction<LCategory> {
        @Override
        LCategory getById(long id) {
            return DBCategory.getInstance().getById(id);
        }

        @Override
        LCategory getByIdAll(long id) {
            return DBCategory.getInstance().getByIdAll(id);
        }

        @Override
        boolean isGidAssigned(LCategory category) {
            return category.getGid() != 0;
        }

        @Override
        short getRequestCode() {
            return LProtocol.JRQST_ADD_CATEGORY;
        }

        @Override
        boolean add_data(LBuffer jdata, LCategory category) {
            //data.putLongAutoInc(category.getPid());
            jdata.putLongAutoInc(0);
            try {
                byte[] name = category.getName().getBytes("UTF-8");
                jdata.putShortAutoInc((short) name.length);
                jdata.putBytesAutoInc(name);
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error when adding category: " + e.getMessage());
                return false;
            }
            jdata.setLen(jdata.getBufOffset());
            return true;
        }
    }

    private class TagJournalFlushAction extends GenericJournalFlushAction<LTag> {
        @Override
        LTag getById(long id) {
            return DBTag.getInstance().getById(id);
        }

        @Override
        LTag getByIdAll(long id) {
            return DBTag.getInstance().getByIdAll(id);
        }

        @Override
        boolean isGidAssigned(LTag tag) {
            return tag.getGid() != 0;
        }

        @Override
        short getRequestCode() {
            return LProtocol.JRQST_ADD_TAG;
        }

        @Override
        boolean add_data(LBuffer jdata, LTag tag) {
            try {
                byte[] name = tag.getName().getBytes("UTF-8");
                jdata.putShortAutoInc((short) name.length);
                jdata.putBytesAutoInc(name);
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error when adding tag: " + e.getMessage());
                return false;
            }
            jdata.setLen(jdata.getBufOffset());
            return true;
        }
    }

    private class VendorJournalFlushAction extends GenericJournalFlushAction<LVendor> {
        @Override
        LVendor getById(long id) {
            return DBVendor.getInstance().getById(id);
        }

        @Override
        LVendor getByIdAll(long id) {
            return DBVendor.getInstance().getByIdAll(id);
        }

        @Override
        boolean isGidAssigned(LVendor vendor) {
            return vendor.getGid() != 0;
        }

        @Override
        short getRequestCode() {
            return LProtocol.JRQST_ADD_VENDOR;
        }

        @Override
        boolean add_data(LBuffer jdata, LVendor vendor) {
            jdata.putByteAutoInc((byte) vendor.getType());
            try {
                byte[] name = vendor.getName().getBytes("UTF-8");
                jdata.putShortAutoInc((short) name.length);
                jdata.putBytesAutoInc(name);
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error when adding vendor: " + e.getMessage());
                return false;
            }
            jdata.setLen(jdata.getBufOffset());
            return true;
        }
    }

    //return TRUE if any journal is actually posted, otherwise FALSE
    //returning FALSE tells service that it can move forward with server polling, which would
    //come back to flush() again to double check *before* polling actually happens.
    private static int errorCount = 0;
    private static boolean removeEntry = false;
    private static boolean newEntry = false;

    private static final int MAX_ERROR_RETRIES = 10;
    private static RecordJournalFlushAction recordJournalFlushAction;
    private static ScheduleJournalFlushAction scheduleJournalFlushAction;
    private static AccountJournalFlushAction accountJournalFlushAction;
    private static CategoryJournalFlushAction categoryJournalFlushAction;
    private static TagJournalFlushAction tagJournalFlushAction;
    private static VendorJournalFlushAction vendorJournalFlushAction;

    public boolean flush() {
        if (recordJournalFlushAction == null) {
            recordJournalFlushAction = new LJournal.RecordJournalFlushAction();
        }
        if (scheduleJournalFlushAction == null) {
            scheduleJournalFlushAction = new LJournal.ScheduleJournalFlushAction();
        }
        if (accountJournalFlushAction == null) {
            accountJournalFlushAction = new LJournal.AccountJournalFlushAction();
        }
        if (categoryJournalFlushAction == null) {
            categoryJournalFlushAction = new LJournal.CategoryJournalFlushAction();
        }
        if (tagJournalFlushAction == null) {
            tagJournalFlushAction = new LJournal.TagJournalFlushAction();
        }
        if (vendorJournalFlushAction == null) {
            vendorJournalFlushAction = new LJournal.VendorJournalFlushAction();
        }

        LStorage.Entry entry = LStorage.getInstance().get();
        if (null == entry) return false;

        if (lastFlushId == entry.id && (System.currentTimeMillis() - lastFlushMs < 15000)) {
            //so not to keep flushing the same journal over and over
            LLog.w(TAG, "journal flush request ignored: " + entry.id + " lastFlushMs: "
                    + lastFlushMs + " delta: " + (lastFlushMs - System.currentTimeMillis()));
            return false;
        }
        LLog.d(TAG, "total flushing count: " + flushCount++);

        removeEntry = false;
        newEntry = false;

        lastFlushId = entry.id;
        lastFlushMs = System.currentTimeMillis();

        LBuffer jdata = new LBuffer(entry.data);
        LBuffer ndata = new LBuffer(MAX_JOURNAL_DATA_BYTES);
        switch (jdata.getShortAutoInc()) {
            case LProtocol.JRQST_ADD_RECORD:
                if (!recordJournalFlushAction.add(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_UPDATE_RECORD:
                if (!recordJournalFlushAction.update(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_DELETE_RECORD:
                if (!recordJournalFlushAction.delete(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_ADD_SCHEDULE:
                if (!scheduleJournalFlushAction.add(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_UPDATE_SCHEDULE:
                if (!scheduleJournalFlushAction.update(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_DELETE_SCHEDULE:
                if (!scheduleJournalFlushAction.delete(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_ADD_ACCOUNT:
                if (!accountJournalFlushAction.add(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_UPDATE_ACCOUNT:
                if (!accountJournalFlushAction.update(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_DELETE_ACCOUNT:
                if (!accountJournalFlushAction.delete(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_ADD_CATEGORY:
                if (!categoryJournalFlushAction.add(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_UPDATE_CATEGORY:
                if (!categoryJournalFlushAction.update(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_DELETE_CATEGORY:
                if (!categoryJournalFlushAction.delete(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_ADD_TAG:
                if (!tagJournalFlushAction.add(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_UPDATE_TAG:
                if (!tagJournalFlushAction.update(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_DELETE_TAG:
                if (!tagJournalFlushAction.delete(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_ADD_VENDOR:
                if (!vendorJournalFlushAction.add(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_UPDATE_VENDOR:
                if (!vendorJournalFlushAction.update(jdata, ndata)) return false;
                break;
            case LProtocol.JRQST_DELETE_VENDOR:
                if (!vendorJournalFlushAction.delete(jdata, ndata)) return false;
                break;
        }

        errorCount = 0;
        if (removeEntry) {
            deleteById(entry.id);
            return false;
        }

        if (newEntry) {
            try {
                entry.data = new byte[ndata.getLen()];
                System.arraycopy(ndata.getBuf(), 0, entry.data, 0, ndata.getLen());
            } catch (Exception e) {
                LLog.e(TAG, "unexpected record post error: " + e.getMessage());
            }
        }
        LAppServer.getInstance().UiPostJournal(entry.id, entry.data);
        return true;
    }

    public void deleteById(int journalId) {
        LStorage.getInstance().release(journalId);
    }

    private static int postCount = 0;
    private static int flushCount = 0;

    private boolean post() {
        if (TextUtils.isEmpty(LPreferences.getUserId())) return false;

        LStorage.Entry entry = new LStorage.Entry();
        if (entry == null) {
            LLog.e(TAG, "unexpected journal post error: out of memory?");
            return false;
        }
        try {
            entry.data = new byte[this.data.getLen()];
            System.arraycopy(this.data.getBuf(), 0, entry.data, 0, this.data.getLen());
        } catch (Exception e) {
            LLog.e(TAG, "unexpected journal post error: " + e.getMessage());
            return false;
        }
        LStorage.getInstance().put(entry);

        LLog.d(TAG, "total posted journal: " + postCount++);
        Intent intent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_NEW_JOURNAL_AVAILABLE));
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(intent);
        return true;
    }

    public boolean getAllAccounts() {
        return post(LProtocol.JRQST_GET_ACCOUNTS);
    }

    public boolean getAllCategories() {
        return post(LProtocol.JRQST_GET_CATEGORIES);
    }

    public boolean getAllTags() {
        return post(LProtocol.JRQST_GET_TAGS);
    }

    public boolean getAllVendors() {
        return post(LProtocol.JRQST_GET_VENDORS);
    }

    public boolean getAllRecords() {
        //return post(LProtocol.JRQST_GET_RECORDS);
        return getRecords(null);
    }

    public boolean getAllSchedules() {
        return post(LProtocol.JRQST_GET_SCHEDULES);
    }

    public boolean getAccountRecords(long aid) {
        return postById(aid, LProtocol.JRQST_GET_ACCOUNT_RECORDS);
    }

    public boolean getAccountSchedules(long aid) {
        return postById(aid, LProtocol.JRQST_GET_ACCOUNT_SCHEDULES);
    }

    public boolean getAccountUsers(long aid) {
        return postById(aid, LProtocol.JRQST_GET_ACCOUNT_USERS);
    }

    public boolean getRecord(long id) {
        return postById(id, LProtocol.JRQST_GET_RECORD);
    }

    public boolean getRecords(long ids[]) {
        data.clear();
        data.putShortAutoInc(LProtocol.JRQST_GET_RECORDS);
        if (null == ids) {
            data.putShortAutoInc((short) 0); // get all records;
        } else {
            data.putShortAutoInc((short) ids.length);
            for (long id : ids) {
                data.putLongAutoInc(id);
            }
        }
        data.setLen(data.getBufOffset());
        return post();
    }

    public boolean addRecord(long id) {
        return postById(id, LProtocol.JRQST_ADD_RECORD);
    }

    public boolean updateRecord(long id) {
        return postById(id, LProtocol.JRQST_UPDATE_RECORD);
    }

    public boolean deleteRecord(long id) {
        return postById(id, LProtocol.JRQST_DELETE_RECORD);
    }

    public boolean getSchedule(long id) {
        return postById(id, LProtocol.JRQST_GET_SCHEDULE);
    }

    public boolean addSchedule(long id) {
        return postById(id, LProtocol.JRQST_ADD_SCHEDULE);
    }

    public boolean updateSchedule(long id) {
        return postById(id, LProtocol.JRQST_UPDATE_SCHEDULE);
    }

    public boolean deleteSchedule(long id) {
        return postById(id, LProtocol.JRQST_DELETE_SCHEDULE);
    }

    public boolean addAccount(long id) {
        return postById(id, LProtocol.JRQST_ADD_ACCOUNT);
    }

    public boolean updateAccount(long id) {
        return postById(id, LProtocol.JRQST_UPDATE_ACCOUNT);
    }

    public boolean deleteAccount(long id) {
        return postById(id, LProtocol.JRQST_DELETE_ACCOUNT);
    }

    public boolean addCategory(long id) {
        return postById(id, LProtocol.JRQST_ADD_CATEGORY);
    }

    public boolean updateCategory(long id) {
        return postById(id, LProtocol.JRQST_UPDATE_CATEGORY);
    }

    public boolean deleteCategory(long id) {
        return postById(id, LProtocol.JRQST_DELETE_CATEGORY);
    }

    public boolean addTag(long id) {
        return postById(id, LProtocol.JRQST_ADD_TAG);
    }

    public boolean updateTag(long id) {
        return postById(id, LProtocol.JRQST_UPDATE_TAG);
    }

    public boolean deleteTag(long id) {
        return postById(id, LProtocol.JRQST_DELETE_TAG);
    }

    public boolean addVendor(long id) {
        return postById(id, LProtocol.JRQST_ADD_VENDOR);
    }

    public boolean updateVendor(long id) {
        return postById(id, LProtocol.JRQST_UPDATE_VENDOR);
    }

    public boolean deleteVendor(long id) {
        return postById(id, LProtocol.JRQST_DELETE_VENDOR);
    }

    public boolean addUserToAccount(long uid, long aid) {
        return postLongLong(uid, aid, LProtocol.JRQST_ADD_USER_TO_ACCOUNT);
    }

    public boolean removeUserFromAccount(long uid, long aid) {
        return postLongLong(uid, aid, LProtocol.JRQST_REMOVE_USER_FROM_ACCOUNT);
    }

    public boolean confirmAccountShare(long aid, long uid, boolean yes) {
        data.clear();
        data.putShortAutoInc(LProtocol.JRQST_CONFIRM_ACCOUNT_SHARE);
        data.putLongAutoInc(aid);
        data.putLongAutoInc(uid);
        data.putByteAutoInc((byte) (yes ? 1 : 0));
        data.setLen(data.getBufOffset());
        return post();
    }

    private boolean post(short jrqst) {
        data.clear();
        data.putShortAutoInc(jrqst);
        data.setLen(data.getBufOffset());
        return post();
    }

    private boolean postById(long id, short jrqst) {
        data.clear();
        data.putShortAutoInc(jrqst);
        data.putLongAutoInc(id);
        data.setLen(data.getBufOffset());
        return post();
    }

    private boolean postLongLong(long long1, long long2, short jrqst) {
        data.clear();
        data.putShortAutoInc(jrqst);
        data.putLongAutoInc(long1);
        data.putLongAutoInc(long2);
        data.setLen(data.getBufOffset());
        return post();
    }
}