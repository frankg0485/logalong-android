package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.R;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.zip.CRC32;

public class LTransaction extends LDbBase {
    public static final int TRANSACTION_TYPE_EXPENSE = 10;
    public static final int TRANSACTION_TYPE_INCOME = 20;
    public static final int TRANSACTION_TYPE_TRANSFER = 30;
    public static final int TRANSACTION_TYPE_TRANSFER_COPY = 31;

    private static CRC32 crc32 = new CRC32();
    private static long crc32(byte[] buf) {
        crc32.reset();
        crc32.update(buf);
        return crc32.getValue();
    }
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    private static byte[] longToBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static int getTypeStringId(int type) {
        switch (type) {
            case TRANSACTION_TYPE_EXPENSE:
                break;
            case TRANSACTION_TYPE_INCOME:
                return R.string.income;
            case TRANSACTION_TYPE_TRANSFER:
                return R.string.transfer;
        }
        return R.string.expense;
    }

    private double value;
    private int type;
    private long changeBy;
    private long createBy;
    private long category;
    private long account;
    private long account2;
    private long rid;
    private long tag;
    private long vendor;
    private long timeStamp;
    private long timeStampCreate;
    private String note;

    private void init() {
        this.timeStamp = System.currentTimeMillis();
        this.value = 0;
        this.type = TRANSACTION_TYPE_EXPENSE;
        this.createBy = 0;
        this.category = 0;
        this.account = 0;
        this.account2 = 0;
        this.tag = 0;
        this.vendor = 0;
        this.rid = 0;
        this.note = "";
    }

    public LTransaction() {
        init();
    }

    public void copy(LTransaction item) {
        this.timeStamp = item.timeStamp;
        this.timeStampLast = item.timeStampLast;
        this.value = item.value;
        this.type = item.type;
        this.createBy = item.createBy;
        this.state = item.state;
        this.id = item.id;
        this.category = item.category;
        this.account = item.account;
        this.account2 = item.account2;
        this.tag = item.tag;
        this.vendor = item.vendor;
        this.note = item.note;
        this.gid = item.gid;
        this.rid = item.rid;
    }

    public LTransaction(LTransaction item) {
        copy(item);
    }

    public boolean isEqual(LTransaction item) {
        return (this.timeStamp == item.timeStamp &&
                this.value == item.value &&
                this.type == item.type &&
                this.createBy == item.createBy &&
                this.category == item.category &&
                this.account == item.account &&
                this.account2 == item.account2 &&
                this.tag == item.tag &&
                this.vendor == item.vendor &&
                this.rid == item.rid &&
                this.note.contentEquals(item.note));
    }

    public LTransaction(double value, int type, long category, long vendor, long tag,
                        long account, long account2, long timeStamp, String note) {
        init();
        this.value = value;
        this.type = type;
        this.category = category;
        this.vendor = vendor;
        this.tag = tag;
        this.account = account;
        this.account2 = account2;
        this.timeStamp = timeStamp;
        this.note = note;
    }

    public LTransaction(long gid, double value, int type, long category, long vendor, long tag,
                        long account, long account2, int createBy, long timeStamp, long timeStampLast, String note) {
        init();
        this.gid = gid;
        this.value = value;
        this.type = type;
        this.category = category;
        this.vendor = vendor;
        this.tag = tag;
        this.account = account;
        this.account2 = account2;
        this.createBy = createBy;
        this.timeStamp = timeStamp;
        this.timeStampLast = timeStampLast;
        this.note = note;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getCategory() {
        return category;
    }

    public void setCategory(long category) {
        this.category = category;
    }

    public long getAccount() {
        return account;
    }

    public void setAccount(long account) {
        this.account = account;
    }

    public long getAccount2() {
        return account2;
    }

    public void setAccount2(long account2) {
        this.account2 = account2;
    }

    public long getTag() {
        return tag;
    }

    public void setTag(long tag) {
        this.tag = tag;
    }

    public long getVendor() {
        return vendor;
    }

    public void setVendor(long vendor) {
        this.vendor = vendor;
    }

    public long getTimeStampCreate() {
        return timeStampCreate;
    }

    public void setTimeStampCreate(long timeStampCreate) {
        this.timeStampCreate = timeStampCreate;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(long createBy) {
        this.createBy = createBy;
    }

    public long getChangeBy() {
        return changeBy;
    }

    public void setChangeBy(long changeBy) {
        this.changeBy = changeBy;
    }

    public long getRid() {
        return rid;
    }

    public void setRid(long rid) {
        this.rid = rid;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void generateRid()
    {
        rid = crc32(UUID.randomUUID().toString().getBytes()) | crc32(longToBytes(System.currentTimeMillis())) << 32;
    }
}
