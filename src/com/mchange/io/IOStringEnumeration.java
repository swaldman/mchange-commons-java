package com.mchange.io;

import java.io.*;

public interface IOStringEnumeration extends IOEnumeration
{
  public boolean hasMoreStrings() throws IOException;
  public String  nextString()     throws IOException;
}
