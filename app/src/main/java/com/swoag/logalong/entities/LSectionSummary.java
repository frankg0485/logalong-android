package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import java.util.HashMap;
import java.util.HashSet;

public class LSectionSummary {
    private static final String TAG = LSectionSummary.class.getSimpleName();

    HashMap<Long, LAccountSummary> map;

    public LSectionSummary() {
        map = new HashMap<Long, LAccountSummary>();
    }

    public boolean addSummary(long id, LAccountSummary summary) {
        map.put(id, summary);
        return true;
    }

    public boolean hasId(long id) {
        return map.containsKey(id);
    }

    public LAccountSummary getSummaryById(long id) {
        return map.get(id);
    }
}
