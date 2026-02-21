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
        Integer a = Integer.valueOf(1);
        Integer b = Integer.valueOf(2);
        Integer c = Integer.valueOf(3);

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

        Integer d = Integer.valueOf(4);
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
