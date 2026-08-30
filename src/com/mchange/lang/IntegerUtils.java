package com.mchange.lang;

public final class IntegerUtils
{
    public final static long UNSIGNED_MAX_VALUE = (Integer.MAX_VALUE * 2) + 1;

    public static int parseInt(String intStr, int dflt)
    {
	if (intStr == null)
	    return dflt;

	try
	    {return Integer.parseInt(intStr);}
	catch (NumberFormatException e)
	    {return dflt;}
    }

    public static int parseInt(String intStr, int radix, int dflt)
    {
	if (intStr == null)
	    return dflt;

	try
	    {return Integer.parseInt(intStr, radix);}
	catch (NumberFormatException e)
	    {return dflt;}
    }

    public static int intFromByteArray(byte[] bytes, int offset)
    {
	int out = 0;
	out |= ((int) ByteUtils.unsignedPromote(bytes[offset + 0])) << 24;
	out |= ((int) ByteUtils.unsignedPromote(bytes[offset + 1])) << 16;
	out |= ((int) ByteUtils.unsignedPromote(bytes[offset + 2])) <<  8;
	out |= ((int) ByteUtils.unsignedPromote(bytes[offset + 3])) <<  0;
	return out;
    }

    public static byte[] byteArrayFromInt(int i)
    {
	byte[] out = new byte[4];
	intIntoByteArray(i, 0, out);
	return out;
    }

    public static void intIntoByteArray(int i, int offset, byte[] bytes)
    {
	bytes[offset + 0] = (byte) ((i >>> 24) & 0xFF);
	bytes[offset + 1] = (byte) ((i >>> 16) & 0xFF);
	bytes[offset + 2] = (byte) ((i >>>  8) & 0xFF);
	bytes[offset + 3] = (byte) ((i >>>  0) & 0xFF);
    }

    public static int intFromByteArrayLittleEndian(byte[] bytes, int offset)
    {
	int out = 0;
	out |= ((int) ByteUtils.unsignedPromote(bytes[offset + 3])) << 24;
	out |= ((int) ByteUtils.unsignedPromote(bytes[offset + 2])) << 16;
	out |= ((int) ByteUtils.unsignedPromote(bytes[offset + 1])) <<  8;
	out |= ((int) ByteUtils.unsignedPromote(bytes[offset + 0])) <<  0;
	return out;
    }

    public static void intIntoByteArrayLittleEndian(int i, int offset, byte[] bytes)
    {
	bytes[offset + 3] = (byte) ((i >>> 24) & 0xFF);
	bytes[offset + 2] = (byte) ((i >>> 16) & 0xFF);
	bytes[offset + 1] = (byte) ((i >>>  8) & 0xFF);
	bytes[offset + 0] = (byte) ((i >>>  0) & 0xFF);
    }

    public static long toUnsigned(int i) { return 0x00000000FFFFFFFFL & i; } 

    private IntegerUtils()
    {}
}
