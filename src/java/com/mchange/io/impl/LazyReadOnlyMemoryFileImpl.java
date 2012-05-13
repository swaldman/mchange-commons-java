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


package com.mchange.io.impl;

import java.io.*;
import com.mchange.io.*;

public class LazyReadOnlyMemoryFileImpl implements ReadOnlyMemoryFile
{
  File   file;
  byte[] bytes    = null;
  long   last_mod = -1;
  int    last_len = -1;

  public LazyReadOnlyMemoryFileImpl(File file)
    {this.file = file;}

  public LazyReadOnlyMemoryFileImpl(String fname)
    {this(new File(fname));}

  public File getFile()
    {return file;}

  public synchronized byte[] getBytes() throws IOException
    {
      update();
      return bytes;
    }

  /* should be called in sync'ed methods */
  void update() throws IOException
    {
      if (file.lastModified() > last_mod)
	{
	  if (bytes != null)
	    last_len = bytes.length;
	  refreshBytes();
	}
    }

  /* should be called in sync'ed methods */
  void refreshBytes() throws IOException
    {
      ByteArrayOutputStream baos = (last_len > 0 ?
				    new ByteArrayOutputStream(2 * last_len) :
				    new ByteArrayOutputStream());

      InputStream is = new BufferedInputStream(new FileInputStream(file));

      for(int b = is.read(); b >= 0; b = is.read()) baos.write((byte) b);
      this.bytes = baos.toByteArray();
      this.last_mod = file.lastModified();
    }
}
