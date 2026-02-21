package com.mchange.io;

import java.io.*;

import com.mchange.v1.io.InputStreamUtils;

public final class FileUtils
{
  public static byte[] getBytes(File file, int max_len) throws IOException
    {
      InputStream is = new BufferedInputStream(new FileInputStream(file));
      try
	{return InputStreamUtils.getBytes(is, max_len);}
      finally
	{InputStreamUtils.attemptClose(is);}
    }

  public static byte[] getBytes(File file) throws IOException
    {
      InputStream is = new BufferedInputStream(new FileInputStream(file));
      try
	{return InputStreamUtils.getBytes(is);}
      finally
	{InputStreamUtils.attemptClose(is);}
    }

  public static String getContentsAsString(File file, String enc)
    throws IOException, UnsupportedEncodingException
    {
      InputStream is = new BufferedInputStream(new FileInputStream(file));
      try
	{return InputStreamUtils.getContentsAsString(is, enc);}
      finally
	{InputStreamUtils.attemptClose(is);}
    }


  public static String getContentsAsString(File file)
    throws IOException
    {
      InputStream is = new BufferedInputStream(new FileInputStream(file));
      try
	{return InputStreamUtils.getContentsAsString(is);}
      finally
	{InputStreamUtils.attemptClose(is);}
    }

  public static String getContentsAsString(File file, int max_len, String enc)
    throws IOException, UnsupportedEncodingException
    {
      InputStream is = new BufferedInputStream(new FileInputStream(file));
      try
	{return InputStreamUtils.getContentsAsString(is, max_len, enc);}
      finally
	{InputStreamUtils.attemptClose(is);}
    }

  public static String getContentsAsString(File file, int max_len)
    throws IOException
    {
      InputStream is = new BufferedInputStream(new FileInputStream(file));
      try
	{return InputStreamUtils.getContentsAsString(is, max_len);}
      finally
	{InputStreamUtils.attemptClose(is);}
    }

  private FileUtils()
    {}
}
