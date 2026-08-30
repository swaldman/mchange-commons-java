package com.mchange.util;

public interface Base64Encoder
{
  public String encode(byte[] bytes);
  public byte[] decode(String b64text) throws Base64FormatException;
}
