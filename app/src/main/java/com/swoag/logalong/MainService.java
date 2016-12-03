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
import android.text.TextUtils;

import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountBalance;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBAccountBalance;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBProvider;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

public class MainService extends Service implements LBroadcastReceiver.BroadcastReceiverListener, Loader.OnLoadCompleteListener {
    private static final String TAG = MainService.class.getSimpleName();

    public static final int CMD_START = 10;
    public static final int CMD_STOP = 20;
    public static final int CMD_ENABLE = 30;
    public static final int CMD_DISABLE = 40;

    private Handler pollHandler;
    private Runnable pollRunnable;
    private Runnable journalPostRunnable;
    private Runnable serviceShutdownRunnable;

    //default to active polling, if polling returned IDLE, switch to IDLE_POLLING interval
    //as soon as any valid command found, go back to active polling.
    static final int NETWORK_IDLE_POLLING_MS = 5000;
    static final int NETWORK_ACTIVE_POLLING_MS = 15 * 60000; //so each polled command has a 15-min window to respond
    static final int NETWORK_JOURNAL_POST_TIMEOUT_MS = 15 * 60000;
    static final int NETWORK_JOURNAL_POST_RETRY_TIMEOUT_MS = 30000;
    static final int SERVICE_SHUTDOWN_MS = 15000;
    private int pollingCount;
    private int logInAttempts;
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        server = LAppServer.getInstance();

        DBScheduledTransaction.scanAlarm();
        DBAccountBalance.deleteAll();

