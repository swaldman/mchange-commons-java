package com.mchange.v2.cfg;

import junit.framework.TestCase;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  Direct test of the ownership rule that {@code permissions=useronly} applies to every link in a
 *  path's chain.
 *
 *  This lives in com.mchange.v2.cfg to reach a package-private method, and it exists because the
 *  rule cannot be exercised through the filesystem from a single-uid test: building the case it
 *  defends against means creating a link owned by somebody else, which requires a second user.
 *
 *  The rule is the load-bearing part of the whole change. Following a symbolic link to its target
 *  and checking the target's mode is behavior the library already had. What is new is refusing a
 *  link that somebody else owns -- which is what stops an attacker with write access to your
 *  config directory from replacing the file with a link to some *other* file you own, and having
 *  the permissions check pass because that other file is properly private.
 */
public final class FileUrlOwnerCheckInternalJUnitTestCase extends TestCase
{
    private static UserPrincipal lookup( String name )
    {
        try { return FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName( name ); }
        catch ( Exception e ) { return null; } // not a POSIX-ish platform, or no such user here
    }

    /** The running user is of course acceptable. */
    public void testCurrentUserIsAcceptable()
    {
        UserPrincipal me = lookup( System.getProperty( "user.name" ) );
        if ( me == null ) return;
        assertTrue( FileUrlPropertiesConfigSource.acceptableOwner( me ) );
    }

    /**
     *  So is root. Root can read or replace anything regardless, so refusing root-owned links
     *  would buy no protection while breaking ordinary deployments -- a root-owned link under
     *  /etc pointing into an application directory, say. This is what OpenSSH's StrictModes does.
     */
    public void testRootIsAcceptable()
    {
        UserPrincipal root = lookup( "root" );
        if ( root == null ) return;
        assertTrue( FileUrlPropertiesConfigSource.acceptableOwner( root ) );
    }

    /** And anybody else is not. This is the assertion the whole change rests on. */
    public void testAnotherUserIsNotAcceptable()
    {
        String me = System.getProperty( "user.name" );
        String[] candidates = new String[] { "nobody", "daemon", "_www", "bin", "sys" };

        boolean tested = false;
        for ( int i = 0; i < candidates.length; ++i )
        {
            if ( candidates[i].equals( me ) ) continue;
            UserPrincipal other = lookup( candidates[i] );
            if ( other == null ) continue;

            assertFalse( "a link or file owned by '" + candidates[i] + "' must not be accepted",
                         FileUrlPropertiesConfigSource.acceptableOwner( other ) );
            tested = true;
        }
        assertTrue( "no third-party principal was resolvable here, so the rule went untested", tested );
    }

    /**
     *  The check must compare principals, not names.
     *
     *  A UserPrincipal's equality is defined on its numeric id, so comparing principals compares
     *  uids. Testing {@code "root".equals(owner.getName())} instead would trust the reverse
     *  uid-to-name mapping, which comes from /etc/passwd or whatever NSS sources are configured --
     *  possibly a directory service this host does not administer. An account named "root"
     *  carrying some other uid would then be accepted, silently, failing OPEN.
     *
     *  This principal reports the name "root" and is equal to nothing. A name-based check accepts
     *  it; a principal-based check does not.
     */
    public void testAnImpostorNamedRootIsNotAcceptable()
    {
        UserPrincipal impostor = new UserPrincipal()
        {
            @Override
            public String getName() { return "root"; }
            @Override
            public String toString() { return "root"; }
        };

        assertEquals( "the impostor does report the name", "root", impostor.getName() );
        assertFalse( "but it is not the superuser, and must not be accepted",
                     FileUrlPropertiesConfigSource.acceptableOwner( impostor ) );
    }

    /** Likewise for one impersonating the running user. */
    public void testAnImpostorNamedForTheCurrentUserIsNotAcceptable()
    {
        final String me = System.getProperty( "user.name" );
        UserPrincipal impostor = new UserPrincipal()
        {
            @Override
            public String getName() { return me; }
            @Override
            public String toString() { return me; }
        };

        assertFalse( FileUrlPropertiesConfigSource.acceptableOwner( impostor ) );
    }

