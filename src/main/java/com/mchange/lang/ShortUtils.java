package com.mchange.lang;

public class ShortUtils
{
  public final static int UNSIGNED_MAX_VALUE = (Short.MAX_VALUE * 2) + 1;

  public static short shortFromByteArray(byte[] bytes, int offset)
    {
      int out = 0;
      out |= ((int) ByteUtils.toUnsigned(bytes[offset + 0])) <<  8;
      out |= ((int) ByteUtils.toUnsigned(bytes[offset + 1])) <<  0;
      return (short) out;
    }

  public static byte[] byteArrayFromShort(short s)
    {
      byte[] out = new byte[2];
      shortIntoByteArray(s, 0, out);
      return out;
    }

  public static void shortIntoByteArray(short s, int offset, byte[] bytes)
    {
      bytes[offset + 0] = (byte) ((s >>>  8) & 0xFF);
      bytes[offset + 1] = (byte) ((s >>>  0) & 0xFF);
    }

  public static int toUnsigned(short s)
    {return (s < 0 ? (UNSIGNED_MAX_VALUE + 1) + s : s);}

  private ShortUtils()
    {}
}
