package com.mchange.io;

import java.io.*;

/**
 * A typed IOEnumeration that returns byte[]'s
 */
public interface IOByteArrayEnumeration extends IOEnumeration
{
  /**
   *  gets the next byte[] in the enumeration. Throws NoSuchElementException
   *  if no more byte arrays remain.
   */
  public byte[] nextBytes() throws IOException;

  /**
   *  checks whether any more byte arrays remain in the enumeration.
   */
  public boolean hasMoreBytes() throws IOException;

  /**
   *  gets the next byte[] in the enumeration, returning it as an Object. 
   *  Throws NoSuchElementException if no more byte arrays remain.
   */
  public Object  nextElement() throws IOException;

  /**
   *  checks whether any more byte arrays remain in the enumeration.
   */
  public boolean hasMoreElements() throws IOException;
}
