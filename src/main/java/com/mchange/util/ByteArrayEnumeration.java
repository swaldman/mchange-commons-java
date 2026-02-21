package com.mchange.util;

import java.util.*;
import com.mchange.io.*;

public interface ByteArrayEnumeration extends MEnumeration, IOByteArrayEnumeration
{
  public byte[] nextBytes();
  public boolean hasMoreBytes();
}
  