        pollHandler = new Handler() {
        };

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if ((!server.UiIsConnected()) && (pollingCount++ > 3)) {
                    LLog.d(TAG, "stop self: unable to connect, after " + pollingCount + " tries");
                    stopSelf();
                } else {
                    LLog.d(TAG, "heart beat polling");
                    server.UiPoll();
                    //default to active polling
                    pollHandler.postDelayed(pollRunnable, NETWORK_ACTIVE_POLLING_MS);
                }
            }
        };

        journalPostRunnable = new Runnable() {
            @Override
            public void run() {
                LJournal.flush();
                pollHandler.postDelayed(journalPostRunnable, NETWORK_JOURNAL_POST_TIMEOUT_MS);
            }
        };

        serviceShutdownRunnable = new Runnable() {
            @Override
            public void run() {
                LLog.d(TAG, "shutdown timeout: shutting down service itself");
                stopSelf();
            }
        };

        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_NETWORK_CONNECTED,
                LBroadcastReceiver.ACTION_USER_CREATED,
                LBroadcastReceiver.ACTION_LOGIN,
                LBroadcastReceiver.ACTION_POLL_ACKED,
                LBroadcastReceiver.ACTION_POLL_IDLE,
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

                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH,
                LBroadcastReceiver.ACTION_SHARE_ACCOUNT_WITH_USER,
                LBroadcastReceiver.ACTION_CONFIRMED_ACCOUNT_SHARE_WITH_UUID,
                LBroadcastReceiver.ACTION_JOURNAL_POSTED,
                LBroadcastReceiver.ACTION_JOURNAL_RECEIVED,
                LBroadcastReceiver.ACTION_SHARE_ACCOUNT_USER_CHANGE}, this);


        updateAccountBalanceRunnable = new Runnable() {
            @Override
            public void run() {
                if (asyncScanBalances.getStatus() != AsyncTask.Status.RUNNING)
                {
                    asyncScanBalances = new AsyncScanBalances();
                    LTask.start(asyncScanBalances, lastLoadedData);
                } else {
                    pollHandler.postDelayed(updateAccountBalanceRunnable, UPDATE_ACCOUNT_BALANCE_DELAY_MS);
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
        cursorLoader.startLoading();
    }

    @Override
    public void onLoadComplete(Loader loader, Object data) {
        if (loader.getId() == LOADER_ID_UPDATE_BALANCE) {
            pollHandler.removeCallbacks(updateAccountBalanceRunnable);
            if (asyncScanBalances.getStatus() != AsyncTask.Status.RUNNING)
            {
                asyncScanBalances = new AsyncScanBalances();
                LTask.start(asyncScanBalances, (Cursor)data);
            } else {
                lastLoadedData = (Cursor)data;
                pollHandler.postDelayed(updateAccountBalanceRunnable, UPDATE_ACCOUNT_BALANCE_DELAY_MS);
            }
        }
    }

    @Override
    public void onDestroy() {
        pollHandler.removeCallbacks(serviceShutdownRunnable);
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.removeCallbacks(journalPostRunnable);
        pollHandler.removeCallbacks(updateAccountBalanceRunnable);
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
        server.disconnect();
        LLog.d(TAG, "service destroyed");

        asyncScanBalances.cancel(true);
        asyncScanBalances = null;

        pollRunnable = null;
        journalPostRunnable = null;
        serviceShutdownRunnable = null;
        updateAccountBalanceRunnable = null;
        pollHandler = null;

        if (cursorLoader != null) {
            cursorLoader.unregisterListener(this);
            cursorLoader.cancelLoad();
            cursorLoader.stopLoading();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int cmd = intent.getIntExtra("cmd", 0);
            switch (cmd) {
                case CMD_START:
                    pollingCount = 0;
                    pollHandler.removeCallbacks(serviceShutdownRunnable);
                    logInAttempts = 0;

                    LLog.d(TAG, "starting service, already connected: " + server.UiIsConnected());
                    if (server.UiIsConnected()) {
                        server.UiLogin();
                    } else {
                        server.connect();
                    }
                    break;

                case CMD_STOP:
                    LLog.d(TAG, "requested to stop service, connected: " + server.UiIsConnected());
                    if (server.UiIsConnected()) {
                        LLog.d(TAG, "post service shutdown runnable");
                        pollHandler.removeCallbacks(serviceShutdownRunnable);
                        pollHandler.postDelayed(serviceShutdownRunnable, SERVICE_SHUTDOWN_MS);
                    } else {
                        stopSelf();
                    }
                    break;
                case CMD_ENABLE:
                    server.enable();
                    break;

                case CMD_DISABLE:
                    server.disable();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    private int journalPostErrorCount = 0;
    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        //on Samsung phone, receiver may still get called when service is destroyed!!
        if (pollHandler == null) {
            LLog.w(TAG, "unexpected, receiver called on destroyed service!");
            return;
        }

        pollHandler.removeCallbacks(serviceShutdownRunnable);
        //default polling policy: active, with longer period
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, NETWORK_ACTIVE_POLLING_MS);

        switch (action) {
            case LBroadcastReceiver.ACTION_NETWORK_CONNECTED:
                pollHandler.removeCallbacks(pollRunnable); //disable polling

                LLog.d(TAG, "network connected");
                server.UiInitScrambler();
                if (TextUtils.isEmpty(LPreferences.getUserName())) {
                    if (!TextUtils.isEmpty(LPreferences.getUserFullName())) {
                        LLog.d(TAG, "user name empty but full name specified, request user name automatically");
                        server.UiRequestUserName();
                    }
                } else {
                    server.UiLogin();
                }
                break;

            case LBroadcastReceiver.ACTION_USER_CREATED:
                pollHandler.removeCallbacks(pollRunnable); //disable polling

                int userId = intent.getIntExtra("id", 0);
                String userName = intent.getStringExtra("name");
                LLog.d(TAG, "user created, id: " + userId + " name: " + userName);
                LPreferences.setUserId(userId);
                LPreferences.setUserName(userName);

                server.UiUpdateUserProfile();
                server.UiLogin();
                break;

            case LBroadcastReceiver.ACTION_LOGIN:
                pollHandler.removeCallbacks(pollRunnable); //disable polling

                if (ret == LProtocol.RSPS_OK) {
                    logInAttempts = 0;
                    server.UiUtcSync();
                    server.UiUpdateUserProfile();
                    LLog.d(TAG, "user logged in");
                    //journal posting and polling start only upon successful login
                    pollHandler.removeCallbacks(journalPostRunnable);
                    pollHandler.post(journalPostRunnable);
                    pollHandler.removeCallbacks(pollRunnable);
                    pollHandler.postDelayed(pollRunnable, 1000); //poll shortly after login
                } else {
                    LLog.e(TAG, "unable to login: " + LPreferences.getUserId() + " name: " + LPreferences.getUserName());
                    if (logInAttempts++ > 3) {
                        //the only reason that user is unable to login: user name no longer valid
                        // thus to wipe it off and let user to reset
                        //
                        LPreferences.setUserName("");
                    } else {
                        server.UiLogin();
                    }
                }
                break;

            case LBroadcastReceiver.ACTION_POLL_IDLE:
                if (LFragmentActivity.upRunning) {
                    server.UiUtcSync();
                    pollHandler.removeCallbacks(pollRunnable);
                    pollHandler.postDelayed(pollRunnable, NETWORK_IDLE_POLLING_MS);
                } else {
                    pollHandler.removeCallbacks(pollRunnable); //disable polling
                    pollHandler.postDelayed(serviceShutdownRunnable, SERVICE_SHUTDOWN_MS);
                }
                break;

            case LBroadcastReceiver.ACTION_POLL_ACKED:
                //LLog.d(TAG, "polling after being acked");
                server.UiPoll();
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH:
                int cacheId = intent.getIntExtra("cacheId", 0);
                server.UiPollAck(cacheId);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SET_ACCOUNT_GID:
                cacheId = intent.getIntExtra("cacheId", 0);
                userId = intent.getIntExtra("id", 0);
                int accountId = intent.getIntExtra("accountId", 0);
                int accountGid = intent.getIntExtra("accountGid", 0);
                String accountName = intent.getStringExtra("accountName");

                if (userId != LPreferences.getUserId()) {
                    LLog.e(TAG, "unexpected user id: " + userId + " myId: " + LPreferences.getUserId());
                } else {
                    LAccount account = DBAccount.getById(accountId);
                    if (account == null) {
                        LLog.w(TAG, "requested account no longer exist? id: " + accountId + " name: " + accountName);
                    } else {
                        if (!accountName.contentEquals(account.getName())) {
                            LLog.w(TAG, "account: " + accountId + " renamed before getting its GID: " + accountName + " -> " + account.getName());
                            account.setName(accountName);
                        }
                        if (account.getGid() != 0 && accountGid != account.getGid()) {
                            LLog.w(TAG, "account GID already set: " + account.getGid() + " and mismatches new request: " + accountGid);
                        }

                        DBAccount.resetGidIfNotUnique(accountGid);

                        account.setGid(accountGid);
                        DBAccount.update(account);
                    }
                }
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
                    ArrayList<Integer> origStates = new ArrayList<Integer>(account.getShareStates());

                    account.removeAllShareUsers();
                    int  myUserId = LPreferences.getUserId();
                    boolean isShared = false;
                    for (int user: shareUSers) if (user == myUserId) {isShared = true; break;}
                    LLog.d(TAG, "account: " + account.getName() + " shared? " + isShared);
                    if (isShared) {
                        for (int user : shareUSers) {
                            if (user == myUserId) continue;

                            boolean newShare = true;
                            for (int ii = 0; ii < origIds.size(); ii++) {
                                if (user == origIds.get(ii) && origStates.get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED_SYNCED) {
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

            case LBroadcastReceiver.ACTION_JOURNAL_POSTED:
                pollHandler.removeCallbacks(journalPostRunnable);
                pollHandler.postDelayed(journalPostRunnable, NETWORK_JOURNAL_POST_TIMEOUT_MS);
                int journalId = intent.getIntExtra("journalId", 0);
                userId = intent.getIntExtra("userId", 0);
                switch (ret) {
                    case LProtocol.RSPS_OK: {
                        LJournal.deleteById(journalId);
                        LJournal.flush();
                        break;
                    }
                    case LProtocol.RSPS_USER_NOT_FOUND:
                    {
                        LLog.w(TAG, "journal post targeting user no longer available, id: " + userId);
                        LJournal.deleteById(journalId);
                        LJournal.flush();
                        break;
                    }
                    default:
                    {
                        // try a few more times, then bail, so not to lock out polling altogether
                        if (journalPostErrorCount++ < 3) {
                            // upon post error, do not flush immediately, instead let it timeout and retry
                            // this is to prevent flooding network layer when something goes wrong.
                            pollHandler.removeCallbacks(journalPostRunnable);
                            pollHandler.postDelayed(journalPostRunnable, NETWORK_JOURNAL_POST_RETRY_TIMEOUT_MS);
                            LLog.w(TAG, "unexpected journal post error, to: " + userId + " reture code: " + ret);
                        } else {
                            LLog.e(TAG, "fatal journal post error, journal skipped, to user: " + userId);
                            LJournal.deleteById(journalId);
                            LJournal.flush();
                            journalPostErrorCount = 0;
                        }
                        break;
                    }
                }

                pollHandler.removeCallbacks(pollRunnable);
                pollHandler.postDelayed(pollRunnable, NETWORK_IDLE_POLLING_MS);
                break;

            case LBroadcastReceiver.ACTION_JOURNAL_RECEIVED:
                cacheId = intent.getIntExtra("cacheId", 0);
                server.UiPollAck(cacheId);

                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                //LLog.d(TAG, "received journal from: " + userId + "@" + userName + " ID: " + cacheId);
                LJournal.receive(intent.getStringExtra("record"));
                break;

            case LBroadcastReceiver.ACTION_UNKNOWN_MSG:
                LLog.w(TAG, "unknown message received");
            case LBroadcastReceiver.ACTION_SERVER_BROADCAST_MSG_RECEIVED:
                cacheId = intent.getIntExtra("cacheId", 0);
                server.UiPollAck(cacheId);
                break;
        }
    }

    private class AsyncScanBalances extends AsyncTask<Cursor, Void, Boolean> {

        private void addUpdateAccountBalance(double[] doubles, long accountId, int year)
        {
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
            if (data == null || data.getCount() == 0) return false;

            try {
                if (isCancelled()) return false;
                else data.moveToFirst();
                Calendar calendar = Calendar.getInstance();
                double[] doubles = new double[12];
                long lastAccountId = 0;
                int lastYear = 0;
                do {
                    long accountId = data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT));
                    double amount = data.getDouble(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                    int type = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
                    //String name = data.getString(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME));
                    calendar.setTimeInMillis(data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)));
                    int year = calendar.get(Calendar.YEAR);
                    int mon = calendar.get(Calendar.MONTH);
                    if (lastAccountId == 0) {
                        lastAccountId = accountId;
                        lastYear = year;
                    }
                    else if (lastAccountId != accountId || lastYear != year) {
                        addUpdateAccountBalance(doubles, lastAccountId, lastYear);

                        lastAccountId = accountId;
                        lastYear = year;
                        for (int ii = 0; ii < 12; ii++) doubles[ii] = 0.0;
                    }
                    doubles[mon] += (type == LTransaction.TRANSACTION_TYPE_INCOME ||
                            type == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY)? amount : -amount;
                } while (!isCancelled() && data.moveToNext());
                if (!isCancelled()) addUpdateAccountBalance(doubles, lastAccountId, lastYear);

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
        }

        @Override
        protected void onPreExecute() {
        }
    }

    private void updateAccountFromReceivedRecord(int accountGid, String name, String receivedRecord) {
        String[] splitRecords = receivedRecord.split(",", -1);
        String rid = "";
        int state = DBHelper.STATE_ACTIVE;
        long timestampLast = 0;
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
            } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_NAME + "old")) {
                oldName = ss[1];
                oldNameFound = true;
            }
        }

        LAccount account = DBAccount.getByGid(accountGid);
        if (account == null) {
            LLog.w(TAG, "account no longer exists, GID: " + accountGid);
        } else {
            boolean conflict = true;
            boolean update = false;

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
                        LTask.start(new MyAccountDeleteTask(), account.getId());
                    }
                }
                if (oldNameFound) account.setName(name);

                if (account.getTimeStampLast() < timestampLast)
                    account.setTimeStampLast(timestampLast);
                update = true;
            } else if (account.getTimeStampLast() <= timestampLast) {
                LLog.w(TAG, "conflict detected, force to update account: " + account.getName());
                if (!TextUtils.isEmpty(name)) account.setName(name);
                if (stateFound) account.setState(state);

                if (account.getState() == DBHelper.STATE_DELETED) {
                    LTask.start(new MyAccountDeleteTask(), account.getId());
                }
                account.setTimeStampLast(timestampLast);
                update = true;
            }

            if (update) {
                //detect name conflict, before applying update
                int ii = 0;
                String nameOrig = name;
                boolean dup = true;
                while (ii++ < 9) {
                    LAccount account1 = DBAccount.getByName(name);
                    if ((account1 != null) && account1.getId() != account.getId()) {
                        name = nameOrig + ii++;
                    } else {
                        dup = false;
                        break;
                    }
                }
                if (dup) {
                    //if there's still dup after 10 tries, we bail and take the name as is.
                    LLog.w(TAG, "FAIL: unresolvable account name duplication found");
                }
                account.setName(name);
                DBAccount.update(account);

                if (!name.contentEquals(nameOrig)) {
                    LLog.d(TAG, "account name conflicts, renaming from: " + nameOrig + " to: " + name);
                    LJournal journal = new LJournal();
                    journal.updateAccount(account, nameOrig);
                }
            }
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
