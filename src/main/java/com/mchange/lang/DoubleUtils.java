package com.mchange.lang;

public final class DoubleUtils
{
    public static byte[] byteArrayFromDouble( double f )
    {
	long i = Double.doubleToLongBits( f );
	return LongUtils.byteArrayFromLong(i);
    }

    public static double doubleFromByteArray( byte[] b, int offset )
    {
	long i = LongUtils.longFromByteArray( b, offset );
	return Double.longBitsToDouble(i);
    }
}

