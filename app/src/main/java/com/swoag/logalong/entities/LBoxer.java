package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class LBoxer {
    public int x;
    public int y;
    public long lx;
    public long ly;
    public LBoxer() {
    }

    public LBoxer(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public LBoxer(long lx, long ly) {
        this.lx = lx;
        this.ly = ly;
    }

    public LBoxer(int x, int y, long lx, long ly) {
        this.x = x;
        this.y = y;
        this.lx = lx;
        this.ly = ly;
    }
}

