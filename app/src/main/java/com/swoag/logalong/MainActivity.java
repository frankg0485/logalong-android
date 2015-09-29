package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.swoag.logalong.adapters.LPagerAdapter;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LUser;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LViewPager;

import java.util.HashMap;


public class MainActivity extends LFragmentActivity
        implements LBroadcastReceiver.BroadcastReceiverListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    FragmentManager fragmentManager;
    LPagerAdapter lPagerAdapter;
    LViewPager mViewPager;
    View footerV;
    View spinner;
    TextView errorMsgV;
    //LoginReceiverHelper loginReceiver;

    Handler pollHandler;
    Runnable pollRunnable;
    static final int NETWORK_POLLING_MS = 30000;
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            LLog.d(TAG, "an instance of main activity is already running");
            finish();
            return;
        }

        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            return;
        }

        LLog.d(TAG, "enter");
        LViewUtils.screenInit();

        //the combo of the following will cause client to reauthenticate upon startup
        //AppPersistency.reauthenticate = true;

        //LNotification.dismiss();
        LProtocol.ui.connect();
        if (LPreferences.getUserName().isEmpty()) {
            LProtocol.ui.requestUserName();
        } else {
            LProtocol.ui.login();
        }

        pollHandler = new Handler() {
        };
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                LProtocol.ui.poll();
                pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
            }
        };
        pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_POLL_ACKED,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH,
                LBroadcastReceiver.ACTION_SHARE_ACCOUNT_WITH_USER,
                LBroadcastReceiver.ACTION_CONFIRMED_ACCOUNT_SHARE,
                LBroadcastReceiver.ACTION_SHARED_TRANSITION_RECORD}, this);

        doOneTimeInit();
        setContentView(R.layout.top);
        footerV = findViewById(R.id.footer);
        spinner = (ProgressBar) findViewById(R.id.progressBar);
        errorMsgV = (TextView) findViewById(R.id.errorMsg);

        //if (YaventPreferences.getLoginState()) {
        footerV.setVisibility(View.GONE);
        //LoginReceiverHelper.checkAccount(this);
        //} else {
        //	loginReceiver = new LoginReceiverHelper(this, spinner, errorMsgV, this);
        //	loginReceiver.register();
        //}

        fragmentManager = getSupportFragmentManager();
        lPagerAdapter = new LPagerAdapter(fragmentManager);

        mViewPager = (LViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(lPagerAdapter);

        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageScrollStateChanged(int state) {
                        super.onPageScrollStateChanged(state);
                        switch (state) {
                            case ViewPager.SCROLL_STATE_IDLE:
                                break;
                            case ViewPager.SCROLL_STATE_DRAGGING:
                            case ViewPager.SCROLL_STATE_SETTLING:
                                break;
                        }
                    }

                    @Override
                    public void onPageSelected(int position) {
                        //LLog.d(TAG, "page " + position + " selected");
                        selectTab(position);
                    }
                });

        LViewUtils.setAlpha(findViewById(R.id.tab0), 0.4f);
        LViewUtils.setAlpha(findViewById(R.id.tab2), 0.4f);
        selectTab(findViewById(R.id.tab1), 1);
        mViewPager.setCurrentItem(1);

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                int backCount = getSupportFragmentManager().getBackStackEntryCount();
                if (backCount == 0) {
                    //footerV.setVisibility(YaventPreferences.getLoginState()? View.GONE : View.VISIBLE);
                }
            }
        });
    }

    public void enablePager() {
        mViewPager.setPagingEnabled(true);
    }

    public void disablePager() {
        mViewPager.setPagingEnabled(false);
    }

    /*@Override
    public void onLoginStatus (Context arg0, boolean success, Intent arg1) {
        if (success) {
            LLog.d(TAG, "login done");
            footerV.setVisibility(View.GONE);
        } else {
            LViewUtils.disableEnableControls(true, (ViewGroup)footerV);
        }
    };
*/
    private View selectedTabView;
    private int deselectedPosition;

    private void selectTab(int position) {
        switch (position) {
            case 0:
                selectTab(findViewById(R.id.tab0), position);
                break;
            case 1:
                selectTab(findViewById(R.id.tab1), position);
                break;
            case 2:
                selectTab(findViewById(R.id.tab2), position);
                break;
        }
    }

    private void selectTab(View v, int position) {
        if (selectedTabView != null) {
            selectedTabView.setSelected(false);
            LViewUtils.setAlpha(selectedTabView, 0.4f);
            LFragment yf = (LFragment) lPagerAdapter.getItem(deselectedPosition);
            if (yf != null) yf.onSelected(false);
        }
        selectedTabView = v;
        selectedTabView.setSelected(true);

        LViewUtils.setAlpha(selectedTabView, 1.0f);
        this.deselectedPosition = position;

        LFragment yf = (LFragment) lPagerAdapter.getItem(position);
        if (yf != null) yf.onSelected(true);
    }

    public void onReports(View v) {
        if (mViewPager.isPagingEnabled())
            mViewPager.setCurrentItem(0);
    }

    public void onNewLog(View v) {
        if (mViewPager.isPagingEnabled())
            mViewPager.setCurrentItem(1);
    }

    public void onAccounts(View v) {
        if (mViewPager.isPagingEnabled())
            mViewPager.setCurrentItem(2);
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideErrorMsg();
        //if (LPreferences.getLoginState()) {
        footerV.setVisibility(View.GONE);
        /*} else {
            if (AppPersistency.loginPending) {
				spinner.setVisibility(View.VISIBLE);
				LViewUtils.disableEnableControls(false, (ViewGroup)footerV);
			} else {
				spinner.setVisibility(View.INVISIBLE);
				LViewUtils.disableEnableControls(true, (ViewGroup)footerV);
			}
		}*/
    }

    @Override
    public void onBackPressed() {
        LFragment yf = (LFragment) lPagerAdapter.getItem(mViewPager.getCurrentItem());
        if (yf != null)
            if (yf.onBackPressed()) return;
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        pollHandler.removeCallbacks(pollRunnable);
        pollRunnable = null;
        pollHandler = null;
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }

        LLog.d(TAG, "destroyed");
        /*if (loginReceiver != null) {
            loginReceiver.unregister();
			loginReceiver = null;
		}
		LNotification.dismiss();*/
        LProtocol.ui.disconnect();
        super.onDestroy();
    }

    public void onLogin(View v) {
        /*if (LConnection.getService() == null) {
            LLog.w(TAG, "service not ready");
			return;
		}

		hideErrorMsg();
		startActivity(new Intent(this, LoginActivity.class));*/
    }

    private void hideErrorMsg() {
        errorMsgV.setVisibility(View.GONE);
    }


    private static String[] accounts = {
            "Checking",
            "Savings",
            "Cash",
            "Credit:Discover",
            "Credit:Master",
            "Credit:Visa",
            "Credit:American Express"
    };

    private static String[] categories = {
            "Clothes",
            "Eating Out",
            "Entertainment",
            "Fuel",
            "Gift",
            "Grocery",
            "Health",
            "House",
            "Kids",
            "Sports",
            "Other",
            "Travel"
    };

    /*private static String[] tags = {
            "General"
    };*/

    private static String[] vendors = {
            "Amazon",
            "Chiptole"
    };

    public static void addAccounts() {
        LAccount account = new LAccount();
        for (int ii = 0; ii < accounts.length; ii++) {
            account.setName(accounts[ii]);
            DBAccess.addAccount(account);
        }
    }

    public static void addCategories() {
        LCategory category = new LCategory();
        for (int ii = 0; ii < categories.length; ii++) {
            category.setName(categories[ii]);
            DBAccess.addCategory(category);
        }
    }

    /*public static void addTags() {
        LTag tag = new LTag();
        for (int ii = 0; ii < tags.length; ii++) {
            tag.setName(tags[ii]);
            DBAccess.addTag(tag);
        }
    }*/

    public static void addVendors() {
        LVendor vendor = new LVendor();
        for (int ii = 0; ii < vendors.length; ii++) {
            vendor.setName(vendors[ii]);
            DBAccess.addVendor(vendor);
        }
    }

    private void initDb() {
        if (DBAccess.getAccountNameById(1).isEmpty()) {
            addAccounts();
        }

        if (DBAccess.getCategoryNameById(1).isEmpty()) {
            addCategories();
        }
        if (DBAccess.getVendorNameById(1).isEmpty()) {
            addVendors();
        }

        /*if (DBAccess.getTagById(1).isEmpty()) {
            addTags();
        }*/
    }

    private void doOneTimeInit() {
        //initDb();
        /*if (LPreferences.getOneTimeInit()) return;
        LPreferences.setOneTimeInit(true);*/
    }

    private void pushAllAccountRecords(int userId, LAccount account) {
        Cursor cursor = DBAccess.getActiveItemsCursorByAccount(account.getId());
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                String category, vendor;
                category = DBAccess.getCategoryNameById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CATEGORY)));
                vendor = DBAccess.getVendorNameById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_VENDOR)));
                int madeBy = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_MADEBY));
                if (madeBy == 0) {
                    madeBy = LPreferences.getUserId();
                }

                String record = DBHelper.TABLE_COLUMN_TYPE + "=" + cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE)) + ","
                        + DBHelper.TABLE_COLUMN_AMOUNT + "=" + cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT)) + ","
                        + DBHelper.TABLE_COLUMN_TIMESTAMP + "=" + cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)) + ","
                        + DBHelper.TABLE_COLUMN_MADEBY + "=" + madeBy + ","
                        + DBHelper.TABLE_COLUMN_ACCOUNT + "=" + account.getName() + ","
                        + DBHelper.TABLE_COLUMN_CATEGORY + "=" + category + ","
                        + DBHelper.TABLE_COLUMN_VENDOR + "=" + vendor;
                LProtocol.ui.shareTransitionRecord(userId, record);
            } while (cursor.moveToNext());
        }
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
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
                //TODO: ask for user confirmation

                LPreferences.setShareUserName(userId, userName);
                LAccount account = DBAccess.getAccountByName(accountName);
                if (account == null) {
                    account = new LAccount();
                    account.setName(accountName);
                    account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                    DBAccess.addAccount(account);
                } else {
                    account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                    DBAccess.updateAccount(account);
                }

                LProtocol.ui.pollAck(cacheId);
                LProtocol.ui.confirmAccountShare(userId, accountName);

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

                String[] splitRecords = record.split(",", -1);
                double amount = 0;
                int type = LTransaction.TRANSACTION_TYPE_EXPENSE;
                int madeBy = 0;
                long timestamp = 0, accountId = 0, categoryId = 0, vendorId = 0;

                for (String str : splitRecords) {
                    String[] ss = str.split("=", -1);
                    if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TYPE)) {
                        type = Integer.valueOf(ss[1]);
                    } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_MADEBY)) {
                        madeBy = Integer.valueOf(ss[1]);
                    } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_AMOUNT)) {
                        amount = Double.valueOf(ss[1]);
                    } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_TIMESTAMP)) {
                        timestamp = Long.valueOf(ss[1]);
                    } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_ACCOUNT)) {
                        LAccount account1 = DBAccess.getAccountByName(ss[1]);
                        if (null == account1) {
                            accountId = DBAccess.addAccount(new LAccount(ss[1]));
                        } else {
                            accountId = account1.getId();
                        }
                    } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_CATEGORY)) {
                        LCategory category1 = DBAccess.getCategoryByName(ss[1]);
                        if (null == category1) {
                            categoryId = DBAccess.addCategory(new LCategory(ss[1]));
                        } else {
                            categoryId = category1.getId();
                        }
                    } else if (ss[0].contentEquals(DBHelper.TABLE_COLUMN_VENDOR)) {
                        LVendor vendor1 = DBAccess.getVendorByName(ss[1]);
                        if (null == vendor1) {
                            vendorId = DBAccess.addVendor(new LVendor(ss[1]));
                        } else {
                            vendorId = vendor1.getId();
                        }
                    }
                }

                LTransaction item = DBAccess.getItemByUserTimestamp(madeBy, timestamp);
                if (item != null) {
                    item.setValue(amount);
                    item.setType(type);
                    item.setAccount(accountId);
                    item.setCategory(categoryId);
                    item.setVendor(vendorId);
                    DBAccess.updateItem(item);
                } else {
                    DBAccess.addItem(new LTransaction(amount, type, categoryId, vendorId, 0, accountId, madeBy, timestamp));
                }
                break;
        }
    }
}
