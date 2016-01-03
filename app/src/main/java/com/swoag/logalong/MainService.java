package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;

import java.util.ArrayList;

public class MainService extends Service implements LBroadcastReceiver.BroadcastReceiverListener {
    private static final String TAG = MainService.class.getSimpleName();

    public static final int CMD_START = 10;
    public static final int CMD_STOP = 20;

    private Handler pollHandler;
    private Runnable pollRunnable;
    static final int NETWORK_POLLING_MS = 5000;
    private boolean requestToStop;
    private int pollingCount;
    private BroadcastReceiver broadcastReceiver;

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DBScheduledTransaction.scanAlarm();

        pollHandler = new Handler() {
        };
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if ((!LProtocol.ui.isConnected()) && (pollingCount++ > 3)) {
                    LLog.d(TAG, "stop self: unable to connect, after " + pollingCount + " tries");
                    stopSelf();
                } else {
                    LLog.d(TAG, "timed polling");
                    LProtocol.ui.poll();
                    pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
                }
            }
        };
        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_NETWORK_CONNECTED,
                LBroadcastReceiver.ACTION_USER_CREATED,
                LBroadcastReceiver.ACTION_LOGIN,
                LBroadcastReceiver.ACTION_POLL_ACKED,
                LBroadcastReceiver.ACTION_POLL_IDLE,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH,
                LBroadcastReceiver.ACTION_SHARE_ACCOUNT_WITH_USER,
                LBroadcastReceiver.ACTION_CONFIRMED_ACCOUNT_SHARE,
                LBroadcastReceiver.ACTION_SHARED_TRANSITION_RECORD,
                LBroadcastReceiver.ACTION_JOURNAL_POSTED,
                LBroadcastReceiver.ACTION_JOURNAL_RECEIVED,
                LBroadcastReceiver.ACTION_SHARE_ACCOUNT_USER_CHANGE}, this);
    }

    @Override
    public void onDestroy() {
        pollHandler.removeCallbacks(pollRunnable);
        pollRunnable = null;
        pollHandler = null;
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
        LProtocol.ui.disconnect();
        LLog.d(TAG, "service destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int cmd = intent.getIntExtra("cmd", 0);
            switch (cmd) {
                case CMD_START:
                    pollingCount = 0;
                    requestToStop = false;
                    LLog.d(TAG, "starting service, already connected: " + LProtocol.ui.isConnected());
                    if (LProtocol.ui.isConnected()) {
                        LProtocol.ui.login();
                    } else {
                        LProtocol.ui.connect();
                    }
                    break;

                case CMD_STOP:
                    LLog.d(TAG, "requested to stop service, connected: " + LProtocol.ui.isConnected());
                    if (LProtocol.ui.isConnected()) {
                        requestToStop = true;
                    } else {
                        stopSelf();
                    }
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_NETWORK_CONNECTED:
                LLog.d(TAG, "network connected");
                LProtocol.ui.initScrambler();
                if (TextUtils.isEmpty(LPreferences.getUserName())) {
                    if (!TextUtils.isEmpty(LPreferences.getUserFullName()))
                        LProtocol.ui.requestUserName();
                } else {
                    LProtocol.ui.login();
                }
                break;

            case LBroadcastReceiver.ACTION_USER_CREATED:
                int userId = intent.getIntExtra("id", 0);
                String userName = intent.getStringExtra("name");
                LPreferences.setUserId(userId);
                LPreferences.setUserName(userName);

                LProtocol.ui.updateUserProfile();
                LProtocol.ui.login();
                break;

            case LBroadcastReceiver.ACTION_LOGIN:
                if (ret == LProtocol.RSPS_OK) {
                    LProtocol.ui.updateUserProfile();
                    LLog.d(TAG, "user logged in");
                    LJournal.flush();
                    pollHandler.postDelayed(pollRunnable, 1000);
                } else {
                    LLog.w(TAG, "unable to login");
                    //the only reason that user is unable to login: user name no longer valid
                    // thus to wipe it off and let user to reset
                    LPreferences.setUserName("");
                }
                break;

            case LBroadcastReceiver.ACTION_POLL_IDLE:
                if (requestToStop || (!LFragmentActivity.upRunning)) {
                    LLog.d(TAG, "IDLE: stop self, requested: " + requestToStop + " active: " + LFragmentActivity.upRunning);
                    stopSelf();
                }
                break;

            case LBroadcastReceiver.ACTION_POLL_ACKED:
                pollHandler.removeCallbacks(pollRunnable);
                pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
                LLog.d(TAG, "polling after being acked");
                LProtocol.ui.poll();
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH:
                int cacheId = intent.getIntExtra("cacheId", 0);
                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                String userFullName = intent.getStringExtra("userFullName");
                String accountName = intent.getStringExtra("accountName");
                String uuid = intent.getStringExtra("UUID");
                byte requireConfirmation = intent.getByteExtra("requireConfirmation", (byte) 0);

                LLog.d(TAG, "requested to share with: " + userFullName + " : " + userName);
                LPreferences.setShareUserName(userId, userName);
                LPreferences.setShareUserFullName(userId, userFullName);
                LAccount account = DBAccount.getByName(accountName);
                if (requireConfirmation == 0) {
                    if (account == null) {
                        account = new LAccount();
                        account.setName(accountName);
                        account.setRid(uuid);
                        account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                        DBAccount.add(account);
                    } else {
                        //NOTE: potential racing issue that would cause account RID inconsistent?
                        //if the racing ever happens where account share has been initiated from both
                        //ends 'simultaneously', account will be left in an inconsistent state, but it
                        //will cure itself when any transaction record is shared among users.
                        account.setRid(uuid);
                        account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                        DBAccount.update(account);
                    }
                }
                LProtocol.ui.pollAck(cacheId);

                // now push all existing records
                if (requireConfirmation == 0)
                    LJournal.pushAllAccountRecords(userId, account);
                break;

            case LBroadcastReceiver.ACTION_SHARE_ACCOUNT_WITH_USER:
                if (ret == LProtocol.RSPS_OK) {
                    int id = intent.getIntExtra("id", 0);
                    String name = LPreferences.getShareUserName(id);
                    accountName = intent.getStringExtra("accountName");
                    requireConfirmation = intent.getByteExtra("requireConfirmation", (byte) 0);

                    account = DBAccount.getByName(accountName);
                    account.addShareUser(id, requireConfirmation == 1 ? LAccount.ACCOUNT_SHARE_INVITED : LAccount.ACCOUNT_SHARE_CONFIRMED);
                    DBAccount.update(account);
                } else {
                    LLog.w(TAG, "unable to complete share request");
                    //displayErrorMsg(LShareAccountDialog.this.getContext().getString(R.string.warning_unable_to_complete_share_request));
                }
                break;

            case LBroadcastReceiver.ACTION_CONFIRMED_ACCOUNT_SHARE:
                cacheId = intent.getIntExtra("cacheId", 0);
                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                accountName = intent.getStringExtra("accountName");
                //TODO: notify user

                LPreferences.setShareUserName(userId, userName);
                account = DBAccount.getByName(accountName);
                if (account == null) {
                    //TODO: the account name has been changed??
                    LLog.w(TAG, "warning: account renamed, account sharing ignored");
                } else {
                    ArrayList<Integer> ids = account.getShareIds();
                    ArrayList<Integer> states = account.getShareStates();
                    for (int jj = 0; jj < ids.size(); jj++) {
                        if (states.get(jj) == LAccount.ACCOUNT_SHARE_CONFIRMED) {
                            LProtocol.ui.shareAccountUserChange(ids.get(jj), userId, true, account.getName(), account.getRid());
                        }
                    }

                    account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                    DBAccount.update(account);
                }

                LProtocol.ui.pollAck(cacheId);

                // now push all existing records
                LJournal.pushAllAccountRecords(userId, account);
                break;

            case LBroadcastReceiver.ACTION_SHARED_TRANSITION_RECORD:
                cacheId = intent.getIntExtra("cacheId", 0);
                String record = intent.getStringExtra("record");
                LProtocol.ui.pollAck(cacheId);

                LJournal.updateItemFromReceivedRecord(record);
                break;

            case LBroadcastReceiver.ACTION_JOURNAL_POSTED:
                if (ret == LProtocol.RSPS_OK) {
                    long journalId = intent.getLongExtra("journalId", 0);
                    LJournal.deleteById(journalId);
                }
                LJournal.flush();
                break;

            case LBroadcastReceiver.ACTION_JOURNAL_RECEIVED:
                cacheId = intent.getIntExtra("cacheId", 0);
                LProtocol.ui.pollAck(cacheId);

                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                LLog.d(TAG, "received journal from: " + userId + "@" + userName + " ID: " + cacheId);
                LJournal.receive(intent.getStringExtra("record"));
                break;

            case LBroadcastReceiver.ACTION_SHARE_ACCOUNT_USER_CHANGE:
                // this is from our shared peer, informing status change for one of the existng shared user
                // or newly added user
                cacheId = intent.getIntExtra("cacheId", 0);
                LProtocol.ui.pollAck(cacheId);

                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                LLog.d(TAG, "received account share user change from: " + userId + "@" + userName);

                int changeUserId = intent.getIntExtra("changeUserId", 0);
                byte change = intent.getByteExtra("change", (byte) 0);
                accountName = intent.getStringExtra("accountName");
                uuid = intent.getStringExtra("UUID");

                account = DBAccount.getByRid(uuid);
                if (account == null) {
                    LLog.w(TAG, "warning: account removed?");
                } else {
                    if (change == 1) {
                        //we receive this add request, *ONLY* because one of the shared peer accepted
                        //this user to share this account, so we add silently.
                        account.addShareUser(changeUserId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                        DBAccount.update(account);

                        //this is to notify the counter part that we received the change request to add me
                        //basically this also says: hey, I confirm you to share account with me, and you've
                        //already got all my records (from one of my shared peer), now it is your turn to
                        //push me all your current records, so we're in sync.
                        LProtocol.ui.shareAccountWithUser(changeUserId, accountName, uuid, false);
                    } else {
                        if (changeUserId == LPreferences.getUserId()) {
                            //sorry, I am removed from the share group by one of the peer.
                            account.removeAllShareUsers();
                        } else {
                            //this is to remove somebody who was removed from group by someone else.
                            account.removeShareUser(changeUserId);
                        }
                        DBAccount.update(account);
                    }
                }
                break;
        }
    }
}
