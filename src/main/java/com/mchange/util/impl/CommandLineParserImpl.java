/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

/*
 * This class is modified from swaldman.util.CommandLineParser,
 * which was written at the MIT Media Lab by Steve Waldman 
 */
package com.mchange.util.impl;

import com.mchange.util.*;

/**
 * @deprecated Use com.mchange.v2.cmdline.CommandLineUtils instead
 */
public class CommandLineParserImpl extends Object implements CommandLineParser
{
  String[] argv;
  String[] validSwitches;
  String[] reqSwitches;
  String[] argSwitches;
  char switch_char;

  public CommandLineParserImpl(String[] argv, String[] validSwitches, String[] reqSwitches, 
			       String[] argSwitches, char switch_char)
  {
    this.argv = argv;
    this.validSwitches = (validSwitches == null ? new String[0] : validSwitches);
    this.reqSwitches   = (reqSwitches == null ? new String[0] : reqSwitches);
    this.argSwitches   = (argSwitches == null ? new String[0] : argSwitches);
    this.switch_char = switch_char;
  }

  public CommandLineParserImpl(String[] argv, String[] validSwitches, String[] reqSwitches, String[] argSwitches)
  {this(argv, validSwitches, reqSwitches, argSwitches, '-');}

  public boolean checkSwitch(String sw)
  {
    for (int i = 0; i < argv.length; ++i)
      if (argv[i].charAt(0) == switch_char && argv[i].equals(switch_char + sw)) 
	return true;
    return false;
  }

  public String findSwitchArg(String sw)
  {
    for (int i = 0; i < argv.length - 1; ++i)
      if (argv[i].charAt(0) == switch_char && argv[i].equals(switch_char + sw))
	return (argv[i+1].charAt(0) == switch_char ? null : argv[i+1]);
    return null;
  }

  public boolean checkArgv()
    {
//       boolean out = checkValidSwitches();
//       System.out.println(out);
//       System.out.println(out &= checkRequiredSwitches());
//       System.out.println(out &= checkSwitchArgSyntax());
//       return out;
      return checkValidSwitches() && checkRequiredSwitches() && checkSwitchArgSyntax();
    }

  boolean checkValidSwitches()
  {
    i_loop: 
    for (int i = 0; i < argv.length; ++ i)
      if (argv[i].charAt(0) == switch_char)
	{
	  for (int j = 0; j < validSwitches.length; ++j)
	    if (argv[i].equals(switch_char + validSwitches[j])) continue i_loop;
	  return false;	
	}
    return true;
  }

  boolean checkRequiredSwitches()
    {
      for (int i = reqSwitches.length; --i >= 0;)
	if (!checkSwitch(reqSwitches[i])) return false;
      return true;
    }

  boolean checkSwitchArgSyntax()
    {
      for (int i = argSwitches.length; --i >= 0;)
	{
	  if (checkSwitch(argSwitches[i]))
	    {
	      String check = findSwitchArg(argSwitches[i]);
	      if (check == null || check.charAt(0) == switch_char)
		return false;
	    }
	}
      return true;
    }

  public int findLastSwitched()
    {
      for (int i = argv.length; --i >= 0;)
	if (argv[i].charAt(0) == switch_char)
	  return i;
      return -1;
    }

  public String[] findUnswitchedArgs()
    {
      String[] bigArray = new String[argv.length];
      int count = 0;
      for (int i = 0; i < argv.length; ++i)
	{
	  if (argv[i].charAt(0) == switch_char)
	    {if (contains(argv[i].substring(1), argSwitches)) ++i;}
	  else bigArray[count++] = argv[i];
	}
      String[] out = new String[count];
      System.arraycopy(bigArray, 0, out, 0, count);
      return out;
    }

  private static boolean contains(String string, String[] list)
    {
      for (int i = list.length; --i >= 0;)
	if (list[i].equals(string)) return true;
      return false;
    }
}


  