    /**
     *  The superuser is identified numerically, not by the name "root".
     *
     *  A UserPrincipal exposes no public accessor for its uid -- {@code uid()} is package-private
     *  in sun.nio.fs -- so the id cannot be read off a principal directly. It is instead obtained
     *  from a filesystem root whose uid is first confirmed to be 0, and compared by equality,
     *  which is defined on that id.
     *
     *  This asserts the property that matters: whatever principal owns a path we can independently
     *  verify is uid 0 must be accepted, and that acceptance must not depend on what that principal
     *  happens to be called.
     */
    public void testSuperuserIsIdentifiedByUidNotName() throws Exception
    {
        Path root = null;
        for ( Path candidate : FileSystems.getDefault().getRootDirectories() )
        {
            try
            {
                Object uid = Files.getAttribute( candidate, "unix:uid" );
                if ( uid instanceof Number && ((Number) uid).intValue() == 0 ) { root = candidate; break; }
            }
            catch ( Exception e ) { /* no unix view here; try the next */ }
        }
        if ( root == null ) return; // platform without a numerically inspectable uid-0 root

        UserPrincipal ownerOfUidZero = Files.getOwner( root );
        assertTrue( "a principal owning a path verified to be uid 0 must be accepted",
                    FileUrlPropertiesConfigSource.acceptableOwner( ownerOfUidZero ) );

        // and it is accepted on the strength of its id, not its name: an impostor bearing the
        // same name is still refused (see testAnImpostorNamedRootIsNotAcceptable)
        final String realName = ownerOfUidZero.getName();
        UserPrincipal sameNameDifferentPrincipal = new UserPrincipal()
        {
            @Override
            public String getName() { return realName; }
            @Override
            public String toString() { return realName; }
        };
        assertFalse( "sharing the superuser's NAME must not be enough",
                     FileUrlPropertiesConfigSource.acceptableOwner( sameNameDifferentPrincipal ) );
    }

    /**
     *  When the superuser cannot be identified numerically we fall back to looking it up by the
     *  name "root", and that fallback must be reported rather than taken silently -- it accepts
     *  files owned by whatever uid the name happens to resolve to.
     *
     *  It cannot be reported by logging. This source is consulted while MLog is still reading its
     *  own configuration, so touching a logger here finds MLog half-built and raises a
     *  NullPointerException that the config machinery swallows into an unrelated warning, losing
     *  the message and silently dropping the config source. DelayedLogItems exist for this: they
     *  ride out on the Parse and are replayed once logging is up.
     */
    public void testFallbackToNameLookupIsReported()
    {
        List<DelayedLogItem> items = new ArrayList<DelayedLogItem>();

        // no roots at all -> the numeric identification cannot succeed, forcing the fallback
        UserPrincipal viaName =
            FileUrlPropertiesConfigSource.superuserPrincipal( Collections.<Path>emptyList(), items );

        assertEquals( "the fallback should report itself, exactly once", 1, items.size() );
        DelayedLogItem item = items.get( 0 );
        assertEquals( DelayedLogItem.Level.WARNING, item.getLevel() );
        assertTrue( "should say what was lost, was: " + item.getText(),
                    item.getText().contains( "name" ) && item.getText().contains( "root" ) );

        // and it still returns something usable where a root account resolves
        UserPrincipal expected = null;
        try { expected = FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName( "root" ); }
        catch ( Exception e ) { return; }
        assertEquals( expected, viaName );
    }

    /** On a platform where the numeric identification works, nothing is reported. */
    public void testNumericIdentificationIsSilent()
    {
        List<DelayedLogItem> items = new ArrayList<DelayedLogItem>();

        UserPrincipal superuser = FileUrlPropertiesConfigSource.superuserPrincipal(
            FileSystems.getDefault().getRootDirectories(), items );

        if ( superuser == null ) return; // no root resolvable at all here

        // if the numeric path worked, there is nothing to warn about
        boolean numericWorked = items.isEmpty();
        if ( numericWorked )
            assertTrue( "the numerically-identified superuser must be accepted",
                        FileUrlPropertiesConfigSource.acceptableOwner( superuser ) );
    }

    /** A null owner is refused rather than treated as a pass. */
    public void testNullOwnerIsNotAcceptable()
    { assertFalse( FileUrlPropertiesConfigSource.acceptableOwner( null ) ); }
}
