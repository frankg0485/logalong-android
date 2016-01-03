package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;

import com.swoag.logalong.adapters.LPagerAdapter;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountShareRequest;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBPorter;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LReminderDialog;
import com.swoag.logalong.views.LShareAccountConfirmDialog;
import com.swoag.logalong.views.LViewPager;

import java.util.ArrayList;

public class MainActivity extends LFragmentActivity
        implements LBroadcastReceiver.BroadcastReceiverListener,
        LShareAccountConfirmDialog.LShareAccountConfirmDialogItf {
    private static final String TAG = MainActivity.class.getSimpleName();

    FragmentManager fragmentManager;
    LPagerAdapter lPagerAdapter;
    LViewPager mViewPager;

    private BroadcastReceiver broadcastReceiver;
    private Handler handler;
    private Runnable confirmAccountShare;
    private boolean shareAccountConfirmDialogOpened = false;

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

        LViewUtils.screenInit();

        handler = new Handler();
        confirmAccountShare = new Runnable() {
            @Override
            public void run() {
                accountShareRequest = LPreferences.getAccountShareRequest();
                if ((accountShareRequest != null) && (!shareAccountConfirmDialogOpened)) {
                    shareAccountConfirmDialogOpened = true;
                    LShareAccountConfirmDialog dialog = new LShareAccountConfirmDialog(MainActivity.this,
                            accountShareRequest, MainActivity.this);
                    dialog.show();
                } else if (!TextUtils.isEmpty(LPreferences.getServerMsg())) {
                    new LReminderDialog(MainActivity.this, LPreferences.getServerMsg()).show();
                    LPreferences.setServerMsg("");
                }
            }
        };
        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH,
                LBroadcastReceiver.ACTION_SERVER_BROADCAST_MSG_RECEIVED
        }, this);

        doOneTimeInit();
        setContentView(R.layout.top);

        fragmentManager = getSupportFragmentManager();
        lPagerAdapter = new LPagerAdapter(fragmentManager);

        mViewPager = (LViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(lPagerAdapter);

        mViewPager.addOnPageChangeListener(
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

        handler.post(confirmAccountShare);
    }

    /*
    public void enablePager() {
        mViewPager.setPagingEnabled(true);
    }

    public void disablePager() {
        mViewPager.setPagingEnabled(false);
    }
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
        MainService.start(this);
    }

    @Override
    protected void onPause() {
        MainService.stop(this);
        super.onPause();
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
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
        if (handler != null) {
            handler.removeCallbacks(confirmAccountShare);
            handler = null;
        }
        if (confirmAccountShare != null) {
            confirmAccountShare = null;
        }

        LLog.d(TAG, "destroyed");
        super.onDestroy();
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
            DBAccount.add(account);
        }
    }

    public static void addCategories() {
        LCategory category = new LCategory();
        for (int ii = 0; ii < categories.length; ii++) {
            category.setName(categories[ii]);
            DBCategory.add(category);
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
            DBVendor.add(vendor);
        }
    }

    private void initDb() {
        if (TextUtils.isEmpty(DBAccount.getNameById(1))) {
            addAccounts();
        }

        if (TextUtils.isEmpty(DBCategory.getNameById(1))) {
            addCategories();
        }
        if (TextUtils.isEmpty(DBVendor.getNameById(1))) {
            addVendors();
        }

        /*if (TextUtils.isEmpty(DBAccess.getTagById(1))) {
            addTags();
        }*/
    }

    private void doOneTimeInit() {
        if (LPreferences.getOneTimeInit()) return;

        initDb();
        DBPorter.restoreUserInfo();

        LPreferences.setOneTimeInit(true);
    }

    private LAccountShareRequest accountShareRequest;

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH:
            case LBroadcastReceiver.ACTION_SERVER_BROADCAST_MSG_RECEIVED:
                handler.post(confirmAccountShare);
                break;
        }
    }

    @Override
    public void onShareAccountConfirmDialogExit(boolean ok, LAccountShareRequest request) {
        if (ok) {
            int userId = request.getUserId();
            String userName = request.getUserName();
            String userFullName = request.getUserFullName();
            String accountName = request.getAccountName();
            String uuid = request.getAccountUuid();

            LPreferences.setShareUserName(userId, userName);
            LPreferences.setShareUserFullName(userId, userFullName);
            LAccount account = DBAccount.getByName(accountName);
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
                account.setState(DBHelper.STATE_ACTIVE);
                account.setRid(uuid);
                account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                DBAccount.update(account);
            }

            // inform all existing peers about this new user
            LProtocol.ui.confirmAccountShare(userId, accountName);

            ArrayList<Integer> ids = account.getShareIds();
            ArrayList<Integer> states = account.getShareStates();
            for (int jj = 0; jj < ids.size(); jj++) {
                if (states.get(jj) == LAccount.ACCOUNT_SHARE_CONFIRMED && ids.get(jj) != userId) {
                    LProtocol.ui.shareAccountUserChange(ids.get(jj), userId, true, account.getName(), account.getRid());
                }
            }

            // now push all existing records
            LJournal.pushAllAccountRecords(userId, account);
        } else {

        }
        shareAccountConfirmDialogOpened = false;
        LPreferences.deleteAccountShareRequest(request);

        handler.post(confirmAccountShare);
    }
}
