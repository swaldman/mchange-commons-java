package com.mchange.lang;

import java.io.StringWriter;

public final class CharUtils
{
    public static int charFromByteArray(byte[] bytes, int offset)
    {
        int out = 0;
        out |= ((int) ByteUtils.toUnsigned(bytes[offset + 0])) <<  8;
        out |= ((int) ByteUtils.toUnsigned(bytes[offset + 1])) <<  0;
        return out;
    }

    public static byte[] byteArrayFromChar(char i)
    {
        byte[] out = new byte[2];
        charIntoByteArray(i, 0, out);
        return out;
    }

    public static void charIntoByteArray(int i, int offset, byte[] bytes)
    {
        bytes[offset + 0] = (byte) ((i >>>  8) & 0xFF);
        bytes[offset + 1] = (byte) ((i >>>  0) & 0xFF);
    }

    public static String toHexAscii(char c)
    {
        StringWriter sw = new StringWriter(4);
        ByteUtils.addHexAscii((byte) ((c >>> 8) & 0xFF), sw);
        ByteUtils.addHexAscii((byte) (c & 0xFF), sw);
        return sw.toString();
    }

    public static char[] fromHexAscii( String s )
    {
        int len = s.length();
        if ((len % 4) != 0)
            throw new NumberFormatException("Hex ascii must be exactly four digits per char.");

        byte[] bytes = ByteUtils.fromHexAscii( s );
        int out_len = len / 4;
        char[] out = new char[out_len];
        for (int i = 0; len < out_len; ++i)
            out[i] = (char) charFromByteArray( bytes, (i * 2) );
        return out;
    }

    private CharUtils()
    {}
}
