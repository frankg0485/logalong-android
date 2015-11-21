package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.text.TextUtils;

import com.swoag.logalong.utils.DBAccess;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TreeMap;

public class LAccountBalance {
    private TreeMap<Integer, double[]> balances;
    private long accountId;
    private long startDate, endDate;
    private double latestBalance;

    public LAccountBalance(long id, int year, String balance) {
        this.accountId = id;
        balances = new TreeMap<Integer, double[]>();
        startDate = Long.MAX_VALUE;
        endDate = 0;

        setYearBalance(year, balance);
    }

    public void setYearBalance(int year, String balance) {
        balances.remove(year);
        balances.put(year, parseBalance(balance));

        double[] doubles = balances.get(year);
        for (int ii = 0; ii < 12; ii++) {

            if (doubles[ii] != 0) {
                long now = getMsOfYearMonth(year, ii, false);
                if (now < startDate) startDate = now;
                now = getMsOfYearMonth(year, ii, true);
                if (now > endDate) endDate = now;
            }
        }
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

        double vOfPremonth = 0;
        for (int ii = 0; ii < 12; ii++) {
            double tmp = balance[ii];
            balance[ii] += value + vOfPremonth;
            vOfPremonth = tmp;
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

    private long getMsOfYearMonth(int year, int month, boolean nextMonth) {
        if (nextMonth) {
            if (month < 11) month++;
            else {
                month = 0;
                year++;
            }
        }

        Calendar now = Calendar.getInstance();
        now.clear();
        now.set(year, month, 1);
        return now.getTimeInMillis();
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
