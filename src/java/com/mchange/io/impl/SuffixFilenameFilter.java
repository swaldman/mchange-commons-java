/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.io.impl;

import java.io.*;

public class SuffixFilenameFilter implements FilenameFilter
{
  public final static int ALWAYS = 0;
  public final static int NEVER  = 1;
  public final static int MATCH  = 2;

  String[] suffixes = null;
  int      accept_dirs;

  public SuffixFilenameFilter(String[] suffixes, int accept_dirs)
    {
      this.suffixes    = suffixes;
      this.accept_dirs = accept_dirs;
    }

  public SuffixFilenameFilter(String suffix, int accept_dirs)
    {
      this.suffixes = new String[]{suffix};
      this.accept_dirs = accept_dirs;
    }

  public boolean accept(File dir, String name)
    {
      if (accept_dirs != MATCH && new File(dir, name).isDirectory()) return (accept_dirs == ALWAYS);
      for (int i = suffixes.length; --i >= 0;)
	if (name.endsWith(suffixes[i])) return true;
      return false;
    }
}
