/*
 * Distributed as part of mchange-commons-java v.0.2.1
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
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


package com.mchange.v1.identicator.test;

import java.sql.*;
import java.util.*;
import java.lang.reflect.*;
import com.mchange.v1.identicator.*;
import com.mchange.v1.lang.*;
import com.mchange.v1.util.*;

public class TestIdHashSet
{
    public static void main(String[] argv)
    {
  	Identicator id = new Identicator()
  	    {
  		public boolean identical(Object a, Object b)
  		{ return ((String) a).charAt(0) == ((String) b).charAt(0); }
		
  		public int hash(Object o)
  		{ return ((String) o).charAt(0); }
  	    };

  	Set s = new IdHashSet(id);
  	System.out.println(s.add("hello"));
  	System.out.println(s.add("world"));
  	System.out.println(s.add("hi"));
  	System.out.println(s.size());
  	Object[] elems = s.toArray();
  	for (int i = 0; i < elems.length; ++i)
  	    System.out.println( elems[i] );
    }
}

