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

package com.mchange.util.impl;

import java.util.*;
import com.mchange.util.*;

public class QuotesAndWhitespaceTokenizer extends StringEnumerationHelperBase
{
  Object     current;
  LinkedList list = new LinkedList();

  public QuotesAndWhitespaceTokenizer(String string) throws IllegalArgumentException
    {
      int i = 0;
      int len = string.length();
      while (i < len)
	{
	  int q = string.indexOf ('"', i);
	  if (q >= 0)
	    {
	      StringTokenizer toks = new StringTokenizer(string.substring(i, q));
	      if (toks.hasMoreTokens()) list.add(toks);
	      int q2 = string.indexOf('"', q + 1);
	      if (q2 == -1)
		throw new IllegalArgumentException("Badly quoted string: " + string);
	      list.add(string.substring(q + 1, q2));
	      i = q2 + 1;
	    }
	  else
	    {
	      StringTokenizer toks = new StringTokenizer(string.substring(i));
	      if (toks.hasMoreTokens()) list.add(toks);
	      break;
	    }
	}
      advance();
    }

  public synchronized boolean hasMoreStrings()
    {return current != null;}

  public synchronized String nextString()
    {
      if (current instanceof String)
	{
	  String out = (String) current;
	  advance();
	  return out;
	}
      else
	{
	  StringTokenizer toks = (StringTokenizer) current;
	  String out = toks.nextToken();
	  if (!toks.hasMoreTokens()) advance();
	  return out;
	}
    }

  private void advance()
    {
      if (list.isEmpty())
	current = null;
      else
	{
	  current = list.getFirst();
	  list.removeFirst();
	}
    }

  public static void main(String[] argv)
    {
      String test = "\t  \n\r";
      //String test = "This is \"only a\" frigging \t\"test\".";
      for (StringEnumeration se = new QuotesAndWhitespaceTokenizer(test); se.hasMoreStrings();)
	System.out.println(se.nextString());
    }
}
