package com.mchange.util;

import java.io.*;

public interface IntEnumeration extends MEnumeration
{
  public boolean hasMoreInts();
  public int     nextInt();
}
