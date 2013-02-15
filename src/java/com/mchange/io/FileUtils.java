/*
 * Distributed as part of mchange-commons-java v.0.2.4
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.io;

import java.io.*;

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
