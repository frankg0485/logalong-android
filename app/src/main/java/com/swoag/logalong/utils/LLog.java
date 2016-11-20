package com.swoag.logalong.utils;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.util.Log;

import com.swoag.logalong.LApp;
import com.swoag.logalong.LoggingService;
import com.swoag.logalong.MainService;
import com.swoag.logalong.network.LRemoteLogging;

public class LLog {
    private static boolean debug = true;

    private static boolean localLog = true;
    private static boolean netLog = true;

    private static String lastMsg = "";
    private static int repeatCount = 0;

    private static boolean opened;
    private static int STACK_INDEX;
    private static String pkg = "";
    static {
        try {
            int ii = 0;
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                ii++;
                if (ste.getClassName().equals(LLog.class.getName())) break;
            }
            STACK_INDEX = ii;
        } catch (Exception e) {}
    }

    private static String caller() {
        try {
            return pkg + "->" + Thread.currentThread().getStackTrace()[STACK_INDEX + 1].getMethodName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static void open(Context context) {
        if (!opened) {
            pkg = context.getPackageName().replaceAll("com.swoag.", "");
        }
        opened = true;
    }

    public static void d(String tag, String message) {
        if (!debug) return;
        if (verifyTag(tag)) {
            if (localLog) Log.d(tag, caller() + ":" + message);
            if (netLog) remoteLog("D: " + tag + caller() + ":" + message);
        }
    }

    public static void e(String tag, String message) {
        if (verifyTag(tag)) {
            if (localLog) Log.e(tag, caller() + ":" + message);
            if (netLog) remoteLog("E: " + tag + caller() + ":" + message);
        }
    }

    public static void i(String tag, String message) {
        if (verifyTag(tag)) {
            if (localLog) Log.i(tag, caller() + ":" + message);
            if (netLog) remoteLog("I: " + tag + caller() + ":" + message);
        }
    }

    public static void v(String tag, String message) {
        if (!debug) return;
        if (verifyTag(tag)) {
            if (localLog) Log.v(tag, caller() + ":" + message);
            if (netLog) remoteLog("V: " + tag + caller() + ":" + message);
        }
    }

    public static void w(String tag, String message) {
        if (verifyTag(tag)) {
            if (localLog) Log.w(tag, caller() + ":" + message);
            if (netLog) remoteLog("W: " + tag + caller() + ":" + message);
        }
    }

    static boolean verifyTag(String tag) {
        return true;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static void remoteLog(String msg) {
        //DISABLE: if (msg != null) return;
        if (!lastMsg.contentEquals(msg)) {
            LRemoteLogging server = LRemoteLogging.getInstance();
            LoggingService.start(LApp.ctx);

            LBuffer buffer = server.getNetBuffer();
            if (null != buffer) {
                buffer.putShortAutoInc(LRemoteLogging.LOGGING_REQUEST_SYNC);
                buffer.putShortAutoInc((short)0);
                if (repeatCount > 0 ) buffer.putStringAutoInc("*** last msg repeated " + repeatCount + " times\n");
                buffer.putStringAutoInc(msg + "\n");
                buffer.setLen(buffer.getBufOffset());
                buffer.putShortAt((short)(buffer.getLen() - 4), 2);
                buffer.setBufOffset(0);
                server.putNetBuffer(buffer);
            } else {
                Log.e("LLog", "remote logging buffer full");
            }
            lastMsg = msg;
            repeatCount = 0;
        } else {
            repeatCount++;
        }
    }
}
