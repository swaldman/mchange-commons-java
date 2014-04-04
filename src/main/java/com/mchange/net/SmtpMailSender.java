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

import java.io.*;
import java.net.*;
import java.util.Properties;

public class SmtpMailSender implements MailSender
{
  InetAddress hostAddr;
  int         port;

  public SmtpMailSender(InetAddress hostAddr, int port)
    {
      this.hostAddr = hostAddr;
      this.port     = port;
    }

  public SmtpMailSender(InetAddress hostAddr)
    {this(hostAddr, SmtpUtils.DEFAULT_SMTP_PORT);}

  public SmtpMailSender(String host, int port) throws UnknownHostException
    {this(InetAddress.getByName(host), port);}

  public SmtpMailSender(String host) throws UnknownHostException
    {this(host, SmtpUtils.DEFAULT_SMTP_PORT);}

  public void sendMail(String from, String[] to, String[] cc,  String[] bcc, String subject, String body, String enc)
    throws IOException, ProtocolException, UnsupportedEncodingException
    {
      if (to == null || to.length < 1)
	throw new SmtpException("You must specify at least one recipient in the \"to\" field.");
      Properties headers = new Properties();
      headers.put("From", from);
      headers.put("To", makeRecipientString(to));
      headers.put("Subject", subject);
      headers.put("MIME-Version", "1.0");
      headers.put("Content-Type", "text/plain; charset=" + MimeUtils.normalEncoding(enc));
      headers.put("X-Generator", this.getClass().getName());

      String[] rcptTo;
      if (cc != null || bcc != null)
	{
	  int recipients = to.length + (cc != null ? cc.length : 0) + (bcc != null ? bcc.length : 0);
	  rcptTo = new String[recipients];
	  int next_r = 0;
	  System.arraycopy(to, 0, rcptTo, next_r, to.length);
	  next_r += to.length;
	  if (cc != null)
	    {
	      System.arraycopy(cc, 0, rcptTo, next_r, cc.length);
	      next_r += cc.length;
	      headers.put("CC", makeRecipientString(cc));
	    }
	  if (bcc != null)
	    System.arraycopy(bcc, 0, rcptTo, next_r, bcc.length);
	}
      else rcptTo = to;
      SmtpUtils.sendMail(hostAddr, port, from, rcptTo, headers, body.getBytes(enc));
    }

  public void sendMail(String from, String[] to, String[] cc,  String[] bcc, String subject, String body)
    throws IOException, ProtocolException
    {
      try
	{sendMail(from, to, cc, bcc, subject, body, System.getProperty("file.encoding"));}
      catch (UnsupportedEncodingException e)
	{throw new InternalError("Default encoding [" + System.getProperty("file.encoding") + "] not supported???");}
    }

  private static String makeRecipientString(String[] to)
    {
      StringBuffer sb = new StringBuffer(256);
      for (int i = 0, len = to.length; i < len; ++i)
	{
	  if (i != 0) sb.append(", ");
	  sb.append(to[i]);
	}
      return sb.toString();
    }

  public static void main(String[] argv)
    {
      try
	{
	  String[] to      = {"stevewaldman@uky.edu"};
	  String[] cc      = {};
	  String[] bcc     = {"stevewaldman@mac.com"};
	  String   from    = "swaldman@mchange.com";
	  String   subject = "Test SmtpMailSender Again";
	  String   body    = "Wheeeee!!!";

	  MailSender sender = new SmtpMailSender("localhost");
	  sender.sendMail(from, to, cc, bcc, subject, body); 
	}
      catch (Exception e)
	{e.printStackTrace();}
    }
}
