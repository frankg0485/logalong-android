package com.swoag.logalong.entities;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import java.util.HashMap;

public class LSectionSummary {
    private static final String TAG = LSectionSummary.class.getSimpleName();

    private HashMap<Long, LAccountSummary> map;
    private HashMap<Long, Boolean> visibleMap;

    public LSectionSummary() {
        map = new HashMap<Long, LAccountSummary>();
        visibleMap = new HashMap<Long, Boolean>();
    }

    public void clear() {
        map.clear();
        visibleMap.clear();
    }

    public void addSummary(long id, LAccountSummary summary) {
        map.put(id, summary);
    }

    public boolean hasId(long id) {
        return map.containsKey(id);
    }

    public LAccountSummary getSummaryById(long id) {
        return map.get(id);
    }

    public void addVisible(long id, boolean visible) {
        visibleMap.put(id, visible);
    }

    public boolean isVisible(long id) {
        return visibleMap.get(id);
    }
}
