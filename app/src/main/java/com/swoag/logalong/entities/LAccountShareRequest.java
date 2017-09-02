package com.swoag.logalong.entities;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

public class LAccountShareRequest {
    private long userId;
    private String userName;
    private String userFullName;
    private String accountName;
    private long accountGid;

    public LAccountShareRequest(long userId, String userName, String userFullName, String accountName, long
            accountGid) {
        this.userId = userId;
        this.userName = userName;
        this.userFullName = userFullName;
        this.accountName = accountName;
        this.accountGid = accountGid;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
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

    public long getAccountGid() {
        return accountGid;
    }

    public void setAccountGid(long accountGid) {
        this.accountGid = accountGid;
    }
}
