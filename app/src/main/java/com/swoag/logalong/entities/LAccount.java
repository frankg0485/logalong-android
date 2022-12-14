package com.swoag.logalong.entities;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.text.TextUtils;

import com.swoag.logalong.utils.LPreferences;

import java.util.ArrayList;

public class LAccount extends LDbBase {
    public static final int ACCOUNT_SHARE_PERMISSION_READ_ONLY   = 0x01;
    public static final int ACCOUNT_SHARE_PERMISSION_READ_WRITE  = 0x03;
    public static final int ACCOUNT_SHARE_PERMISSION_OWNER       = 0x08;
    public static final int ACCOUNT_SHARE_INVITED = 0x10;
    public static final int ACCOUNT_SHARE_NA = 0x20;

    private String rid;
    private boolean showBalance;

    private long shareTimeStampLast;

    private ArrayList<Long> shareIds;
    private ArrayList<Integer> shareStates;
    private long id;

    private void init() {
        this.shareTimeStampLast = 0;
        this.showBalance = true;
        this.shareIds = new ArrayList<Long>();
        this.shareStates = new ArrayList<Integer>();
    }

    public LAccount() {
        super();
        init();
    }

    public LAccount(String name) {
        super(name);
        init();
        this.name = name;
    }

    public LAccount(int state, String name) {
        super(state, name);
        init();
    }

    public LAccount(int state, String name, ArrayList<Long> shareIds, ArrayList<Integer> shareStates) {
        super(state, name);
        init();
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
            if (shareIds.get(ii) == LPreferences.getUserIdNum()) continue;
            if (shareStates.get(ii) <= ACCOUNT_SHARE_PERMISSION_OWNER) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Long> getShareIds() {
        return shareIds;
    }

    public void setShareIds(ArrayList<Long> shareIds) {
        this.shareIds = shareIds;
    }

    public ArrayList<Integer> getShareStates() {
        return shareStates;
    }

    public void setShareStates(ArrayList<Integer> shareStates) {
        this.shareStates = shareStates;
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
                shareIds.add(Long.parseLong(sb[2 * ii + 1]));
            }
            if ((sb.length % 2) == 0) {
                shareTimeStampLast = 0;
            } else {
                shareTimeStampLast = Long.parseLong(sb[sb.length - 1]);
            }
        }
    }

    public long getOwner() {
        if (shareIds == null || shareStates == null) return LPreferences.getUserIdNum();

        for (int ii = 0; ii < shareStates.size(); ii++) {
            if (shareStates.get(ii) == ACCOUNT_SHARE_PERMISSION_OWNER) {
                return shareIds.get(ii);
            }
        }
        return LPreferences.getUserIdNum();
    }

    public void setOwner(long id) {
        addShareUser(id, ACCOUNT_SHARE_PERMISSION_OWNER);
    }

    public void addShareUser(long id, int state) {
        if (shareIds == null || shareStates == null) {
            shareIds = new ArrayList<Long>();
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

    public void removeShareUser(long id) {
        if (shareIds == null || shareStates == null) {
            return;
        }

        for (int ii = 0; ii < shareIds.size(); ii++) {
            if (shareIds.get(ii) == id) {
                shareIds.remove(ii);
                shareStates.remove(ii);
                ii--;
            }
        }
    }

    public void removeAllShareUsers() {
        shareIds.clear();
        shareStates.clear();
    }

    public int getShareUserState(long id) {
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

    public long getShareTimeStampLast() {
        return shareTimeStampLast;
    }

    public void setShareTimeStampLast(long shareTimeStampLast) {
        this.shareTimeStampLast = shareTimeStampLast;
    }

    public boolean isShowBalance() {
        return showBalance;
    }

    public void setShowBalance(boolean showBalance) {
        this.showBalance = showBalance;
    }
}
