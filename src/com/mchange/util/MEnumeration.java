package com.mchange.util;

import java.util.*;
import com.mchange.io.*;

public interface MEnumeration extends IOEnumeration, Enumeration
{
  public static MEnumeration EMPTY = com.mchange.util.impl.EmptyMEnumeration.SINGLETON;

  @Override
  public Object  nextElement();
  @Override
  public boolean hasMoreElements();
}
