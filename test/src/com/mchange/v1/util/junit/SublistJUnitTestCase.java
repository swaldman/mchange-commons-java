package com.mchange.v1.util.junit;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.mchange.v1.util.Sublist;

import junit.framework.TestCase;

/**
 *  Sublist is a live window [start_index, end_index) onto a parent list.
 *
 *  <p>setParent assigned <code>this.start_index = end_index</code>, so the window always
 *  began where it should have ended: every Sublist reported size() == 0 and read from the
 *  wrong offset, whatever it was constructed with. Unchanged since the git migration and
 *  unused inside the library, so nothing depended on the broken behaviour -- and nothing
 *  exercised it either.</p>
 */
public class SublistJUnitTestCase extends TestCase
{
    private List<String> parent()
    { return new LinkedList<String>( Arrays.asList( "a", "b", "c", "d", "e" ) ); }

    /** The window is the one asked for, not an empty one at its far end. */
    public void testWindowSizeAndContents()
    {
        Sublist<String> sub = new Sublist<String>( parent(), 1, 4 );

        assertEquals( "size() must be end_index - start_index.", 3, sub.size() );
        assertEquals( "b", sub.get(0) );
        assertEquals( "c", sub.get(1) );
        assertEquals( "d", sub.get(2) );
        assertEquals( Arrays.asList( "b", "c", "d" ), sub );
    }

    public void testWindowFromZero()
    {
        Sublist<String> sub = new Sublist<String>( parent(), 0, 2 );
        assertEquals( 2, sub.size() );
        assertEquals( "a", sub.get(0) );
        assertEquals( "b", sub.get(1) );
    }

    public void testEmptyWindowIsEmpty()
    {
        assertEquals( 0, new Sublist<String>( parent(), 2, 2 ).size() );
        assertEquals( 0, new Sublist<String>().size() );
    }

    /** Writes go through to the parent at the right offset. */
    public void testSetWritesThroughToParent()
    {
        List<String> parent = parent();
        Sublist<String> sub = new Sublist<String>( parent, 1, 4 );

        assertEquals( "set must return the element previously at that index.", "c", sub.set( 1, "C" ) );
        assertEquals( "C", sub.get(1) );
        assertEquals( "The write must land at the parent's offset index.",
                      Arrays.asList( "a", "b", "C", "d", "e" ), parent );
    }

    public void testAddInsertsIntoParentAndGrowsWindow()
    {
        List<String> parent = parent();
        Sublist<String> sub = new Sublist<String>( parent, 1, 4 );

        sub.add( 0, "X" );

        assertEquals( "Adding must grow the window by one.", 4, sub.size() );
        assertEquals( "X", sub.get(0) );
        assertEquals( "b", sub.get(1) );
        assertEquals( Arrays.asList( "a", "X", "b", "c", "d", "e" ), parent );
    }

    public void testRemoveDeletesFromParentAndShrinksWindow()
    {
        List<String> parent = parent();
        Sublist<String> sub = new Sublist<String>( parent, 1, 4 );

        assertEquals( "remove must return the element removed.", "b", sub.remove( 0 ) );
        assertEquals( "Removing must shrink the window by one.", 2, sub.size() );
        assertEquals( "c", sub.get(0) );
        assertEquals( Arrays.asList( "a", "c", "d", "e" ), parent );
    }

    public void testIndexOutOfBounds()
    {
        Sublist<String> sub = new Sublist<String>( parent(), 1, 4 );

        try { sub.set( 3, "nope" ); fail( "set past the end of the window must throw." ); }
        catch ( IndexOutOfBoundsException expected ) {}

        try { sub.remove( 3 ); fail( "remove past the end of the window must throw." ); }
        catch ( IndexOutOfBoundsException expected ) {}
    }

    public void testConstructorRejectsWindowPastParentEnd()
    {
        try
        {
            new Sublist<String>( parent(), 1, 99 );
            fail( "A window extending past the parent's end must be rejected." );
        }
        catch ( IndexOutOfBoundsException expected ) {}
    }
}
