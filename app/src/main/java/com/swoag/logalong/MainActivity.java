package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.swoag.logalong.adapters.LPagerAdapter;
import com.swoag.logalong.com.swoag.logalong.views.LViewPager;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LViewUtils;

public class MainActivity extends LFragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    FragmentManager fragmentManager;
    LPagerAdapter lPagerAdapter;
    LViewPager mViewPager;
    View footerV;
    View spinner;
    TextView errorMsgV;
    //LoginReceiverHelper loginReceiver;

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
    protected void onDestroy() {
        LLog.d(TAG, "destroyed");
        /*if (loginReceiver != null) {
            loginReceiver.unregister();
			loginReceiver = null;
		}
		LNotification.dismiss();*/
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
            "Kids",
            "Sports",
            "Travel",
            "General"
    };

    /*private static String[] tags = {
            "General"
    };*/

    private static String[] vendors = {
            "Chiptole",
            "Corner Bakery",
            "JCPenny",
            "Jimmy Johns",
            "McDonalds",
            "Panera"
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
        if (DBAccess.getAccountById(1).isEmpty()) {
            addAccounts();
        }

        if (DBAccess.getCategoryById(1).isEmpty()) {
            addCategories();
        }
        if (DBAccess.getVendorById(1).isEmpty()) {
            addVendors();
        }

        /*if (DBAccess.getTagById(1).isEmpty()) {
            addTags();
        }*/
    }

    private void doOneTimeInit() {
        initDb();
        /*if (LPreferences.getOneTimeInit()) return;
        LPreferences.setOneTimeInit(true);*/
    }
}
