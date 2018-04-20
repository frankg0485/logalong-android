package com.swoag.logalong.entities;

/**
 * Created by mgao on 4/18/18.
 */

public class LSearch {
    private boolean bShowAll;
    private boolean bAccounts;
    private long[] accounts;
    private boolean bCategories;
    private long[] categories;
    private boolean bVendors;
    private long[] vendors;
    private boolean bTags;
    private long[] tags;
    private boolean bTypes;
    private long[] types;
    private boolean bAllTime;
    private boolean bByEditTime;
    private long timeFrom;
    private long timeTo;
    private boolean bAllValue;
    private float valueFrom;
    private float valueTo;

    public LSearch() {
        this.bShowAll = true;
        this.bAccounts = true;
        this.accounts = null;
        this.bCategories = true;
        this.categories = null;
        this.bVendors = true;
        this.vendors = null;
        this.bTags = true;
        this.tags = null;
        this.bTypes = true;
        this.types = null;

        this.bAllTime = true;
        this.bByEditTime = false;
        this.timeFrom = this.timeTo = 0;

        this.bAllValue = true;
        this.valueFrom = this.valueTo = 0f;
    }

    public LSearch(LSearch search) {
        this.bShowAll = search.bShowAll;
        this.bAccounts = search.bAccounts;
        this.accounts = (search.accounts != null)? search.accounts.clone() : null;
        this.bCategories = search.bCategories;
        this.categories = (search.categories != null)? search.categories.clone() : null;
        this.bVendors = search.bVendors;
        this.vendors = (search.vendors != null)? search.vendors.clone() : null;
        this.bTags = search.bTags;
        this.tags = (search.tags != null)? search.tags.clone() : null;
        this.bTypes = search.bTypes;
        this.types = (search.types != null)? search.types.clone() : null;

        this.bAllTime = search.bAllTime;
        this.bByEditTime = search.bByEditTime;
        this.timeFrom = search.timeFrom;
        this.timeTo = search.timeTo;

        this.bAllValue = search.bAllValue;
        this.valueFrom = search.valueFrom;
        this.valueTo = search.valueTo;
    }

    public boolean isEqual(LSearch search) {
        if (this.bShowAll != search.bShowAll
                || this.bAllTime != search.bAllTime
                || this.bAllValue != search.bAllValue) return false;
        if (this.bShowAll && this.bAllTime && this.bAllValue) return true;
        return false;
    }

    public boolean isbShowAll() {
        return bShowAll;
    }

    public void setbShowAll(boolean bShowAll) {
        this.bShowAll = bShowAll;
    }

    public boolean isbAccounts() {
        return bAccounts;
    }

    public void setbAccounts(boolean bAccounts) {
        this.bAccounts = bAccounts;
    }

    public long[] getAccounts() {
        return accounts;
    }

    public void setAccounts(long[] accounts) {
        this.accounts = accounts;
    }

    public boolean isbCategories() {
        return bCategories;
    }

    public void setbCategories(boolean bCategories) {
        this.bCategories = bCategories;
    }

    public long[] getCategories() {
        return categories;
    }

    public void setCategories(long[] categories) {
        this.categories = categories;
    }

    public boolean isbVendors() {
        return bVendors;
    }

    public void setbVendors(boolean bVendors) {
        this.bVendors = bVendors;
    }

    public long[] getVendors() {
        return vendors;
    }

    public void setVendors(long[] vendors) {
        this.vendors = vendors;
    }

    public boolean isbTags() {
        return bTags;
    }

    public void setbTags(boolean bTags) {
        this.bTags = bTags;
    }

    public long[] getTags() {
        return tags;
    }

    public void setTags(long[] tags) {
        this.tags = tags;
    }

    public boolean isbTypes() {
        return bTypes;
    }

    public void setbTypes(boolean bTypes) {
        this.bTypes = bTypes;
    }

    public long[] getTypes() {
        return types;
    }

    public void setTypes(long[] types) {
        this.types = types;
    }

    public boolean isbAllTime() {
        return bAllTime;
    }

    public void setbAllTime(boolean bAllTime) {
        this.bAllTime = bAllTime;
    }

    public boolean isbByEditTime() {
        return bByEditTime;
    }

    public void setbByEditTime(boolean bByEditTime) {
        this.bByEditTime = bByEditTime;
    }

    public long getTimeFrom() {
        return timeFrom;
    }

    public void setTimeFrom(long timeFrom) {
        this.timeFrom = timeFrom;
    }

    public long getTimeTo() {
        return timeTo;
    }

    public void setTimeTo(long timeTo) {
        this.timeTo = timeTo;
    }

    public boolean isbAllValue() {
        return bAllValue;
    }

    public void setbAllValue(boolean bAllValue) {
        this.bAllValue = bAllValue;
    }

    public float getValueFrom() {
        return valueFrom;
    }

    public void setValueFrom(float valueFrom) {
        this.valueFrom = valueFrom;
    }

    public float getValueTo() {
        return valueTo;
    }

    public void setValueTo(float valueTo) {
        this.valueTo = valueTo;
    }
}
