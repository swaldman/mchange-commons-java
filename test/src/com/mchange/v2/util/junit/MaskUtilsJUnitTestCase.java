package com.mchange.v2.util.junit;

import junit.framework.TestCase;

import com.mchange.v2.util.MaskUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  MaskUtils holds the masking policy apart from any one place it is applied, so that a caller
 *  with a single value in hand -- a toString() printing a JDBC URL, an exception message naming
 *  the URL that failed to parse -- can reach it without assembling a Properties.
 *
 *  <p><b>The behavioural coverage of URL masking lives next door, in
 *  PropertiesMaskerJUnitTestCase, and deliberately stays there.</b> Those cases drive
 *  maskUrlCredentials(..) through the shipping call site, which is the better place to pin what
 *  gets masked and what gets retained: userinfo, query and <code>;</code>-separated parameters,
 *  the pre-signed signatures of three cloud vendors, matrix parameters, and every retention.
 *  Duplicating them here would double the maintenance and halve the signal.</p>
 *
 *  <h3>So what is left for this file</h3>
 *
 *  <p>Only what is invisible or unreachable from there.</p>
 *
 *  <ul>
 *    <li><b>The return contract.</b> maskUrlCredentials(..) answers null for "not URL-shaped"
 *        and the value itself for "URL-shaped, nothing to mask". Through PropertiesMasker both
 *        appear identically -- the value is retained either way -- so nothing over there can
 *        tell them apart. The URL transformer's decision to claim a value or decline it rests
 *        on exactly that difference.</li>
 *    <li><b>Null.</b> Not reachable through a Properties at all, which cannot hold a null
 *        value. It became reachable the moment the method went public, and the first caller
 *        waiting on it passes a bean property that defaults to null.</li>
 *    <li><b>The promise never to throw.</b> The javadoc says a malformed URL is treated as
 *        prose. Nothing pinned that, and the parser is a hand-rolled index walk over
 *        delimiters, which is exactly the shape that throws StringIndexOutOfBounds on a
 *        degenerate input.</li>
 *    <li><b>lowercaseLettersDigitsOnly(..)</b>, newly public and previously untested.</li>
 *    <li><b>The published sets</b>, including the two deliberate divergences between the
 *        key-name set and the URL-parameter set, which no behavioural test states outright.</li>
 *  </ul>
 */
public final class MaskUtilsJUnitTestCase extends TestCase
{
    private final static String MASK = "****";

    // ------------------------------------------------- the null / unchanged / masked trichotomy

    /**
     *  THE DISTINCTION THIS FILE EXISTS FOR. Three outcomes, and the caller must be able to
     *  tell them apart: not URL-shaped (null), URL-shaped but carrying nothing secret (the
     *  value, unchanged and identical), and URL-shaped with a credential (a changed value).
     *
     *  <p>PropertiesMasker.DANGEROUS_URL_VALUE_TRANSFORMER claims a value only in the third
     *  case, and claiming suppresses key-name masking for that key -- so collapsing the first
     *  two into one answer would let a transformer silently claim values it did nothing to.</p>
     */
    public void testTheThreeOutcomesAreDistinguishable()
    {
        assertNull( "prose is not URL-shaped and must answer null",
                    MaskUtils.maskUrlCredentials( "this is prose" ) );

        String clean = "jdbc:postgresql://db:5432/prod?ssl=true";
        assertEquals( "a URL with nothing to mask must come back unchanged",
                      clean, MaskUtils.maskUrlCredentials( clean ) );

        String dirty = "jdbc:postgresql://db:5432/prod?password=s3cret";
        String masked = MaskUtils.maskUrlCredentials( dirty );
        assertNotNull( masked );
        assertFalse( "a credential must not survive", masked.contains( "s3cret" ) );
        assertFalse( "and the result must differ from the input, or the transformer declines",
                     masked.equals( dirty ) );
    }

    /**
     *  Null is the answer for anything with no "://" in it, which includes values that look
     *  URL-ish. A JDBC URL for an embedded database has a scheme and colons but no authority,
     *  and mailto: has userinfo-looking text that is not userinfo.
     */
    public void testValuesWithoutAnAuthorityAnswerNull()
    {
        List<String> notUrlShaped = Arrays.asList(
            "",
            "30",
            "jdbc:h2:mem:test",                         // embedded, no authority
            "jdbc:oracle:thin:scott/tiger@//h:1521/db", // Oracle thin: credentials, but no "://"
            "mailto:someone@example.com",
            "C:\\Users\\steve\\app.properties",
            "/etc/myapp/config.properties"
        );
        for ( String v : notUrlShaped )
            assertNull( "should not be treated as URL-shaped: '" + v + "'",
                        MaskUtils.maskUrlCredentials( v ) );
    }

