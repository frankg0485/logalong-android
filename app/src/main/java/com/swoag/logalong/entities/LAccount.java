package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class LAccount {
    private static final String TAG = LAccount.class.getSimpleName();

    public static final int ACCOUNT_STATE_ACTIVE = 10;
    public static final int ACCOUNT_STATE_DELETED = 20;

    int state;
    String name;
    int shareIds[];

    public LAccount() {
        this.state = ACCOUNT_STATE_ACTIVE;
        this.name = "";
        this.shareIds = null;
    }

    public LAccount(String name) {
        this.state = ACCOUNT_STATE_ACTIVE;
        this.name = name;
        this.shareIds = null;
    }

    public LAccount(int state, String name) {
        this.state = state;
        this.name = name;
        this.shareIds = null;
    }

    public LAccount(int state, String name, int[] shareIds) {
        this.state = state;
        this.name = name;
        this.shareIds = shareIds;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public int[] getShareIds() {
        return shareIds;
    }

    public String getShareIdsString() {
        if (shareIds == null) return "";
        String str = "";
        for (int ii = 0; ii < shareIds.length - 1; ii++) {
            str += String.valueOf(shareIds[ii]) + ",";
        }
        str += String.valueOf(shareIds[shareIds.length - 1]);
        return str;
    }

    public void setShareIds(int[] shareIds) {
        this.shareIds = shareIds;
    }

    public void setSharedIdsString(String str) {
        if (!str.isEmpty()) {
            String[] sb = str.split(",");
            shareIds = new int[sb.length];
            for (int ii = 0; ii < sb.length; ii++) {
                shareIds[ii] = Integer.parseInt(sb[ii]);
            }
        }
    }

    public void setName(String name) {
        this.name = name;
    }
}
