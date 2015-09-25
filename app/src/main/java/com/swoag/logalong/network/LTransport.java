package com.swoag.logalong.network;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import com.swoag.logalong.utils.LBuffer;

public class LTransport {
    private static final String TAG = LTransport.class.getSimpleName();
    private static int sequence;

    public static void scramble(byte[] buf, int off, int bytes, int scrambler) {
        byte[] ss = new byte[4];
        ss[0] = (byte) ((scrambler >>> 24) & 0xff);
        ss[1] = (byte) ((scrambler >>> 16) & 0xff);
        ss[2] = (byte) ((scrambler >>> 8) & 0xff);
        ss[3] = (byte) (scrambler & 0xff);

        //LLog.d(TAG, String.format("scrambler: %x %x %x %x", ss[0], ss[1], ss[2], ss[3]));
        for (int ii = 0; ii < bytes; ii++) {
            //LLog.d(TAG, String.format("%x : %x %x", (off + ii), buf[off + ii], ss[ii & 0x03]));
            buf[off + ii] ^= ss[ii & 0x03];
            //LLog.d(TAG, String.format("%x : %x", (off + ii), buf[off + ii]));
        }
    }

    public static void scramble(LBuffer buf, int scrambler) {
        int len = buf.getShortAt(buf.getBufOffset() + 2);
        if (len < 8) return;

        //LLog.d(TAG, String.format("offset: %d len: %d scramber: %x", buf.getBufOffset(), len, scrambler));
        scramble(buf.getBuf(), buf.getBufOffset() + 6, len - 6, scrambler);
    }

    public static boolean send_rqst(LAppServer server, short rqst, int datai, String datas, int scrambler) {
        LBuffer buf = server.getNetBuffer();
        if (buf == null) return false;
        buf.putShortAutoInc(LProtocol.PACKET_SIGNATURE1);
        buf.putShortAutoInc((short) 0);
        buf.putShortAutoInc(rqst);
        buf.putIntAutoInc(datai);

        int len = 0;
        try {
            len = (short) datas.getBytes("UTF-8").length;
        } catch (Exception e) {
        }

        buf.putShortAutoInc((short) len);
        buf.putStringAutoInc(datas);

        len = LProtocol.PACKET_PAYLOAD_LENGTH(buf.getBufOffset());
        buf.putShortAt((short) len, 2);
        buf.setLen(len);

        buf.setBufOffset(0);
        scramble(buf, scrambler);
        server.putNetBuffer(buf);
        return true;
    }

    public static boolean send_rqst(LAppServer server, short rqst, String data, int scrambler) {
        LBuffer buf = server.getNetBuffer();
        if (buf == null) return false;
        buf.putShortAutoInc(LProtocol.PACKET_SIGNATURE1);
        buf.putShortAutoInc((short) 0);
        buf.putShortAutoInc(rqst);

        int len = 0;
        try {
            len = (short) data.getBytes("UTF-8").length;
        } catch (Exception e) {
        }

        buf.putShortAutoInc((short) len);
        buf.putStringAutoInc(data);

        len = LProtocol.PACKET_PAYLOAD_LENGTH(buf.getBufOffset());
        buf.putShortAt((short) len, 2);
        buf.setLen(len);

        buf.setBufOffset(0);
        scramble(buf, scrambler);
        server.putNetBuffer(buf);
        return true;
    }

    public static boolean send_rqst(LAppServer server, short rqst, int data, int scrambler) {
        LBuffer buf = server.getNetBuffer();
        if (buf == null) return false;

        buf.putShortAutoInc(LProtocol.PACKET_SIGNATURE1);
        buf.putShortAutoInc((short) 12);

        buf.putShortAutoInc(rqst);
        buf.putIntAutoInc(data);
        buf.setLen(12);

        buf.setBufOffset(0);
        scramble(buf, scrambler);
        server.putNetBuffer(buf);
        return true;
    }

    public static boolean send_rqst(LAppServer server, short rqst, int scrambler) {
        LBuffer buf = server.getNetBuffer();
        if (buf == null) return false;

        buf.putShortAutoInc(LProtocol.PACKET_SIGNATURE1);
        buf.putShortAutoInc((short) 8);

        buf.putShortAutoInc(rqst);
        buf.setLen(8);

        buf.setBufOffset(0);
        scramble(buf, scrambler);
        server.putNetBuffer(buf);
        return true;
    }
}
