package com.mchange.v2.cfg;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystems;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.NoSuchFileException;

import com.mchange.v2.net.QueryStringParser;

import static com.mchange.v2.cfg.DelayedLogItem.Level;

public final class FileUrlPropertiesConfigSource implements PropertiesConfigSource, VetoableConfig
{
    private final static Set<String> QUERY_KEYS;
    private final static Set<String> PERMISSIONS_VALUES;

    private final static String PERMISSIONS_KEY = "permissions";
    private final static String PERMISSIONS_USER_ONLY_LC = "useronly";

    private final static String REQUIRED_KEY = "required";

    static
    {
        Set<String> tmp0 = new HashSet<>();
        tmp0.add(PERMISSIONS_KEY);
        tmp0.add(REQUIRED_KEY); // NOTE: every key handled below must be registered here, or the
                                // validation loop in propertiesFromSource rejects it as unsupported
                                // before the code that handles it can ever run
        QUERY_KEYS = Collections.unmodifiableSet(tmp0);

        Set<String> tmp1 = new HashSet<>();
        tmp1.add(PERMISSIONS_USER_ONLY_LC);
        PERMISSIONS_VALUES = Collections.unmodifiableSet(tmp1);
    }

    public static boolean isFileUrlIdentifier( String identifier )
    { return identifier.toLowerCase().startsWith("file:"); }

