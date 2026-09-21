package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.mchange.v2.cfg.AbstractBooleanProperty.Details;

/**
 *  AbstractBooleanProperty.Details: what a boolean property observed, and the two questions
 *  callers ask of it.
 *
 *  <p>Exhaustive rather than illustrative. The space is three slots over three values --
 *  TRUE, FALSE and "not set" -- so all twenty-seven combinations fit comfortably, and a table
 *  states the expected answers where prose would only gesture at them.</p>
 *
 *  <p>This is a value object whose methods are each a line or two, which is exactly why it is
 *  worth covering. Every defect found in it so far has been a single token that type-checked
 *  in the wrong position: a third term left unboxed where the first two were compared against
 *  null, and an equals that compared this.currentConfigProperty against
 *  other.earlySystemProperty. Neither was reachable by reading carefully; both are trivially
 *  reachable by enumerating.</p>
 */
public class BooleanPropertyDetailsInternalJUnitTestCase extends TestCase
{
    private final static Boolean[] VALUES = { Boolean.TRUE, Boolean.FALSE, null };

    private static Details details( Boolean early, Boolean currentSys, Boolean currentCfg )
    { return new Details( early, currentSys, currentCfg ); }

    private static String describe( Boolean early, Boolean currentSys, Boolean currentCfg )
    { return "early=" + early + " currentSys=" + currentSys + " currentCfg=" + currentCfg; }

    // ==================== isUnconfigured ====================

    /**
     *  Unconfigured means no source said anything. The bug this replaces left the third term
     *  unboxed, so the method threw NullPointerException in exactly the case it existed to
     *  detect -- the all-null one -- while returning correctly whenever an earlier term
     *  short-circuited it.
     */
    public void testIsUnconfiguredIsTrueOnlyWhenEverySlotIsUnset()
    {
        for ( Boolean e : VALUES )
            for ( Boolean s : VALUES )
                for ( Boolean c : VALUES )
                {
                    boolean expected = ( e == null && s == null && c == null );
                    assertEquals( describe( e, s, c ), expected, details( e, s, c ).isUnconfigured() );
                }
    }

    // ==================== isCurrentExplicitUnconflicted ====================

    /**
     *  "Current configuration says something, it does not contradict itself, and it does not
     *  contradict what was sealed." Enumerated against an independent restatement of the rule
     *  rather than against a transcription of the implementation, so that a defect in the
     *  branching shows up as a disagreement rather than being reproduced in the expectation.
     */
    public void testIsCurrentExplicitUnconflicted()
    {
        for ( Boolean e : VALUES )
            for ( Boolean s : VALUES )
                for ( Boolean c : VALUES )
                {
                    // independent statement of the intent: gather what the current sources say,
                    // require at least one, require they agree, require the sealed value not to differ
                    List<Boolean> current = new ArrayList<Boolean>();
                    if ( s != null ) current.add( s );
                    if ( c != null ) current.add( c );

                    boolean expected;
                    if ( current.isEmpty() )
                        expected = false;
                    else
                    {
                        Boolean first = current.get( 0 );
                        boolean currentAgrees = true;
                        for ( Boolean v : current )
                            if ( ! v.equals( first ) ) currentAgrees = false;
                        expected = currentAgrees && ( e == null || e.equals( first ) );
                    }

                    assertEquals( describe( e, s, c ), expected,
                                  details( e, s, c ).isCurrentExplicitUnconflicted() );
                }
    }

    /** The branch structure claims to be exhaustive; nothing in the space may reach its throw. */
    public void testIsCurrentExplicitUnconflictedNeverThrows()
    {
        for ( Boolean e : VALUES )
            for ( Boolean s : VALUES )
                for ( Boolean c : VALUES )
                {
                    try
                    { details( e, s, c ).isCurrentExplicitUnconflicted(); }
                    catch ( RuntimeException t )
                    { fail( "Reached the supposedly unreachable case: " + describe( e, s, c ) + " -- " + t ); }
                }
    }

    // ==================== equals and hashCode ====================

    /**
     *  Two Details are equal exactly when all three slots match. The bug this replaces
     *  compared this.currentConfigProperty against other.earlySystemProperty, so an object
     *  equalled itself -- through the identity short-circuit -- but not an identical copy.
     */
    public void testEqualsHoldsExactlyWhenEverySlotMatches()
    {
        for ( Boolean e1 : VALUES )
          for ( Boolean s1 : VALUES )
            for ( Boolean c1 : VALUES )
              for ( Boolean e2 : VALUES )
                for ( Boolean s2 : VALUES )
                  for ( Boolean c2 : VALUES )
                  {
                      boolean expected = ( e1 == e2 && s1 == s2 && c1 == c2 );   // Boolean constants, identity is fine
                      Details a = details( e1, s1, c1 );
                      Details b = details( e2, s2, c2 );
                      assertEquals( describe( e1, s1, c1 ) + "  vs  " + describe( e2, s2, c2 ),
                                    expected, a.equals( b ) );
                  }
    }

    /** including that a distinct object with identical content is equal -- the case that failed. */
    public void testAnIdenticalCopyIsEqualNotMerelyTheSameObject()
    {
        Details a = details( Boolean.TRUE, null, Boolean.FALSE );
        Details b = details( Boolean.TRUE, null, Boolean.FALSE );

        assertNotSame( "Precondition: genuinely distinct objects.", a, b );
        assertEquals( a, b );
        assertEquals( "and symmetrically.", b, a );
    }

    public void testEqualsIsSymmetricAndReflexiveAcrossTheSpace()
    {
        for ( Boolean e : VALUES )
            for ( Boolean s : VALUES )
                for ( Boolean c : VALUES )
                {
                    Details a = details( e, s, c );
                    assertEquals( "reflexive: " + describe( e, s, c ), a, a );
                    assertEquals( "reflexive by value: " + describe( e, s, c ), a, details( e, s, c ) );

                    for ( Boolean e2 : VALUES )
                        for ( Boolean s2 : VALUES )
                            for ( Boolean c2 : VALUES )
                            {
                                Details b = details( e2, s2, c2 );
                                assertEquals( "symmetric: " + describe( e, s, c ) + " vs " + describe( e2, s2, c2 ),
                                              a.equals( b ), b.equals( a ) );
                            }
                }
    }

    /**
     *  The contract: equal objects must agree on hashCode. Violating it is what made the
     *  earlier equals bug more than cosmetic -- Details in a hash-based collection would have
     *  behaved erratically, and this package uses exactly that idiom for warn-once
     *  suppression.
     */
    public void testEqualObjectsShareAHashCode()
    {
        for ( Boolean e : VALUES )
            for ( Boolean s : VALUES )
                for ( Boolean c : VALUES )
                    assertEquals( "equal objects must hash alike: " + describe( e, s, c ),
                                  details( e, s, c ).hashCode(), details( e, s, c ).hashCode() );
    }

    /** and the three slots must be distinguishable, or the hash discards what equals keeps. */
    public void testHashCodeDistinguishesTheSlots()
    {
        assertFalse( "early and config must not be interchangeable in the hash",
                     details( Boolean.TRUE, null, null ).hashCode()
                     == details( null, null, Boolean.TRUE ).hashCode() );
        assertFalse( "current-system and config must not be interchangeable in the hash",
                     details( null, Boolean.TRUE, null ).hashCode()
                     == details( null, null, Boolean.TRUE ).hashCode() );
    }

    public void testEqualsRejectsOtherTypesAndNull()
    {
        Details a = details( Boolean.TRUE, Boolean.FALSE, null );

        assertFalse( a.equals( null ) );
        assertFalse( a.equals( "not a Details" ) );
    }
}
