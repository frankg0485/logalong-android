package com.swoag.logalong.adapters;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.fragments.SettingsFragment;
import com.swoag.logalong.fragments.NewTransactionFragment;
import com.swoag.logalong.fragments.ViewTransactionFragment;

public class LPagerAdapter extends FragmentStatePagerAdapter {
    private static final String TAG = LPagerAdapter.class.getSimpleName();

    public LPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        Fragment fragment;
        //LLog.d(TAG, "instantiating fragment item: " + i);
        switch (i) {
            case 1:
            default:
                fragment = LFragment.getInstanceOf(NewTransactionFragment.class);
                break;

            case 0:
                fragment = LFragment.getInstanceOf(ViewTransactionFragment.class);
                break;

            case 2:
                fragment = LFragment.getInstanceOf(SettingsFragment.class);
                break;
        }

        //Bundle args = new Bundle();
        //args.putInt(MainFragment.ARG_OBJECT, i + 1);
        //fragment.setArguments(args);
        //LLog.d(TAG, "returned fragment: " + fragment);
        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

	/*@Override
	public CharSequence getPageTitle(int position) {
		return "OBJECT " + (position + 1);
	}*/

    //http://stackoverflow.com/questions/11937622/illegalstateexception-myfragment-is-not-currently-in-the-fragmentmanager
    //The FragmentStatePagerAdapter is a horrible piece of code riddled with bugs acknowledge or not by Google and so I use this code to fix this particular crash:
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // Yet another bug in FragmentStatePagerAdapter that destroyItem is called on fragment that hasnt been added. Need to catch
        try {
            super.destroyItem(container, position, object);
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
        }
    }
}
