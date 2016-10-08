package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.os.Environment;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class LStorage {
    private static final String TAG = LStorage.class.getSimpleName();
    private static final int MAX_RECORD_LENGTH = 1024;
    private static final int MAX_CACHE_LENGTH = (16 * 1024 * 1024);
    private static LStorage instance = null;
    private static int runningId = (new Random()).nextInt();
    private static int SIGNATURE = 0xa55a55aa;

    private RandomAccessFile file;
    private Object lock;
    private Memory memory;

    public LStorage() {
    }

    public static LStorage getInstance() {
        if (null == instance) {
            instance = new LStorage();
            instance.open();
        }
        return instance;
    }

    public static class Entry {
        public int id;
        public byte[] data;
        public int userId;

        public Entry() {
        }
    }

    public int getCacheLength() {
        synchronized (lock) {
            if (file == null) {
                return memory.getLength();
            }
            return LPreferences.getCacheLength();
        }
    }

    private void reset() {
        LPreferences.setCacheReadPointer(0);
        LPreferences.setCacheWritePointer(0);
        LPreferences.setCacheLength(0);
    }

    public boolean put(Entry entry) {
        boolean ret = false;

        synchronized (lock) {
            if (file == null) {
                return memory.put(entry);
            }

            if (LPreferences.getCacheLength() >= MAX_CACHE_LENGTH - MAX_RECORD_LENGTH) {
                LLog.w(TAG, "cache full");
                return false;
            }

            long offset = LPreferences.getCacheWritePointer();
            if (offset > MAX_CACHE_LENGTH - MAX_RECORD_LENGTH)
                offset = 0;

            try {
                file.seek(offset);
                file.writeInt(SIGNATURE);

                entry.id = runningId;
                file.writeInt(entry.id);
                file.writeInt(entry.userId);
                file.writeShort(entry.data.length);
                file.write(entry.data);

                long newOffset = file.getFilePointer();
                LPreferences.setCacheWritePointer(newOffset);

                LPreferences.setCacheLength(LPreferences.getCacheLength() + (int)(newOffset - offset));

                runningId++;
                ret = true;
            } catch (Exception e) {
                LLog.e(TAG, "unable to write cache");
                reset();
            }
        }
        return ret;
    }

    public Entry get() {
        Entry entry = new Entry();
        synchronized (lock) {
            if (file == null) {
                return memory.get();
            }

            if (LPreferences.getCacheLength() <= 0) return null;
            long offset = LPreferences.getCacheReadPointer();

            try {
                file.seek(offset);
                int signature = file.readInt();
                if (signature != SIGNATURE) {
                    LLog.e(TAG, "cache corrupted upon read: " + signature);
                    reset();
                    return null;
                }

                entry.id = file.readInt();
                entry.userId = file.readInt();
                short bytes = file.readShort();
                entry.data = new byte[bytes];
                file.read(entry.data, 0, bytes);
            } catch (Exception e) {
                LLog.e(TAG, "unable to read cache");
                entry = null;
                reset();
            }
        }
        return entry;
    }

    public boolean release(int id) {
        boolean ret = false;
        synchronized (lock) {
            if (file == null) {
                return memory.release(id);
            }

            long offset = LPreferences.getCacheReadPointer();

            try {
                file.seek(offset);

                int signature = file.readInt();
                if (signature != SIGNATURE) {
                    LLog.e(TAG, "cache corrupted upon release: " + signature);
                    reset();
                    return false;
                }
                int entryId = file.readInt();
                if (id == entryId) {
                    file.skipBytes(4); //int userId
                    short bytes = file.readShort();
                    file.skipBytes(bytes);

                    long newOffset = file.getFilePointer();
                    LPreferences.setCacheLength(LPreferences.getCacheLength() - (int)(newOffset - offset));

                    if (newOffset > MAX_CACHE_LENGTH - MAX_RECORD_LENGTH)
                        newOffset = 0;

                    LPreferences.setCacheReadPointer(newOffset);
                    ret = true;
                } else {
                    LLog.w(TAG, "invalid cache: " + id + " : " + entryId);
                }
            } catch (Exception e) {
                LLog.e(TAG, "unable to release cache");
                reset();
            }
        }
        return ret;
    }

    public boolean open() {
        boolean ret = false;
        lock = new Object();
        try {
            if (isExternalStorageWritable()) {
                File path = openDir("cache");
                if (path != null) {
                    File f = new File(path, "journal");
                    file = new RandomAccessFile(f, "rwd");
                    ret = file != null;
                }
            }
        } catch (Exception e) {
            LLog.w(TAG, "unable to open journal file: " + getPath("cache") + "/journal");
            //cache in memory
        }

        if (!ret) {
            file = null;
            memory = new Memory();
            ret = memory.open();
        }
        return ret;
    }

    public boolean close() {
        synchronized (lock) {
            try {
                if (file != null)
                    file.close();
                else
                    memory.close();
            } catch (Exception e) {
            }
        }
        return true;
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        return false;
    }

    private static String getPath(String subdir) {
        return "logalong" + File.separator + subdir;
    }

    public static File openDir(String subdir) {
        File path = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), getPath(subdir));
        if (!path.exists()) {
            if (!path.mkdirs()) {
                return null;
            }
        }
        return path;
    }

    private class Memory {
        ArrayList<Entry> cache;

        boolean open() {
            cache = new ArrayList<Entry>();
            return true;
        }

        void close() {
            cache = null;
        }

        int getLength() {
            return cache.size();
        }

        boolean put(Entry entry) {
            entry.id = runningId;
            cache.add(entry);
            runningId++;
            return true;
        }

        Entry get() {
            if (cache.size() <= 0) return null;
            return cache.get(0);
        }

        boolean release(int id) {
            if (cache.size() <= 0) return false;
            Entry entry = cache.get(0);

            if (entry.id != id) {
                LLog.w(TAG, "memory cache broken on release, id mismatch: " + id + " : " + entry.id);
                return false;
            }

            cache.remove(0);
            return true;
        }
    }
}
