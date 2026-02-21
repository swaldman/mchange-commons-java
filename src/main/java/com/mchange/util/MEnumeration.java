package com.mchange.util;

import java.util.*;
import com.mchange.io.*;

public interface MEnumeration extends IOEnumeration, Enumeration
{
  public static MEnumeration EMPTY = com.mchange.util.impl.EmptyMEnumeration.SINGLETON;

  public Object  nextElement();
  public boolean hasMoreElements();
}