    public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception
    {
	if ( isFileUrlIdentifier( identifier ) )
        {
            String                   fileUrl;
            Map<String,List<String>> parsedQueryString;

            // we cannot log directly from here. this source is consulted while MLog is still
            // initializing its own configuration, so touching a logger would find MLog half-built.
            // DelayedLogItems exist for exactly this: they are replayed once logging is up.
            List<DelayedLogItem> delayedLogItems = new ArrayList<DelayedLogItem>();

            int qm_index = identifier.indexOf('?');
            if (qm_index >= 0)
            {
                fileUrl = identifier.substring(0, qm_index);
                String rawQueryString = identifier.substring(qm_index+1);
                parsedQueryString = QueryStringParser.parseQueryString(rawQueryString);
            }
            else
            {
                fileUrl = identifier;
                parsedQueryString = Collections.emptyMap();
            }

            for (String s : parsedQueryString.keySet())
            {
                if (!QUERY_KEYS.contains(s))
                    throw new InsecureConfigurationException(this, identifier, "identifier query string contains an unsupported key '" + s + "'. Note that keys are case-sensitive.", null, delayedLogItems);
            }
            boolean enforceUserOnlyPermissions = false;
            boolean requiredConfig = false;
            boolean explicitlyNotRequired = false;

            List<String> permissionsValues = parsedQueryString.get(PERMISSIONS_KEY);

            if (permissionsValues != null)
            {
                if (permissionsValues.size() == 0)
                    throw new InsecureConfigurationException(this, identifier, "'" + identifier + "' specifies a '" + PERMISSIONS_KEY + "' key but no value. Please supply a value, or remove the key.", null, delayedLogItems);

                for (String s : permissionsValues)
                {
                    if (!PERMISSIONS_VALUES.contains(s.toLowerCase()))
                        throw new InsecureConfigurationException(this, identifier, "identifier query string contains an unsupported value '" + s + "' for key '" + PERMISSIONS_KEY + "'.", null, delayedLogItems);
                    if (PERMISSIONS_USER_ONLY_LC.equalsIgnoreCase(s))
                        enforceUserOnlyPermissions = true;
                }
            }

            List<String> requiredValues = parsedQueryString.get(REQUIRED_KEY);
            if (requiredValues != null)
            {
                int sz = requiredValues.size();
                if (sz == 0)
                    throw new InsecureConfigurationException(this, identifier, "'" + identifier + "' specifies a '" + REQUIRED_KEY + "' key but no value. Please supply a value, or remove the key.", null, delayedLogItems);
                else if (sz > 1)
                    throw new InsecureConfigurationException(this, identifier, "'" + identifier + "' specifies a '" + REQUIRED_KEY + "' key but too many values (" + sz + "). Please supply a unique value, or remove the key.", null, delayedLogItems);
                else
                {
                    String requiredStr = requiredValues.get(0).toLowerCase();
                    if ("true".equals(requiredStr)) requiredConfig = true;
                    else if ("false".equals(requiredStr))
                    {
                        requiredConfig = false;
                        explicitlyNotRequired = true;
                    }
                    else throw new InsecureConfigurationException(this, identifier, "'" + identifier + "' specifies a '" + REQUIRED_KEY + "' key, which must take a value 'true' or 'false', but instead takes a value of '" + requiredStr + "'.", null, delayedLogItems);
                }
            }


            Path propsPath = Paths.get(new URI(fileUrl));

            // this if clause should never be satisfied, because the URI parse should have failed on a relative file path
            // nevertheless, the code that enforces that behavior is invisible to me and I'd rather backstop it.
            if (!propsPath.isAbsolute())
            {
                String msg = "Configuration resouces can be loaded only from absolute paths. '" + propsPath + "' is not. Ignoring.";
                delayedLogItems.add( new DelayedLogItem( Level.WARNING, msg, null ) );
                throw new ConfigParseException(msg, null, delayedLogItems);
            }

            String propsPathStr = propsPath.toString();

            try
            {
                // when useronly is enforced we open the path the check resolved to, so that the file
                // examined and the file read are the same file. otherwise we open what we were given.
                Path readPath = enforceUserOnlyPermissions
                                  ? checkUserOnlyAndResolve( identifier, requiredConfig, explicitlyNotRequired, propsPath, delayedLogItems )
                                  : propsPath;

                Properties props = new Properties();

                try (InputStream is = new BufferedInputStream(new FileInputStream(readPath.toFile())))
                { props.load(is); }

                // the file itself is sound. it can still be deleted by whoever can write a
                // directory above it, which no check on the file can prevent -- so advise.
                if (enforceUserOnlyPermissions)
                    warnIfRemovableByOthers( identifier, readPath, requiredConfig, delayedLogItems );

                return new PropertiesConfigSource.Parse(props, delayedLogItems);
            }
            catch (OwnLogCarryingMissingFileException e)
            { throw handleExceptionIndicatingFileNotFound( identifier, requiredConfig, e, delayedLogItems ); }
            catch (FileNotFoundException e)
            { throw handleExceptionIndicatingFileNotFound( identifier, requiredConfig, e, delayedLogItems  ); }
            catch (NoSuchFileException e)
            { throw handleExceptionIndicatingFileNotFound( identifier, requiredConfig, e, delayedLogItems  ); }
        }
        else
            throw new IllegalArgumentException("FileUrlPropertiesConfigSource accepts only identifiers beginning with 'file:', found " + identifier);
    }

