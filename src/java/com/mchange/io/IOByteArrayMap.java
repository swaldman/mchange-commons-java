/*
 * Distributed as part of mchange-commons-java v.0.2.3
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


/*
 *  IOByteArrayMap.java
 *
 *  A map of byte[] to byte[] that may be disk or network bound.
 *  This interface is not intended to be expressed by implementations
 *  supporting duplicate keys.
 *
 *  (C) Copyright 1998, Machinery For Change, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

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


