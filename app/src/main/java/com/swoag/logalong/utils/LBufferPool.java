package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import java.util.ArrayList;
import java.util.List;

public class LBufferPool {
    private static final String TAG = LBufferPool.class.getSimpleName();

    private final int bufferSize;
    private final int maxPoolSize;
    private int currentPoolSize;
    private boolean poolAlive;
    private boolean poolEnabled;
    private final List<LBuffer> emptyBuffers;
    private final List<LBuffer> filledBuffers;
    private Object lock;

    public LBufferPool(int bufferSize, int poolSize) {
        this.bufferSize = bufferSize;
        maxPoolSize = (poolSize > 2) ? poolSize : 2;
        currentPoolSize = 2;

        emptyBuffers = new ArrayList<LBuffer>();
        filledBuffers = new ArrayList<LBuffer>();
        for (int ii = 0; ii < currentPoolSize; ii++) {
            emptyBuffers.add(new LBuffer(bufferSize));
        }
        poolAlive = true;
        lock = new Object();
    }

    public void stop() {
        synchronized (lock) {
            poolAlive = false;
            lock.notifyAll();
        }
    }

    public void enable(boolean yes) {
        synchronized (lock) {
            poolEnabled = yes;
            lock.notifyAll();
        }
    }

    public LBuffer getWriteBuffer() {
        synchronized (lock) {
            while (poolAlive && poolEnabled) {
                if (emptyBuffers.size() > 0) {
                    LBuffer buf = emptyBuffers.remove(0);
                    buf.setBufOffset(0);
                    return buf;
                } else if (currentPoolSize < maxPoolSize) {
                    currentPoolSize++;
                    return new LBuffer(bufferSize);
                } else {
                    try {
                        lock.wait();
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    public LBuffer getWriteBufferMayFail() {
        synchronized (lock) {
            while (poolAlive && poolEnabled) {
                if (emptyBuffers.size() > 0) {
                    LBuffer buf = emptyBuffers.remove(0);
                    buf.setBufOffset(0);
                    return buf;
                } else if (currentPoolSize < maxPoolSize) {
                    currentPoolSize++;
                    return new LBuffer(bufferSize);
                } else {
                    break;
                }
            }
        }
        return null;
    }

    public LBuffer getWriteBufferNeverFail() {
        synchronized (lock) {
            if (emptyBuffers.size() > 0) {
                LBuffer buf = emptyBuffers.remove(0);
                buf.setBufOffset(0);
                return buf;
            } else {
                currentPoolSize++;
                return new LBuffer(bufferSize);
            }
        }
    }

    public void putWriteBuffer(LBuffer buffer) {
        if (buffer == null) return;
        synchronized (lock) {
            filledBuffers.add((LBuffer) buffer);
            lock.notifyAll();
        }
    }

    public void putWriteBuffer(LBuffer buffer, boolean priority) {
        if (buffer == null) return;
        synchronized (lock) {
            if (priority) filledBuffers.add(0, (LBuffer) buffer);
            else filledBuffers.add((LBuffer) buffer);
            lock.notifyAll();
        }
    }

    public LBuffer getReadBuffer() {
        synchronized (lock) {
            while (poolAlive && poolEnabled) {
                if (filledBuffers.size() > 0) {
                    LBuffer buf = filledBuffers.remove(0);
                    buf.setBufOffset(0);
                    return buf;
                } else {
                    //LLog.w(TAG, "buffer underflow, no more data to process.");
                    try {
                        lock.wait();
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    public void putReadBuffer(LBuffer buffer) {
        if (buffer == null) return;
        synchronized (lock) {
            emptyBuffers.add((LBuffer) buffer);
            lock.notifyAll();
        }
    }

    public void flush() {
        LBuffer buf = null;
        synchronized (lock) {
            while (poolAlive && (filledBuffers.size() > 0)) {
                buf = filledBuffers.remove(0);
                buf.setBufOffset(0);
                emptyBuffers.add(buf);
            }
            lock.notifyAll();
        }
    }
}
