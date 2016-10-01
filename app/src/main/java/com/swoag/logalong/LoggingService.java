package com.swoag.logalong;
/* Copyright (C) 2016 SWOAG Technology <www.swoag.com> */


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.swoag.logalong.network.LRemoteLogging;


public class LoggingService extends Service {
    private static final String TAG = LoggingService.class.getSimpleName();

    public static final int CMD_START = 10;
    public static final int CMD_STOP = 20;

    private Handler timerHandler;
    private Runnable timerRunnable;

    private LRemoteLogging remoteLoggingServer = LRemoteLogging.getInstance();

    public static void start(Context context) {
        if (!LRemoteLogging.getInstance().isConnected()) {
            Intent serviceIntent = new Intent(context, LoggingService.class);
            serviceIntent.putExtra("cmd", LoggingService.CMD_START);
            context.startService(serviceIntent);
        }
    }

    public static void stop(Context context) {
        Intent serviceIntent = new Intent(context, LoggingService.class);
        serviceIntent.putExtra("cmd", LoggingService.CMD_STOP);
        context.startService(serviceIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        timerHandler = new Handler() {
        };

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        };
    }

    @Override
    public void onDestroy() {
        timerHandler.removeCallbacks(timerRunnable);
        timerRunnable = null;
        timerHandler = null;
        remoteLoggingServer.disconnect();
        Log.d(TAG, "service destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int cmd = intent.getIntExtra("cmd", 0);
            switch (cmd) {
                case CMD_STOP:
                    Log.d(TAG, "requested to stop service");
                    stopSelf();
                    break;

                case CMD_START:
                    Log.d(TAG, "requested to start remote logging");
                    timerHandler.removeCallbacks(timerRunnable);
                    remoteLoggingServer.connect();
                    timerHandler.postDelayed(timerRunnable, 15000);
                    break;
            }
        }
        return START_NOT_STICKY;
    }
}
