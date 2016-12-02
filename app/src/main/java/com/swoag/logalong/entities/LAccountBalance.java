package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import java.util.TreeMap;

public class LAccountBalance {
    private TreeMap<Integer, double[]> balances;
    private long accountId;
    private double latestBalance;

    private long id;
    private int state;
    private int year;
    private String balance;
    private long lastChangeTimestamp;

    public LAccountBalance() {
    }

    public LAccountBalance(long id, int year, String balance, long lastChangeTimestamp) {
        this.accountId = id;
        this.year = year;
        this.balance = balance;
        this.lastChangeTimestamp = lastChangeTimestamp;

        balances = new TreeMap<Integer, double[]>();
        setYearBalance(year, balance);
    }

    public void setBalanceValues(double[] balances) {
        balance = "";
        for (int ii = 0; ii < balances.length - 1; ii++) {
            balance += String.valueOf(balances[ii]) + ",";
        }
        balance += String.valueOf(balances[balances.length - 1]);
    }

    public int getYear() {
        return year;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getBalance() {
        return balance;
    }

    public long getLastChangeTimestamp() {
        return lastChangeTimestamp;
    }

    public int getState() {
        return state;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public void setLastChangeTimestamp(long lastChangeTimestamp) {
        this.lastChangeTimestamp = lastChangeTimestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public LAccountBalance(long id, int year, String balance) {
        this.accountId = id;
        balances = new TreeMap<Integer, double[]>();
        setYearBalance(year, balance);
    }

    public void setYearBalance(int year, String balance) {
        balances.remove(year);
        balances.put(year, parseBalance(balance));
    }

    public void modify(int year, int month, double amount) {
        balances.get(year)[month] += amount;
    }

    public String getYearBalanceString(int year) {
        String str = "";
        double bal[] = balances.get(year);
        for (int ii = 0; ii < bal.length - 1; ii++) {
            str += String.valueOf(bal[ii]) + ",";
        }
        str += String.valueOf(bal[bal.length - 1]);
        return str;
    }

    public double[] getYearBalanceAccumulated(int year) {
        double value = 0;
        double[] balance = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (int y : balances.keySet()) {
            double[] bal = balances.get(y);
            if (y < year) {
                for (int ii = 0; ii < 12; ii++) value += bal[ii];
            } else {
                if (y == year) {
                    for (int ii = 0; ii < 12; ii++) balance[ii] = bal[ii];
                }
                break;
            }
        }

        double vOfNow = 0;
        for (int ii = 0; ii < 12; ii++) {
            vOfNow += balance[ii];
            balance[ii] = value + vOfNow;
        }

        return balance;
    }

    public double getYearBalanceAccumulated() {
        double value = 0;
        for (int y : balances.keySet()) {
            double[] bal = balances.get(y);
            for (int ii = 0; ii < 12; ii++) value += bal[ii];
        }

        return value;
    }

    private double[] parseBalance(String str) {
        double[] balance = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        if (str != null) {
            try {
                String[] sb = str.split(",");
                for (int ii = 0; ii < 12; ii++) {
                    balance[ii] = Double.parseDouble(sb[ii]);
                }
            } catch (Exception e) {
            }
        }
        return balance;
    }
}
