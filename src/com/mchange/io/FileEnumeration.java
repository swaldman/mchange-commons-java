package com.mchange.io;

import java.io.*;

public interface FileEnumeration extends IOEnumeration
{
  public boolean hasMoreFiles() throws IOException;
  public File nextFile() throws IOException;
}
