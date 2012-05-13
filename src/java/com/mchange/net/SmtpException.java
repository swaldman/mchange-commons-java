package com.mchange.net;

public class SmtpException extends ProtocolException
{
  int resp_num;

  public SmtpException()
    {super();}

  public SmtpException(String msg)
    {super(msg);}

  public SmtpException(int num, String msg)
    {
      this(msg);
      this.resp_num = num;
    }

  public int getSmtpResponseNumber()
    {return resp_num;}
}
