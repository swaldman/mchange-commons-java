package com.mchange.v1.identicator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 *  Lives in com.mchange.v1.identicator rather than a junit subpackage because IdHashKey is
 *  package-private, and IdList's only constructor takes a List of them.
 *
 *  <p>These collections key on an Identicator rather than on equals, so the whole point of
 *  them is that an equal-but-not-identical element is a different element. Two members had
 *  quietly stopped doing that:</p>
 *
 *  <ul>
 *    <li>IdList.contains built the wrapper its siblings all build, then asked the backing
 *        list for the <em>unwrapped</em> object. The backing list holds only wrappers, so
 *        the answer was false for everything actually in the list, and contains contradicted
 *        containsAll, indexOf and the iterator.</li>
 *    <li>IdHashSet(Collection, Identicator) sized its backing set from the collection and
 *        then never added the collection's elements, so the copy constructor produced an
 *        empty set.</li>
 *  </ul>
 *
 *  <p>Both are "a value computed and then not used", which no amount of testing the
 *  <em>other</em> methods would reveal.</p>
 */
public class IdentifiedCollectionsInternalJUnitTestCase extends TestCase
{
    private final Identicator identity = new StrongIdentityIdenticator();

    private IdList<String> newIdList()
    { return new IdList<String>( identity, new LinkedList<IdHashKey>() ); }

    /** contains must find what the list holds, and must agree with its siblings. */
    public void testIdListContainsFindsMembers()
    {
        String a = new String( "a" );
        String b = new String( "b" );

        IdList<String> list = newIdList();
        list.add( a );
        list.add( b );

        assertTrue( "contains must find an element the list holds.", list.contains( a ) );
        assertTrue( "contains must find an element the list holds.", list.contains( b ) );
        assertTrue( "containsAll must agree with contains.", list.containsAll( Arrays.asList( a, b ) ) );
        assertEquals( "indexOf must agree with contains.", 0, list.indexOf( a ) );
        assertEquals( "indexOf must agree with contains.", 1, list.indexOf( b ) );
        assertEquals( "The iterator must agree with contains.", a, list.iterator().next() );
    }

    /**
     *  and must still refuse an equal-but-distinct instance, so the fix restored the
     *  identicator's semantics rather than sliding into equals.
     */
    public void testIdListContainsIsIdentityNotEquality()
    {
        String a = new String( "a" );
        String equalButDistinct = new String( "a" );
        assertEquals( "Precondition: the two Strings are equal.", a, equalButDistinct );
        assertNotSame( "Precondition: the two Strings are distinct objects.", a, equalButDistinct );

        IdList<String> list = newIdList();
        list.add( a );

        assertTrue( list.contains( a ) );
        assertFalse( "An identity-keyed list must not find an equal-but-distinct instance.",
                     list.contains( equalButDistinct ) );
        assertEquals( -1, list.indexOf( equalButDistinct ) );
    }

    public void testIdListContainsAfterRemove()
    {
        String a = new String( "a" );

        IdList<String> list = newIdList();
        list.add( a );
        assertTrue( list.contains( a ) );

        assertTrue( list.remove( a ) );
        assertFalse( "contains must agree with remove.", list.contains( a ) );
        assertTrue( list.isEmpty() );
    }

    public void testIdListContainsOnEmptyList()
    { assertFalse( newIdList().contains( "anything" ) ); }

    /** The copy constructor must copy. */
    public void testIdHashSetCopyConstructorCopiesElements()
    {
        String a = new String( "a" );
        String b = new String( "b" );
        String c = new String( "c" );
        List<String> source = Arrays.asList( a, b, c );

        Set<String> set = new IdHashSet<String>( source, identity );

        assertEquals( "The copy constructor must yield the elements it was given.", 3, set.size() );
        assertFalse( set.isEmpty() );
        assertTrue( set.contains( a ) );
        assertTrue( set.contains( b ) );
        assertTrue( set.contains( c ) );
        assertTrue( "containsAll must agree.", set.containsAll( source ) );
    }

    /** and the copy must keep identity semantics, not fall back on equals. */
    public void testIdHashSetCopyConstructorKeepsIdentitySemantics()
    {
        String a = new String( "a" );
        String equalButDistinct = new String( "a" );

        Set<String> set = new IdHashSet<String>( Arrays.asList( a ), identity );

        assertTrue( set.contains( a ) );
        assertFalse( "An identity-keyed set must not find an equal-but-distinct instance.",
                     set.contains( equalButDistinct ) );
    }

    /**
     *  An identity-keyed set must keep equal-but-distinct instances apart, which is exactly
     *  what a HashSet would collapse. Sizing-without-adding would have hidden this too.
     */
    public void testIdHashSetCopyConstructorKeepsEqualButDistinctElementsApart()
    {
        String a1 = new String( "a" );
        String a2 = new String( "a" );

        Set<String> set = new IdHashSet<String>( Arrays.asList( a1, a2 ), identity );

        assertEquals( "Two equal but distinct instances are two elements under identity.", 2, set.size() );
    }

    public void testIdHashSetCopyConstructorOnEmptyCollection()
    {
        Set<String> set = new IdHashSet<String>( new LinkedList<String>(), identity );
        assertTrue( set.isEmpty() );
        assertEquals( 0, set.size() );
    }
}
