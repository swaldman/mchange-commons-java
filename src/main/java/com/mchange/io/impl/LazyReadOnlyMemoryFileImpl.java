/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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
