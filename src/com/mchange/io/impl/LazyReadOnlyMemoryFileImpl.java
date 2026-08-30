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
