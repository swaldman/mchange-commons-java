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
