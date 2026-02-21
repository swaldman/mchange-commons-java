package com.mchange.io;

import java.io.*;

/**
 * A map of byte[] to byte[] that may be disk or network bound.
 * This interface is not intended to be expressed by implementations
 * supporting duplicate keys.
 */
public interface IOByteArrayMap
{
  /**
   *  Gets the byte array associated with key, or none if
   *  this key is not present.
   */
  public byte[] get(byte[] key) throws IOException;

  /**
   *  Associates the byte[] key with the byte[] value in 
   *  the hash. If key is already present in the map, the
   *  old value associated with it is replaced by <TT>value</TT>.
   */
  public void put(byte[] key, byte[] value) throws IOException;

  /**
   *  Associates the byte[] key with the byte[] value in 
   *  the hash. Fails (and returns false) if key is
   *  already present in the map.
   */
  public boolean putNoReplace(byte[] key, byte[] value) throws IOException;

  /**
   *  Removes the key, value pair whose key is the argument. 
   */
  public boolean remove(byte[] key) throws IOException; 

  /**
   *  Returns true iff <TT>key</TT> is present.
   */
  public boolean containsKey(byte[] key) throws IOException;

  /**
   *  Returns a list of all keys in the hash, provided no inserts
   *  or deletes are made while the Enumeration is untraversed.
   *  If inserts or deletes are made. the behavior is undefined. 
   */
  public IOByteArrayEnumeration keys() throws IOException;
}


