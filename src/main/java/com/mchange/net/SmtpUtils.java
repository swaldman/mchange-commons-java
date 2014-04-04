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

package com.mchange.net;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Properties;
import com.mchange.io.OutputStreamUtils;
import com.mchange.io.ReaderUtils;
import com.mchange.net.SocketUtils;

public final class SmtpUtils 
{
  private final static String ENC         = "8859_1";
  private final static String CRLF        = "\r\n";
  private final static String CHARSET     = "charset";
  private final static int    CHARSET_LEN = CHARSET.length();

  public final static int DEFAULT_SMTP_PORT = 25;

  public static void sendMail(InetAddress smtpHost, int smtpPort, 
			      String mailFrom, String[] rcptTo,
			      Properties headers,
			      byte[] bodyBytes)
    throws IOException, SmtpException
    {
      Socket           s    = null;
      DataOutputStream dout = null;
      BufferedReader   br   = null;

      try
	{
	  s    = new Socket(smtpHost, smtpPort);
	  dout = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
	  br   = new BufferedReader(new InputStreamReader(s.getInputStream(), ENC));
	  ensureResponse(br, 200, 300);
	  dout.writeBytes("HELO " + s.getLocalAddress().getHostName() + CRLF);
	  //System.out.print("HELO " + s.getLocalAddress().getHostName() + CRLF);
	  dout.flush();
	  ensureResponse(br, 200, 300);
	  dout.writeBytes("MAIL FROM: " + mailFrom + CRLF);
	  //System.out.print("MAIL FROM: " + mailFrom + CRLF);
	  dout.flush();
	  ensureResponse(br, 200, 300);
	  for (int i = rcptTo.length; --i >= 0;)
	    {
	      dout.writeBytes("RCPT TO: " + rcptTo[i] + CRLF);
	      //System.out.print("RCPT TO: " + rcptTo[i] + CRLF);
	      dout.flush();
	      ensureResponse(br, 200, 300);
	    }
	  dout.writeBytes("DATA" + CRLF);
	  //System.out.print("DATA" + CRLF);
	  dout.flush();
	  ensureResponse(br, 300, 400);

	  for (Enumeration e = headers.keys(); e.hasMoreElements();)
	    {
	      String key = (String) e.nextElement();
	      String value = headers.getProperty(key);
	      dout.writeBytes(key + ": " + value + CRLF);
	    }
	  dout.writeBytes(CRLF);
	  dout.write(bodyBytes);
	  dout.writeBytes(CRLF + '.' + CRLF);
	  dout.flush();
	  ensureResponse(br, 200, 300);
	  dout.writeBytes("QUIT" + CRLF);
	  dout.flush();
	}
      catch (UnsupportedEncodingException e)
	{
	  e.printStackTrace();
	  throw new InternalError(ENC + " not supported???");
	}
      finally
	{
	  OutputStreamUtils.attemptClose(dout);
	  ReaderUtils.attemptClose(br);
	  SocketUtils.attemptClose(s);
	}
    }

  private static String encodingFromContentType(String contentType)
    {
      int cs_index = contentType.indexOf(CHARSET);
      if (cs_index >= 0)
	{
	  String cs = contentType.substring(cs_index + CHARSET_LEN);
	  cs = cs.trim();
	  //is this a good way to handle some bizarre situation
	  //where charset shows up other than as an attribute?
	  if (cs.charAt(0) != '=') return encodingFromContentType(cs);
	  cs = cs.substring(1).trim();
	  int semidex = cs.indexOf(';');
	  if (semidex >= 0)
	    cs = cs.substring(0, semidex);
	  return cs;
	}
      else return null;
    }

  private static byte[] bytesFromBodyString(String body, String enc) throws UnsupportedEncodingException
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, enc));
      pw.print(body);
      pw.flush();
      return baos.toByteArray();
    }

  private static void ensureResponse(BufferedReader br, int begin_inclusive, int end_exclusive) 
    throws IOException, SmtpException
    {
      String smtpRespStr;
      int smtp_resp_num;
      smtpRespStr = br.readLine();
      //System.out.println(smtpRespStr);
      try
	{
	  smtp_resp_num = Integer.parseInt(smtpRespStr.substring(0,3));
	  while (smtpRespStr.charAt(3) == '-') smtpRespStr = br.readLine();
	  if (smtp_resp_num < begin_inclusive || smtp_resp_num >= end_exclusive)
	    throw new SmtpException(smtp_resp_num, smtpRespStr);
	}
      catch (NumberFormatException e)
	{throw new SmtpException("Bad SMTP response while mailing document!");}
    }

  public static void main(String[] argv)
    {
      try
	{
	  InetAddress smtpHost = InetAddress.getByName("mailhub.mchange.com");
	  int         smtpPort = 25;
	  String      mailFrom = "octavia@mchange.com";
	  String[]    rcptTo   = {"swaldman@mchange.com", "sw-lists@mchange.com"};

	  Properties props = new Properties();
	  props.put("From", "goolash@mchange.com");
	  props.put("To", "garbage@mchange.com");
	  props.put("Subject", "Test test test AGAIN...");

	  byte[] bodyBytes = "This is a test AGAIN! Imagine that!".getBytes(ENC);

	  sendMail(smtpHost, smtpPort, mailFrom, rcptTo, props, bodyBytes);
	}
      catch (Exception e)
	{e.printStackTrace();}
    }

  private SmtpUtils()
    {}
}
