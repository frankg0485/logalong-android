package com.swoag.logalong;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Build;
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
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBPorter;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LReminderDialog;
import com.swoag.logalong.views.LShareAccountConfirmDialog;
import com.swoag.logalong.views.LViewPager;

import java.util.UUID;

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

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    private boolean userClick = false;

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
        LLog.i(TAG, "VERSION=" + BuildConfig.VERSION_NAME + " DEVICE=" + getDeviceName());
        LViewUtils.screenInit();

        //DEBUG: invalidate user
        //LPreferences.setUserId("");

        handler = new Handler();
        confirmAccountShare = new Runnable() {
            @Override
            public void run() {
                accountShareRequest = LPreferences.getAccountShareRequest();

                if (accountShareRequest != null) {
                    if (LPreferences.getShareAccept(accountShareRequest.getUserId())) {
                        LJournal journal = new LJournal();
                        journal.confirmAccountShare(accountShareRequest.getAccountGid(),
                                accountShareRequest.getUserId(), true);
                        LPreferences.deleteAccountShareRequest(accountShareRequest);

                        handler.post(confirmAccountShare);
                    } else if (!shareAccountConfirmDialogOpened) {
                        shareAccountConfirmDialogOpened = true;

                        LShareAccountConfirmDialog dialog = new LShareAccountConfirmDialog(MainActivity.this,
                                accountShareRequest, MainActivity.this);
                        dialog.show();
                    }
                } else if (!TextUtils.isEmpty(LPreferences.getServerMsg())) {
                    new LReminderDialog(MainActivity.this, LPreferences.getServerMsg()).show();
                    LPreferences.setServerMsg("");
                }
            }
        };

        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_UI_SHARE_ACCOUNT,
                LBroadcastReceiver.ACTION_SERVER_BROADCAST_MSG_RECEIVED
        }, this);

        doOneTimeInit();
        setContentView(R.layout.top);

        fragmentManager = getSupportFragmentManager();
        lPagerAdapter = new LPagerAdapter(fragmentManager);

        mViewPager = (LViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(lPagerAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int outOfBoundCount;
            private int position;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //LLog.d(TAG, "position: " + position + " offset: " + positionOffset + " pixels: " +
                // positionOffsetPixels);
                if (positionOffset == 0 && positionOffsetPixels == 0) {
                    outOfBoundCount++;
                    this.position = position;
                }
            }

            @Override
            public void onPageSelected(int position) {
                //LLog.d(TAG, "selected position: " + position);
                selectTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_IDLE:
                        if (outOfBoundCount > 5 && !userClick) {
                            if (this.position == 0) {
                                //LLog.d(TAG, "Out of boundary on: left");
                                startActivity(new Intent(MainActivity.this, ChartActivity.class));
                            } else if (this.position == 2) {
                                //LLog.d(TAG, "Out of boundary on: right");
                            }
                        }
                        userClick = false;
                        break;
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        outOfBoundCount = 0;
                        break;
                    case ViewPager.SCROLL_STATE_SETTLING:
                        break;
                }
                //LLog.d(TAG, "state changed to: " + state);
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
        if (mViewPager.isPagingEnabled()) {
            mViewPager.setCurrentItem(0);
            userClick = true;
        }
    }

    public void onNewLog(View v) {
        if (mViewPager.isPagingEnabled()) {
            mViewPager.setCurrentItem(1);
            userClick = true;
        }
    }

    public void onAccounts(View v) {
        if (mViewPager.isPagingEnabled()) {
            mViewPager.setCurrentItem(2);
            userClick = true;
        }
    }

    public void onCharts(View v) {
        startActivity(new Intent(MainActivity.this, ChartActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainService.start(this);
    }

    @Override
    protected void onPause() {
        //LLog.d(TAG, "onPause");
        MainService.stop(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        LFragment yf = (LFragment) lPagerAdapter.getItem(mViewPager.getCurrentItem());
        if (yf != null)
            if (yf.onBackPressed()) return;

        if (mViewPager.getCurrentItem() != 1) {
            mViewPager.setCurrentItem(1);
            return;
        }
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
            "Credit:Master",
            "Credit:Visa"
    };

    private static String[] categories = {
            "Clothes",
            "Eating Out",
            "Fuel",
            "Grocery",
            "Health",
            "House:Mortgage",
            "House:Maintenance",
            "Other",
            "Travel"
    };

    /*private static String[] tags = {
            "General"
    };*/

    /*private static String[] vendors = {
            "Amazon",
            "Chipotle"
    };*/

    public static void addAccounts() {
        LAccount account = new LAccount();
        for (int ii = 0; ii < accounts.length; ii++) {
            account.setName(accounts[ii]);
            DBAccount.getInstance().add(account);
        }
    }

    public static void addCategories() {
        LCategory category = new LCategory();
        for (int ii = 0; ii < categories.length; ii++) {
            category.setName(categories[ii]);
            DBCategory.getInstance().add(category);
            //TODO: category.setRid(UUID.randomUUID().toString());
        }
    }

    /*public static void addTags() {
        LTag tag = new LTag();
        for (int ii = 0; ii < tags.length; ii++) {
            tag.setName(tags[ii]);
            DBAccess.addTag(tag);
            tag.setRid(UUID.randomUUID().toString());
        }
    }*/

    /*public static void addVendors() {
        LVendor vendor = new LVendor();
        for (int ii = 0; ii < vendors.length; ii++) {
            vendor.setName(vendors[ii]);
            DBVendor.add(vendor);
            vendor.setRid(UUID.randomUUID().toString());
        }
    }*/

    private void initDb() {
        if (TextUtils.isEmpty(DBAccount.getInstance().getNameById(1))) {
            addAccounts();
        }

        if (TextUtils.isEmpty(DBCategory.getInstance().getNameById(1))) {
            addCategories();
        }
        /*if (TextUtils.isEmpty(DBVendor.getNameById(1))) {
            addVendors();
        }*/

        /*if (TextUtils.isEmpty(DBAccess.getTagById(1))) {
            addTags();
        }*/
    }

    private void doOneTimeInit() {
        AppPersistency.clearViewHistory();
        if (LPreferences.getOneTimeInit()) return;

        if (!DBPorter.restoreDeviceId()) {
            LPreferences.setDeviceId(UUID.randomUUID().toString());
            DBPorter.saveDeviceId();
        }

        //initDb();
        LPreferences.setOneTimeInit(true);
    }

    private LAccountShareRequest accountShareRequest;

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_UI_SHARE_ACCOUNT:
            case LBroadcastReceiver.ACTION_SERVER_BROADCAST_MSG_RECEIVED:
                handler.post(confirmAccountShare);
                break;
        }
    }

    @Override
    public void onShareAccountConfirmDialogExit(boolean ok, LAccountShareRequest request) {
        LJournal journal = new LJournal();
        journal.confirmAccountShare(request.getAccountGid(), request.getUserId(), ok);

        shareAccountConfirmDialogOpened = false;
        LPreferences.deleteAccountShareRequest(request);

        handler.postDelayed(confirmAccountShare, 1500);
    }
}
