package com.swoag.logalong;
    /* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountBalance;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBAccountBalance;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBProvider;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LTask;

import java.util.Calendar;
import java.util.HashSet;

public class MainService extends Service implements LBroadcastReceiver.BroadcastReceiverListener,
        Loader.OnLoadCompleteListener {
    private static final String TAG = MainService.class.getSimpleName();

    private static final int CMD_START = 10;
    private static final int CMD_STOP = 20;
    private static final int CMD_ENABLE = 30;
    private static final int CMD_DISABLE = 40;
    private static final int CMD_SCAN_BALANCE = 50;

    private static final short NOTIFICATION_UPDATE_USER_PROFILE = 0x001;
    private static final short NOTIFICATION_ADD_ACCOUNT = 0x010;
    private static final short NOTIFICATION_UPDATE_ACCOUNT = 0x011;
    private static final short NOTIFICATION_DELETE_ACCOUNT = 0x012;

    private boolean loggedIn = false;
    private Handler serviceHandler;
    private Runnable pollRunnable;
    private Runnable serviceShutdownRunnable;
    private boolean accountBalanceSynced = false;

    //default to active polling, if polling returned IDLE, switch to IDLE_POLLING interval
    //as soon as any valid command found, go back to active polling.
    static final int NETWORK_IDLE_POLLING_MS = 5000;
    static final int NETWORK_ACTIVE_POLLING_MS = 15 * 60000; //so each polled command has a
    // 15-min window to respond
    static final int SERVICE_SHUTDOWN_MS = 15000;
    private BroadcastReceiver broadcastReceiver;
    private LAppServer server;


    static final int UPDATE_ACCOUNT_BALANCE_DELAY_MS = 3000;
    private Runnable updateAccountBalanceRunnable;
    private static final int LOADER_ID_UPDATE_BALANCE = 10;
    private CursorLoader cursorLoader;
    private Cursor lastLoadedData;
    AsyncScanBalances asyncScanBalances;

    public static void start(Context context) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", MainService.CMD_START);
        context.startService(serviceIntent);
    }

    public static void stop(Context context) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", MainService.CMD_STOP);
        context.startService(serviceIntent);
    }

    public static void enable(Context context) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", MainService.CMD_ENABLE);
        context.startService(serviceIntent);
    }

    public static void disable(Context context) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", MainService.CMD_DISABLE);
        context.startService(serviceIntent);
    }

    public static void scanBalance(Context context) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", MainService.CMD_SCAN_BALANCE);
        context.startService(serviceIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        server = LAppServer.getInstance();

        DBScheduledTransaction.scanAlarm();

        serviceHandler = new Handler() {
        };

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (loggedIn) {
                    LLog.d(TAG, "heart beat polling");
                    //polling happens only where there's no pending journal
                    if (!LJournal.flush()) server.UiPoll();
                    //default to active polling
                    serviceHandler.postDelayed(pollRunnable, NETWORK_ACTIVE_POLLING_MS);
                }
            }
        };

        serviceShutdownRunnable = new Runnable() {
            @Override
            public void run() {
                LLog.d(TAG, "shutdown timeout: shutting down service itself");
                if (!accountBalanceSynced) {
                    cursorLoader.startLoading();
                } else {
                    stopSelf();
                }
            }
        };

        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_NETWORK_CONNECTED,
                LBroadcastReceiver.ACTION_USER_CREATED,
                LBroadcastReceiver.ACTION_LOG_IN,
                LBroadcastReceiver.ACTION_CONNECTED_TO_SERVER,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SET_ACCOUNT_GID,
                LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_ACCOUNT_SHARE,
                LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_ACCOUNT_INFO,
                LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_SHARE_USER_PROFILE,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORD,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORDS,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_CATEGORY,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_PAYER,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_TAG,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_PAYER_CATEGORY,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_SCHEDULE,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH,
                LBroadcastReceiver.ACTION_POST_JOURNAL,
                LBroadcastReceiver.ACTION_POLL,
                LBroadcastReceiver.ACTION_POLL_ACK,
                LBroadcastReceiver.ACTION_NEW_JOURNAL_AVAILABLE}, this);


        updateAccountBalanceRunnable = new Runnable() {
            @Override
            public void run() {
                if (asyncScanBalances.getStatus() != AsyncTask.Status.RUNNING) {
                    asyncScanBalances = new AsyncScanBalances();
                    LTask.start(asyncScanBalances, lastLoadedData);
                } else {
                    serviceHandler.postDelayed(updateAccountBalanceRunnable,
                            UPDATE_ACCOUNT_BALANCE_DELAY_MS);
                }
            }
        };

        asyncScanBalances = new AsyncScanBalances();
        String projection = "a." + DBHelper.TABLE_COLUMN_AMOUNT + ","
                + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + ","
                + "a." + DBHelper.TABLE_COLUMN_TYPE + ","
                + "a." + DBHelper.TABLE_COLUMN_ACCOUNT;
        cursorLoader = new CursorLoader(
                this,
                DBProvider.URI_TRANSACTIONS_ACCOUNT,
                new String[]{projection},
                "a." + DBHelper.TABLE_COLUMN_STATE + "=?",
                new String[]{"" + DBHelper.STATE_ACTIVE},
                "b._id ASC, " + "a." + DBHelper.TABLE_COLUMN_TIMESTAMP + " DESC");

        cursorLoader.registerListener(LOADER_ID_UPDATE_BALANCE, this);
        //cursorLoader.startLoading();
    }

    @Override
    public void onLoadComplete(Loader loader, Object data) {
        if (loader.getId() == LOADER_ID_UPDATE_BALANCE) {
            //LLog.d(TAG, "update balance DB loader completed");
            serviceHandler.removeCallbacks(updateAccountBalanceRunnable);
            if (asyncScanBalances.getStatus() != AsyncTask.Status.RUNNING) {
                asyncScanBalances = new AsyncScanBalances();
                LTask.start(asyncScanBalances, (Cursor) data);
            } else {
                lastLoadedData = (Cursor) data;
                serviceHandler.postDelayed(updateAccountBalanceRunnable,
                        UPDATE_ACCOUNT_BALANCE_DELAY_MS);
            }
        }
    }

    @Override
    public void onDestroy() {
        serviceHandler.removeCallbacks(serviceShutdownRunnable);
        serviceHandler.removeCallbacks(pollRunnable);
        serviceHandler.removeCallbacks(updateAccountBalanceRunnable);
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
        server.disconnect();
        LLog.d(TAG, "service destroyed");

        asyncScanBalances.cancel(true);
        asyncScanBalances = null;

        pollRunnable = null;
        serviceShutdownRunnable = null;
        updateAccountBalanceRunnable = null;
        serviceHandler = null;

        if (cursorLoader != null) {
            cursorLoader.unregisterListener(this);
            cursorLoader.cancelLoad();
            cursorLoader.stopLoading();
        }

        accountBalanceSynced = false;
        loggedIn = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int cmd = intent.getIntExtra("cmd", 0);
            switch (cmd) {
                case CMD_START:
                    serviceHandler.removeCallbacks(serviceShutdownRunnable);
                    if (!TextUtils.isEmpty(LPreferences.getUserId())) {
                        server.connect();
                    }
                    break;

                case CMD_STOP:
                    LLog.d(TAG, "shutdown in responding to stop command");
                    serviceHandler.removeCallbacks(serviceShutdownRunnable);
                    serviceHandler.postDelayed(serviceShutdownRunnable, SERVICE_SHUTDOWN_MS);
                    break;
                case CMD_ENABLE:
                    server.enable();
                    break;

                case CMD_DISABLE:
                    server.disable();
                    break;

                case CMD_SCAN_BALANCE:
                    cursorLoader.startLoading();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    private int journalPostErrorCount = 0;

    /*
     +----------------+      +----------------+      +-----------+      +-----------------+
     |                |      |                |      |           |      |                 |
     | network thread | ---> | response parse | ---> | broadcast | ---> | service         |
     |                |      |                |      |           |      |                 |
     +---^------------+      +----------------+      +-----------+      +---^-------------+
         |                                                 |                |       |   |
         |                                                 |                |       |   |
      +---------------+      +-----------+                 |    +---------------+   |   |
      |               |      |           | <---------------'    |               |   |   |
      | request queue | <--- | activity  | -------------------> | journal queue |   |   |
      |               |      |           |                      |               |   |   |
      +--^----^-------+      +-----------+                      +---------------+   |   |
         |    |                                                                     |   |
         |    |                                                 +---------------+   |   |
         |    '-------------------------------------------------| flush journal |<--'   |
         |                                                      +---------------+       |
         |                                                                              |
         |                                                 +---------------+            |
         '-------------------------------------------------| poll server   |<-----------'
                                                           +---------------+
    */
    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        //on Samsung phone, receiver may still get called when service is destroyed!!
        if (serviceHandler == null) {
            LLog.w(TAG, "unexpected, receiver called on destroyed service!");
            return;
        }
        serviceHandler.removeCallbacks(pollRunnable); //disable polling by default

        //serviceHandler.removeCallbacks(serviceShutdownRunnable);
        //default polling policy: active, with longer period
        //serviceHandler.removeCallbacks(pollRunnable);
        //serviceHandler.postDelayed(pollRunnable, NETWORK_ACTIVE_POLLING_MS);
        if (!loggedIn) {
            switch (action) {
                case LBroadcastReceiver.ACTION_NETWORK_CONNECTED:
                    LLog.d(TAG, "network connected");
                    server.UiInitScrambler();
                    break;

                case LBroadcastReceiver.ACTION_CONNECTED_TO_SERVER:
                    if (!TextUtils.isEmpty(LPreferences.getUserId())) {
                        server.UiLogIn(LPreferences.getUserId(), LPreferences.getUserPass());
                    }
                    break;

                case LBroadcastReceiver.ACTION_LOG_IN:
                    loggedIn = true;
                    //journal posting and polling start only upon successful login
                    serviceHandler.postDelayed(pollRunnable, 1000); //poll shortly after login
                    break;
            }
        } else {
            switch (action) {
                case LBroadcastReceiver.ACTION_NEW_JOURNAL_AVAILABLE:
                    LJournal.flush();
                    break;

                case LBroadcastReceiver.ACTION_POST_JOURNAL:
                    boolean moreJournal = true;
                    int journalId = intent.getIntExtra("journalId", 0);
                    if (LProtocol.RSPS_OK == ret || LProtocol.RSPS_MORE == ret) {
                        short jrqstId = intent.getShortExtra("jrqstId", (short) 0);
                        short jret = intent.getShortExtra("jret", (short) 0);
                        if (LProtocol.RSPS_OK != jret) {
                            LLog.w(TAG, "journal request " + jrqstId + " failed.");
                        } else {
                            switch (jrqstId) {
                                case LProtocol.JRQST_ADD_ACCOUNT:
                                    int id = intent.getIntExtra("id", 0);
                                    int gid = intent.getIntExtra("gid", 0);
                                    int uid = intent.getIntExtra("uid", 0);

                                    LAccount account = DBAccount.getByGid(gid);
                                    if (null != account) {
                                        if (account.getId() != id) {
                                            LLog.e(TAG, "unexpected error, account GID: " + gid + " already taken " +
                                                    "by " + account.getName());
                                            //this is an unrecoverable error, we'll delete the dangling account
                                            DBAccount.deleteById(account.getId());
                                        }

                                    }
                                    DBAccount.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, gid);
                                    break;

                                case LProtocol.JRQST_ADD_CATEGORY:
                                    id = intent.getIntExtra("id", 0);
                                    gid = intent.getIntExtra("gid", 0);

                                    LCategory category = DBCategory.getByGid(gid);
                                    if (null != category) {
                                        if (category.getId() != id) {
                                            LLog.e(TAG, "unexpected error, category GID: " + gid + " already taken " +
                                                    "by " + category.getName());
                                        }
                                    }
                                    DBCategory.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, gid);
                                    break;
                                case LProtocol.JRQST_ADD_TAG:
                                    id = intent.getIntExtra("id", 0);
                                    gid = intent.getIntExtra("gid", 0);

                                    LTag tag = DBTag.getByGid(gid);
                                    if (null != tag) {
                                        if (tag.getId() != id) {
                                            LLog.e(TAG, "unexpected error, tag GID: " + gid + " already taken " +
                                                    "by " + tag.getName());
                                        }
                                    }
                                    DBTag.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, gid);
                                    break;
                                case LProtocol.JRQST_ADD_VENDOR:
                                    id = intent.getIntExtra("id", 0);
                                    gid = intent.getIntExtra("gid", 0);

                                    LVendor vendor = DBVendor.getByGid(gid);
                                    if (null != vendor) {
                                        if (vendor.getId() != id) {
                                            LLog.e(TAG, "unexpected error, vendor GID: " + gid + " already taken " +
                                                    "by " + vendor.getName());
                                        }
                                    }
                                    DBVendor.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, gid);
                                    break;
                                case LProtocol.JRQST_ADD_RECORD:
                                    id = intent.getIntExtra("id", 0);
                                    long lgid = intent.getLongExtra("gid", 0L);
                                    LTransaction transaction = DBTransaction.getByGid(lgid);
                                    if (null != transaction) {
                                        if (transaction.getId() == id) {
                                            LLog.e(TAG, "unexpected error, record GID: " + lgid + " already taken ");
                                        }
                                    }
                                    DBTransaction.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, lgid);
                                    break;
                                case LProtocol.JRQST_GET_ACCOUNTS:
                                    gid = intent.getIntExtra("gid", 0);
                                    uid = intent.getIntExtra("uid", 0);
                                    String name = intent.getStringExtra("name");
                                    account = DBAccount.getByGid(gid);
                                    if (null != account) {
                                        account.setName(name);
                                        DBAccount.update(account);
                                    } else {
                                        account = new LAccount();
                                        account.setGid(gid);
                                        account.setName(name);
                                        DBAccount.add(account);
                                    }
                                    break;
                                case LProtocol.JRQST_GET_CATEGORIES:
                                    gid = intent.getIntExtra("gid", 0);
                                    name = intent.getStringExtra("name");
                                    category = DBCategory.getByGid(gid);
                                    if (null != category) {
                                        category.setName(name);
                                        DBCategory.update(category);
                                    } else {
                                        category = new LCategory();
                                        category.setGid(gid);
                                        category.setName(name);
                                        DBCategory.add(category);
                                    }
                                    break;
                                case LProtocol.JRQST_GET_TAGS:
                                    gid = intent.getIntExtra("gid", 0);
                                    name = intent.getStringExtra("name");
                                    tag = DBTag.getByGid(gid);
                                    if (null != tag) {
                                        tag.setName(name);
                                        DBTag.update(tag);
                                    } else {
                                        tag = new LTag();
                                        tag.setGid(gid);
                                        tag.setName(name);
                                        DBTag.add(tag);
                                    }
                                    break;
                                case LProtocol.JRQST_GET_VENDORS:
                                    gid = intent.getIntExtra("gid", 0);
                                    int type = intent.getIntExtra("type", LVendor.TYPE_PAYEE);
                                    name = intent.getStringExtra("name");
                                    vendor = DBVendor.getByGid(gid);
                                    if (null != vendor) {
                                        vendor.setName(name);
                                        vendor.setType(type);
                                        DBVendor.update(vendor);
                                    } else {
                                        vendor = new LVendor();
                                        vendor.setGid(gid);
                                        vendor.setName(name);
                                        vendor.setType(type);
                                        DBVendor.add(vendor);
                                    }
                                    break;
                                case LProtocol.JRQST_GET_RECORDS:
                                    lgid = intent.getLongExtra("gid", 0L);
                                    int aid = intent.getIntExtra("aid", 0);
                                    int aid2 = intent.getIntExtra("aid2", 0);
                                    int cid = intent.getIntExtra("cid", 0);
                                    int tid = intent.getIntExtra("tid", 0);
                                    int vid = intent.getIntExtra("vid", 0);
                                    type = intent.getByteExtra("type", (byte) LTransaction.TRANSACTION_TYPE_EXPENSE);
                                    double amount = intent.getDoubleExtra("amount", 0);
                                    long timestamp = intent.getLongExtra("timestamp", 0L);
                                    int createUid = intent.getIntExtra("createBy", 0);
                                    int changeUid = intent.getIntExtra("changeBy", 0);
                                    long createTime = intent.getLongExtra("createTime", 0L);
                                    long changeTime = intent.getLongExtra("changeTime", 0L);
                                    String note = intent.getStringExtra("note");
                                    transaction = DBTransaction.getByGid(lgid);
                                    boolean create = true;
                                    if (null != transaction) {
                                        create = false;
                                    } else {
                                        transaction = new LTransaction();
                                    }
                                    transaction.setGid(lgid);
                                    transaction.setAccount(DBAccount.getIdByGid(aid));
                                    transaction.setAccount2(DBAccount.getIdByGid(aid2));
                                    transaction.setCategory(DBCategory.getIdByGid(cid));
                                    transaction.setTag(DBTag.getIdByGid(tid));
                                    transaction.setVendor(DBVendor.getIdByGid(vid));
                                    transaction.setType(type);
                                    transaction.setValue(amount);
                                    transaction.setCreateBy(createUid);
                                    transaction.setChangeBy(changeUid);
                                    transaction.setTimeStamp(timestamp);
                                    transaction.setTimeStampCreate(createTime);
                                    transaction.setTimeStampLast(changeTime);
                                    transaction.setNote(note);

                                    if (create) DBTransaction.add(transaction);
                                    else DBTransaction.update(transaction);

                                    break;

                                case LProtocol.JRQST_UPDATE_ACCOUNT:
                                case LProtocol.JRQST_DELETE_ACCOUNT:
                                    break;
                                default:
                                    LLog.w(TAG, "unknown journal request: " + jrqstId);
                                    break;
                            }
                        }
                        if (LProtocol.RSPS_OK == ret) {
                            LJournal.deleteById(journalId);
                            moreJournal = LJournal.flush();
                        }
                    } else {
                        // try a few more times, then bail, so not to lock out polling altogether
                        if (journalPostErrorCount++ < 3) {
                            LLog.w(TAG, "unexpected journal post error: " + ret);
                            //retry happens when one of the following happens
                            // - new journal request
                            // - polling timer expired
                            moreJournal = false;
                        } else {
                            journalPostErrorCount = 0;
                            LLog.e(TAG, "fatal journal post error, journal skipped");
                            LJournal.deleteById(journalId);
                            moreJournal = LJournal.flush();
                        }
                    }

                    //no more active journal, start polling
                    if (!moreJournal) {
                        serviceHandler.postDelayed(pollRunnable, NETWORK_IDLE_POLLING_MS);
                    }
                    break;

                case LBroadcastReceiver.ACTION_POLL:
                    if (LProtocol.RSPS_OK == ret) {
                        long id = intent.getLongExtra("id", 0);
                        short nid = intent.getShortExtra("nid", (short) 0);
                        switch (nid) {
                            case NOTIFICATION_ADD_ACCOUNT:
                                int gid = intent.getIntExtra("int1", 0);
                                int uid = intent.getIntExtra("int2", 0);
                                String name = intent.getStringExtra("txt1");
                                LAccount account = DBAccount.getByGid(gid);
                                if (null != account) {
                                    account.setName(name);
                                    DBAccount.update(account);
                                } else {
                                    account = new LAccount();
                                    account.setGid(gid);
                                    account.setName(name);
                                    DBAccount.add(account);
                                }
                                Intent uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_UI_UPDATE_ACCOUNT));
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                break;

                            case NOTIFICATION_UPDATE_ACCOUNT:
                                gid = intent.getIntExtra("int1", 0);
                                name = intent.getStringExtra("txt1");
                                account = DBAccount.getByGid(gid);
                                if (null != account) {
                                    account.setName(name);
                                    DBAccount.update(account);
                                }
                                uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_UI_UPDATE_ACCOUNT));
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                break;

                            case NOTIFICATION_DELETE_ACCOUNT:
                                gid = intent.getIntExtra("int1", 0);
                                account = DBAccount.getByGid(gid);
                                if (null != account) {
                                    LTask.start(new MyAccountDeleteTask(), account.getId());
                                    DBAccount.deleteById(account.getId());
                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_ACCOUNT));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_UPDATE_USER_PROFILE:
                                LPreferences.setUserName(intent.getStringExtra("txt1"));
                                uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_UI_UPDATE_USER_PROFILE));
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                break;

                            default:
                                LLog.w(TAG, "unexpected notification id: " + nid);

                        }
                        server.UiPollAck(id);
                    } else {
                        //no more
                        if (!LJournal.flush()) {
                            if (LFragmentActivity.upRunning) {
                                //server.UiUtcSync();
                                serviceHandler.postDelayed(pollRunnable, NETWORK_IDLE_POLLING_MS);
                            } else {
                                LLog.d(TAG, "no activity visible, shutdown now");
                                serviceHandler.postDelayed(serviceShutdownRunnable,
                                        SERVICE_SHUTDOWN_MS);
                            }
                        }
                    }
                    break;

                case LBroadcastReceiver.ACTION_POLL_ACK:
                    if (!LJournal.flush()) {
                        server.UiPoll();
                    }
                    break;
