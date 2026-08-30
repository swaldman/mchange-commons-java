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
