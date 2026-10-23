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
    /** Refers to the constant; its spelling is pinned once, in testTheMaskIsSixAsterisks. */
    private final static String MASK = MaskUtils.MASK;

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
     *  URL-shaped means "begins with a scheme", NOT "has an authority". The distinction is the
     *  whole point: an authority is where userinfo lives, but parameters need no authority, and
     *  most embedded databases put their credentials in parameters. So null is reserved for
     *  values carrying no scheme at all.
     */
    public void testValuesWithoutASchemeAnswerNull()
    {
        List<String> notUrlShaped = Arrays.asList(
            "",
            "30",
            "this is prose",
            "/etc/myapp/config.properties",   // no colon anywhere
            "http//host/no/colon",            // the colon is what makes a scheme
            ":leading-colon",                 // a scheme needs at least one character
            "1abc:x",                         // ... and must not begin with a digit
            "has space:x"                     // ... and may not contain a space
        );
        for ( String v : notUrlShaped )
            assertNull( "should not be treated as URL-shaped: '" + v + "'",
                        MaskUtils.maskUrlCredentials( v ) );
    }

    /**
     *  THE CASE THE SCHEME TEST EXISTS FOR. Every one of these is a real driver's real spelling,
     *  carries a real credential, and has no authority -- so the earlier "://" test concluded
     *  there was nothing here to read and handed back null, erasing the URL from the dump while
     *  leaving the password in it anywhere else it appeared.
     */
    public void testHostlessJdbcUrlsStillGetTheirParametersMasked()
    {
        assertEquals( "jdbc:h2:mem:testdb;USER=sa;PASSWORD=" + MASK,
                      MaskUtils.maskUrlCredentials( "jdbc:h2:mem:testdb;USER=sa;PASSWORD=s3cret" ) );
        assertEquals( "jdbc:h2:file:/data/db;USER=sa;PASSWORD=" + MASK,
                      MaskUtils.maskUrlCredentials( "jdbc:h2:file:/data/db;USER=sa;PASSWORD=s3cret" ) );
        assertEquals( "jdbc:derby:memory:db;create=true;user=app;password=" + MASK,
                      MaskUtils.maskUrlCredentials( "jdbc:derby:memory:db;create=true;user=app;password=s3cret" ) );
        assertEquals( "jdbc:hsqldb:file:/db/mydb;user=SA;password=" + MASK,
                      MaskUtils.maskUrlCredentials( "jdbc:hsqldb:file:/db/mydb;user=SA;password=s3cret" ) );
        assertEquals( "jdbc:firebirdsql:embedded:/path/db?user=app&password=" + MASK,
                      MaskUtils.maskUrlCredentials( "jdbc:firebirdsql:embedded:/path/db?user=app&password=s3cret" ) );
    }

    /**
     *  And the retention half, which matters as much: a hostless URL with nothing secret in it
     *  comes back unchanged and EQUAL, not null. Answering null here is what put "jdbcUrl ->
     *  null" into c3p0's pool-initialization log for every embedded database.
     */
    public void testHostlessJdbcUrlsWithoutCredentialsComeBackUnchanged()
    {
        for ( String url : new String[] { "jdbc:h2:mem:test",
                                          "jdbc:sqlite:/var/db/app.sqlite",
                                          "jdbc:derby:memory:db;create=true",
                                          "mailto:someone@example.com" } )
            assertEquals( url, MaskUtils.maskUrlCredentials( url ) );
    }

    /**
     *  Oracle's thin driver is a known miss, and knowingly so rather than by oversight:
     *  <code>scott/tiger@</code> sits behind no delimiter this recognizes, and reading an '@'
     *  with no authority to anchor it would mask path segments in every URL containing one.
     *  Such a value is caught by its key name or not at all.
     *
     *  <p>What changed with the scheme test is only the answer's shape -- the value comes back
     *  unchanged now rather than null, because it does have a scheme. The credential survives
     *  either way, which is what this pins.</p>
     */
    public void testOracleThinCredentialsAreAKnownMiss()
    {
        String url = "jdbc:oracle:thin:scott/tiger@//h:1521/db";
        assertEquals( "URL-shaped, so not null -- but nothing here is masked", url,
                      MaskUtils.maskUrlCredentials( url ) );
    }

    /**
     *  Bare <code>key</code>, behaviourally, in both parameter forms. The complement -- that a
     *  config key named <code>whitelistBaseKey</code> is NOT masked -- is pinned next door in
     *  testNonSensitiveKeysAreRetained, because that is a key-name question and this is not.
     */
    public void testBareKeyIsMaskedAsAUrlParameter()
    {
        assertEquals( "jdbc:h2:file:/db?cipher=sqlcipher&key=" + MASK,
                      MaskUtils.maskUrlCredentials( "jdbc:h2:file:/db?cipher=sqlcipher&key=s3cret" ) );
        assertEquals( "https://api.example.com/v1/items?key=" + MASK + "&pretty=true",
                      MaskUtils.maskUrlCredentials( "https://api.example.com/v1/items?key=AIzaSyFAKE&pretty=true" ) );
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
            "x://s3cret@",                       // one-component userinfo: ambiguous, so masked
            "x://user:s3cret@",                  // userinfo with no host
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
     *  accident -- but they diverge on purpose, and the divergences are what a future tidying
     *  would quietly undo.
     *
     *  <ul>
     *    <li><code>key</code> is masked as a URL parameter, where <code>?key=</code> is how
     *        Google's APIs and SQLCipher pass a secret, and must NOT be a key-name substring,
     *        where it would mask this library's own <code>whitelistBaseKey</code> and
     *        <code>overrideWhitelistKey</code>.</li>
     *    <li><code>signature</code> likewise: in a pre-signed URL the signature IS the
     *        capability, while a signature sitting in a config value generally reveals no
     *        signing key.</li>
     *  </ul>
     *
     *  <p><code>user</code> is in NEITHER, and that is the policy rather than an oversight. It
     *  was in both before 0.15.0 -- the key-name set carried the specific <code>c3p0.user</code>
     *  and the parameter set a bare <code>user</code>. A username identifies rather than
     *  authorizes, so the asymmetry licensing loose masking does not reach it, and with per-user
     *  pools it is often the question being asked. Asserted here in the negative because the
     *  entries are gone: nothing else would notice them coming back.</p>
     */
    public void testTheTwoSetsDivergeOnlyWhereIntended()
    {
        Set<String> keys = MaskUtils.DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS;
        Set<String> url  = MaskUtils.DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_SUBSTRINGS;

        assertFalse( "a bare 'key' key-name substring would mask whitelistBaseKey and its kind",
                     keys.contains( "key" ) );
        assertTrue( "'?key=' is how several services pass a secret", url.contains( "key" ) );

        assertFalse( "a signature in a config value is not itself a credential",
                     keys.contains( "signature" ) );
        assertTrue( "in a pre-signed URL the signature is the capability",
                    url.contains( "signature" ) );

        // The username policy, in both sets and in every spelling that used to appear.
        for ( String spelling : new String[] { "user", "c3p0.user", "username" } )
        {
            assertFalse( "accounts are no longer masked by key name: " + spelling,
                         keys.contains( spelling ) );
            assertFalse( "nor as URL parameters: " + spelling, url.contains( spelling ) );
        }

        Set<String> expectedUrl = new HashSet<String>( keys );
        expectedUrl.add( "key" );
        expectedUrl.add( "signature" );
        assertEquals( "the sets must differ at exactly the two entries above",
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

    /**
     *  THE ONLY PLACE the mask's spelling is pinned as a literal. Every other assertion in this
     *  suite and in PropertiesMaskerJUnitTestCase refers to MaskUtils.MASK, so changing the mask
     *  deliberately touches this one test -- while changing it by accident still fails here.
     *
     *  <p>An earlier version had all three test files spell the mask literally, on the reasoning
     *  that pinning the literal catches an accidental change. It does, twenty-three times over,
     *  which turned one deliberate edit into twenty-three failures and taught nothing the first
     *  failure had not.</p>
     *
     *  <p>Six rather than four because c3p0's AuthMaskingProperties used six asterisks for
     *  years: deployments grep their logs for <code>password=******</code>.</p>
     */
    public void testTheMaskIsSixAsterisks()
    {
        assertEquals( "******", MaskUtils.MASK );
    }
}
