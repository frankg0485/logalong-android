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
    private final List<LBuffer> emptLBuffers;
    private final List<LBuffer> filledBuffers;

    public LBufferPool(int bufferSize, int poolSize) {
        this.bufferSize = bufferSize;
        maxPoolSize = (poolSize > 2) ? poolSize : 2;
        currentPoolSize = 2;

        emptLBuffers = new ArrayList<LBuffer>();
        filledBuffers = new ArrayList<LBuffer>();
        for (int ii = 0; ii < currentPoolSize; ii++) {
            emptLBuffers.add(new LBuffer(bufferSize));
        }
        poolAlive = true;
    }

    public void stop() {
        poolAlive = false;
        synchronized (emptLBuffers) {
            emptLBuffers.notifyAll();
        }
        synchronized (filledBuffers) {
            filledBuffers.notifyAll();
        }
    }

    public void enable(boolean yes) {
        poolEnabled = yes;
        synchronized (emptLBuffers) {
            emptLBuffers.notifyAll();
        }
        synchronized (filledBuffers) {
            filledBuffers.notifyAll();
        }
    }

    public LBuffer getWriteBuffer() {
        synchronized (emptLBuffers) {
            while (poolAlive && poolEnabled) {
                if (emptLBuffers.size() > 0) {
                    LBuffer buf = emptLBuffers.remove(0);
                    buf.setBufOffset(0);
                    return buf;
                } else if (currentPoolSize < maxPoolSize) {
                    currentPoolSize++;
                    return new LBuffer(bufferSize);
                } else {
                    //LLog.w(TAG, "buffer overflow, audio processing too slow?");
                    try {
                        emptLBuffers.wait();
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    public LBuffer getWriteBufferNeverFail() {
        synchronized (emptLBuffers) {
            if (emptLBuffers.size() > 0) {
                LBuffer buf = emptLBuffers.remove(0);
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
        synchronized (filledBuffers) {
            filledBuffers.add((LBuffer) buffer);
            filledBuffers.notifyAll();
        }
    }

    public void putWriteBuffer(LBuffer buffer, boolean priority) {
        if (buffer == null) return;
        synchronized (filledBuffers) {
            if (priority) filledBuffers.add(0, (LBuffer) buffer);
            else filledBuffers.add((LBuffer) buffer);
            filledBuffers.notifyAll();
        }
    }

    public LBuffer getReadBuffer() {
        synchronized (filledBuffers) {
            while (poolAlive && poolEnabled) {
                if (filledBuffers.size() > 0) {
                    LBuffer buf = filledBuffers.remove(0);
                    buf.setBufOffset(0);
                    return buf;
                } else {
                    //LLog.w(TAG, "buffer underflow, no more data to process.");
                    try {
                        filledBuffers.wait();
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    public void putReadBuffer(LBuffer buffer) {
        if (buffer == null) return;
        synchronized (emptLBuffers) {
            emptLBuffers.add((LBuffer) buffer);
            emptLBuffers.notifyAll();
        }
    }

    public void flush() {
        LBuffer buf = null;
        while (poolAlive) {
            synchronized (filledBuffers) {
                if (filledBuffers.size() > 0) {
                    buf = filledBuffers.remove(0);
                    buf.setBufOffset(0);
                } else {
                    break;
                }
            }
            synchronized (emptLBuffers) {
                emptLBuffers.add(buf);
                buf = null;
                emptLBuffers.notifyAll();
            }
        }
    }
}
