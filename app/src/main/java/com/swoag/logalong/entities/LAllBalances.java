package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.database.Cursor;

import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBHelper;

import java.util.HashMap;
import java.util.HashSet;

public class LAllBalances {
    private HashSet<Long> ids;
    private HashMap<Long, LAccountBalance> balances;

    public LAllBalances(Cursor cursor) {
        balances = new HashMap<Long, LAccountBalance>();
        ids = new HashSet<Long>();

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                long account = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT));
                int year = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_YEAR));
                String balance = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_BALANCE));
                LAccountBalance accountBalance = balances.get(account);
                if (accountBalance == null) {
                    accountBalance = new LAccountBalance(account, year, balance);
                    balances.put(account, accountBalance);
                    ids.add(account);
                } else {
                    accountBalance.setYearBalance(year, balance);
                }
            } while (cursor.moveToNext());
        }
    }

    public double getBalance(int year, int month) {
        double val = 0;
        if (ids != null) {
            for (long id : ids) {
                LAccountBalance balance = balances.get(id);
                if (balance != null) {
                    double[] b = balance.getYearBalanceAccumulated(year);
                    val += b[month];
                }
            }
        }
        return val;
    }

    public double getBalance(long accountId, int year, int month) {
        LAccountBalance balance = balances.get(accountId);
        if (null == balance) return 0;

        double[] b = balance.getYearBalanceAccumulated(year);
        return b[month];
    }

    public double getBalance() {
        double val = 0;
        if (ids != null) {
            for (long id : ids) {
                LAccountBalance balance = balances.get(id);
                if (balance != null) {
                    val += balance.getYearBalanceAccumulated();
                }
            }
        }
        return val;
    }

    public double getBalance(long accountId) {
        LAccountBalance balance = balances.get(accountId);
        if (null == balance) return 0;

        return balance.getYearBalanceAccumulated();
    }
}
