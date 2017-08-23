package com.swoag.logalong.entities;
/* Copyright (C) 2017 SWOAG Technology <www.swoag.com> */

public class LTransactionDetails extends LDbBase {

    private LTransaction transaction;
    private LCategory category;
    private LAccount account;
    private LAccount account2;
    private LTag tag;
    private LVendor vendor;

    public LTransactionDetails() {
        transaction = new LTransaction();
        category = new LCategory();
        account = new LAccount();
        account2 = new LAccount();
        tag = new LTag();
        vendor = new LVendor();
    }

    public LTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(LTransaction transaction) {
        this.transaction = transaction;
    }

    public LCategory getCategory() {
        return category;
    }

    public void setCategory(LCategory category) {
        this.category = category;
    }

    public LAccount getAccount() {
        return account;
    }

    public void setAccount(LAccount account) {
        this.account = account;
    }

    public LAccount getAccount2() {
        return account2;
    }

    public void setAccount2(LAccount account2) {
        this.account2 = account2;
    }

    public LTag getTag() {
        return tag;
    }

    public void setTag(LTag tag) {
        this.tag = tag;
    }

    public LVendor getVendor() {
        return vendor;
    }

    public void setVendor(LVendor vendor) {
        this.vendor = vendor;
    }

    public long getId() {
        return transaction.getId();
    }

    public void setId(long id) {
        transaction.setId(id);
    }

    public long getGid() {
        return transaction.getGid();
    }

    public void setGid(long gid) {
        transaction.setGid(gid);
    }

    public int getState() {
        return transaction.getState();
    }

    public void setState(int state) {
        transaction.setState(state);
    }
}
