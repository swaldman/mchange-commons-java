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

package com.mchange.v2.util.junit;

import java.util.Iterator;
import java.util.Map;

import com.mchange.v2.util.DoubleWeakHashMap;

import junit.framework.TestCase;

public class DoubleWeakHashMapJUnitTestCase extends TestCase
{
    // only needed for disabled testWeakness below
    //final static int ARRAY_SZ = 1024 * 1024  /* * 32 * 1024 */;

    public void testGetNeverAdded()
    {
        Map m = new DoubleWeakHashMap();
        assertNull( m.get("foo") );
    }
    
    public void testHardAdds()
    {
        Integer a = new Integer(1);
        Integer b = new Integer(2);
        Integer c = new Integer(3);
        
        String poop = new String("poop");
        String scoop = new String("scoop");
        String doop = new String("dcoop");
        
        Map m = new DoubleWeakHashMap();
        m.put(a, poop);
        m.put(b, scoop);
        m.put(c, doop);
        assertEquals("Size should be three, viewed via Map directly.", m.size(), 3);
        assertEquals("Size should be three, viewed via keySet .", m.keySet().size(), 3);
        assertEquals("Size should be three, viewed via values Collection.", m.values().size(), 3);
        
        int count = 0;
        for (Iterator ii = m.keySet().iterator(); ii.hasNext();)
        {
            count += ((Integer) ii.next()).intValue();
        }
        assertEquals("Count should be six, viewed via values Collection.", count, 6);
        
        Integer d = new Integer(4);
        m.put(d, poop);
        m.values().remove(poop);
        assertEquals("After removing a doubled value, size should be 2", m.size(), 2);
    }

    /*
    //this often fails, because System.gc() is not reliable
    public void testWeakness()
    {
        Integer a = new Integer(1);
        Integer b = new Integer(2);
        Integer c = new Integer(3);
        
        String poop = new String("poop");

        Map m = new DoubleWeakHashMap();
        m.put(a, poop);
        m.put(b, new Object());
        m.put(c, new Object());
        
        //race condition... b & c might already have been removed... but i doubt it
        assertEquals("1) Weak values should not yet have been removed (but not guaranteed! sometimes fails without a defect!)", 3, m.size());
        
        // we are relying that a full, synchronous GC occurs,
        // which is not guaranteed in all VMs
        System.gc();
        
        // let's see if we can force a deeper gc via a big array creation
        byte[] bArray = new byte[ARRAY_SZ];
	System.gc();
	//try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
        
        assertEquals("2) Weak values should have been automatically removed (but not guaranteed! sometimes fails without a defect!)", 1, m.size());
        
        m.put( new Object(), b);
        
        //race condition... b & c might already have been removed... but i doubt it
        assertEquals("3) Weak key should not yet have been removed (but not guaranteed! sometimes fails without a defect!)", 2, m.size());

        System.gc();
        // let's see if we can force a deeper gc via a big array creation
        bArray = new byte[ARRAY_SZ];

        assertEquals("4) Weak key should have been automatically removed (but not guaranteed! sometimes fails without a defect!)", m.size(), 1);
    }
    */
}
