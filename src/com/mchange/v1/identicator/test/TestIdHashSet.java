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

