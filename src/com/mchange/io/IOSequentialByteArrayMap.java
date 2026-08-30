package com.mchange.io;

import java.io.*;
import com.mchange.util.*;

public interface IOSequentialByteArrayMap extends IOByteArrayMap
{
  public ByteArrayComparator getByteArrayComparator();

  public Cursor getCursor();

  public interface Cursor
  {
    public ByteArrayBinding getFirst()                           throws IOException;
    public ByteArrayBinding getNext()                            throws IOException;
    public ByteArrayBinding getPrevious()                        throws IOException;
    public ByteArrayBinding getLast()                            throws IOException;
    public ByteArrayBinding getCurrent()                         throws IOException;
    public ByteArrayBinding find(byte[] key)                     throws IOException;
    public ByteArrayBinding findGreaterThanOrEqual(byte[] bytes) throws IOException;
    public ByteArrayBinding findLessThanOrEqual(byte[] bytes)    throws IOException;

    public void deleteCurrent()              throws IOException;
    public void replaceCurrent(byte[] value) throws IOException;
  }
}
