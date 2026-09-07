package com.mchange.util.impl;

import java.util.*;
import com.mchange.util.*;

public class EmptyMEnumeration implements MEnumeration
{
  public static MEnumeration SINGLETON = new EmptyMEnumeration();

  private EmptyMEnumeration()       {}

  @Override
  public Object   nextElement()     {throw new NoSuchElementException();}
  @Override
  public boolean  hasMoreElements() {return false;}
}