/*
                case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH:
                    int cacheId = intent.getIntExtra("cacheId", 0);
                    server.UiPollAck(cacheId);
                    break;
            case LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_SHARE_USER_PROFILE:
                cacheId = intent.getIntExtra("cacheId", 0);
                userName = intent.getStringExtra("userName");
                userId = intent.getIntExtra("userId", 0);
                String userFullName = intent.getStringExtra("userFullName");
                LPreferences.setShareUserName(userId, userName);
                LPreferences.setShareUserFullName(userId, userFullName);
                server.UiPollAck(cacheId);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_ACCOUNT_SHARE:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                int numShareUsers = intent.getShortExtra("numShareUsers", (short)0);
                int[] shareUSers = intent.getIntArrayExtra("shareUsers");

                LLog.d(TAG, "requested to update account share for: " + accountGid);
                LAccount account = DBAccount.getByGid(accountGid);
                if (account == null) {
                    LLog.w(TAG, "requested account no longer exist? account gid: " + accountGid);
                } else {
                    LLog.d(TAG, "requested to update account share for: " + account.getName());
                    HashSet<Integer> newShareUsers = new HashSet<Integer>();
                    ArrayList<Integer> origIds = new ArrayList<Integer>(account.getShareIds());
                    ArrayList<Integer> origStates = new ArrayList<Integer>(account.getShareStates
                    ());

                    account.removeAllShareUsers();
                    int  myUserId = 0;//LPreferences.getUserId();
                    boolean isShared = false;
                    for (int user: shareUSers) if (user == myUserId) {isShared = true; break;}
                    LLog.d(TAG, "account: " + account.getName() + " shared? " + isShared);
                    if (isShared) {
                        for (int user : shareUSers) {
                            if (user == myUserId) continue;

                            boolean newShare = true;
                            for (int ii = 0; ii < origIds.size(); ii++) {
                                if (user == origIds.get(ii) && origStates.get(ii) == LAccount
                                .ACCOUNT_SHARE_CONFIRMED_SYNCED) {
                                    newShare = false;
                                    break;
                                }
                            }
                            if (newShare) {
                                LLog.d(TAG, "new share from user: " + user);
                                newShareUsers.add(user);
                            }
                            account.addShareUser(user, LAccount.ACCOUNT_SHARE_CONFIRMED_SYNCED);
                        }
                    }
                    DBAccount.update(account);

                    //sync records to newly added share user
                    for (int user: newShareUsers) {
                        LJournal.pushAllAccountRecords(user, account);
                    }
                }
                server.UiPollAck(cacheId);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_ACCOUNT_INFO:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                accountName = intent.getStringExtra("accountName");
                String record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                updateAccountFromReceivedRecord(accountGid, accountName, record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORD:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateItemFromReceivedRecord(accountGid, record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_SCHEDULE:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                int accountGid2 = intent.getIntExtra("accountGid2", 0);
                String schedule = intent.getStringExtra("schedule");
                server.UiPollAck(cacheId);
                LJournal.updateScheduleFromReceivedRecord(accountGid, accountGid2, schedule);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORDS:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                record = intent.getStringExtra("record");
                int done = intent.getIntExtra("done", LProtocol.RECORDS_RECEIVING);
                switch (done) {
                    case LProtocol.RECORDS_RECEIVED_FULL:
                        LLog.d(TAG, "records receiving from " + accountGid + " DONE");
                        server.UiPollAck(cacheId);
                        break;
                    case LProtocol.RECORDS_RECEIVED_PART:
                        //records not received properly, poll again
                        server.UiPoll();
                        break;
                    default:
                        break;
                }
                LJournal.updateItemFromReceivedRecord(accountGid, record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_CATEGORY:
                cacheId = intent.getIntExtra("cacheId", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateCategoryFromReceivedRecord(record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_PAYER:
                cacheId = intent.getIntExtra("cacheId", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateVendorFromReceivedRecord(record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_TAG:
                cacheId = intent.getIntExtra("cacheId", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateTagFromReceivedRecord(record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_PAYER_CATEGORY:
                cacheId = intent.getIntExtra("cacheId", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateVendorCategoryFromReceivedRecord(record);
                break;

*/
                case LBroadcastReceiver.ACTION_UNKNOWN_MSG:
                    LLog.w(TAG, "unknown message received");
                    break;

                //case LBroadcastReceiver.ACTION_SERVER_BROADCAST_MSG_RECEIVED:
                //    cacheId = intent.getIntExtra("cacheId", 0);
                //    server.UiPollAck(cacheId);
                //    break;
            }
        }
    }

    private class AsyncScanBalances extends AsyncTask<Cursor, Void, Boolean> {

        private void addUpdateAccountBalance(double[] doubles, long accountId, int year) {
            boolean newEntry = false;
            LAccountBalance balance = DBAccountBalance.getByAccountId(accountId, year);
            if (balance == null) {
                balance = new LAccountBalance();
                newEntry = true;
            }
            balance.setState(DBHelper.STATE_ACTIVE);
            balance.setAccountId(accountId);
            balance.setYear(year);
            balance.setBalanceValues(doubles);
            if (newEntry) {
                DBAccountBalance.add(balance);
            } else {
                DBAccountBalance.update(balance);
            }
        }

        @Override
        protected Boolean doInBackground(Cursor... params) {
            Cursor data = params[0];
            if (data == null || data.getCount() == 0) {
                LLog.d(TAG, "no account left, deleting all balances");
                DBAccountBalance.deleteAll(); //clean up balances if all accounts are removed.
                return false;
            }

            HashSet<Long> accounts = DBAccount.getAllActiveIds();

            try {
                if (isCancelled()) return false;
                else data.moveToFirst();
                Calendar calendar = Calendar.getInstance();
                double[] doubles = new double[12];
                long lastAccountId = 0;
                int lastYear = 0;
                do {
                    long accountId = data.getLong(data.getColumnIndexOrThrow(DBHelper
                            .TABLE_COLUMN_ACCOUNT));
                    accounts.remove(accountId);

                    double amount = data.getDouble(data.getColumnIndexOrThrow(DBHelper
                            .TABLE_COLUMN_AMOUNT));
                    int type = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                    //String name = data.getString(data.getColumnIndexOrThrow(DBHelper
                    // .TABLE_COLUMN_NAME));
                    calendar.setTimeInMillis(data.getLong(data.getColumnIndexOrThrow(DBHelper
                            .TABLE_COLUMN_TIMESTAMP)));
                    int year = calendar.get(Calendar.YEAR);
                    int mon = calendar.get(Calendar.MONTH);
                    if (lastAccountId == 0) {
                        lastAccountId = accountId;
                        lastYear = year;
                    } else if (lastAccountId != accountId || lastYear != year) {
                        addUpdateAccountBalance(doubles, lastAccountId, lastYear);

                        lastAccountId = accountId;
                        lastYear = year;
                        for (int ii = 0; ii < 12; ii++) doubles[ii] = 0.0;
                    }
                    doubles[mon] += (type == LTransaction.TRANSACTION_TYPE_INCOME ||
                            type == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) ? amount : -amount;
                } while (!isCancelled() && data.moveToNext());
                if (!isCancelled()) addUpdateAccountBalance(doubles, lastAccountId, lastYear);

                for (long account : accounts) {
                    DBAccountBalance.deleteByAccountId(account);
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onCancelled(Boolean result) {
        }

        @Override
        protected void onPostExecute(Boolean result) {
            accountBalanceSynced = true;
            LLog.d(TAG, "account balance synchronized");
            serviceHandler.removeCallbacks(serviceShutdownRunnable);
            serviceHandler.postDelayed(serviceShutdownRunnable, SERVICE_SHUTDOWN_MS);
        }

        @Override
        protected void onPreExecute() {
        }
    }

    private class MyAccountDeleteTask extends AsyncTask<Long, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Long... params) {
            Long accountId = params[0];

            DBTransaction.deleteByAccount(accountId);
            DBScheduledTransaction.deleteByAccount(accountId);

            DBAccountBalance.deleteByAccountId(accountId);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
        }

        @Override
        protected void onPreExecute() {
        }
    }
}
