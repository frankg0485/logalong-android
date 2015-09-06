package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.database.Cursor;

import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBHelper;

import java.util.HashMap;
import java.util.HashSet;

public class LAllBalances {
    HashSet<Long> ids;
    HashMap<Long, LAccountBalance> balances;
    private static LAllBalances instance;

    public static LAllBalances getInstance() {
        return getInstance(false);
    }

    public static LAllBalances getInstance(boolean forceScan) {
        if (instance == null || forceScan) {
            instance = new LAllBalances(forceScan);
        }

        return instance;
    }

    private void getAllAccountIds() {
        ids = new HashSet<Long>();

        Cursor cursor = DBAccess.getAllAccountsCursor();
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                ids.add(cursor.getLong(0));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
    }

    private LAllBalances(boolean forceScan) {
        getAllAccountIds();

        balances = new HashMap<Long, LAccountBalance>();
        for (long id : ids) {
            LAccountBalance balance = new LAccountBalance(id);
            if (forceScan) balance.scanBalance();
            balances.put(id, balance);
        }
    }
}
