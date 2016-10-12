package com.swoag.logalong.entities;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

public class LAccountShareRequest {
    private int userId;
    private String userName;
    private String userFullName;
    private String accountName;
    private int accountGid;
    private int shareAccountGid;

    public LAccountShareRequest(int userId, String userName, String userFullName, String accountName, int accountGid, int shareAccountGid) {
        this.userId = userId;
        this.userName = userName;
        this.userFullName = userFullName;
        this.accountName = accountName;
        this.accountGid = accountGid;
        this.shareAccountGid = shareAccountGid;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public int getAccountGid() {
        return accountGid;
    }

    public void setAccountUuid(int accountGid) {
        this.accountGid = accountGid;
    }

    public int getShareAccountGid() {
        return shareAccountGid;
    }

    public void setShareAccountGid(int shareAccountGid) {
        this.shareAccountGid = shareAccountGid;
    }
}
