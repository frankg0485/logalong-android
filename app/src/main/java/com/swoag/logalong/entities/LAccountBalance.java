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
    double latestBalance;

    public LAccountBalance(long accountId, boolean forceScan) {
        this.balances = new HashMap<Integer, double[]>();
        this.accountId = accountId;
        startDate = Long.MAX_VALUE;
        endDate = 0;
        latestBalance = 0.0;

        LBoxer boxer = new LBoxer();

        if (!forceScan) {
            ArrayList<String> b = DBAccess.getAccountBalance(accountId, boxer);
            for (int ii = 0; ii < b.size() / 2; ii++) {
                int year = Integer.parseInt(b.get(ii));
                balances.put(year, parseBalance(b.get(ii + 1)));
            }
        }
        if (forceScan || balances.size() == 0) {
            scanBalance(boxer);
        }

        if (this.balances.size() <= 0) return;
        latestBalance = balances.get(boxer.y)[11];

        // set start end date if there's balance
        Calendar now = Calendar.getInstance();
        now.clear();
        double[] bal = balances.get(boxer.x);
        int ii;
        for (ii = 0; ii < 12; ii++) {
            if (bal[ii] != 0) break;
        }
        if (ii == 12) ii = 11;
        now.set(boxer.x, ii, 1);
        startDate = now.getTimeInMillis();

        now.clear();
        bal = balances.get(boxer.y);
        double lastV = bal[11];
        for (ii = 10; ii >= 0; ii--) {
            if (bal[ii] != lastV) {
                ii++;
                break;
            }
        }
        if (ii == -1) ii = 0;
        now.set(boxer.y, ii, 1);
        endDate = now.getTimeInMillis();
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public HashMap<Integer, double[]> getBalances() {
        return balances;
    }

    /*public void setBalances(HashMap<Integer, double[]> balances) {
        this.balances = balances;
    }*/

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
            // this could be null due to,
            // - there's simply no transaction available for this account
            // - requested year is beyond this account's start/end point
            // regardless of what happens, we'll return a valid year balance array
            // however, do NOT update database here, as what's in database always correponds
            // to actual start/end date.

            balance = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            if (balances.size() > 0) {
                for (int ii = 0; ii < 12; ii++) {
                    balance[ii] = latestBalance;
                }
            }
        }
        return balance;
    }

    // TBD: update to fill all holes, update latestBalance, endDate
    /*
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
    */

    // heavy API, use with caution
    private boolean scanBalance(LBoxer boxer) {
        balances = DBAccess.scanAccountBalanceById(accountId, boxer);

        // update database
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
