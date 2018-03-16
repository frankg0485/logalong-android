package com.swoag.logalong;
    /* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

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
import com.swoag.logalong.entities.LAccountShareRequest;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LScheduledTransaction;
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
import com.swoag.logalong.utils.LBuffer;
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
    private static final int CMD_SCAN_BALANCE = 50;

    private static final short NOTIFICATION_UPDATE_USER_PROFILE = 0x001;
    private static final short NOTIFICATION_ADD_SHARE_USER = 0x002;
    private static final short NOTIFICATION_ADD_ACCOUNT = 0x010;
    private static final short NOTIFICATION_UPDATE_ACCOUNT = 0x011;
    private static final short NOTIFICATION_DELETE_ACCOUNT = 0x012;
    private static final short NOTIFICATION_UPDATE_ACCOUNT_GID = 0x013;
    private static final short NOTIFICATION_ADD_CATEGORY = 0x020;
    private static final short NOTIFICATION_UPDATE_CATEGORY = 0x021;
    private static final short NOTIFICATION_DELETE_CATEGORY = 0x022;
    private static final short NOTIFICATION_ADD_TAG = 0x030;
    private static final short NOTIFICATION_UPDATE_TAG = 0x031;
    private static final short NOTIFICATION_DELETE_TAG = 0x032;
    private static final short NOTIFICATION_ADD_VENDOR = 0x040;
    private static final short NOTIFICATION_UPDATE_VENDOR = 0x041;
    private static final short NOTIFICATION_DELETE_VENDOR = 0x042;
    private static final short NOTIFICATION_GET_RECORD = 0x050;
    private static final short NOTIFICATION_UPDATE_RECORD = 0x051;
    private static final short NOTIFICATION_DELETE_RECORD = 0x052;
    private static final short NOTIFICATION_GET_RECORDS = 0x053;
    private static final short NOTIFICATION_ADD_SCHEDULE = 0x060;
    private static final short NOTIFICATION_UPDATE_SCHEDULE = 0x061;
    private static final short NOTIFICATION_DELETE_SCHEDULE = 0x062;
    private static final short NOTIFICATION_REQUEST_ACCOUNT_SHARE = 0x101;
    private static final short NOTIFICATION_DECLINE_ACCOUNT_SHARE = 0x102;
    private static final short NOTIFICATION_UPDATE_ACCOUNT_USER = 0x103;
    private static final short NOTIFICATION_GET_ACCOUNT_RECORDS = 0x201;
    private static final short NOTIFICATION_GET_ACCOUNTS = 0x202;
    private static final short NOTIFICATION_GET_CATEGORIES = 0x203;
    private static final short NOTIFICATION_GET_VENDORS = 0x204;
    private static final short NOTIFICATION_GET_TAGS = 0x205;
    private static final short NOTIFICATION_GET_ACCOUNT_SCHEDULES = 0x211;

    private boolean loggedIn = false;
    private Handler serviceHandler;
    private Runnable pollRunnable;
    private Runnable serviceShutdownRunnable;
    private boolean accountBalanceSynced = false;

    static final int NETWORK_IDLE_POLLING_MS = 1000;
    static final int MAX_POLLING_COUNT_UPON_PUSH_NOTIFICATION = 5;
    static int pollingCount = MAX_POLLING_COUNT_UPON_PUSH_NOTIFICATION;

    static final int SERVICE_SHUTDOWN_MS = 15000;
    private BroadcastReceiver broadcastReceiver;
    private LAppServer server;


    static final int UPDATE_ACCOUNT_BALANCE_DELAY_MS = 3000;
    private Runnable updateAccountBalanceRunnable;
    private static final int LOADER_ID_UPDATE_BALANCE = 10;
    private CursorLoader cursorLoader;
    private Cursor lastLoadedData;
    AsyncScanBalances asyncScanBalances;
    private LJournal journal;

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

        //DBScheduledTransaction.scanAlarm();
        journal = new LJournal();

        serviceHandler = new Handler() {
        };

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (loggedIn) {
                    LLog.d(TAG, "heart beat journal flushing");
                    //polling happens only where there's no pending journal
                    if (!journal.flush()) {
                        LLog.d(TAG, "heart beat polling without pending journal");
                        gatedPoll();//server.UiPoll();
                    }
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
                LBroadcastReceiver.ACTION_NETWORK_DISCONNECTED,
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
                LBroadcastReceiver.ACTION_POST_JOURNAL,
                LBroadcastReceiver.ACTION_POLL,
                LBroadcastReceiver.ACTION_POLL_ACK,
                LBroadcastReceiver.ACTION_NEW_JOURNAL_AVAILABLE,
                LBroadcastReceiver.ACTION_PUSH_NOTIFICATION}, this);


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
        journal = null;
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
                        //LLog.d(TAG, "connecting to server");
                        server.connect();
                        //poll once shortly after connect: in case server is already connected
                        serviceHandler.postDelayed(pollRunnable, 1000);
                    }
                    break;

                case CMD_STOP:
                    LLog.d(TAG, "shutdown in responding to stop command");
                    serviceHandler.removeCallbacks(serviceShutdownRunnable);
                    serviceHandler.postDelayed(serviceShutdownRunnable, SERVICE_SHUTDOWN_MS);
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

        //LLog.d(TAG, "action: " + action + ":" + LBroadcastReceiver.getActionName(action) + " logged in: " + loggedIn);
        if (action == LBroadcastReceiver.ACTION_NETWORK_DISCONNECTED) {
            loggedIn = false;
            return;
        }

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
                    if (LProtocol.RSPS_OK == ret) {
                        loggedIn = true;
                        //journal posting and polling start only upon successful login
                        serviceHandler.postDelayed(pollRunnable, 1000); //poll shortly after login
                    }
                    break;
            }
        } else {
            if (!(action == LBroadcastReceiver.ACTION_POLL && LProtocol.RSPS_OK != ret)) {
                Intent uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_UI_NET_BUSY));
                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
            }

            switch (action) {
                case LBroadcastReceiver.ACTION_NEW_JOURNAL_AVAILABLE:
                    //LLog.d(TAG, "flushing journal upon creation ...");
                    if (!journal.flush()) serviceHandler.postDelayed(pollRunnable, NETWORK_IDLE_POLLING_MS);
                    break;

                case LBroadcastReceiver.ACTION_POST_JOURNAL:
                    boolean moreJournal = true;
                    int journalId = intent.getIntExtra("journalId", 0);
                    //LLog.d(TAG, "post journal: " + journalId + " status: " + ret);

                    if (LProtocol.RSPS_OK == ret || LProtocol.RSPS_MORE == ret) {
                        short jrqstId = intent.getShortExtra("jrqstId", (short) 0);
                        short jret = intent.getShortExtra("jret", (short) 0);
                        //LLog.d(TAG, "post journal rsps ok, rqstId: " + jrqstId + " status: " + jret);
                        if (LProtocol.RSPS_OK != jret) {
                            LLog.w(TAG, "journal request " + jrqstId + " failed.");
                        } else {
                            switch (jrqstId) {
                                case LProtocol.JRQST_ADD_ACCOUNT:
                                    long id = intent.getLongExtra("id", 0L);
                                    long gid = intent.getLongExtra("gid", 0L);
                                    long uid = intent.getLongExtra("uid", 0L);

                                    DBAccount dbAccount = DBAccount.getInstance();
                                    LAccount account = dbAccount.getByGid(gid);
                                    if (null != account) {
                                        if (account.getId() != id) {
                                            LLog.e(TAG, "unexpected error, account GID: " + gid + " already taken " +
                                                    "by " + account.getName());
                                            //this is an unrecoverable error, we'll delete the dangling account
                                            dbAccount.deleteById(account.getId());
                                        }
                                    }

                                    account = dbAccount.getById(id);
                                    if (null != account) {
                                        account.setOwner(uid);
                                        account.setGid(gid);
                                        dbAccount.update(account);
                                    }
                                    break;

                                case LProtocol.JRQST_ADD_CATEGORY:
                                    id = intent.getLongExtra("id", 0L);
                                    gid = intent.getLongExtra("gid", 0L);

                                    DBCategory dbCategory = DBCategory.getInstance();
                                    LCategory category = dbCategory.getByGid(gid);
                                    if (null != category) {
                                        if (category.getId() != id) {
                                            LLog.e(TAG, "unexpected error, category GID: " + gid + " already taken " +
                                                    "by " + category.getName());
                                            dbCategory.deleteById(category.getId());
                                        }
                                    }
                                    dbCategory.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, gid);
                                    break;
                                case LProtocol.JRQST_ADD_TAG:
                                    id = intent.getLongExtra("id", 0L);
                                    gid = intent.getLongExtra("gid", 0L);

                                    DBTag dbTag = DBTag.getInstance();
                                    LTag tag = dbTag.getByGid(gid);
                                    if (null != tag) {
                                        if (tag.getId() != id) {
                                            LLog.e(TAG, "unexpected error, tag GID: " + gid + " already taken " +
                                                    "by " + tag.getName());
                                            dbTag.deleteById(tag.getId());
                                        }
                                    }
                                    dbTag.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, gid);
                                    break;
                                case LProtocol.JRQST_ADD_VENDOR:
                                    id = intent.getLongExtra("id", 0L);
                                    gid = intent.getLongExtra("gid", 0L);

                                    DBVendor dbVendor = DBVendor.getInstance();
                                    LVendor vendor = dbVendor.getByGid(gid);
                                    if (null != vendor) {
                                        if (vendor.getId() != id) {
                                            LLog.e(TAG, "unexpected error, vendor GID: " + gid + " already taken " +
                                                    "by " + vendor.getName());
                                            dbVendor.deleteById(vendor.getId());
                                        }
                                    }
                                    dbVendor.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, gid);
                                    break;
                                case LProtocol.JRQST_ADD_RECORD:
                                    DBTransaction dbTransaction = DBTransaction.getInstance();
                                    id = intent.getLongExtra("id", 0L);
                                    gid = intent.getLongExtra("gid", 0L);

                                    LTransaction transaction = dbTransaction.getByGid(gid);
                                    if (null != transaction) {
                                        if (transaction.getId() == id) {
                                            LLog.e(TAG, "unexpected error, record GID: " + gid + " already taken ");
                                        }
                                        dbTransaction.deleteById(transaction.getId());
                                    }
                                    dbTransaction.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, gid);
                                    break;
                                case LProtocol.JRQST_ADD_SCHEDULE:
                                    DBScheduledTransaction dbSchTransaction = DBScheduledTransaction.getInstance();
                                    id = intent.getLongExtra("id", 0L);
                                    gid = intent.getLongExtra("gid", 0L);

                                    LScheduledTransaction scheduledTransaction = dbSchTransaction.getByGid(gid);
                                    if (null != scheduledTransaction) {
                                        if (scheduledTransaction.getId() == id) {
                                            LLog.e(TAG, "unexpected error, schedule GID: " + gid + " already taken ");
                                        }
                                        dbSchTransaction.deleteById(scheduledTransaction.getId());
                                    }
                                    dbSchTransaction.updateColumnById(id, DBHelper.TABLE_COLUMN_GID, gid);
                                    break;
                                case LProtocol.JRQST_GET_ACCOUNTS:
                                    gid = intent.getLongExtra("gid", 0L);
                                    uid = intent.getLongExtra("uid", 0L);
                                    String name = intent.getStringExtra("name");

                                    dbAccount = DBAccount.getInstance();
                                    account = dbAccount.getByGid(gid);
                                    if (null != account) {
                                        account.setOwner(uid);
                                        account.setName(name);
                                        dbAccount.update(account);
                                    } else {
                                        account = new LAccount();
                                        account.setOwner(uid);
                                        account.setGid(gid);
                                        account.setName(name);
                                        dbAccount.add(account);
                                    }
                                    journal.getAccountUsers(gid);
                                    break;
                                case LProtocol.JRQST_GET_ACCOUNT_USERS:
                                    gid = intent.getLongExtra("aid", 0L);
                                    dbAccount = DBAccount.getInstance();
                                    account = dbAccount.getByGid(gid);
                                    if (null != account) {
                                        account.setSharedIdsString(intent.getStringExtra("users"));
                                        dbAccount.update(account);
                                    } else {
                                        LLog.w(TAG, "account: " + gid + " no longer exists");
                                    }
                                    break;

                                case LProtocol.JRQST_GET_CATEGORIES:
                                    gid = intent.getLongExtra("gid", 0L);
                                    name = intent.getStringExtra("name");
                                    dbCategory = DBCategory.getInstance();
                                    category = dbCategory.getByGid(gid);
                                    if (null != category) {
                                        category.setName(name);
                                        dbCategory.update(category);
                                    } else {
                                        category = new LCategory();
                                        category.setGid(gid);
                                        category.setName(name);
                                        dbCategory.add(category);
                                    }
                                    break;
                                case LProtocol.JRQST_GET_TAGS:
                                    gid = intent.getLongExtra("gid", 0L);
                                    name = intent.getStringExtra("name");
                                    dbTag = DBTag.getInstance();
                                    tag = dbTag.getByGid(gid);
                                    if (null != tag) {
                                        tag.setName(name);
                                        dbTag.update(tag);
                                    } else {
                                        tag = new LTag();
                                        tag.setGid(gid);
                                        tag.setName(name);
                                        dbTag.add(tag);
                                    }
                                    break;
                                case LProtocol.JRQST_GET_VENDORS:
                                    gid = intent.getLongExtra("gid", 0L);
                                    int type = intent.getIntExtra("type", LVendor.TYPE_PAYEE);
                                    name = intent.getStringExtra("name");
                                    dbVendor = DBVendor.getInstance();
                                    vendor = dbVendor.getByGid(gid);
                                    if (null != vendor) {
                                        vendor.setName(name);
                                        vendor.setType(type);
                                        dbVendor.update(vendor);
                                    } else {
                                        vendor = new LVendor();
                                        vendor.setGid(gid);
                                        vendor.setName(name);
                                        vendor.setType(type);
                                        dbVendor.add(vendor);
                                    }
                                    break;
                                case LProtocol.JRQST_GET_RECORD:
                                case LProtocol.JRQST_GET_RECORDS:
                                case LProtocol.JRQST_GET_ACCOUNT_RECORDS:
                                    gid = intent.getLongExtra("gid", 0L);
                                    //LLog.d(TAG, "get record: " + gid);
                                    long aid = intent.getLongExtra("aid", 0);
                                    long aid2 = intent.getLongExtra("aid2", 0);
                                    long cid = intent.getLongExtra("cid", 0);
                                    long tid = intent.getLongExtra("tid", 0);
                                    long vid = intent.getLongExtra("vid", 0);
                                    type = intent.getByteExtra("type", (byte) LTransaction.TRANSACTION_TYPE_EXPENSE);
                                    double amount = intent.getDoubleExtra("amount", 0);
                                    long rid = intent.getLongExtra("recordId", 0L);
                                    long timestamp = intent.getLongExtra("timestamp", 0L);
                                    long createUid = intent.getLongExtra("createBy", 0);
                                    long changeUid = intent.getLongExtra("changeBy", 0);
                                    long createTime = intent.getLongExtra("createTime", 0L);
                                    long changeTime = intent.getLongExtra("changeTime", 0L);
                                    String note = intent.getStringExtra("note");
                                    dbTransaction = DBTransaction.getInstance();
                                    transaction = dbTransaction.getByGid(gid);
                                    boolean create = true;
                                    if (null != transaction) {
                                        create = false;
                                    } else {
                                        if (type == LTransaction.TRANSACTION_TYPE_TRANSFER)
                                            transaction = dbTransaction.getByRid(rid, false);
                                        else if (type == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY)
                                            transaction = dbTransaction.getByRid(rid, true);
                                        if (null != transaction) {
                                            create = false;
                                        } else
                                            transaction = new LTransaction();
                                    }
                                    dbAccount = DBAccount.getInstance();
                                    transaction.setGid(gid);
                                    transaction.setAccount(dbAccount.getIdByGid(aid));
                                    transaction.setAccount2(dbAccount.getIdByGid(aid2));
                                    transaction.setCategory(DBCategory.getInstance().getIdByGid(cid));
                                    transaction.setTag(DBTag.getInstance().getIdByGid(tid));
                                    transaction.setVendor(DBVendor.getInstance().getIdByGid(vid));
                                    transaction.setType(type);
                                    transaction.setValue(amount);
                                    transaction.setCreateBy(createUid);
                                    transaction.setChangeBy(changeUid);
                                    transaction.setRid(rid);
                                    transaction.setTimeStamp(timestamp);
                                    transaction.setTimeStampCreate(createTime);
                                    transaction.setTimeStampLast(changeTime);
                                    transaction.setNote(note);

                                    if (create) dbTransaction.add(transaction);
                                    else dbTransaction.update(transaction);

                                    break;

                                case LProtocol.JRQST_GET_SCHEDULE:
                                case LProtocol.JRQST_GET_SCHEDULES:
                                case LProtocol.JRQST_GET_ACCOUNT_SCHEDULES:
                                    gid = intent.getLongExtra("gid", 0L);
                                    aid = intent.getLongExtra("aid", 0);
                                    aid2 = intent.getLongExtra("aid2", 0);
                                    cid = intent.getLongExtra("cid", 0);
                                    tid = intent.getLongExtra("tid", 0);
                                    vid = intent.getLongExtra("vid", 0);
                                    type = intent.getByteExtra("type", (byte) LTransaction.TRANSACTION_TYPE_EXPENSE);
                                    amount = intent.getDoubleExtra("amount", 0);
                                    rid = intent.getLongExtra("recordId", 0L);
                                    timestamp = intent.getLongExtra("timestamp", 0L);
                                    createUid = intent.getLongExtra("createBy", 0);
                                    changeUid = intent.getLongExtra("changeBy", 0);
                                    createTime = intent.getLongExtra("createTime", 0L);
                                    changeTime = intent.getLongExtra("changeTime", 0L);
                                    note = intent.getStringExtra("note");

                                    long nextTime = intent.getLongExtra("nextTime", 0L);
                                    byte interval = intent.getByteExtra("interval", (byte) 0);
                                    byte unit = intent.getByteExtra("unit", (byte) 0);
                                    byte count = intent.getByteExtra("count", (byte) 0);
                                    boolean enabled = intent.getByteExtra("count", (byte) 0) == 0 ? false : true;

                                    dbSchTransaction = DBScheduledTransaction.getInstance();
                                    scheduledTransaction = dbSchTransaction.getByGid(gid);

                                    create = true;
                                    if (null != scheduledTransaction) {
                                        create = false;
                                    } else {
                                        scheduledTransaction = new LScheduledTransaction();
                                    }
                                    dbAccount = DBAccount.getInstance();
                                    scheduledTransaction.setGid(gid);
                                    scheduledTransaction.setAccount(dbAccount.getIdByGid(aid));
                                    scheduledTransaction.setAccount2(dbAccount.getIdByGid(aid2));
                                    scheduledTransaction.setCategory(DBCategory.getInstance().getIdByGid(cid));
                                    scheduledTransaction.setTag(DBTag.getInstance().getIdByGid(tid));
                                    scheduledTransaction.setVendor(DBVendor.getInstance().getIdByGid(vid));
                                    scheduledTransaction.setType(type);
                                    scheduledTransaction.setValue(amount);
                                    scheduledTransaction.setCreateBy(createUid);
                                    scheduledTransaction.setChangeBy(changeUid);
                                    scheduledTransaction.setRid(rid);
                                    scheduledTransaction.setTimeStamp(timestamp);
                                    scheduledTransaction.setTimeStampCreate(createTime);
                                    scheduledTransaction.setTimeStampLast(changeTime);
                                    scheduledTransaction.setNote(note);

                                    scheduledTransaction.setNextTime(nextTime);
                                    scheduledTransaction.setRepeatInterval(interval);
                                    scheduledTransaction.setRepeatUnit(unit);
                                    scheduledTransaction.setRepeatCount(count);
                                    scheduledTransaction.setEnabled(enabled);

                                    if (create) dbSchTransaction.add(scheduledTransaction);
                                    else dbSchTransaction.update(scheduledTransaction);

                                    break;

                                case LProtocol.JRQST_UPDATE_ACCOUNT:
                                case LProtocol.JRQST_DELETE_ACCOUNT:
                                case LProtocol.JRQST_UPDATE_CATEGORY:
                                case LProtocol.JRQST_DELETE_CATEGORY:
                                case LProtocol.JRQST_UPDATE_TAG:
                                case LProtocol.JRQST_DELETE_TAG:
                                case LProtocol.JRQST_UPDATE_VENDOR:
                                case LProtocol.JRQST_DELETE_VENDOR:
                                case LProtocol.JRQST_UPDATE_RECORD:
                                case LProtocol.JRQST_DELETE_RECORD:
                                case LProtocol.JRQST_UPDATE_SCHEDULE:
                                case LProtocol.JRQST_DELETE_SCHEDULE:
                                case LProtocol.JRQST_CONFIRM_ACCOUNT_SHARE:
                                case LProtocol.JRQST_ADD_USER_TO_ACCOUNT:
                                    break;
                                default:
                                    LLog.w(TAG, "unknown journal request: " + jrqstId);
                                    break;
                            }
                        }
                        if (LProtocol.RSPS_OK == ret) {
                            journal.deleteById(journalId);
                            //LLog.d(TAG, "flushing journal upon completion ...");
                            moreJournal = journal.flush();
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
                            journal.deleteById(journalId);
                            moreJournal = journal.flush();
                        }
                    }

                    //no more active journal, start polling
                    if (!moreJournal) {
                        serviceHandler.postDelayed(pollRunnable, NETWORK_IDLE_POLLING_MS);
                    }
                    break;

                case LBroadcastReceiver.ACTION_POLL:
                    pollRequested = false;

                    if (LProtocol.RSPS_OK == ret) {
                        long id = intent.getLongExtra("id", 0);
                        short nid = intent.getShortExtra("nid", (short) 0);
                        switch (nid) {
                            case NOTIFICATION_ADD_ACCOUNT:
                                long gid = intent.getLongExtra("int1", 0L);
                                long uid = intent.getLongExtra("int2", 0L);
                                String name = intent.getStringExtra("txt1");

                                DBAccount dbAccount = DBAccount.getInstance();
                                LAccount account = dbAccount.getByGid(gid);
                                if (null != account) {
                                    account.setOwner(uid);
                                    account.setName(name);
                                    dbAccount.update(account);
                                } else {
                                    account = dbAccount.getByName(name);
                                    if (null != account) {
                                        account.setOwner(uid);
                                        account.setGid(gid);
                                        dbAccount.update(account);
                                    } else {
                                        account = new LAccount();
                                        account.setOwner(uid);
                                        account.setGid(gid);
                                        account.setName(name);
                                        dbAccount.add(account);
                                    }
                                }
                                Intent uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_UI_UPDATE_ACCOUNT));
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                break;

                            case NOTIFICATION_UPDATE_ACCOUNT:
                                gid = intent.getLongExtra("int1", 0L);
                                name = intent.getStringExtra("txt1");

                                dbAccount = DBAccount.getInstance();
                                account = dbAccount.getByGid(gid);
                                if (null != account) {
                                    account.setName(name);
                                    dbAccount.update(account);
                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_ACCOUNT));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_DELETE_ACCOUNT:
                                gid = intent.getLongExtra("int1", 0L);
                                dbAccount = DBAccount.getInstance();
                                account = dbAccount.getByGid(gid);
                                if (null != account) {
                                    LTask.start(new DBAccount.MyAccountDeleteTask(), account.getId());
                                    dbAccount.deleteById(account.getId());
                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_ACCOUNT));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_UPDATE_ACCOUNT_GID:
                                gid = intent.getLongExtra("int1", 0L);
                                long gid2 = intent.getLongExtra("int2", 0L);

                                dbAccount = DBAccount.getInstance();
                                account = dbAccount.getByGid(gid);
                                if (null != account) {
                                    account.setGid(gid2);
                                    dbAccount.update(account);
                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_ACCOUNT));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_ADD_CATEGORY:
                                gid = intent.getLongExtra("int1", 0L);
                                long pid = intent.getLongExtra("int2", 0L);
                                name = intent.getStringExtra("txt1");
                                DBCategory dbCategory = DBCategory.getInstance();
                                LCategory category = dbCategory.getByGid(gid);
                                if (null != category) {
                                    category.setName(name);
                                    //category.setPid(pid);
                                    dbCategory.update(category);
                                } else {
                                    category = dbCategory.getByName(name);
                                    if (null != category) {
                                        category.setGid(gid);
                                        //category.setPid(pid);
                                        dbCategory.update(category);
                                    } else {
                                        category = new LCategory();
                                        category.setGid(gid);
                                        category.setName(name);
                                        dbCategory.add(category);
                                    }
                                }
                                uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_UI_UPDATE_CATEGORY));
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                break;

                            case NOTIFICATION_UPDATE_CATEGORY:
                                gid = intent.getLongExtra("int1", 0L);
                                pid = intent.getLongExtra("int2", 0L);
                                name = intent.getStringExtra("txt1");
                                dbCategory = DBCategory.getInstance();
                                category = dbCategory.getByGid(gid);
                                if (null != category) {
                                    category.setName(name);
                                    //category.setPid(pid);
                                    dbCategory.update(category);

                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_CATEGORY));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_DELETE_CATEGORY:
                                gid = intent.getLongExtra("int1", 0L);
                                dbCategory = DBCategory.getInstance();
                                category = dbCategory.getByGid(gid);
                                if (null != category) {
                                    dbCategory.deleteById(category.getId());
                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_CATEGORY));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_ADD_TAG:
                                gid = intent.getLongExtra("int1", 0L);
                                name = intent.getStringExtra("txt1");
                                DBTag dbTag = DBTag.getInstance();
                                LTag tag = dbTag.getByGid(gid);
                                if (null != tag) {
                                    tag.setName(name);
                                    dbTag.update(tag);
                                } else {
                                    tag = dbTag.getByName(name);
                                    if (null != tag) {
                                        tag.setGid(gid);
                                        dbTag.update(tag);
                                    } else {
                                        tag = new LTag();
                                        tag.setGid(gid);
                                        tag.setName(name);
                                        dbTag.add(tag);
                                    }
                                }
                                uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_UI_UPDATE_TAG));
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                break;

                            case NOTIFICATION_UPDATE_TAG:
                                gid = intent.getLongExtra("int1", 0L);
                                name = intent.getStringExtra("txt1");
                                dbTag = DBTag.getInstance();
                                tag = dbTag.getByGid(gid);
                                if (null != tag) {
                                    tag.setName(name);
                                    dbTag.update(tag);

                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_TAG));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_DELETE_TAG:
                                gid = intent.getLongExtra("int1", 0L);
                                dbTag = DBTag.getInstance();
                                tag = dbTag.getByGid(gid);
                                if (null != tag) {
                                    dbTag.deleteById(tag.getId());
                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_TAG));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;
                            case NOTIFICATION_ADD_VENDOR:
                                gid = intent.getLongExtra("int1", 0L);
                                long type = intent.getLongExtra("int2", 0L);
                                name = intent.getStringExtra("txt1");
                                DBVendor dbVendor = DBVendor.getInstance();
                                LVendor vendor = dbVendor.getByGid(gid);
                                if (null != vendor) {
                                    vendor.setName(name);
                                    vendor.setType((int) type);
                                    dbVendor.update(vendor);
                                } else {
                                    vendor = dbVendor.getByName(name);
                                    if (null != vendor) {
                                        vendor.setGid(gid);
                                        dbVendor.update(vendor);
                                    } else {
                                        vendor = new LVendor();
                                        vendor.setGid(gid);
                                        vendor.setName(name);
                                        vendor.setType((int) type);
                                        dbVendor.add(vendor);
                                    }
                                }
                                uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_UI_UPDATE_VENDOR));
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                break;

                            case NOTIFICATION_UPDATE_VENDOR:
                                gid = intent.getLongExtra("int1", 0L);
                                type = intent.getLongExtra("int2", 0L);
                                name = intent.getStringExtra("txt1");
                                dbVendor = DBVendor.getInstance();
                                vendor = dbVendor.getByGid(gid);
                                if (null != vendor) {
                                    vendor.setName(name);
                                    vendor.setType((int) type);
                                    dbVendor.update(vendor);
                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_VENDOR));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_DELETE_VENDOR:
                                gid = intent.getLongExtra("int1", 0L);
                                dbVendor = DBVendor.getInstance();
                                vendor = dbVendor.getByGid(gid);
                                if (null != vendor) {
                                    dbVendor.deleteById(vendor.getId());
                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_VENDOR));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_GET_RECORD:
                            case NOTIFICATION_UPDATE_RECORD:
                                gid = intent.getLongExtra("int1", 0L);
                                journal.getRecord(gid);
                                break;

                            case NOTIFICATION_DELETE_RECORD:
                                gid = intent.getLongExtra("int1", 0L);
                                DBTransaction dbTransaction = DBTransaction.getInstance();
                                LTransaction transaction = dbTransaction.getByGid(gid);
                                if (null != transaction) {
                                    dbTransaction.deleteById(transaction.getId());
                                }
                                break;

                            case NOTIFICATION_GET_RECORDS:
                                byte[] blob = intent.getByteArrayExtra("blob");
                                LBuffer data = new LBuffer(blob);
                                long[] ids = new long[blob.length / Long.BYTES];
                                for (int ii = 0; ii < ids.length; ii++) {
                                    ids[ii] = data.getLongAutoInc();
                                }
                                journal.getRecords(ids);
                                break;

                            case NOTIFICATION_ADD_SCHEDULE:
                            case NOTIFICATION_UPDATE_SCHEDULE:
                                gid = intent.getLongExtra("int1", 0L);
                                journal.getSchedule(gid);
                                break;

                            case NOTIFICATION_DELETE_SCHEDULE:
                                gid = intent.getLongExtra("int1", 0L);
                                DBScheduledTransaction dbScheduledTransaction = DBScheduledTransaction.getInstance();
                                LScheduledTransaction scheduledTransaction = dbScheduledTransaction.getByGid(gid);
                                if (null != scheduledTransaction) {
                                    dbScheduledTransaction.deleteById(scheduledTransaction.getId());
                                }
                                break;

                            case NOTIFICATION_UPDATE_USER_PROFILE:
                                LPreferences.setUserName(intent.getStringExtra("txt1"));
                                uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_UI_UPDATE_USER_PROFILE));
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                break;

                            case NOTIFICATION_ADD_SHARE_USER:
                                uid = intent.getLongExtra("int1", 0L);
                                LPreferences.setShareUserId(uid, intent.getStringExtra("txt1"));
                                LPreferences.setShareUserName(uid, intent.getStringExtra("txt2"));
                                break;

                            case NOTIFICATION_REQUEST_ACCOUNT_SHARE:
                                long aid = intent.getLongExtra("int1", 0L);
                                uid = intent.getLongExtra("int2", 0L);

                                long shareAccept = LPreferences.getShareAccept(uid);
                                if (shareAccept != 0 && (shareAccept + 24 * 3600 * 1000 > System.currentTimeMillis())) {
                                    LJournal journal = new LJournal();
                                    journal.confirmAccountShare(aid, uid, true);
                                } else {
                                    name = intent.getStringExtra("txt1");
                                    LAccountShareRequest shareRequest = new LAccountShareRequest(uid, LPreferences
                                            .getShareUserId(uid), LPreferences.getShareUserName(uid), name, aid);
                                    LPreferences.addAccountShareRequest(shareRequest);

                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_SHARE_ACCOUNT));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_DECLINE_ACCOUNT_SHARE:
                                aid = intent.getLongExtra("int1", 0L);
                                uid = intent.getLongExtra("int2", 0L);
                                dbAccount = DBAccount.getInstance();
                                account = dbAccount.getByGid(aid);
                                if (null != account) {
                                    //only remove if share state is INVITED, in other words, do not
                                    //unshare a previously confirmed share here
                                    if (LAccount.ACCOUNT_SHARE_INVITED == account.getShareUserState(uid)) {
                                        account.removeShareUser(uid);
                                        dbAccount.update(account);

                                        uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                                .ACTION_UI_UPDATE_ACCOUNT));
                                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                    }
                                }
                                break;

                            case NOTIFICATION_UPDATE_ACCOUNT_USER:
                                aid = intent.getLongExtra("int1", 0L);
                                dbAccount = DBAccount.getInstance();
                                account = dbAccount.getByGid(aid);
                                if (null != account) {
                                    account.setSharedIdsString(intent.getStringExtra("txt1"));
                                    dbAccount.update(account);

                                    uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                            .ACTION_UI_UPDATE_ACCOUNT));
                                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                                }
                                break;

                            case NOTIFICATION_GET_ACCOUNT_RECORDS:
                                aid = intent.getLongExtra("int1", 0L);
                                journal.getAccountRecords(aid);
                                break;

                            case NOTIFICATION_GET_ACCOUNT_SCHEDULES:
                                aid = intent.getLongExtra("int1", 0L);
                                journal.getAccountSchedules(aid);
                                break;

                            case NOTIFICATION_GET_ACCOUNTS:
                                journal.getAllAccounts();
                                break;

                            case NOTIFICATION_GET_CATEGORIES:
                                journal.getAllCategories();
                                break;

                            case NOTIFICATION_GET_VENDORS:
                                journal.getAllVendors();
                                break;

                            case NOTIFICATION_GET_TAGS:
                                journal.getAllTags();
                                break;

                            default:
                                LLog.w(TAG, "unexpected notification id: " + nid);
                        }
                        pollingCount = MAX_POLLING_COUNT_UPON_PUSH_NOTIFICATION;
                        server.UiPollAck(id);
                    } else {
                        //no more
                        //LLog.d(TAG, "flushing journal upon polling ends ...");
                        if (!journal.flush()) {
                            if (LFragmentActivity.upRunning) {
                                //server.UiUtcSync();
                                if (pollingCount++ < MAX_POLLING_COUNT_UPON_PUSH_NOTIFICATION) {
                                    serviceHandler.postDelayed(pollRunnable, NETWORK_IDLE_POLLING_MS);
                                }

                                Intent uiIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_UI_NET_IDLE));
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(uiIntent);
                            } else {
                                LLog.d(TAG, "no activity visible, shutdown now");
                                serviceHandler.postDelayed(serviceShutdownRunnable,
                                        SERVICE_SHUTDOWN_MS);
                            }
                        }
                    }
                    break;

                case LBroadcastReceiver.ACTION_PUSH_NOTIFICATION:
                    //LLog.d(TAG, "received push notification --- fall through");
                    //reset polling count upon receiving push notification from server
                    //we'll keep polling up to MAX_POLLING_COUNT_UPON_PUSH_NOTIFICATION times, till a positive
                    //polling result from server: this is to handle the case where server sends the notification
                    //but underlying database hasn't got a chance to flush.
                    pollingCount = 0;
                    //FALL THROUGH
                case LBroadcastReceiver.ACTION_POLL_ACK:
                    //LLog.d(TAG, "flushing journal upon poll ack");
                    if (!journal.flush()) {
                        //LLog.d(TAG, "poll again upon ack without pending journal");
                        gatedPoll();//server.UiPoll();
                    }
                    break;

                case LBroadcastReceiver.ACTION_UNKNOWN_MSG:
                    LLog.w(TAG, "flushing journal upon unknown message received");
                    if (!journal.flush()) gatedPoll(); //serviceHandler.postDelayed(pollRunnable, NETWORK_IDLE_POLLING_MS);
                    break;

                //case LBroadcastReceiver.ACTION_SERVER_BROADCAST_MSG_RECEIVED:
                //    cacheId = intent.getIntExtra("cacheId", 0);
                //    server.UiPollAck(cacheId);
                //    break;
            }
        }
    }

    private boolean pollRequested = false;
    private void gatedPoll() {
        if (!pollRequested) {
            pollRequested = server.UiPoll();
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
                //LLog.d(TAG, "no account left, deleting all balances");
                DBAccountBalance.deleteAll(); //clean up balances if all accounts are removed.
                return false;
            }

            HashSet<Long> accounts = DBAccount.getInstance().getAllActiveIds();

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
            //LLog.d(TAG, "account balance synchronized");
            serviceHandler.removeCallbacks(serviceShutdownRunnable);
            serviceHandler.postDelayed(serviceShutdownRunnable, SERVICE_SHUTDOWN_MS);
        }

        @Override
        protected void onPreExecute() {
        }
    }
}
