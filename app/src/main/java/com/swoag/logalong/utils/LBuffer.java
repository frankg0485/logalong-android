package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class LBuffer {
    private static final String TAG = LBuffer.class.getSimpleName();

    private int bytes; // current valid bytes in buffer: when reading, bytes tracks number of bytes left in buffer
    private int offset; // current read/write position
    private byte[] array; // the backing array

    public LBuffer(int size) {
        array = new byte[size];
        offset = 0;
        bytes = 0;
    }

    public byte[] getBuf() {
        return array;
    }

    public int getBufOffset() {
        return offset;
    }

    public void setBufOffset(int off) {
        offset = off;
    }

    public int size() {
        return array.length;
    }

    public int getLen() {
        return this.bytes;
    }

    public void setLen(int bytes) {
        this.bytes = bytes;
    }

    public int append(byte[] buf) {
        if (offset + bytes + buf.length > array.length) {
            LLog.e(TAG, "buffer overlow: " + buf.length + "@" + bytes + " offset: " + offset);
            return -1;
        }
        System.arraycopy(buf, 0, array, offset + bytes, buf.length);
        bytes += buf.length;
        return 0;
    }

    public int append(LBuffer buf) {
        int len = buf.getLen();
        if (offset + bytes + len > array.length) {
            LLog.e(TAG, "buffer overlow: " + len + "@" + bytes + " offset: " + offset);
            return -1;
        }
        System.arraycopy(buf.getBuf(), 0, array, offset + bytes, len);
        bytes += len;
        return 0;
    }

    public short getShortAt(int off) {
        return (short) ((array[off] & 0xff) | (0xff00 & (array[off + 1] << 8)));
    }

    public short getShort() {
        return (short) ((array[offset] & 0xff) | (0xff00 & (array[offset + 1] << 8)));
    }

    public int getIntAt(int off) {
        return (int) ((array[off] & 0xff) | (0xff00 & (array[off + 1] << 8)) |
                (0xff0000 & (array[off + 2] << 16)) | (0xff000000 & (array[off + 3] << 24)));
    }

    public byte getByteAutoInc() {
        offset++;
        return array[offset - 1];
    }

    public short getShortAutoInc() {
        short ret = (short) ((array[offset] & 0xff) | (0xff00 & (array[offset + 1] << 8)));
        offset += 2;
        return ret;
    }

    public int getIntAutoInc() {
        int ret = (array[offset] & 0xff) |
                (0xff00 & (array[offset + 1] << 8)) |
                (0xff0000 & (array[offset + 2] << 16)) |
                (0xff000000 & (array[offset + 3] << 24));
        offset += 4;
        return ret;
    }

    public long getLongAutoInc() {
        long ret = (array[offset] & 0xff) |
                (0xff00 & (array[offset + 1] << 8)) |
                (0xff0000 & (array[offset + 2] << 16)) |
                (0xff000000 & (array[offset + 3] << 24)) |
                (0xff00000000L & ((long) array[offset + 4] << 32)) |
                (0xff0000000000L & ((long) array[offset + 5] << 40)) |
                (0xff000000000000L & ((long) array[offset + 6] << 48)) |
                (0xff00000000000000L & ((long) array[offset + 7] << 56));
        offset += 8;
        return ret;
    }

    public byte[] getBytesAutoInc(int bytes) {
        byte[] tmp = new byte[bytes];
        System.arraycopy(array, offset, tmp, 0, bytes);
        offset += bytes;

        return tmp;
    }

    public short[] getShortsAutoInc(int shorts) {
        short[] tmp = new short[shorts];
        for (int ii = 0; ii < shorts; ii++)
            tmp[ii] = getShortAutoInc();

        return tmp;
    }

    public int[] getIntsAutoInc(int ints) {
        int[] tmp = new int[ints];
        for (int ii = 0; ii < ints; ii++)
            tmp[ii] = getIntAutoInc();
        return tmp;
    }

    public String getStringAutoInc(int bytes) {
        byte[] tmp = new byte[bytes];
        System.arraycopy(array, offset, tmp, 0, bytes);
        offset += bytes;
        try {
            return new String(tmp, "UTF-8");
        } catch (Exception e) {
            LLog.w(TAG, "unable to decode string");
        }
        return null;
    }

    public int putByteAutoInc(byte b) {
        array[offset] = b;
        offset++;
        return 0;
    }

    public int putShortAutoInc(short val) {
        array[offset] = (byte) (val & 0xff);
        array[offset + 1] = (byte) ((val >>> 8) & 0xff);
        offset += 2;
        return 0;
    }

    public int putShortAt(short val, int index) {
        array[index] = (byte) (val & 0xff);
        array[index + 1] = (byte) ((val >>> 8) & 0xff);
        return 0;
    }

    public int putIntAutoInc(int val) {
        array[offset] = (byte) (val & 0xff);
        array[offset + 1] = (byte) ((val >>> 8) & 0xff);
        array[offset + 2] = (byte) ((val >>> 16) & 0xff);
        array[offset + 3] = (byte) ((val >>> 24) & 0xff);
        offset += 4;
        return 0;
    }

    public int putIntAt(int val, int index) {
        array[index] = (byte) (val & 0xff);
        array[index + 1] = (byte) ((val >>> 8) & 0xff);
        array[index + 2] = (byte) ((val >>> 16) & 0xff);
        array[index + 3] = (byte) ((val >>> 24) & 0xff);
        return 0;
    }

    public int putLongAutoInc(long val) {
        array[offset] = (byte) (val & 0xff);
        array[offset + 1] = (byte) ((val >>> 8) & 0xff);
        array[offset + 2] = (byte) ((val >>> 16) & 0xff);
        array[offset + 3] = (byte) ((val >>> 24) & 0xff);
        array[offset + 4] = (byte) ((val >>> 32) & 0xff);
        array[offset + 5] = (byte) ((val >>> 40) & 0xff);
        array[offset + 6] = (byte) ((val >>> 48) & 0xff);
        array[offset + 7] = (byte) ((val >>> 56) & 0xff);
        offset += 8;
        return 0;
    }

    public int putStringAutoInc(String str) {
        try {
            byte[] bytes = str.getBytes("UTF-8");
            System.arraycopy(bytes, 0, array, offset, bytes.length);
            offset += bytes.length;
            return 0;
        } catch (Exception e) {
        }
        return -1;
    }

    public int putBytesAutoInc(byte[] bytes) {
        System.arraycopy(bytes, 0, array, offset, bytes.length);
        offset += bytes.length;
        return 0;
    }

    public int putBytesAutoInc(byte[] bytes, int off, int length) {
        System.arraycopy(bytes, off, array, offset, length);
        offset += length;
        return 0;
    }

    public int putShortsAutoInc(short[] shorts) {
        int len = shorts.length;
        if (len == 0) return 0;

        byte[] bytes = new byte[len << 2];

        for (int ii = 0, jj = 0; ii < len; ii++) {
            short val = shorts[ii];
            bytes[jj++] = (byte) ((val) & 0xff);
            bytes[jj++] = (byte) ((val >>> 8) & 0xff);
        }

        System.arraycopy(bytes, 0, array, offset, bytes.length);
        offset += bytes.length;
        return 0;
    }

    public int putIntsAutoInc(int[] ints) {
        int len = ints.length;
        if (len == 0) return 0;
        byte[] bytes = new byte[len << 2];

        for (int ii = 0, jj = 0; ii < len; ii++) {
            int val = ints[ii];
            bytes[jj++] = (byte) ((val) & 0xff);
            bytes[jj++] = (byte) ((val >>> 8) & 0xff);
            bytes[jj++] = (byte) ((val >>> 16) & 0xff);
            bytes[jj++] = (byte) ((val >>> 24) & 0xff);
        }

        System.arraycopy(bytes, 0, array, offset, bytes.length);
        offset += bytes.length;
        return 0;
    }

    public int putIntsAutoInc(int[] ints, int off, int count) {
        LLog.d(TAG, "off: " + off + " count: " + count + "/" + ints.length);
        if (count == 0) return 0;
        byte[] bytes = new byte[count << 2];

        for (int ii = 0, jj = 0; ii < count; ii++) {
            int val = ints[ii + off];
            bytes[jj++] = (byte) ((val) & 0xff);
            bytes[jj++] = (byte) ((val >>> 8) & 0xff);
            bytes[jj++] = (byte) ((val >>> 16) & 0xff);
            bytes[jj++] = (byte) ((val >>> 24) & 0xff);
        }

        System.arraycopy(bytes, 0, array, offset, bytes.length);
        offset += bytes.length;
        return 0;
    }

    public void modLen(int mod) {
        bytes += mod;
    }

    public void skip(int bytes) {
        offset += bytes;
    }

    public void setOffset(int off) {
        offset = off;
    }

    public void clear() {
        offset = 0;
        bytes = 0;
    }

    public void reset() {
        if (offset != 0) {
            //byte[] a = new byte[array.length];
            System.arraycopy(array, offset, array, 0, bytes);
            offset = 0;
            //array = a;
        }
    }

    public LBuffer dup() {
        LBuffer buf = new LBuffer(size());
        System.arraycopy(array, 0, buf.getBuf(), 0, array.length);
        buf.setLen(array.length);
        return buf;
    }
}
