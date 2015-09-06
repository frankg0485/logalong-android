package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.text.TextUtils;

import com.swoag.logalong.utils.DBAccess;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class LAccountBalance {
    HashMap<Integer, double[]> balances;
    long accountId;
    long startDate, endDate;

    public LAccountBalance(long accountId) {
        this.balances = new HashMap<Integer, double[]>();
        this.accountId = accountId;
        startDate = Long.MAX_VALUE;
        endDate = 0;

        ArrayList<String> b = DBAccess.getAccountBalance(accountId);
        for (int ii = 0; ii < b.size() / 2; ii++) {
            int year = Integer.parseInt(b.get(ii));
            balances.put(year, parseBalance(b.get(ii + 1)));
        }
        if (balances.size() == 0) {
            scanBalance();
        }
        // set start end date
        for (Integer year : balances.keySet()) {
            Calendar now = Calendar.getInstance();
            now.clear();
            double[] bal = balances.get(year);
            int ii;
            boolean empty = true;
            for (ii = 0; ii < 12; ii++) {
                if (bal[ii] != 0) {
                    empty = false;
                    break;
                }
            }
            if (ii == 12) ii = 11;

            if (!empty) {
                now.set(year, ii, 1);
                long ms = now.getTimeInMillis();
                now.clear();
                if (ii == 11) {
                    ii = 0;
                    year++;
                } else ii++;
                now.set(year, ii, 1);
                long me = now.getTimeInMillis();

                if (startDate > ms) startDate = ms;
                if (endDate < me) endDate = me;
            }
        }
    }

    private double[] parseBalance(String str) {
        double[] balance = new double[12];
        try {
            String[] sb = str.split(",");
            for (int ii = 0; ii < 12; ii++) {
                balance[ii] = Double.parseDouble(sb[ii]);
            }
        } catch (Exception e) {
        }
        return balance;
    }

    public double[] getYearBalance(int year) {
        double[] balance = balances.get(year);

        if (null == balance) {
            String bs = DBAccess.getAccountBalance(accountId, year);
            balance = parseBalance(bs);
        }
        return balance;
    }

    public void setYearBalance(int year, double[] balance) {
        balances.remove(year);
        balances.put(year, balance);
        String str = "";
        for (int ii = 0; ii < balance.length - 1; ii++) {
            str += String.valueOf(balance[ii]) + ",";
        }
        str += String.valueOf(balance[balance.length - 1]);
        DBAccess.updateAccountBalance(accountId, year, str);
    }

    // heavy API, use with caution
    public boolean scanBalance() {
        balances = DBAccess.scanAccountById(accountId);

        // add empty entry for current year if none is present
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        if (balances.get(year) == null) {
            double[] balance = new double[]{
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            };
            balances.put(year, balance);
        }

        // update DB
        for (Integer yr : balances.keySet()) {
            double[] bal = balances.get(yr);
            String str = "";
            for (int ii = 0; ii < bal.length - 1; ii++) {
                str += String.valueOf(bal[ii]) + ",";
            }
            str += String.valueOf(bal[bal.length - 1]);
            DBAccess.updateAccountBalance(accountId, yr, str);
        }

        return true;
    }

    public long getStartDate() {
        return startDate;
    }

    public long getEndDate() {
        return endDate;
    }
}
