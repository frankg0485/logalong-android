package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.util.Log;

public class LLog {
    public static boolean debug = true;
	public static boolean netDebug = false;

	private static boolean opened;
	private static int STACK_INDEX;
	private static String pkg="";
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
			Log.d(tag, caller() + ":" + message);
		}
	}

	public static void e(String tag, String message) {
		if (verifyTag(tag)) {
			Log.e(tag, caller() + ":" + message);
		}
	}

	public static void i(String tag, String message) {
		if (verifyTag(tag)) {
			Log.i(tag, caller() + ":" + message);
		}
	}

	public static void v(String tag, String message) {
		if (!debug) return;
		if (verifyTag(tag)) {
			Log.v(tag, caller() + ":" + message);
		}
	}

	public static void w(String tag, String message) {
		if (verifyTag(tag)) {
			Log.w(tag, caller() + ":" + message);
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
}