    /**
     *  Verifies that nothing between the named path and the file it ultimately denotes is exposed
     *  to third parties, and returns the resolved file so the caller can open what was checked.
     *
     *  Symbolic links are followed rather than refused -- linking config into place is a normal
     *  deployment practice -- but every link in the chain must be owned by us (or by root), since
     *  an attacker cannot chown a link to someone else. A link we own is a link we made.
     *
     *  Only the final regular file has its permission bits examined. A link's own mode is not a
     *  usable signal: it defaults to rwxr-xr-x, so requiring owner-only bits would refuse nearly
     *  every symlink, and on Linux link modes are fixed at rwxrwxrwx and ignored by the kernel,
     *  with no portable way to change them.
     *
     *  Hard links need no special handling. A hard link IS the file -- same inode, same owner,
     *  same permissions -- so it arrives here as a regular file and gets the full check.
     *
     *  A missing file, or a dangling link, raises NoSuchFileException from readAttributes, which
     *  the caller routes to its ordinary not-found handling. Absence is not insecurity.
     */
    private Path checkUserOnlyAndResolve( String identifier, boolean requiredConfig, boolean explicitlyNotRequired, Path start, List<DelayedLogItem> delayedLogItems ) throws Exception
    {
        Path        p    = start;
        Set<Path>   seen = new HashSet<>();
        List<Path>  hops = new ArrayList<>();

        for (;;)
        {
            PosixFileAttributes attrs;
            try
            { attrs = Files.readAttributes( p, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS ); }
            catch (NoSuchFileException e)
            {
                if (requiredConfig)
                {
                    // if required is true, the missing file exception will trigger an InsecureConfigurationException
                    String msg = "Required file for identifier '" + identifier + "' was not present when its permissions were checked.";
                    delayedLogItems.add( new DelayedLogItem( Level.FINE, msg, e ) );
                    throw new OwnLogCarryingMissingFileException(e.getMessage(), e, delayedLogItems);
                }
                else if (explicitlyNotRequired)
                {
                    // if required is false and explicitlyNotRequired is true, the missing file exception
                    // will be forwarded to the BasicMultiPropertiesConfig and warn
                    delayedLogItems.add( MConfig.skippingFileNotFoundDelayedItem(identifier, e) );
                    throw new OwnLogCarryingMissingFileException(e.getMessage(), e, delayedLogItems);
                }
                else // the file is not required, but only by default
                {
                    String msg =
                        "No file found for SECURITY-SENSITIVE 'permissions=useronly' identifier '" + identifier +
                        "'. The absence of this configuration will be SKIPPED and IGNORED. (Set 'required=true' if you " +
                        "wish to insist on this file's presence, set 'required=false' explicitly to eliminate this message.)";
                    DelayedLogItem dli = new DelayedLogItem( Level.WARNING, msg, e );
                    delayedLogItems.add( dli );
                    // the items must ride out ON the exception: we are about to throw, and a
                    // Parse -- the only other way they reach the caller -- is built only on
                    // the success path, so anything left in this list would be discarded.
                    throw new OwnLogCarryingMissingFileException( msg, e, delayedLogItems );
                }
            } // the file, or a link target, simply is not there
            catch (Exception e)
            {
                throw new InsecureConfigurationException(
                   this,
                   identifier,
                   "This configuration was specified as requiring specific file permissions, but the current environment does not support reading file permissions, or the read failed. " +
                   "Either eliminate the permissions requirement from config source identifier '" + identifier + "' or else run in an environment that supports POSIX file permissions.",
                   e,
                   delayedLogItems
                );
            }

            hops.add( p );

            if (! acceptableOwner( attrs.owner(), delayedLogItems ) )
                throw new InsecureConfigurationException(
                    this, identifier,
                    "For '" + identifier + "', useronly permissions are set, but " +
                    (attrs.isSymbolicLink() ? "symbolic link" : "file") + " '" + p + "' is owned by '" +
                    attrs.owner().getName() + "', neither the current user ('" + currentUserName() + "') nor root." +
                    viaSuffix( hops ), null, delayedLogItems );

            if (! attrs.isSymbolicLink() )
            {
                Set<PosixFilePermission> permissions = new HashSet<>( attrs.permissions() );
                permissions.remove(PosixFilePermission.OWNER_READ);
                permissions.remove(PosixFilePermission.OWNER_WRITE);
                permissions.remove(PosixFilePermission.OWNER_EXECUTE);
                if (permissions.size() != 0)
                    throw new InsecureConfigurationException(
                        this, identifier,
                        "For '" + identifier + "', useronly permissions are set, but file '" + p +
                        "' has other permissions set: " + permissions + viaSuffix( hops ), null, delayedLogItems );
                return p;
            }

            if (! seen.add( p ) )
                throw new InsecureConfigurationException(
                    this, identifier,
                    "For '" + identifier + "', useronly permissions are set, but the path is a cycle of " +
                    "symbolic links that never reaches a file." + viaSuffix( hops ), null, delayedLogItems );

            Path target = Files.readSymbolicLink( p );
            // a link target may be relative, in which case it is relative to the link's own
            // directory -- NOT to the process working directory
            p = target.isAbsolute() ? target : p.getParent().resolve( target );
        }
    }

