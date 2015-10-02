package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.database.Cursor;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LVendor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class DBPorter {
    private static final String TAG = DBPorter.class.getSimpleName();

    public DBPorter() {
    }

    public static void exportDb(String filename) {
        try {
            MyCSV myCSV = new MyCSV();

            Cursor cursor = DBAccess.getAllActiveItemsCursor();
            myCSV.add(DBHelper.TABLE_COLUMN_TYPE + ","
                    + DBHelper.TABLE_COLUMN_AMOUNT + ","
                    + DBHelper.TABLE_COLUMN_TIMESTAMP + ","
                    + DBHelper.TABLE_COLUMN_ACCOUNT + ","
                    + DBHelper.TABLE_COLUMN_CATEGORY + ","
                    + DBHelper.TABLE_COLUMN_VENDOR);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    String account, category, vendor;
                    account = DBAccess.getAccountNameById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT)));
                    category = DBAccess.getCategoryNameById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CATEGORY)));
                    vendor = DBAccess.getVendorNameById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_VENDOR)));

                    String row = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE)) + ","
                            + cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT)) + ","
                            + cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)) + ","
                            + account + ","
                            + category + ","
                            + vendor;
                    myCSV.add(row);
                } while (cursor.moveToNext());
            }

            String fname = LApp.ctx.getFilesDir() + filename;
            FileOutputStream fos = new FileOutputStream(fname);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(myCSV);
            oos.close();
        } catch (Exception e) {
            LLog.e(TAG, "unable to export database to file: " + filename);
        }
    }

    public static void importDb(String filename) {
        try {
            String fname = LApp.ctx.getFilesDir() + filename;
            FileInputStream fis = new FileInputStream(fname);
            ObjectInputStream ois = new ObjectInputStream(fis);
            MyCSV myCSV = (MyCSV) ois.readObject();

            HashMap<String, Long> accountMap = new HashMap<String, Long>();
            HashMap<String, Long> categoryMap = new HashMap<String, Long>();
            HashMap<String, Long> vendorMap = new HashMap<String, Long>();

            boolean header = true;
            for (String str : myCSV.getCsv()) {
                LLog.d(TAG, "" + str);
                if (header) {
                    header = false;
                } else {
                    String[] ss = str.split(",", -1);
                    double amount;
                    int type;
                    long timestamp, accountId, categoryId, vendorId;
                    String account, category, vendor;

                    type = Integer.valueOf(ss[0]);
                    amount = Double.valueOf(ss[1]);
                    timestamp = Long.valueOf(ss[2]);
                    account = ss[3];
                    category = ss[4];
                    vendor = ss[5];

                    Long ll = accountMap.get(account);
                    if (null == ll) {
                        accountId = DBAccess.addAccount(new LAccount(account));
                        accountMap.put(account, accountId);
                    } else {
                        accountId = ll.longValue();
                    }

                    ll = categoryMap.get(category);
                    if (null == ll) {
                        if (category.isEmpty()) categoryId = 0;
                        else {
                            categoryId = DBAccess.addCategory(new LCategory(category));
                            categoryMap.put(category, categoryId);
                        }
                    } else {
                        categoryId = ll.longValue();
                    }

                    ll = vendorMap.get(vendor);
                    if (null == ll) {
                        if (vendor.isEmpty()) vendorId = 0;
                        else {
                            vendorId = DBAccess.addVendor(new LVendor(vendor));
                            vendorMap.put(vendor, vendorId);
                        }
                    } else {
                        vendorId = ll.longValue();
                    }

                    DBAccess.addItem(new LTransaction(amount, type, categoryId, vendorId, 0, accountId, timestamp));
                }
            }

            ois.close();
        } catch (Exception e) {
            LLog.e(TAG, "unable to importport database from file: " + filename);
        }
    }

    private static class MyCSV implements Serializable {
        ArrayList<String> csv;


        public MyCSV() {
            this.csv = new ArrayList<String>();
        }

        public void add(String row) {
            csv.add(row);
        }

        public ArrayList<String> getCsv() {
            return csv;
        }
    }
}
