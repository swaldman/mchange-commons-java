package com.mchange.util;

import com.mchange.io.*;

public interface ByteArrayMap extends IOByteArrayMap
{
  /**
   *  Gets the byte array associated with key, or none if
   *  this key is not present.
   */
  public byte[] get(byte[] key);

  /**
   *  Associates the byte[] key with the byte[] value in 
   *  the hash. If key is already present in the map, the
   *  old value associated with it is replaced by <TT>value</TT>.
   */
  public void put(byte[] key, byte[] value);

  /**
   *  Associates the byte[] key with the byte[] value in 
   *  the hash. Fails (and returns false) if key is
   *  already present in the map.
   */
  public boolean putNoReplace(byte[] key, byte[] value);

  /**
   *  Removes the key, value pair whose key is the argument. 
   */
  public boolean remove(byte[] key);

  /**
   *  Returns true iff <TT>key</TT> is present.
   */
  public boolean containsKey(byte[] key);

  /**
   *  Returns a list of all keys in the hash, provided no inserts
   *  or deletes are made while the Enumeration is untraversed.
   *  If inserts or deletes are made. the behavior is undefined. 
   */
  public IOByteArrayEnumeration keys();

  /**
   *  returns a simple Enumeration (that does not throw IOExceptions)
   */
  public ByteArrayEnumeration mkeys();
}
