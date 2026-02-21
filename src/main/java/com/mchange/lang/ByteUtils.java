package com.mchange.lang;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

public final class ByteUtils
{
  public final static short UNSIGNED_MAX_VALUE = (Byte.MAX_VALUE * 2) + 1;

  /**
   * @deprecated prefer unsignedPromote(...)
   */  
  public static short toUnsigned(byte b)
    {return (short) (b < 0 ? (UNSIGNED_MAX_VALUE + 1) + b : b);}

  public static int unsignedPromote(byte b)
    { return b & 0xff; }

  public static String toHexAscii(byte b)
    {
      StringWriter sw = new StringWriter(2);
      addHexAscii(b, sw);
      return sw.toString();
    }

  public static String toLowercaseHexAscii(byte b)
    {
      StringWriter sw = new StringWriter(2);
      addLowercaseHexAscii(b, sw);
      return sw.toString();
    }

  public static String toHexAscii(byte[] bytes)
    {
      int len = bytes.length;
      StringWriter sw = new StringWriter(len * 2);
      for (int i = 0; i < len; ++i)
	addHexAscii(bytes[i], sw);
      return sw.toString();
    }

  public static String toLowercaseHexAscii(byte[] bytes)
    {
      int len = bytes.length;
      StringWriter sw = new StringWriter(len * 2);
      for (int i = 0; i < len; ++i)
	addLowercaseHexAscii(bytes[i], sw);
      return sw.toString();
    }

  public static byte[] fromHexAscii(String s) throws NumberFormatException
    {
      try
	{
	  int len = s.length();
	  if ((len % 2) != 0)
	    throw new NumberFormatException("Hex ascii must be exactly two digits per byte.");
	  
	  int out_len = len / 2;
	  byte[] out = new byte[out_len];
	  int i = 0;
	  StringReader sr = new StringReader(s); 
	  while (i < out_len)
	    {
	      int val = (16 * fromHexDigit(sr.read())) + fromHexDigit(sr.read()); 
	      out[i++] = (byte) val;
	    }
	  return out;
	}
      catch (IOException e)
	{throw new InternalError("IOException reading from StringReader?!?!");}
    }

  static void addHexAscii(byte b, StringWriter sw)
  {
      int ub = unsignedPromote(b);
      int h1 = ub / 16;
      int h2 = ub % 16;
      sw.write(toHexDigit(h1));
      sw.write(toHexDigit(h2));
  }

  static void addLowercaseHexAscii(byte b, StringWriter sw )
  {
      int ub = unsignedPromote(b);
      int h1 = ub / 16;
      int h2 = ub % 16;
      sw.write(toLowercaseHexDigit(h1));
      sw.write(toLowercaseHexDigit(h2));
  }

  private static int fromHexDigit(int c) throws NumberFormatException
    {
      if (c >= 0x30 && c < 0x3A)
	return c - 0x30;
      else if (c >= 0x41 && c < 0x47)
	return c - 0x37;
      else if (c >= 0x61 && c < 0x67)
	return c - 0x57;
      else 
	throw new NumberFormatException('\'' + c + "' is not a valid hexadecimal digit.");
    }

  /* note: we do no arg. checking, because     */
  /* we only ever call this from addHexAscii() */
  /* above, and we are sure the args are okay  */
  private static char toHexDigit(int h)
    {
	char out;
	if (h <= 9) out = (char) (h + 0x30);
	else out = (char) (h + 0x37);
	//System.err.println(h + ": " + out);
	return out;
    }


  /* note: we do no arg. checking, because     */
  /* we only ever call this from addHexAscii() */
  /* above, and we are sure the args are okay  */
  private static char toLowercaseHexDigit(int h)
    {
	char out;
	if (h <= 9) out = (char) (h + 0x30);
	else out = (char) (h + 0x57);
	return out;
    }


  private ByteUtils()
    {}
}
