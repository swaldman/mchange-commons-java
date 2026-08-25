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
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.NoSuchFileException;

import com.mchange.v2.net.QueryStringParser;

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
                    throw new InsecureConfigurationException(this, identifier, "identifier query string contains an unsupported key '" + s + "'. Note that keys are case-sensitive.");
            }
            boolean enforceUserOnlyPermissions = false;
            boolean requiredConfig = false;

            List<String> permissionsValues = parsedQueryString.get(PERMISSIONS_KEY);

            if (permissionsValues != null)
            {
                if (permissionsValues.size() == 0)
                    throw new InsecureConfigurationException(this, identifier, "'" + identifier + "' specifies a '" + PERMISSIONS_KEY + "' key but no value. Please supply a value, or remove the key.");

                for (String s : permissionsValues)
                {
                    if (!PERMISSIONS_VALUES.contains(s.toLowerCase()))
                        throw new InsecureConfigurationException(this, identifier, "identifier query string contains an unsupported value '" + s + "' for key '" + PERMISSIONS_KEY + "'.");
                    if (PERMISSIONS_USER_ONLY_LC.equalsIgnoreCase(s))
                        enforceUserOnlyPermissions = true;
                }
            }

            List<String> requiredValues = parsedQueryString.get(REQUIRED_KEY);
            if (requiredValues != null)
            {
                int sz = requiredValues.size();
                if (sz == 0)
                    throw new InsecureConfigurationException(this, identifier, "'" + identifier + "' specifies a '" + REQUIRED_KEY + "' key but no value. Please supply a value, or remove the key.");
                else if (sz > 1)
                    throw new InsecureConfigurationException(this, identifier, "'" + identifier + "' specifies a '" + REQUIRED_KEY + "' key but too many values (" + sz + "). Please supply a unique value, or remove the key.");
                else
                {
                    String requiredStr = requiredValues.get(0).toLowerCase();
                    if ("true".equals(requiredStr)) requiredConfig = true;
                    else if ("false".equals(requiredStr)) requiredConfig = false;
                    else throw new InsecureConfigurationException(this, identifier, "'" + identifier + "' specifies a '" + REQUIRED_KEY + "' key, which must take a value 'true' or 'false', but instead takes a value of '" + requiredStr + "'.");
                }
            }


            Path propsPath = Paths.get(new URI(fileUrl));

            // this if clause should never be satisfied, because the URI parse should have failed on a relative file path
            // nevertheless, the code that enforces that behavior is invisible to me and I'd rather backstop it.
            if (!propsPath.isAbsolute()) 
                throw new IOException("Configuration resouces can be loaded only from absolute paths. '" + propsPath + "' is not.");

            String propsPathStr = propsPath.toString();

            // we cannot log directly from here. this source is consulted while MLog is still
            // initializing its own configuration, so touching a logger would find MLog half-built.
            // DelayedLogItems exist for exactly this: they are replayed once logging is up.
            List<DelayedLogItem> delayedLogItems = new ArrayList<DelayedLogItem>();

            try
            {
                // when useronly is enforced we open the path the check resolved to, so that the file
                // examined and the file read are the same file. otherwise we open what we were given.
                Path readPath = enforceUserOnlyPermissions
                                  ? checkUserOnlyAndResolve( identifier, propsPath, delayedLogItems )
                                  : propsPath;

                Properties props = new Properties();

                try (InputStream is = new BufferedInputStream(new FileInputStream(readPath.toFile())))
                { props.load(is); }

                return new PropertiesConfigSource.Parse(props, delayedLogItems);
            }
            catch (FileNotFoundException e)
            { throw handleExceptionIndicatingFileNotFound( identifier, requiredConfig, e ); }
            catch (NoSuchFileException e)
            { throw handleExceptionIndicatingFileNotFound( identifier, requiredConfig, e ); }
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
    private Path checkUserOnlyAndResolve( String identifier, Path start, List<DelayedLogItem> delayedLogItems ) throws Exception
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
            { throw e; } // the file, or a link target, simply is not there
            catch (Exception e)
            {
                throw new InsecureConfigurationException(
                   this,
                   identifier,
                   "This configuration was specified as requiring specific file permissions, but the current environment does not support reading file permissions, or the read failed. " +
                   "Either eliminate the permissions requirement from config source identifier '" + identifier + "' or else run in an environment that supports POSIX file permissions.",
                   e
                );
            }

            hops.add( p );

            if (! acceptableOwner( attrs.owner(), delayedLogItems ) )
                throw new InsecureConfigurationException(
                    this, identifier,
                    "For '" + identifier + "', useronly permissions are set, but " +
                    (attrs.isSymbolicLink() ? "symbolic link" : "file") + " '" + p + "' is owned by '" +
                    attrs.owner().getName() + "', neither the current user ('" + currentUserName() + "') nor root." +
                    viaSuffix( hops ) );

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
                        "' has other permissions set: " + permissions + viaSuffix( hops ) );
                return p;
            }

            if (! seen.add( p ) )
                throw new InsecureConfigurationException(
                    this, identifier,
                    "For '" + identifier + "', useronly permissions are set, but the path is a cycle of " +
                    "symbolic links that never reaches a file." + viaSuffix( hops ) );

            Path target = Files.readSymbolicLink( p );
            // a link target may be relative, in which case it is relative to the link's own
            // directory -- NOT to the process working directory
            p = target.isAbsolute() ? target : p.getParent().resolve( target );
        }
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

    private Exception handleExceptionIndicatingFileNotFound(String identifier, boolean requiredConfig, Exception e)
    {
        if (requiredConfig)
            return new InsecureConfigurationException(this, identifier, "Existence of the file specified by '" + identifier +"' is required for this configuration, but the file does not exist.");
        else
            return e;
    }
}

