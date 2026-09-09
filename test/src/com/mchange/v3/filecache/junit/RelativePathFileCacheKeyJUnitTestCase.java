package com.mchange.v3.filecache.junit;

import java.io.File;
import java.net.URL;

import com.mchange.v3.filecache.RelativePathFileCacheKey;

import junit.framework.TestCase;

/**
 *  RelativePathFileCacheKey's constructor is the only thing standing between a caller's
 *  string and two very different resolutions of it.
 *
 *  <p>getURL() resolves the path against a parent URL, and getCacheFilePath() hands the
 *  same string straight back to FileCache, which does new File(cacheDir, path). The two
 *  disagree about what is dangerous. URL resolution normalizes ".." and clamps it at the
 *  host, so a traversal there is merely ugly; File does not, so the identical string
 *  walks out of the cache directory. Every rejection below is therefore asserted for the
 *  reason it exists, not just as "throws something".</p>
 */
public class RelativePathFileCacheKeyJUnitTestCase extends TestCase
{
    private URL base() throws Exception
    { return new URL("ftp://ftp.sec.gov/"); }

    private void assertRejected(String relPath, String why) throws Exception
    {
        try
        {
            new RelativePathFileCacheKey( base(), relPath );
            fail( "Expected rejection of [" + relPath + "]: " + why );
        }
        catch ( IllegalArgumentException expected )
        { /* the documented type */ }
    }

    // ---------- what it must accept ----------

    public void testOrdinaryRelativePath() throws Exception
    {
        String path = "edgar/data/320193/000032019320000096.txt";
        RelativePathFileCacheKey key = new RelativePathFileCacheKey( base(), path );

        assertEquals( "getCacheFilePath must hand back the path unchanged, so the cache mirrors the server's layout.",
                      path, key.getCacheFilePath() );
        assertEquals( new URL("ftp://ftp.sec.gov/edgar/data/320193/000032019320000096.txt"), key.getURL() );
    }

    /**
     *  The ".." rejection is by path segment, not by substring: a name that merely
     *  contains two dots is an ordinary name and must still be accepted.
     */
    public void testDotsWithinASegmentAreNotTraversal() throws Exception
    {
        assertEquals( "a..b/c.txt", new RelativePathFileCacheKey( base(), "a..b/c.txt" ).getCacheFilePath() );
        assertEquals( "x/..y/z.txt", new RelativePathFileCacheKey( base(), "x/..y/z.txt" ).getCacheFilePath() );
        assertEquals( "f..", new RelativePathFileCacheKey( base(), "f.." ).getCacheFilePath() );
    }

    // ---------- absolute paths ----------

    public void testRootAbsolutePathRejected() throws Exception
    { assertRejected( "/edgar/data/1.txt", "a leading / is not a relative path" ); }

    public void testSchemeRelativePathRejected() throws Exception
    { assertRejected( "//evil.example.com/x.txt", "a // spec would retarget the host" ); }

    /**
     *  new URL(context, spec) lets a spec carrying its own scheme replace the context
     *  outright, so without this check the key's URL would point at an entirely
     *  different server than the parent it was built from.
     */
    public void testAbsoluteUrlSpecRejected() throws Exception
    {
        assertRejected( "http://evil.example.com/x.txt", "a spec with its own scheme discards the parent URL" );
        assertRejected( "ftp://other.example.com/x.txt", "even the same scheme must not change host" );
    }

    // ---------- traversal ----------

    /**
     *  The case that matters: getCacheFilePath() is resolved with new File(cacheDir, path),
     *  which does not normalize, so this string escapes the cache directory outright. The
     *  test states that escape explicitly, since the URL side would have looked harmless.
     */
    public void testParentTraversalRejected() throws Exception
    {
        assertRejected( "../../../etc/passwd", "walks out of the cache directory" );

        // why it matters, made concrete -- asserted as "not beneath cacheDir" rather than
        // against a literal path, since getCanonicalPath resolves symlinks (/etc is
        // /private/etc on macOS) and that is not what is under test here.
        File cacheDir = new File( "/cache" );
        File escaped  = new File( cacheDir, "../../../etc/passwd" );
        assertFalse( "Precondition: File does not normalize '..' away, so this really does escape the cache directory.",
                     escaped.getCanonicalPath().startsWith( cacheDir.getCanonicalPath() + File.separator ) );
    }

    public void testEmbeddedTraversalRejected() throws Exception
    {
        assertRejected( "edgar/../../../etc/passwd", "a traversal after a valid segment is still a traversal" );
        assertRejected( "a/../b", "any .. segment" );
    }

    public void testBareDotDotRejected() throws Exception
    { assertRejected( "..", "the whole path is a traversal" ); }

    /**
     *  A backslash is an ordinary character in a URL path but a file separator on Windows,
     *  so this form traverses there while looking inert on a Unix build.
     */
    public void testBackslashRejected() throws Exception
    {
        assertRejected( "..\\..\\etc\\passwd", "backslash is a file separator on Windows" );
        assertRejected( "edgar\\data\\1.txt", "a URL path does not use backslashes" );
    }

