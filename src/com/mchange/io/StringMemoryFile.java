package com.mchange.io;

import java.io.*;

public interface StringMemoryFile extends ReadOnlyMemoryFile
{ 
  /**
   * Converts the contents of the file to which this object is bound
   * to a String using the default character encoding.
   */
  public String asString() throws IOException;

  /**
   * Converts the contents of the file to which this object is bound
   * to a String using the specified character encoding.
   */
  public String asString(String enc) throws IOException, UnsupportedEncodingException;
}
