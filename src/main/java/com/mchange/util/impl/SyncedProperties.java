/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

package com.mchange.util.impl;

import java.io.*;
import java.util.*;
import com.mchange.io.InputStreamUtils;
import com.mchange.io.OutputStreamUtils;

public class SyncedProperties 
{
  private final static String[] SA_TEMPLATE  = new String[0];
  private final static byte     H_START_BYTE = (byte) '#';
  private final static byte[]   H_LF_BYTES;
  private final static String   ASCII = "8859_1";
  
  static
    {
      try
	{H_LF_BYTES    = System.getProperty("line.separator", "\r\n").getBytes(ASCII);}
      catch (UnsupportedEncodingException e)
	{throw new InternalError("Encoding " + ASCII + " not supported ?!?");}
    }

  Properties props;
  byte[]     headerBytes;
  File       file;
  long       last_mod = -1;

  public SyncedProperties(File file, String header) throws IOException
    {this(file, makeHeaderBytes(header));}

  public SyncedProperties(File file, String[] header) throws IOException
    {this(file, makeHeaderBytes(header));}

  public SyncedProperties(File file) throws IOException
    {this(file, (byte[]) null);}

  private SyncedProperties(File file, byte[] headerBytes) throws IOException
    {
      if (file.exists())
	{
	  if (!file.isFile())
	    throw new IOException(file.getPath() + ": Properties file can't be a directory or special file!");
	  if (headerBytes == null)
	    {
	      //I'd rather not use a localized reader; I'd rather have a fixed, location
	      //independent file format, but Properties uses a localized print stream to write.
	      BufferedReader br = null;
	      try
		{
		  br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

		  List list = new LinkedList();
		  String line = br.readLine();
		  while (line.trim().equals(""))
		    line = br.readLine();
		  while (line.charAt(0) == '#')
		    list.add(line.substring(1).trim());
		  headerBytes = makeHeaderBytes((String[]) list.toArray(SA_TEMPLATE));
		}
	      finally
		{if (br != null) br.close();}
	    }
	}
      /*
      else //workaround fact that canWrite() returns false one non-existent but creatable files by creating...
	{
	  OutputStream os = null;
	  try
	    {
	      os = new FileOutputStream(file);
	      os.write('\n');
	      os.flush();
	    }
	  finally
	    {if (os == null) os.close();}
	}
      */
      if (!file.canWrite())
	throw new IOException("Can't write to file " + file.getPath());
      this.props       = new Properties();
      this.headerBytes = headerBytes;
      this.file        = file;
      ensureUpToDate();
    }

  public synchronized String getProperty(String property) throws IOException
    {
      ensureUpToDate();
      return props.getProperty(property);
    }

  public synchronized String getProperty(String property, String defaultValue) throws IOException
    {
      String out = props.getProperty(property);
      return (out == null ? defaultValue : out);
    }

  public synchronized void put(String property, String value) throws IOException
    {
      ensureUpToDate();
      props.put(property, value);
      rewritePropsFile();
    }

  public synchronized void remove(String property) throws IOException
    {
      ensureUpToDate();
      props.remove(property);
      rewritePropsFile();
    }

  public synchronized void clear() throws IOException
    {
      ensureUpToDate();
      props.clear();
      rewritePropsFile();
    }

  public synchronized boolean contains(String value) throws IOException
    {
      ensureUpToDate();
      return props.contains(value);
    }

  public synchronized boolean containsKey(String key) throws IOException
    {
      ensureUpToDate();
      return props.containsKey(key);
    }

  public synchronized Enumeration elements() throws IOException
    {
      ensureUpToDate();
      return props.elements();
    }

  public synchronized Enumeration keys() throws IOException
    {
      ensureUpToDate();
      return props.keys();
    }

  public synchronized int size() throws IOException
    {
      ensureUpToDate();
      return props.size();
    }

  public synchronized boolean isEmpty() throws IOException
    {
      ensureUpToDate();
      return props.isEmpty();
    }

  private synchronized void ensureUpToDate() throws IOException
    {
      long new_mod = file.lastModified();
      if (new_mod > last_mod)
	{
	  InputStream is = null; 
	  try
	    {
	      is = new BufferedInputStream(new FileInputStream(file));
	      props.clear();
	      props.load(is);
	      this.last_mod = new_mod;
	    }
	  finally
	    {InputStreamUtils.attemptClose(is);}
	}
    }

  private synchronized void rewritePropsFile() throws IOException
    {
      OutputStream os = null;
      try
	{
	  os = new BufferedOutputStream(new FileOutputStream(file));
	  if (headerBytes != null) os.write(headerBytes);
	  props.store(os, null);
	  os.flush();
	  this.last_mod = file.lastModified();
	}
      finally
	{OutputStreamUtils.attemptClose(os);}
    }

  private static byte[] makeHeaderBytes(String[] header)
    {
      try
	{
	  ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  for (int i = 0, len = header.length; i < len; ++i)
	    {
	      baos.write(H_START_BYTE);
	      baos.write(header[i].getBytes());
	      baos.write(H_LF_BYTES);
	    }
	  return baos.toByteArray();
	}
      catch (IOException e)
	{throw new InternalError("IOException working with ByteArrayOutputStream?!?");}
    }

  private static byte[] makeHeaderBytes(String header)
    {
      try
	{
	  ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  baos.write(H_START_BYTE);
	  baos.write(header.getBytes());
	  baos.write(H_LF_BYTES);
	  return baos.toByteArray();
	}
      catch (IOException e)
	{throw new InternalError("IOException working with ByteArrayOutputStream?!?");}
    }
}