    // ---------- the parent URL must name a directory ----------

    /**
     *  RFC relative resolution treats a parent's last segment as a file name and replaces
     *  it, so "ftp://host/edgar" + "data/x.txt" resolves to "ftp://host/data/x.txt" -- a
     *  sibling, not a child. The cache file would still land at cacheDir/data/x.txt, so
     *  the URL and the cache path would quietly disagree about where the thing lives.
     *  Requiring the trailing slash makes that a error rather than a silent mismatch.
     */
    public void testParentUrlWithoutTrailingSlashRejected() throws Exception
    {
        try
        {
            new RelativePathFileCacheKey( new URL("ftp://ftp.sec.gov/edgar"), "data/x.txt" );
            fail( "A parent URL that does not name a directory must be rejected." );
        }
        catch ( IllegalArgumentException expected )
        {}

        // the resolution that motivates the guard
        assertEquals( "Precondition: without the trailing slash the last segment is replaced, not descended into.",
                      new URL("ftp://ftp.sec.gov/data/x.txt"),
                      new URL( new URL("ftp://ftp.sec.gov/edgar"), "data/x.txt" ) );
    }

    public void testParentUrlWithTrailingSlashAccepted() throws Exception
    {
        RelativePathFileCacheKey key =
            new RelativePathFileCacheKey( new URL("ftp://ftp.sec.gov/edgar/"), "data/x.txt" );
        assertEquals( new URL("ftp://ftp.sec.gov/edgar/data/x.txt"), key.getURL() );
        assertEquals( "data/x.txt", key.getCacheFilePath() );
    }

    /**
     *  Requiring the trailing slash also makes the "resolves beneath the parent" check
     *  exact. The comparison is a string prefix, so with a slashless parent it could match
     *  mid-segment: "ftp://host/a" is a prefix of "ftp://host/ab/x.txt", which is a sibling
     *  of /a rather than anything beneath it. A parent ending in '/' cannot straddle a
     *  segment boundary that way.
     */
    public void testSiblingCannotSlipPastThePrefixCheck() throws Exception
    {
        try
        {
            new RelativePathFileCacheKey( new URL("ftp://ftp.sec.gov/a"), "ab/x.txt" );
            fail( "'/a' + 'ab/x.txt' resolves to the sibling '/ab/x.txt' and must not be accepted." );
        }
        catch ( IllegalArgumentException expected )
        {}
    }

    // ---------- nulls, blanks, whitespace ----------

    /**
     *  relPath.trim() used to run before the null check, so a null path threw
     *  NullPointerException rather than the IllegalArgumentException the constructor
     *  advertises. This asserts the type, which is the part that was wrong.
     */
    public void testNullRelPathThrowsIllegalArgumentNotNullPointer() throws Exception
    {
        try
        {
            new RelativePathFileCacheKey( base(), null );
            fail( "A null relative path must be rejected." );
        }
        catch ( NullPointerException npe )
        { fail( "A null relative path must raise IllegalArgumentException, not NullPointerException." ); }
        catch ( IllegalArgumentException expected )
        {}
    }

    public void testNullParentUrlRejected() throws Exception
    {
        try
        {
            new RelativePathFileCacheKey( null, "edgar/x.txt" );
            fail( "A null parent URL must be rejected." );
        }
        catch ( IllegalArgumentException expected )
        {}
    }

    public void testBlankAndUntrimmedRejected() throws Exception
    {
        assertRejected( "", "empty" );
        assertRejected( "   ", "all whitespace" );
        assertRejected( " edgar/x.txt", "leading whitespace" );
        assertRejected( "edgar/x.txt ", "trailing whitespace" );
    }

    // ---------- value semantics ----------

    public void testEqualsAndHashCode() throws Exception
    {
        RelativePathFileCacheKey a = new RelativePathFileCacheKey( base(), "edgar/x.txt" );
        RelativePathFileCacheKey b = new RelativePathFileCacheKey( base(), "edgar/x.txt" );
        RelativePathFileCacheKey c = new RelativePathFileCacheKey( base(), "edgar/y.txt" );

        assertEquals( "Keys built from the same parent and path must be equal.", a, b );
        assertEquals( "and must agree on hashCode, since EdgarFileCache maps on these.",
                      a.hashCode(), b.hashCode() );
        assertFalse( a.equals( c ) );
        assertFalse( a.equals( "not a key" ) );
        assertFalse( a.equals( null ) );
    }

    /**
     *  EdgarFileCache builds a Map keyed on getCacheFilePath() and looks entries up by the
     *  server path, so the path must round-trip exactly.
     */
    public void testCacheFilePathRoundTrips() throws Exception
    {
        String path = "edgar/data/1234/0001.txt";
        assertEquals( path, new RelativePathFileCacheKey( base(), path ).getCacheFilePath() );
    }
}