    /**
     *  The Oracle thin case above is a genuine miss, not an oversight, and is recorded here so
     *  that it is a known limit rather than a surprise. Such a value is caught only by its key
     *  name -- which is why key-name masking is not replaceable by value inspection.
     */
    public void testOracleThinCredentialsAreAKnownMiss()
    {
        assertNull( MaskUtils.maskUrlCredentials( "jdbc:oracle:thin:scott/tiger@//h:1521/db" ) );
    }

    /** Reachable only now that the method is public: a Properties cannot hold a null value. */
    public void testNullAnswersNullRatherThanThrowing()
    {
        assertNull( MaskUtils.maskUrlCredentials( null ) );
    }

    // ------------------------------------------------------------------- never throws

    /**
     *  The javadoc promises a malformed URL is treated as prose. The implementation is a
     *  hand-rolled walk over delimiter indices, so the inputs that matter are the ones where a
     *  delimiter is present but what should surround it is not. This asserts only that none of
     *  them throws; what each conceals is the business of the two tests below.
     */
    public void testDegenerateUrlsDoNotThrow()
    {
        List<String> degenerate = Arrays.asList(
            "://",
            "://?password=s3cret",
            "x://",
            "x://@",
            "x://:@",
            "x://s3cret@",
            "x://user:s3cret@",
            "x://h/?",
            "x://h/?&&&",
            "x://h/?=s3cret",
            "x://h/?password",
            "x://h/?password=",
            "x://h/;",
            "x://h/#",
            "x://h/#password=s3cret",
            "x://h/?password=s3cret#frag",
            "x://user:s3cret@h:99/a;b?c=d&password=s3cret#frag"
        );
        for ( String v : degenerate )
        {
            try
            { MaskUtils.maskUrlCredentials( v ); }
            catch ( Exception e )
            { fail( "threw " + e.getClass().getName() + " on '" + v + "'" ); }
        }
    }

    /** Wherever a credential sits somewhere the policy reaches, a broken URL must not excuse it. */
    public void testDegenerateUrlsStillConceal()
    {
        List<String> mustConceal = Arrays.asList(
            "://?password=s3cret",
            "x://s3cret@",                       // userinfo with no host
            "x://user:s3cret@",
            "x://h/?password=s3cret#frag",
            "x://user:s3cret@h:99/a;b?c=d&password=s3cret#frag"
        );
        for ( String v : mustConceal )
        {
            String masked = MaskUtils.maskUrlCredentials( v );
            assertNotNull( "should have been treated as URL-shaped: '" + v + "'", masked );
            assertFalse( "leaked from '" + v + "' -> '" + masked + "'", masked.contains( "s3cret" ) );
        }
    }

    /**
     *  Two shapes where the secret survives, recorded so each is a known limit rather than a
     *  surprise. Both follow from the policy being a denylist over names: where there is no
     *  name, or the name is not where parameters are read, there is nothing to match.
     *
     *  <ul>
     *    <li>A nameless parameter, <code>?=secret</code>. The empty name matches no sensitive
     *        substring. Malformed, and vanishingly rare.</li>
     *    <li>A fragment. Everything after <code>#</code> is copied through untouched, because a
     *        fragment carries no parameters this code means to interpret -- and a fragment is
     *        not sent to the server, so a credential there is a misconfiguration already.</li>
     *  </ul>
     */
    public void testSecretsBeyondTheReachOfNameTestsAreKnownMisses()
    {
        assertTrue( "a nameless parameter value is not masked",
                    MaskUtils.maskUrlCredentials( "x://h/?=s3cret" ).contains( "s3cret" ) );
        assertTrue( "a fragment is copied through untouched",
                    MaskUtils.maskUrlCredentials( "x://h/#password=s3cret" ).contains( "s3cret" ) );
    }

    /**
     *  A value may be masked twice -- a library masking one URL, then a dump masking the whole
     *  Properties it sits in. The second pass must not corrupt the first's output, and must not
     *  report a change that would make a transformer claim an already-clean value.
     */
    public void testMaskingIsIdempotent()
    {
        String url  = "jdbc:sqlserver://user:s3cret@db:1433;databaseName=prod;password=s3cret";
        String once = MaskUtils.maskUrlCredentials( url );
        assertNotNull( once );
        assertFalse( once.contains( "s3cret" ) );
        assertEquals( "a second pass must change nothing", once, MaskUtils.maskUrlCredentials( once ) );
    }

    // ------------------------------------------------------- lowercaseLettersDigitsOnly

