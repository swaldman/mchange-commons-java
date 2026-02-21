package com.mchange.net;

import java.io.*;

public interface MailSender
{
  /**
   *  cc and bcc may be null.
   */
  public void sendMail(String from, String[] to, String[] cc,  String[] bcc, String subject, String body, String encoding)
    throws IOException, ProtocolException, UnsupportedEncodingException;

  /**
   * cc and bcc may be null.
   * Uses the default encoding...
   */
  public void sendMail(String from, String[] to, String[] cc,  String[] bcc, String subject, String body)
    throws IOException, ProtocolException;
}
