package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.text.TextUtils;

import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.LPreferences;

import java.util.ArrayList;
import java.util.UUID;

public class LAccount {
    private static final String TAG = LAccount.class.getSimpleName();

    public static final int ACCOUNT_SHARE_NA = 0;
    public static final int ACCOUNT_SHARE_INVITED = 1;
    public static final int ACCOUNT_SHARE_CONFIRMED = 2;
    public static final int ACCOUNT_SHARE_DECLINED = 3;
    public static final int ACCOUNT_SHARE_CONFIRMED_SYNCED = 4;

    //added to local database, but not yet invited, this is for GUI update only
    public static final int ACCOUNT_SHARE_PREPARED = 5;

    private int state;
    private String name;
    private String rid;
    private int gid;

    private long timeStampLast;
    private long shareTimeStampLast;

    private ArrayList<Integer> shareIds;
    private ArrayList<Integer> shareStates;
    private long id;

    private void init() {
        this.state = DBHelper.STATE_ACTIVE;
        this.timeStampLast = LPreferences.getServerUtc();
        this.shareTimeStampLast = 0;
        this.rid = UUID.randomUUID().toString();
        this.name = "";
        this.shareIds = new ArrayList<Integer>();
        this.shareStates = new ArrayList<Integer>();
    }

    public LAccount() {
        init();
    }

    public LAccount(String name) {
        init();
        this.name = name;
    }

    public LAccount(String name, String rid) {
        init();
        this.name = name;
        this.rid = rid;
    }

    public LAccount(int state, String name) {
        init();
        this.state = state;
        this.name = name;
    }

    public LAccount(int state, String name, ArrayList<Integer> shareIds, ArrayList<Integer> shareStates) {
        init();
        this.state = state;
        this.name = name;
        this.shareIds = shareIds;
        this.shareStates = shareStates;
    }

    public boolean isAnySharePending() {
        if (shareIds == null || shareStates == null || shareIds.size() < 1 || shareStates.size() < 1)
            return false;
        for (int ii = 0; ii < shareStates.size(); ii++) {
            if (shareStates.get(ii) == ACCOUNT_SHARE_INVITED) {
                return true;
            }
        }
        return false;
    }

    public boolean isShareConfirmed() {
        if (shareIds == null || shareStates == null || shareIds.size() < 1 || shareStates.size() < 1)
            return false;
        for (int ii = 0; ii < shareStates.size(); ii++) {
            if (shareStates.get(ii) == ACCOUNT_SHARE_CONFIRMED_SYNCED) {
                return true;
            }
        }
        return false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public ArrayList<Integer> getShareIds() {
        return shareIds;
    }

    public void setShareIds(ArrayList<Integer> shareIds) {
        this.shareIds = shareIds;
    }

    public ArrayList<Integer> getShareStates() {
        return shareStates;
    }

    public void setShareStates(ArrayList<Integer> shareStates) {
        this.shareStates = shareStates;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShareIdsString() {
        if (shareIds == null || shareStates == null || shareIds.size() < 1 || shareStates.size() < 1)
            return "";
        String str = "";
        for (int ii = 0; ii < shareIds.size(); ii++) {
            str += String.valueOf(shareStates.get(ii)) + ",";
            str += String.valueOf(shareIds.get(ii)) + ",";
        }
        str += String.valueOf(shareTimeStampLast);
        return str;
    }

    public void setSharedIdsString(String str) {
        if (!TextUtils.isEmpty(str)) {
            String[] sb = str.split(",");
            shareIds.clear();
            shareStates.clear();
            for (int ii = 0; ii < sb.length / 2; ii++) {
                shareStates.add(Integer.parseInt(sb[2 * ii]));
                shareIds.add(Integer.parseInt(sb[2 * ii + 1]));
            }
            if ((sb.length % 2) == 0) {
                shareTimeStampLast = 0;
            } else {
                shareTimeStampLast = Long.parseLong(sb[sb.length - 1]);
            }
        }
    }

    public void addShareUser(int id, int state) {
        if (shareIds == null || shareStates == null) {
            shareIds = new ArrayList<Integer>();
            shareStates = new ArrayList<Integer>();
        }

        for (int ii = 0; ii < shareIds.size(); ii++) {
            if (shareIds.get(ii) == id) {
                shareStates.set(ii, state);
                return;
            }
        }

        shareStates.add(state);
        shareIds.add(id);
    }

    public void removeShareUser(int id) {
        if (shareIds == null || shareStates == null) {
            return;
        }

        for (int ii = 0; ii < shareIds.size(); ii++) {
            if (shareIds.get(ii) == id) {
                shareIds.remove(ii);
                shareStates.remove(ii);
            }
        }
    }

    public void removeAllShareUsers() {
        shareIds.clear();
        shareStates.clear();
    }

    public int getShareUserState(int id) {
        if (shareIds == null || shareStates == null) {
            return ACCOUNT_SHARE_NA;
        }
        for (int ii = 0; ii < shareIds.size(); ii++) {
            if (shareIds.get(ii) == id) {
                return shareStates.get(ii);
            }
        }
        return ACCOUNT_SHARE_NA;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public long getTimeStampLast() {
        return timeStampLast;
    }

    public void setTimeStampLast(long timeStampLast) {
        this.timeStampLast = timeStampLast;
    }

    public long getShareTimeStampLast() {
        return shareTimeStampLast;
    }

    public void setShareTimeStampLast(long shareTimeStampLast) {
        this.shareTimeStampLast = shareTimeStampLast;
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }
}