    /**
     *  The normalization both name tests run on each side, so that one set entry covers the
     *  dotted, hyphenated, underscored and camel-cased spellings of a name. Digits survive
     *  because real parameter names carry them -- MySQL's password1, password2, password3.
     */
    public void testNormalizationLowercasesAndDropsEverythingElse()
    {
        assertEquals( "apikey", MaskUtils.lowercaseLettersDigitsOnly( "apiKey" ) );
        assertEquals( "apikey", MaskUtils.lowercaseLettersDigitsOnly( "api.key" ) );
        assertEquals( "apikey", MaskUtils.lowercaseLettersDigitsOnly( "api-key" ) );
        assertEquals( "apikey", MaskUtils.lowercaseLettersDigitsOnly( "api_key" ) );
        assertEquals( "apikey", MaskUtils.lowercaseLettersDigitsOnly( "API KEY" ) );
        assertEquals( "password1", MaskUtils.lowercaseLettersDigitsOnly( "password1" ) );
        assertEquals( "xamzsignature", MaskUtils.lowercaseLettersDigitsOnly( "X-Amz-Signature" ) );
        assertEquals( "", MaskUtils.lowercaseLettersDigitsOnly( "" ) );
        assertEquals( "", MaskUtils.lowercaseLettersDigitsOnly( "..--__" ) );
    }

    // ------------------------------------------------------------------- the published sets

    /** The sets are security configuration; a caller able to empty one could disable masking. */
    public void testPublishedSubstringSetsAreUnmodifiable()
    {
        List<Set<String>> sets = Arrays.asList( MaskUtils.DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS,
                                                MaskUtils.DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_SUBSTRINGS,
                                                MaskUtils.DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_EXACT_NAMES );
        for ( Set<String> set : sets )
        {
            try
            {
                set.clear();
                fail( "a caller able to clear this set could disable masking process-wide" );
            }
            catch ( UnsupportedOperationException expected )
            { /* as it should be */ }
        }
    }

    /**
     *  The URL-parameter set is DERIVED from the key-name set, so the two cannot drift by
     *  accident -- but they diverge at three entries on purpose, and those are what a future
     *  tidying would quietly undo.
     *
     *  <ul>
     *    <li><code>user</code> is masked as a URL parameter, where it is the universal JDBC
     *        account, and must NOT be a key-name substring, where it would mask
     *        <code>user.home</code> and <code>user.dir</code>.</li>
     *    <li><code>signature</code> likewise: in a pre-signed URL the signature IS the
     *        capability, while a signature sitting in a config value generally reveals no
     *        signing key.</li>
     *    <li><code>c3p0.user</code> is a key-name test and means nothing as a parameter
     *        name.</li>
     *  </ul>
     */
    public void testTheTwoSetsDivergeOnlyWhereIntended()
    {
        Set<String> keys = MaskUtils.DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS;
        Set<String> url  = MaskUtils.DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_SUBSTRINGS;

        assertFalse( "a bare 'user' key-name substring would mask user.home and user.dir",
                     keys.contains( "user" ) );
        assertTrue( "'user' is the universal JDBC account parameter", url.contains( "user" ) );

        assertFalse( "a signature in a config value is not itself a credential",
                     keys.contains( "signature" ) );
        assertTrue( "in a pre-signed URL the signature is the capability",
                    url.contains( "signature" ) );

        assertTrue( keys.contains( "c3p0.user" ) );
        assertFalse( "'c3p0.user' is meaningless as a parameter name", url.contains( "c3p0.user" ) );

        Set<String> expectedUrl = new HashSet<String>( keys );
        expectedUrl.remove( "c3p0.user" );
        expectedUrl.add( "user" );
        expectedUrl.add( "signature" );
        assertEquals( "the sets must differ at exactly the three entries above",
                      expectedUrl, url );
    }

    /**
     *  The exact-name set exists for names too short to be safe substrings: normalized,
     *  <code>sig</code> occurs inside <code>design</code>, <code>assign</code> and
     *  <code>signal</code>, all plausible parameter names. So each entry must behave as a whole
     *  name and <b>only</b> as a whole name -- masked on its own, ignored when embedded.
     *
     *  <p>That second half is the load-bearing one, and it is also what disqualifies an entry
     *  from belonging here at all: a name the substring set would already catch stays masked
     *  when embedded, and so fails this. An earlier version of this test asked merely whether
     *  the entry appeared in the substring set, which a near-miss like <code>password</code>
     *  (the set holds <code>pass</code>) walked straight past.</p>
     */
    public void testExactNamesMatchWholeAndOnlyWhole()
    {
        for ( String name : MaskUtils.DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_EXACT_NAMES )
        {
            String whole    = MaskUtils.maskUrlCredentials( "x://h/?" + name + "=s3cret" );
            String embedded = MaskUtils.maskUrlCredentials( "x://h/?zz" + name + "zz=s3cret" );

            assertFalse( "an exact name must be masked on its own: " + name,
                         whole.contains( "s3cret" ) );
            assertTrue( "an exact name must be ignored when embedded, or it did not need to be "
                        + "an exact name -- the substring set already covers it: " + name,
                        embedded.contains( "s3cret" ) );
        }
    }

    /** The mask itself, because every assertion in this suite and the next spells it literally. */
    public void testMaskIsWhatTheTestsAssume()
    {
        assertEquals( MASK, MaskUtils.MASK );
    }
}
