package com.mchange.util;

import java.util.*;
import com.mchange.io.*;

public interface ByteArrayEnumeration extends MEnumeration, IOByteArrayEnumeration
{
  @Override
  public byte[] nextBytes();
  @Override
  public boolean hasMoreBytes();
}
  
