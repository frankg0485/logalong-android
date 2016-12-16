package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.swoag.logalong.utils.LLog;

import java.util.HashMap;

public class LFragment extends Fragment {
    private static final String TAG = LFragment.class.getSimpleName();
    public static final int SCREEN_ACTIVE = 100;
    public static final int SCREEN_TIMEOUT = 200;
    public static final int SCREEN_TIMEOUT_FULLSCREEN = 300;
    public int mInstanceId;

    private static HashMap<String, LFragment> map = new HashMap<String, LFragment>();

    public static Fragment getInstanceOf(Class<?> klass) {
        LFragment frag = map.get(klass.getName() + 0);
        if (frag == null) {
            try {
                //LLog.d(TAG, "new instance: " + klass);
                frag = (LFragment) klass.newInstance();
                frag.mInstanceId = 0;
                putInstanceOf(klass, 0, frag);
            } catch (Exception e) {
                LLog.e(TAG, "unable to instantiate: " + klass + "@" + e.getMessage());
            }
        }
        //LLog.d(TAG, "get instance: " + klass + "@" + frag);
        return frag;
    }

    public static Fragment getInstanceOf(Class<?> klass, int id) {
        LFragment frag = map.get(klass.getName() + id);
        if (frag == null) {
            try {
                //LLog.d(TAG, "new instance: " + klass + "@" + id);
                frag = (LFragment) klass.newInstance();
                frag.mInstanceId = id;
                putInstanceOf(klass, id, frag);
            } catch (Exception e) {
                LLog.e(TAG, "unable to instantiate: " + klass + "@" + e.getMessage());
            }
        }
        //LLog.d(TAG, "get instance: " + klass + "@" + frag);
        return frag;
    }

    private static void putInstanceOf(Class<?> klass, int id, LFragment obj) {
        //LLog.d(TAG, "put instance: " + klass + "@" + obj);
        map.put(klass.getName() + id, obj);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mInstanceId = savedInstanceState.getInt("instanceId");
        }
        putInstanceOf(this.getClass(), mInstanceId, this);
        //LLog.d(TAG, "class: " + this.getClass().getSimpleName());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        putInstanceOf(this.getClass(), mInstanceId, null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("instanceId", mInstanceId);
        super.onSaveInstanceState(outState);
    }

    public boolean gotoSleep(int mode) {
        return false;
    }

    public void onSelected(boolean selected) {
    }

    public boolean onBackPressed() {
        return false;
    }

    //	@Override
    //	public int describeContents() {
    //		return 0;
    //	}
    //
    //	@Override
    //	public void writeToParcel(Parcel dest, int flags) {
    //	}
}
