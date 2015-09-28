package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import java.util.ArrayList;
import java.util.HashSet;

public class LAccount {
    private static final String TAG = LAccount.class.getSimpleName();

    public static final int ACCOUNT_STATE_ACTIVE = 10;
    public static final int ACCOUNT_STATE_DELETED = 20;

    public static final int ACCOUNT_SHARE_NA = 0;
    public static final int ACCOUNT_SHARE_INVITED = 1;
    public static final int ACCOUNT_SHARE_CONFIRMED = 2;
    public static final int ACCOUNT_SHARE_DECLINED = 3;

    //added to local database, but not yet invited, this is for GUI update only
    public static final int ACCOUNT_SHARE_PREPARED = 4;

    int state;
    String name;
    ArrayList<Integer> shareIds;
    ArrayList<Integer> shareStates;
    long id;

    public LAccount() {
        this.state = ACCOUNT_STATE_ACTIVE;
        this.name = "";
        this.shareIds = new ArrayList<Integer>();
        this.shareStates = new ArrayList<Integer>();
    }

    public LAccount(String name) {
        this.state = ACCOUNT_STATE_ACTIVE;
        this.name = name;
        this.shareIds = new ArrayList<Integer>();
        this.shareStates = new ArrayList<Integer>();
    }

    public LAccount(int state, String name) {
        this.state = state;
        this.name = name;
        this.shareIds = new ArrayList<Integer>();
        this.shareStates = new ArrayList<Integer>();
    }

    public LAccount(int state, String name, ArrayList<Integer> shareIds, ArrayList<Integer> shareStates) {
        this.state = state;
        this.name = name;
        this.shareIds = shareIds;
        this.shareStates = shareStates;
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
        for (int ii = 0; ii < shareIds.size() - 1; ii++) {
            str += String.valueOf(shareStates.get(ii)) + ",";
            str += String.valueOf(shareIds.get(ii)) + ",";
        }
        str += String.valueOf(shareStates.get(shareIds.size() - 1)) + ",";
        str += String.valueOf(shareIds.get(shareIds.size() - 1));
        return str;
    }

    public void setSharedIdsString(String str) {
        if (!str.isEmpty()) {
            String[] sb = str.split(",");
            shareIds.clear();
            shareStates.clear();
            for (int ii = 0; ii < sb.length / 2; ii++) {
                shareStates.add(Integer.parseInt(sb[2 * ii]));
                shareIds.add(Integer.parseInt(sb[2 * ii + 1]));
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
}
