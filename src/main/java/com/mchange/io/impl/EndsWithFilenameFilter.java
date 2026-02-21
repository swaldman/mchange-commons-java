package com.mchange.io.impl;

import java.io.*;

public class EndsWithFilenameFilter implements FilenameFilter
{
  public final static int ALWAYS = 0;
  public final static int NEVER  = 1;
  public final static int MATCH  = 2;

  String[] endings = null;
  int      accept_dirs;

  public EndsWithFilenameFilter(String[] endings, int accept_dirs)
    {
      this.endings    = endings;
      this.accept_dirs = accept_dirs;
    }

  public EndsWithFilenameFilter(String ending, int accept_dirs)
    {
      this.endings = new String[]{ending};
      this.accept_dirs = accept_dirs;
    }

  public boolean accept(File dir, String name)
    {
      if (accept_dirs != MATCH && new File(dir, name).isDirectory()) return (accept_dirs == ALWAYS);
      for (int i = endings.length; --i >= 0;)
	if (name.endsWith(endings[i])) return true;
      return false;
    }
}
