package com.mchange.util;

import java.io.*;
import com.mchange.io.*;

public interface StringEnumeration extends MEnumeration, IOStringEnumeration
{
  @Override
  public boolean hasMoreStrings();
  @Override
  public String  nextString();
}
