package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.database.Cursor;
import android.text.TextUtils;

import com.swoag.logalong.LApp;
import com.swoag.logalong.MainService;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LScheduledTransaction;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LVendor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class DBPorter {
    private static final String TAG = DBPorter.class.getSimpleName();

    public DBPorter() {
    }

    private static String exportTransactionItem(Cursor cursor) {
        LAccount lAccount = DBAccount.getById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT)));
        LAccount lAccount2 = DBAccount.getById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT2)));
        LCategory lCategory = DBCategory.getById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CATEGORY)));
        LVendor lVendor = DBVendor.getById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_VENDOR)));
        LTag lTag = DBTag.getById(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TAG)));

        String row = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT)) + ",";

        if (lCategory != null)
            row += lCategory.getName() + "," + lCategory.getGid() + ",";
        else
            row += "," + ",";

        if (lAccount != null)
            row += lAccount.getName() + "," + lAccount.getRid() + ",";
        else
            row += "," + ",";

        if (lAccount2 != null)
            row += lAccount2.getName() + "," + lAccount2.getRid() + ",";
        else
            row += "," + ",";

        if (lTag != null)
            row += lTag.getName() + "," + lTag.getGid() + ",";
        else
            row += "," + ",";

        if (lVendor != null)
            row += lVendor.getName() + "," + lVendor.getGid() + "," + lVendor.getType() + ",";
        else
            row += "," + "," + ",";

        row += cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP)) + ","
                + cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE)) + ","
                + cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE)) + ","
                + cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE)) + ","
                + cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_MADEBY)) + ","
                + cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID)) + ","
                + cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NOTE));
        return row;
    }

    private static void exportSchedules(MyCSV myCSV) {
        myCSV.add("---");
        Cursor cursor = DBScheduledTransaction.getCursor(null);

        if (cursor == null || cursor.getCount() < 1) return;

        cursor.moveToFirst();
        do {
            String row = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_REPEAT_UNIT)) + ",";
            row += cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_REPEAT_INTERVAL)) + ",";
            row += cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_REPEAT_COUNT)) + ",";

            row += exportTransactionItem(cursor);
            myCSV.add(row);
        } while (cursor.moveToNext());

        cursor.close();
    }

    public static boolean exportDb(int dbVersion) {
        if (!LStorage.isExternalStorageWritable()) return false;

        try {
            File path = openDbDir();
            if (path == null) return false;
            File oldFile = getOldFile(path);

            MyCSV myCSV = new MyCSV();

            Cursor cursor = DBTransaction.getAllCursor();
            if (cursor == null || cursor.getCount() < 1) {
                if (cursor != null) cursor.close();
                return false;
            }

            //myCSV.add(LPreferences.getUserId() + "," + LPreferences.getUserName() + "," + LPreferences.getUserFullName());
            myCSV.add(DBHelper.TABLE_COLUMN_AMOUNT + "," +
                    DBHelper.TABLE_COLUMN_CATEGORY + "," + DBHelper.TABLE_COLUMN_RID + "," +
                    DBHelper.TABLE_COLUMN_ACCOUNT + "," + DBHelper.TABLE_COLUMN_RID + "," +
                    DBHelper.TABLE_COLUMN_ACCOUNT2 + "," + DBHelper.TABLE_COLUMN_RID + "," +
                    DBHelper.TABLE_COLUMN_TAG + "," + DBHelper.TABLE_COLUMN_RID + "," +
                    DBHelper.TABLE_COLUMN_VENDOR + "," + DBHelper.TABLE_COLUMN_RID + "," +
                    DBHelper.TABLE_COLUMN_TIMESTAMP + "," +
                    DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE + "," +
                    DBHelper.TABLE_COLUMN_TYPE + "," +
                    DBHelper.TABLE_COLUMN_STATE + "," +
                    DBHelper.TABLE_COLUMN_MADEBY + "," +
                    DBHelper.TABLE_COLUMN_RID + "," +
                    DBHelper.TABLE_COLUMN_NOTE);

            cursor.moveToFirst();
            do {
                myCSV.add(exportTransactionItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();

            exportSchedules(myCSV);

            File file = new File(path, generateFilename(dbVersion));
            FileOutputStream fos = new FileOutputStream(file);

            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(myCSV);
            oos.close();

            if (oldFile != null) {
                oldFile.delete();
            }
        } catch (Exception e) {
            LLog.e(TAG, "unable to export database version: " + dbVersion);
            return false;
        }
        return true;
    }

    private static File openCacheDir() {
        return LStorage.openDir("cache");
    }

    public static boolean saveDeviceId() {
        if (!LStorage.isExternalStorageWritable()) return false;

        try {
            File path = openCacheDir();
            if (path == null) return false;

            File file = new File(path, "device.id");
            if (file != null) {
                file.delete();
            }
            file = new File(path, "device.id");

            MyCSV myCSV = new MyCSV();
            myCSV.add(LPreferences.getDeviceId() + "," + System.currentTimeMillis());

            FileOutputStream fos = new FileOutputStream(file);

            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(myCSV);
            oos.close();
        } catch (Exception e) {
            LLog.e(TAG, "unable to export device ID");
            return false;
        }
        return true;
    }

    public static boolean restoreDeviceId() {
        try {
            File path = openCacheDir();
            if (path == null) return false;
            File file = new File(path, "device.id");
            if (file == null) return false;

            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            MyCSV myCSV = (MyCSV) ois.readObject();

            for (String str : myCSV.getCsv()) {
                String[] ss = str.split(",", -1);
                LPreferences.setDeviceId(ss[0]);
                //TODO: timestamp support
                break;
            }

            ois.close();
        } catch (Exception e) {
            LLog.e(TAG, "unable to restore user info");
            return false;
        }
        return true;
    }

    public static boolean importDb(int dbVersion) {
        boolean ret = false;
        try {
            File path = openDbDir();
            if (path == null) return false;
            File file = getOldFile(path);
            if (file == null) return false;

            //TODO: handle DB version mismatch
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            MyCSV myCSV = (MyCSV) ois.readObject();

            HashMap<String, Long> accountMap = new HashMap<String, Long>();
            HashMap<String, Long> categoryMap = new HashMap<String, Long>();
            HashMap<String, Long> vendorMap = new HashMap<String, Long>();
            HashMap<String, Long> tagMap = new HashMap<String, Long>();

            boolean header = true;
            boolean userInfo = true;
            boolean schedule = false;
            for (String str : myCSV.getCsv()) {
                LLog.d(TAG, "" + str);

                if (str.contentEquals("---")) {
                    schedule = true;
                    continue;
                }

                String[] ss = str.split(",", -1);
                if (userInfo) {
                    userInfo = false;
                    //LPreferences.setUserId(Integer.valueOf(ss[0]));
                    LPreferences.setUserName(ss[1]);
                    //LPreferences.setUserFullName(ss[2]);
                } else if (header) {
                    header = false;
                } else {
                    int ii = 0;
                    Long ll;

                    int repeatInterval = 0, repeatUnit = 0, repeatCount = 0;

                    if (schedule) {
                        repeatUnit = Integer.valueOf(ss[ii++]);
                        repeatInterval = Integer.valueOf(ss[ii++]);
                        repeatCount = Integer.valueOf(ss[ii++]);
                    }

                    double amount = Double.valueOf(ss[ii++]);

                    String category = ss[ii++];
                    long categoryId;
                    if (TextUtils.isEmpty(category)) categoryId = 0;
                    else {
                        ll = categoryMap.get(category);
                        if (null == ll) {
                            categoryId = DBCategory.getIdByName(category);
                            if (categoryId != 0) {
                                DBCategory.updateColumnById(categoryId, DBHelper.TABLE_COLUMN_RID, ss[ii]);
                            } else {
                                //TODO: categoryId = DBCategory.add(new LCategory(category, ss[ii]));
                            }
                            categoryMap.put(category, categoryId);
                        } else {
                            categoryId = ll.longValue();
                        }
                    }
                    ii++;

                    String account = ss[ii++];
                    long accountId;
                    if (TextUtils.isEmpty(account)) accountId = 0;
                    else {
                        ll = accountMap.get(account);
                        if (null == ll) {
                            accountId = DBAccount.getIdByName(account);
                            if (accountId != 0) {
                                DBAccount.updateColumnById(accountId, DBHelper.TABLE_COLUMN_RID, ss[ii]);
                            } else {
                                accountId = DBAccount.add(new LAccount(account, ss[ii]));
                            }
                            accountMap.put(account, accountId);
                        } else {
                            accountId = ll.longValue();
                        }
                    }
                    ii++;

                    String account2 = ss[ii++];
                    long account2Id;
                    if (TextUtils.isEmpty(account2)) account2Id = 0;
                    else {
                        ll = accountMap.get(account2);
                        if (null == ll) {
                            account2Id = DBAccount.getIdByName(account2);
                            if (account2Id != 0) {
                                DBAccount.updateColumnById(account2Id, DBHelper.TABLE_COLUMN_RID, ss[ii]);
                            } else {
                                account2Id = DBAccount.add(new LAccount(account2, ss[ii]));
                            }
                            accountMap.put(account2, account2Id);
                        } else {
                            account2Id = ll.longValue();
                        }
                    }
                    ii++;

                    String tag = ss[ii++];
                    long tagId;
                    if (TextUtils.isEmpty(tag)) tagId = 0;
                    else {
                        ll = tagMap.get(tag);
                        if (null == ll) {
                            tagId = DBTag.getIdByName(tag);
                            if (tagId != 0) {
                                DBTag.updateColumnById(tagId, DBHelper.TABLE_COLUMN_RID, ss[ii]);
                            } else {
                                //TODO: tagId = DBTag.add(new LTag(tag, ss[ii]));
                            }
                            tagMap.put(tag, tagId);
                        } else {
                            tagId = ll.longValue();
                        }
                    }
                    ii++;

                    String vendor = ss[ii++];
                    long vendorId;
                    if (TextUtils.isEmpty(vendor)) vendorId = 0;
                    else {
                        ll = vendorMap.get(vendor);
                        if (null == ll) {
                            vendorId = DBVendor.getIdByName(vendor);
                            if (vendorId != 0) {
                                DBVendor.updateColumnById(vendorId, DBHelper.TABLE_COLUMN_RID, ss[ii]);
                            } else {
                                //TODO: vendorId = DBVendor.add(new LVendor(vendor, ss[ii], Integer.valueOf(ss[ii + 1])));
                            }
                            vendorMap.put(vendor, vendorId);
                        } else {
                            vendorId = ll.longValue();
                        }
                    }
                    ii += 2;

                    long timestamp = Long.valueOf(ss[ii++]);
                    long timestampLast = Long.valueOf(ss[ii++]);
                    int type = Integer.valueOf(ss[ii++]);
                    int state = Integer.valueOf(ss[ii++]);
                    int madeby = Integer.valueOf(ss[ii++]);
                    String rid = ss[ii++];
                    String note = ss[ii];

                    LTransaction trans = new LTransaction(0, amount, type, categoryId, vendorId, tagId,
                            accountId, account2Id, madeby, timestamp, timestampLast, note);

                    if (schedule) {
                        long sid = DBScheduledTransaction.getIdByRid(rid);
                        trans.setId(sid);

                        LScheduledTransaction lScheduledTransaction = new LScheduledTransaction(repeatInterval, repeatUnit, repeatCount, 0, trans);
                        if (sid != 0) {
                            trans.setId(sid);
                            DBScheduledTransaction.update(lScheduledTransaction);
                        } else {
                            DBScheduledTransaction.add(lScheduledTransaction);
                        }
                    } else {
                        long tid = DBTransaction.getIdByRid(rid);
                        if (tid != 0) {
                            trans.setId(tid);
                            DBTransaction.update(trans, false);
                        } else {
                            DBTransaction.add(trans, false, false);
                        }
                    }
                }
            }
            if (schedule) DBScheduledTransaction.scanAlarm();

            ois.close();

            MainService.scanBalance(LApp.ctx);
            //LPreferences.setLastDbRestoreDate("" + (new SimpleDateFormat("EEEE, MMM d yyyy")).format(new Date()));
            ret = true;
        } catch (Exception e) {
            LLog.e(TAG, "unable to importport database version: " + dbVersion);
            ret = false;
        }

        //remove stale file if error happens.
        if (!ret) {
            try {
                File path = openDbDir();
                if (path == null) return false;
                File file = getOldFile(path);
                if (file != null) {
                    file.delete();
                }
            } catch (Exception e) {
            }
        }

        return ret;
    }

    public static String getExportDate() {
        File path = openDbDir();
        if (path == null) return "N/A";
        File file = getOldFile(path);
        if (file == null) return "N/A";

        String str[] = file.getName().split("\\.", -1);
        long ms = Long.valueOf(str[1]);

        return "" + (new SimpleDateFormat("EEEE, MMM d yyyy")).format(new Date(ms));
    }

    /*
    public static String getImportDate() {
        return LPreferences.getLastDbRestoreDate();
    }
    */

    private static File openDbDir() {
        return LStorage.openDir("db");
    }

    private static File getOldFile(File path) {
        for (File f : path.listFiles()) {
            String name = f.getName();
            if (name.contains(".") && name.matches("\\d+(\\.\\d+)?"))
                return f;
        }
        return null;
    }

    private static String generateFilename(int dbVersion) {
        return dbVersion + "." + System.currentTimeMillis();
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
