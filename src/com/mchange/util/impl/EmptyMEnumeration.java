package com.mchange.util.impl;

import java.util.*;
import com.mchange.util.*;

public class EmptyMEnumeration implements MEnumeration
{
  public static MEnumeration SINGLETON = new EmptyMEnumeration();

  private EmptyMEnumeration()       {}

  public Object   nextElement()     {throw new NoSuchElementException();}
  public boolean  hasMoreElements() {return false;}
}
