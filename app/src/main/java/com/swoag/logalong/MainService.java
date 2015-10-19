package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;

import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;

import java.util.ArrayList;
import java.util.UUID;

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

        pollHandler = new Handler() {
        };
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if ((!LProtocol.ui.isConnected()) && (pollingCount++ > 3)) stopSelf();
                else {
                    LProtocol.ui.poll();
                    pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
                }
            }
        };
        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_NETWORK_CONNECTED,
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
                    LProtocol.ui.connect();
                    break;

                case CMD_STOP:
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

    private void pushAllAccountRecords(int userId, LAccount account) {
        Cursor cursor = DBTransaction.getCursorByAccount(account.getId());
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                LTransaction item = new LTransaction();
                DBTransaction.getValues(cursor, item);
                String record = LJournal.transactionItemString(item);
                LProtocol.ui.shareTransitionRecord(userId, record);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_NETWORK_CONNECTED:
                LProtocol.ui.initScrambler();
                if (LPreferences.getUserName().isEmpty()) {
                    //LProtocol.ui.requestUserName();
                } else {
                    LProtocol.ui.login();
                }
                break;

            case LBroadcastReceiver.ACTION_LOGIN:
                if (ret == LProtocol.RSPS_OK) {
                    LLog.d(TAG, "user logged in");
                    LJournal.flush();
                    pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
                } else {
                    LLog.w(TAG, "unable to login");
                }
                break;

            case LBroadcastReceiver.ACTION_POLL_IDLE:
                if (requestToStop) {
                    stopSelf();
                }
                break;

            case LBroadcastReceiver.ACTION_POLL_ACKED:
                pollHandler.removeCallbacks(pollRunnable);
                pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
                LProtocol.ui.poll();
                break;

            case LBroadcastReceiver.ACTION_SHARE_ACCOUNT_WITH_USER:
                if (ret == LProtocol.RSPS_OK) {
                    int id = intent.getIntExtra("id", 0);
                    String name = LPreferences.getShareUserName(id);
                    String accountName = intent.getStringExtra("accountName");

                    LAccount account = DBAccess.getAccountByName(accountName);
                    account.addShareUser(id, LAccount.ACCOUNT_SHARE_INVITED);
                    DBAccess.updateAccount(account);
                    LPreferences.setShareUserName(id, name);
                } else {
                    LLog.w(TAG, "unable to complete share request");
                    //displayErrorMsg(LShareAccountDialog.this.getContext().getString(R.string.warning_unable_to_complete_share_request));
                }
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH:
                int cacheId = intent.getIntExtra("cacheId", 0);
                int userId = intent.getIntExtra("id", 0);
                String userName = intent.getStringExtra("userName");
                String accountName = intent.getStringExtra("accountName");
                String uuid = intent.getStringExtra("UUID");
                byte requireConfirmation = intent.getByteExtra("requireConfirmation", (byte) 0);
                //TODO: ask for user confirmation

                LPreferences.setShareUserName(userId, userName);
                LAccount account = DBAccess.getAccountByName(accountName);
                if (account == null) {
                    account = new LAccount();
                    account.setName(accountName);
                    account.setRid(UUID.fromString(uuid));
                    account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                    DBAccess.addAccount(account);
                } else {
                    account.setRid(UUID.fromString(uuid));
                    account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                    DBAccess.updateAccount(account);
                }
                LProtocol.ui.pollAck(cacheId);

                // inform all existing peers about this new user if confirmation were required
                if (requireConfirmation == 1) {
                    LProtocol.ui.confirmAccountShare(userId, accountName);

                    ArrayList<Integer> ids = account.getShareIds();
                    ArrayList<Integer> states = account.getShareStates();
                    for (int jj = 0; jj < ids.size(); jj++) {
                        if (states.get(jj) == LAccount.ACCOUNT_SHARE_CONFIRMED && jj != userId) {
                            LProtocol.ui.shareAccountUserChange(ids.get(jj), userId, true, account.getName(), account.getRid().toString());
                        }
                    }
                }

                // now push all existing records
                pushAllAccountRecords(userId, account);
                break;

            case LBroadcastReceiver.ACTION_CONFIRMED_ACCOUNT_SHARE:
                cacheId = intent.getIntExtra("cacheId", 0);
                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                accountName = intent.getStringExtra("accountName");
                //TODO: notify user

                LPreferences.setShareUserName(userId, userName);
                account = DBAccess.getAccountByName(accountName);
                if (account == null) {
                    //TODO: the account name has been changed??
                    LLog.w(TAG, "warning: account renamed, account sharing ignored");
                } else {
                    ArrayList<Integer> ids = account.getShareIds();
                    ArrayList<Integer> states = account.getShareStates();
                    for (int jj = 0; jj < ids.size(); jj++) {
                        if (states.get(jj) == LAccount.ACCOUNT_SHARE_CONFIRMED) {
                            LProtocol.ui.shareAccountUserChange(ids.get(jj), userId, true, account.getName(), account.getRid().toString());
                        }
                    }

                    account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                    DBAccess.updateAccount(account);
                }

                LProtocol.ui.pollAck(cacheId);

                // now push all existing records
                pushAllAccountRecords(userId, account);
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
                    DBAccess.deleteJournalById(journalId);
                }
                break;

            case LBroadcastReceiver.ACTION_JOURNAL_RECEIVED:
                cacheId = intent.getIntExtra("cacheId", 0);
                LProtocol.ui.pollAck(cacheId);

                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                LLog.d(TAG, "received journal from: " + userId + "@" + userName);
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

                account = DBAccess.getAccountByUuid(UUID.fromString(uuid));
                if (account == null) {
                    LLog.w(TAG, "warning: account removed?");
                } else {
                    if (change == 1) {
                        account.addShareUser(changeUserId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                        DBAccess.updateAccount(account);
                        LProtocol.ui.shareAccountWithUser(changeUserId, accountName, uuid, false);
                    } else {
                        if (changeUserId == LPreferences.getUserId()) {
                            account.removeAllShareUsers();
                        } else {
                            account.removeShareUser(changeUserId);
                        }
                        DBAccess.updateAccount(account);
                    }
                }
                break;
        }
    }
}
