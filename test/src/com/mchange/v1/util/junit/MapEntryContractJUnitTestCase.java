package com.mchange.v1.util.junit;

import java.util.Map;

import com.mchange.v1.util.SimpleMapEntry;

import junit.framework.TestCase;

/**
 *  Map.Entry's contract, on com.mchange.v1.util's implementation of it.
 *
 *  <p>SimpleMapEntry.setValue captured its own argument rather than the field --
 *  <code>V old = value;</code> where it meant <code>V old = this.value;</code> -- so it
 *  replaced the value correctly but reported the new one as the old one. Every caller
 *  reading the result got back exactly what it had just passed in. The method had been
 *  wrong for as long as it existed, which is what a test asserting only "the value
 *  changed" would never have caught: the map was always right, only the report was
 *  wrong.</p>
 */
public class MapEntryContractJUnitTestCase extends TestCase
{
    public void testSetValueReturnsPreviousValue()
    {
        SimpleMapEntry<String,String> entry = new SimpleMapEntry<String,String>( "key", "original" );

        assertEquals( "original", entry.getValue() );

        String returned = entry.setValue( "replacement" );

        assertEquals( "setValue must return the value previously held, not the one just set.",
                      "original", returned );
        assertEquals( "getValue must report the value just set.", "replacement", entry.getValue() );
        assertEquals( "setValue must not disturb the key.", "key", entry.getKey() );
    }

    /**
     *  A second setValue must return what the first one set. Under the defect this returned
     *  the second argument both times, so the two calls were indistinguishable.
     */
    public void testRepeatedSetValue()
    {
        SimpleMapEntry<String,String> entry = new SimpleMapEntry<String,String>( "key", "first" );

        assertEquals( "first",  entry.setValue( "second" ) );
        assertEquals( "second", entry.setValue( "third"  ) );
        assertEquals( "third",  entry.getValue() );
    }

    public void testSetValueToNullAndBack()
    {
        SimpleMapEntry<String,String> entry = new SimpleMapEntry<String,String>( "key", "original" );

        assertEquals( "original", entry.setValue( null ) );
        assertNull( entry.getValue() );
        assertNull( "setValue must return the previous value even when that value was null.",
                    entry.setValue( "restored" ) );
        assertEquals( "restored", entry.getValue() );
    }

    /**
     *  AbstractMapEntry supplies equals and hashCode for every entry in this library, so its
     *  conformance to Map.Entry is worth pinning: an entry equals any Map.Entry with an equal
     *  key and value, and hashes to key.hashCode() ^ value.hashCode().
     */
    public void testEqualsAndHashCodeContract()
    {
        SimpleMapEntry<String,String> entry = new SimpleMapEntry<String,String>( "key", "value" );
        Map.Entry<String,String> equivalent =
            new java.util.AbstractMap.SimpleEntry<String,String>( "key", "value" );

        assertEquals( "An entry must equal any Map.Entry with an equal key and value.", equivalent, entry );
        assertEquals( "and must agree with it on hashCode.", equivalent.hashCode(), entry.hashCode() );
        assertEquals( "hashCode must be key.hashCode() ^ value.hashCode(), per Map.Entry.",
                      "key".hashCode() ^ "value".hashCode(), entry.hashCode() );

        assertFalse( entry.equals( new java.util.AbstractMap.SimpleEntry<String,String>( "key", "other" ) ) );
        assertFalse( entry.equals( "not an entry" ) );
    }

    public void testEqualsAndHashCodeWithNulls()
    {
        SimpleMapEntry<String,String> entry = new SimpleMapEntry<String,String>( null, null );

        assertEquals( "A null key and value must hash to zero, per Map.Entry.", 0, entry.hashCode() );
        assertEquals( new java.util.AbstractMap.SimpleEntry<String,String>( null, null ), entry );
    }
}
