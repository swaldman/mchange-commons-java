package com.mchange.util;

import java.io.*;
import com.mchange.io.*;

public interface StringEnumeration extends MEnumeration, IOStringEnumeration
{
  public boolean hasMoreStrings();
  public String  nextString();
}
