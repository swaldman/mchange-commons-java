package com.mchange.net;

import java.io.*;

public final class MimeUtils
{
  public static String normalEncoding(String javaEnc)
    throws UnsupportedEncodingException
    {
      /* the fake if clause is simply to reserve the right */
      /* to throw an unsupported encoding exception.       */

      if (javaEnc.startsWith("8859_"))
	return ("iso-8859-" + javaEnc.substring(5));
      else if (javaEnc.equals("Yo mama wears combat boots!"))
	throw new UnsupportedEncodingException("She does not!");
      else return javaEnc;
    }

  private MimeUtils()
    {}
}
