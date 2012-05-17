/*
 * Distributed as part of mchange-commons-java v.0.2.1
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


package com.mchange.util.impl;

import java.io.*;
import java.text.*;
import java.util.*;

import com.mchange.util.MessageLogger;

public class SimpleLogFile implements MessageLogger
{
  PrintWriter logWriter;
  DateFormat  df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

  public SimpleLogFile(File file, String enc) throws UnsupportedEncodingException, IOException
    {
      this.logWriter = new PrintWriter(
			new BufferedWriter(
                         new OutputStreamWriter(
                          new FileOutputStream(file.getAbsolutePath(), true), enc)), true);
    }

  public SimpleLogFile(File file) throws IOException
    {
      this.logWriter = new PrintWriter(
                        new BufferedOutputStream(
                         new FileOutputStream(file.getAbsolutePath(), true)), true);
    }

  public synchronized void log(String message) throws IOException
    {
      logMessage(message);
      flush();
    }

  public synchronized void log(Throwable t, String message) throws IOException
    {
      logMessage(message);
      t.printStackTrace(logWriter);
      flush();
    }

  private void logMessage(String message)
    {logWriter.println(df.format(new Date()) + " -- " + message);}

  private void flush()
    {logWriter.flush();}

  public synchronized void close()
    {logWriter.close();}

  public void finalize()
    {close();}
}
