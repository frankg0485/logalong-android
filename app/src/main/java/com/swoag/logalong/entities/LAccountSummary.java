package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class LAccountSummary {
    private double balance;
    private double expense;
    private double income;
    private String name;
    private long id;

    public LAccountSummary() {
        this.balance = 0;
        this.expense = 0;
        this.income = 0;
        this.id = 0;
        this.name = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getExpense() {
        return expense;
    }

    public void setExpense(double expense) {
        this.expense = expense;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = income;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