    /**
     *  Advisory only. Never vetoes, never affects what is read.
     *
     *  The permission and ownership checks above establish that nobody else can change what this
     *  file says. They cannot establish that the file will still be there: deletion is controlled
     *  by the directories above it, not by the file. Whoever can write a containing directory can
     *  unlink the configuration, and can do so at any depth -- a tight 0700 directory inside a
     *  world-writable parent is not safe, because the parent's writer can rename the whole subtree
     *  aside and put their own in its place. So we walk all the way up.
     *
     *  That matters here more than it might elsewhere because of an asymmetry in this class: with
     *  required=true a vanished file throws, loudly. Otherwise it is skipped at FINE and the
     *  application simply runs on without configuration it was written to expect. The quiet case
     *  is the dangerous one, so it is the one we warn about, and required=true is the remedy we
     *  suggest -- it converts a silent disappearance into a loud failure.
     *
     *  We deliberately impose no requirement on directory structure. A general configuration
     *  library cannot demand a dedicated directory the way ssh demands ~/.ssh; config lives in
     *  shared trees for legitimate reasons. We only point out what we notice.
     *
     *  And we notice only some of it. This is a BEST-EFFORT detection, deliberately biased toward
     *  silence: where we cannot establish that a risk is present, we say nothing rather than guess.
     *  So the absence of a warning is NOT an assurance that a file is safe from deletion -- it may
     *  equally mean we could not tell. We stay silent, specifically, when the real path will not
     *  resolve, so that there is no chain of directories to examine at all; when a directory along
     *  the way cannot be stat'ed, since being unable to look at something is not evidence against
     *  it, so the walk skips it and continues upward; and when the sticky bit cannot be read
     *  because the non-standard "unix" attribute view is unavailable, since without it a
     *  world-writable directory cannot be told apart from a world-writable sticky one like /tmp,
     *  and warning about both would be worse than warning about neither.
     *
     *  Two further gaps are inherent rather than incidental. First, mode bits are not the only
     *  thing that can grant write access to a directory: an ACL can, on filesystems that carry
     *  them, and we do not examine ACLs -- so a directory we pass over in silence may still be
     *  writable by someone else. Second, this is a point-in-time observation, as every check here
     *  is; a directory's mode or ownership can change after we have looked at it.
     *
     *  Erring toward silence is the right bias for an advisory that cannot be switched off. A
     *  warning that fires when it should not teaches people to ignore the warning, which costs
     *  more than the cases we miss.
     */
    private void warnIfRemovableByOthers( String identifier, Path readPath, boolean requiredConfig,
                                          List<DelayedLogItem> delayedLogItems )
    {
        // a vanished required file already throws. there is no silent failure to warn about.
        if ( requiredConfig ) return;

        Path start;
        try { start = readPath.toRealPath(); }
        catch ( Exception e ) { return; } // cannot resolve it, cannot advise about it

        // NOTE: the walk must start from the REAL path. getParent() is purely lexical, so for a
        // file under /etc on a system where /etc is a symlink to /private/etc, the lexical parents
        // are /etc and / -- and /private itself is never examined at all.
        for ( Path dir = start.getParent(); dir != null; dir = dir.getParent() )
        {
            PosixFileAttributes attrs;
            try
            { attrs = Files.readAttributes( dir, PosixFileAttributes.class ); }
            catch ( Exception e )
            { continue; } // a directory we cannot stat is not evidence of risk. keep walking.

            String problem = null;

            if (! acceptableOwner( attrs.owner(), delayedLogItems ) )
            {
                // note that the sticky bit does not save us here: in a sticky directory the
                // directory's own owner may still remove anything within it.
                problem = "is owned by '" + attrs.owner().getName() + "', neither the current user nor root";
            }
            else
            {
                Set<PosixFilePermission> perms = attrs.permissions();
                boolean writableByOthers = perms.contains( PosixFilePermission.GROUP_WRITE )
                                        || perms.contains( PosixFilePermission.OTHERS_WRITE );
                if ( writableByOthers && Boolean.FALSE.equals( sticky( dir ) ) )
                    problem = "is writable by others (" + PosixFilePermissions.toString( perms ) + ") and is not sticky";
            }

            if ( problem != null )
            {
                delayedLogItems.add( new DelayedLogItem( DelayedLogItem.Level.WARNING,
                    "For '" + identifier + "', useronly permissions are set, but directory '" + dir + "' " +
                    problem + ". Anyone able to write that directory can delete this configuration. Because it " +
                    "is not marked required, reads would then silently proceed without it. Consider adding " +
                    "required=true, so that a disappearance fails loudly instead." ) );
                return; // one warning: the nearest offending directory is the one to fix first
            }
        }
    }

