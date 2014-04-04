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
