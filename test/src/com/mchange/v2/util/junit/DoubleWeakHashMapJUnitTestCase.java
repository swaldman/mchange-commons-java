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
        Map<String,String> m = new DoubleWeakHashMap<String,String>();
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

        Map<Integer,String> m = new DoubleWeakHashMap<Integer,String>();
        m.put(a, poop);
        m.put(b, scoop);
        m.put(c, doop);
        assertEquals("Size should be three, viewed via Map directly.", m.size(), 3);
        assertEquals("Size should be three, viewed via keySet .", m.keySet().size(), 3);
        assertEquals("Size should be three, viewed via values Collection.", m.values().size(), 3);

        int count = 0;
        for (Iterator<Integer> ii = m.keySet().iterator(); ii.hasNext();)
        {
            count += ii.next().intValue();
        }
        assertEquals("Count should be six, viewed via values Collection.", count, 6);

        Integer d = Integer.valueOf(4);
        m.put(d, poop);
        m.values().remove(poop);
        assertEquals("After removing a doubled value, size should be 2", m.size(), 2);
    }

    /**
     *  Every element of Map.Entry.setValue's contract, on the entry this map hands out.
     *
     *  <p>Two distinct defects lived here. setValue used to return the inner WVal wrapper
     *  rather than the value it wrapped, so callers got back an object of a package-private
     *  type instead of their own value. And the entry keeps a <em>hard</em> reference to its
     *  key and value -- deliberately, so that an entry a caller is holding cannot start
     *  reporting null underneath them as the referents are collected -- but setValue updated
     *  the map without updating that reference, so getValue() on the very entry you had just
     *  called setValue() on still answered with the old value.</p>
     *
     *  <p>The third assertion below is the one that failed: it reads back through the same
     *  entry object rather than through the map or a freshly obtained entry, which is
     *  precisely what the stale hard reference made wrong.</p>
     */
    public void testEntrySetValueHonorsContract()
    {
        Integer key = Integer.valueOf(1);
        String  original    = new String("original");
        String  replacement = new String("replacement");

        Map<Integer,String> m = new DoubleWeakHashMap<Integer,String>();
        m.put( key, original );

        Map.Entry<Integer,String> entry = m.entrySet().iterator().next();
        assertEquals( "The entry should report the value put into the map.", original, entry.getValue() );

        Object returned = entry.setValue( replacement );

        assertEquals( "setValue must return the value previously mapped, not the new one.", original, returned );
        assertEquals( "setValue must return the value itself, not an internal wrapper.", String.class, returned.getClass() );
        assertEquals( "getValue on the same entry must reflect the value just set.", replacement, entry.getValue() );
        assertEquals( "The map must reflect the value set through the entry.", replacement, m.get( key ) );
        assertEquals( "Setting a value must not change the map's size.", 1, m.size() );
    }

    /**
     *  A second setValue through the same entry must return what the first one set, which it
     *  can only do if the first updated both the map and the entry's own reference.
     */
    public void testRepeatedEntrySetValue()
    {
        Integer key = Integer.valueOf(1);
        Map<Integer,String> m = new DoubleWeakHashMap<Integer,String>();
        m.put( key, "first" );

        Map.Entry<Integer,String> entry = m.entrySet().iterator().next();
        assertEquals( "first",  entry.setValue("second") );
        assertEquals( "second", entry.setValue("third")  );
        assertEquals( "third",  entry.getValue() );
        assertEquals( "third",  m.get( key ) );
    }

    /**
     *  The entry handed out must satisfy Map.Entry's equals and hashCode contract, since
     *  callers may put entries in collections of their own.
     */
    public void testEntryEqualsAndHashCode()
    {
        Integer key = Integer.valueOf(7);
        Map<Integer,String> m = new DoubleWeakHashMap<Integer,String>();
        m.put( key, "value" );

        Map.Entry<Integer,String> entry = m.entrySet().iterator().next();

        assertEquals( "Key should be the one put.", key, entry.getKey() );
        assertEquals( "An entry must equal any Map.Entry with an equal key and value.",
                      new java.util.AbstractMap.SimpleEntry<Integer,String>( key, "value" ), entry );
        assertEquals( "hashCode must be key.hashCode() ^ value.hashCode(), per Map.Entry.",
                      key.hashCode() ^ "value".hashCode(), entry.hashCode() );
    }

    /**
     *  entrySet, keySet and values must agree with one another and with the map.
     */
    public void testViewsAgree()
    {
        Map<Integer,String> m = new DoubleWeakHashMap<Integer,String>();
        Integer a = Integer.valueOf(1);
        Integer b = Integer.valueOf(2);
        m.put( a, "a" );
        m.put( b, "b" );

        assertEquals( 2, m.size() );
        assertEquals( 2, m.entrySet().size() );
        assertEquals( 2, m.keySet().size() );
        assertEquals( 2, m.values().size() );

        int seen = 0;
        for ( Map.Entry<Integer,String> e : m.entrySet() )
        {
            assertEquals( "Each entry's value must be what the map maps its key to.",
                          m.get( e.getKey() ), e.getValue() );
            ++seen;
        }
        assertEquals( "Iterating entrySet must visit every mapping.", 2, seen );

        assertTrue( m.keySet().contains( a ) );
        assertTrue( m.values().contains( "b" ) );
    }

    /**
     *  put must return the previously mapped value, and the map must not grow when a key is
     *  overwritten. Same failure shape as the setValue defect, one level up.
     */
    public void testPutReturnsPreviousValue()
    {
        Integer key = Integer.valueOf(1);
        Map<Integer,String> m = new DoubleWeakHashMap<Integer,String>();

        assertNull( "put must return null for a key not previously mapped.", m.put( key, "one" ) );
        Object previous = m.put( key, "two" );
        assertEquals( "put must return the value previously mapped.", "one", previous );
        assertEquals( "put must return the value itself, not an internal wrapper.", String.class, previous.getClass() );
        assertEquals( "Overwriting a key must not grow the map.", 1, m.size() );
        assertEquals( "two", m.get( key ) );
    }

    /**
     *  remove must likewise hand back the value, not the wrapper around it.
     */
    public void testRemoveReturnsValue()
    {
        Integer key = Integer.valueOf(1);
        Map<Integer,String> m = new DoubleWeakHashMap<Integer,String>();
        m.put( key, "one" );

        Object removed = m.remove( key );
        assertEquals( "remove must return the value that was mapped.", "one", removed );
        assertEquals( "remove must return the value itself, not an internal wrapper.", String.class, removed.getClass() );
        assertTrue( m.isEmpty() );
        assertNull( m.remove( key ) );
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
