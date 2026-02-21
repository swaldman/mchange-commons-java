package com.mchange.util;

public interface ByteArrayComparator
{
  /**
   * Must return a value<PRE>
   *     less than 0    iff left < right
   *     equal to 0     iff left = right
   *     greater than 0 iff left > right
   * </PRE>
   */
  public int compare(byte[] left, byte[] right);
}
