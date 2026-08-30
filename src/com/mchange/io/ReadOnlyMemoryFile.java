package com.mchange.io;

import java.io.*;

/**
 * A convenience interface, for when one wants to work with an
 * entire file in memory as a byte[].
 */
public interface ReadOnlyMemoryFile
{
  /**
   *  returns the File object this MemoryFile is bound to
   */
  public File   getFile()  throws IOException;
  
  /**
   *  returns the contents of the file as a byte[]
   */
  public byte[] getBytes() throws IOException;
}