    /**
     *  TRUE, FALSE, or null when we cannot tell.
     *
     *  The distinction matters. World-writable directories are ordinary and safe when sticky --
     *  /tmp and /var/tmp are drwxrwxrwt on every Unix -- because a sticky directory lets only a
     *  file's owner (or the directory's owner, or root) unlink it. Warning about those would be a
     *  false positive on the most common shared directories there are, which is exactly how an
     *  advisory teaches people to ignore it.
     *
     *  PosixFilePermission cannot express this: it is the nine rwx bits and nothing else. So we
     *  read the raw mode, which requires the non-standard "unix" attribute view -- "posix" is the
     *  standard one -- exactly as the superuser check above does. Where it is unavailable we
     *  return null and simply decline to judge that directory, rather than guess. Unlike the
     *  superuser fallback there is nothing to report: no security decision is weakened, we are
     *  only withholding advice.
     */
    private static Boolean sticky( Path dir )
    {
        try
        {
            Object mode = Files.getAttribute( dir, "unix:mode" );
            if ( mode instanceof Number )
                return Boolean.valueOf( (((Number) mode).intValue() & 01000) != 0 );
        }
        catch ( Exception e ) { /* view unsupported; fall through to null */ }
        return null;
    }

    /** Names the chain walked, when there was one, so a failure says which hop was at fault. */
    private static String viaSuffix( List<Path> hops )
    {
        if ( hops.size() < 2 ) return "";
        StringBuilder sb = new StringBuilder( " (reached via " );
        for ( int i = 0; i < hops.size(); ++i )
        {
            if ( i > 0 ) sb.append( " -> " );
            sb.append( '\'' ).append( hops.get(i) ).append( '\'' );
        }
        return sb.append( ')' ).toString();
    }

    private static String currentUserName()
    { return System.getProperty( "user.name" ); }

    /**
     *  We accept root as well as the current user. Root can read or replace anything regardless,
     *  so refusing root-owned links would break ordinary deployments -- a root-owned link under
     *  /etc pointing into an application directory, say -- while buying no protection. This is
     *  what OpenSSH's StrictModes does for the same problem.
     *
     *  NOTE: both tests compare UserPrincipals, never names. That matters. A principal's equality
     *  is defined on the numeric id, so comparing principals compares uids; a name is used only to
     *  look one up. Testing the name instead -- asking whether the owner is called "root" -- would
     *  trust a reverse uid-to-name mapping that this process does not control. It comes from
     *  /etc/passwd or from whatever NSS sources are configured, possibly a directory service, and
     *  an account named "root" carrying some other uid would then be accepted. That failure would
     *  be silent and would fail OPEN. Comparing principals cannot fail that way: if a lookup does
     *  not resolve, we simply do not accept, which fails closed.
     */
    // package-private for testing: this is the rule that actually blocks a planted link, and it
    // cannot be exercised through the filesystem without a second uid
    static boolean acceptableOwner( UserPrincipal owner, List<DelayedLogItem> delayedLogItems )
    {
        if ( owner == null ) return false;
        if ( owner.equals( principalOrNull( currentUserName() ) ) ) return true;

        UserPrincipal superuser = superuserPrincipal( delayedLogItems );
        return superuser != null && owner.equals( superuser );
    }

