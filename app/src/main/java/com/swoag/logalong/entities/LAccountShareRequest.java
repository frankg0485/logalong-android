package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.views.LShareAccountConfirmDialog;

public class LAccountShareRequest {
    private int userId;
    private String userName;
    private String userFullName;
    private String accountName;
    private String accountUuid;

    public LAccountShareRequest(int userId, String userName, String userFullName, String accountName, String accountUuid) {
        this.userId = userId;
        this.userName = userName;
        this.userFullName = userFullName;
        this.accountName = accountName;
        this.accountUuid = accountUuid;
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

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
