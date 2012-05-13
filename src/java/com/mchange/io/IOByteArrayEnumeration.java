/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
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
 *  IOByteArrayEnumeration.java
 *  A typed IOEnumeration that returns byte[]'s
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