    /** Convenience for callers with nothing to collect into, notably tests. */
    static boolean acceptableOwner( UserPrincipal owner )
    { return acceptableOwner( owner, new ArrayList<DelayedLogItem>() ); }

    /**
     *  The superuser, identified numerically rather than by the name "root".
     *
     *  Looking the superuser up as {@code lookupPrincipalByName("root")} would trust the name-to-uid
     *  mapping, which this process does not control -- it comes from /etc/passwd or from whatever
     *  NSS sources are configured, possibly a directory service. Were "root" ever to name some other
     *  uid, we would accept files owned by that uid, silently, and fail OPEN. So instead we ask a
     *  filesystem root for its owner, but only after confirming numerically that it is uid 0. The
     *  principal we get back can then be compared by identity, since UserPrincipal equality is
     *  defined on the numeric id.
     *
     *  Note that we never read the id off the principal: there is no public accessor for it
     *  ({@code uid()} is package-private in sun.nio.fs), which is why this is done via a path.
     *
     *  The "unix" attribute view is not standard -- "posix" is -- so a provider could offer POSIX
     *  permissions without it. In that case we fall back to the name lookup: no worse than having
     *  no numeric check at all, and better than refusing every root-owned link on such a platform.
     */
    private static UserPrincipal superuserPrincipal( List<DelayedLogItem> delayedLogItems )
    { return superuserPrincipal( FileSystems.getDefault().getRootDirectories(), delayedLogItems ); }

    // package-private, and taking the roots to try, so a test can exercise the fallback branch --
    // which is otherwise unreachable on any platform where the numeric lookup works
    static UserPrincipal superuserPrincipal( Iterable<Path> roots, List<DelayedLogItem> delayedLogItems )
    {
        for ( Path root : roots )
        {
            try
            {
                Object uid = Files.getAttribute( root, "unix:uid", LinkOption.NOFOLLOW_LINKS );
                if ( uid instanceof Number && ((Number) uid).intValue() == 0 )
                    return Files.getOwner( root, LinkOption.NOFOLLOW_LINKS );
            }
            catch ( Exception e )
            { /* unreadable or no unix view -- try the next root, then fall back below */ }
        }

        if ( delayedLogItems != null )
            delayedLogItems.add( new DelayedLogItem(
                DelayedLogItem.Level.WARNING,
                "Could not identify the superuser numerically: no filesystem root reported a 'unix:uid' of 0. " +
                "Falling back to looking the superuser up by the name 'root', which trusts a name-to-uid mapping " +
                "this process does not control. A 'permissions=useronly' check will accept files and links owned " +
                "by whatever uid that name resolves to." ) );

        return principalOrNull( "root" );
    }

    /** A principal by name, or null if it does not resolve. Null never compares equal to an owner. */
    private static UserPrincipal principalOrNull( String name )
    {
        if ( name == null ) return null;
        try
        { return FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName( name ); }
        catch ( Exception e )
        { return null; }
    }

    private Exception handleExceptionIndicatingFileNotFound(String identifier, boolean requiredConfig, Exception e, List<DelayedLogItem> delayedLogItems)
    {
        if (requiredConfig)
            return new InsecureConfigurationException(this, identifier, "Existence of the file specified by '" + identifier +"' is required for this configuration, but the file does not exist.", e, delayedLogItems);
        else if (e instanceof ConfigParseException)
            return e;
        else
        {
            delayedLogItems.add( MConfig.skippingFileNotFoundDelayedItem(identifier, e) );
            return new OwnLogCarryingMissingFileException(e.getMessage(), e, delayedLogItems);
        }
    }
}

