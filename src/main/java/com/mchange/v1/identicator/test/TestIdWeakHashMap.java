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

