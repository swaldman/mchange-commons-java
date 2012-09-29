/*
 * Distributed as part of mchange-commons-java v.0.2.3
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

import java.util.*;
import com.mchange.v1.identicator.*;


public class TestIdWeakHashMap
{
    final static Identicator id = new Identicator()
    {
	public boolean identical(Object a, Object b)
	{ return ((String) a).charAt(0) == ((String) b).charAt(0); }
	
	public int hash(Object o)
	{ return ((String) o).charAt(0); }
    };

    final static Map weak = new IdWeakHashMap( id );

    public static void main(String[] argv)
    {
	doAdds();
	System.gc();
	show();
	setRemoveHi();
	System.gc();
	show();
    }

    static void setRemoveHi()
    {
	String bye = new String("bye");
	weak.put(bye, "");
	Set ks = weak.keySet();
	ks.remove("hi");
	show();
    }

    static void doAdds()
    {
	String s0 = "hi"; //remember, this one is in the internal string pool!
	String s1 = new String("hello");
	String s2 = new String("yoohoo");
	String s3 = new String("poop");

	weak.put(s0, "");
	weak.put(s1, "");
	weak.put(s2, "");
	weak.put(s3, "");

	show();
    }

    static void show()
    {
	System.out.println("elements:");
	for (Iterator ii = weak.keySet().iterator(); ii.hasNext(); )
	    System.out.println( "\t" + ii.next() );

	System.out.println("size: " + weak.size() );
    }
}

