package com.mchange.v2.cfg;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
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
                    throw new InsecureConfigurationException("identifier query string contains an unsupported key '" + s + "'. Note that keys are case-sensitive.");
            }
            boolean enforceUserOnlyPermissions = false;
            boolean requiredConfig = false;

            List<String> permissionsValues = parsedQueryString.get(PERMISSIONS_KEY);

            if (permissionsValues != null)
            {
                if (permissionsValues.size() == 0)
                    throw new InsecureConfigurationException("'" + identifier + "' specifies a '" + PERMISSIONS_KEY + "' key but no value. Please supply a value, or remove the key.");

                for (String s : permissionsValues)
                {
                    if (!PERMISSIONS_VALUES.contains(s.toLowerCase()))
                        throw new InsecureConfigurationException("identifier query string contains an unsupported value '" + s + "' for key '" + PERMISSIONS_KEY + "'.");
                    if (PERMISSIONS_USER_ONLY_LC.equalsIgnoreCase(s))
                        enforceUserOnlyPermissions = true;
                }
            }

            List<String> requiredValues = parsedQueryString.get(REQUIRED_KEY);
            if (requiredValues != null)
            {
                int sz = requiredValues.size();
                if (sz == 0)
                    throw new InsecureConfigurationException("'" + identifier + "' specifies a '" + REQUIRED_KEY + "' key but no value. Please supply a value, or remove the key.");
                else if (sz > 1)
                    throw new InsecureConfigurationException("'" + identifier + "' specifies a '" + REQUIRED_KEY + "' key but too many values (" + sz + "). Please supply a unique value, or remove the key.");
                else
                {
                    String requiredStr = requiredValues.get(0).toLowerCase();
                    if ("true".equals(requiredStr)) requiredConfig = true;
                    else if ("false".equals(requiredStr)) requiredConfig = false;
                    else throw new InsecureConfigurationException("'" + identifier + "' specifies a '" + REQUIRED_KEY + "' key, which must take a value 'true' or 'false', but instead takes a value of '" + requiredStr + "'.");
                }
            }


            Path propsPath = Paths.get(new URI(fileUrl));

            // this if clause should never be satisfied, because the URI parse should have failed on a relative file path
            // nevertheless, the code that enforces that behavior is invisible to me and I'd rather backstop it.
            if (!propsPath.isAbsolute()) 
                throw new IOException("Configuration resouces can be loaded only from absolute paths. '" + propsPath + "' is not.");

            String propsPathStr = propsPath.toString();

            try
            {
                if (enforceUserOnlyPermissions)
                {
                    Set<PosixFilePermission> permissions;
                    try
                    { permissions = Files.getPosixFilePermissions(propsPath); }
                    catch (NoSuchFileException e)
                    { throw e; }
                    catch (Exception e)
                    {
                        throw new InsecureConfigurationException(
                           "This configuration was specified as requiring specific file permissions, but the current environment does not supprt reading file permissions, or the read failed. " +
                           "Either eliminate the permissions requirement from config source identifier '" + identifier + "' or else run in an environment that supports POSIX file permissions.",
                           e
                        );
                    }

                    permissions.remove(PosixFilePermission.OWNER_READ);
                    permissions.remove(PosixFilePermission.OWNER_WRITE);
                    permissions.remove(PosixFilePermission.OWNER_EXECUTE);
                    if (permissions.size() != 0)
                        throw new InsecureConfigurationException("For '" + identifier + "', useronly permissions are set, but file '" + propsPathStr + "' has other permissions set: " + permissions);
                }

                Properties props = new Properties();

                try (InputStream is = new BufferedInputStream(new FileInputStream(propsPath.toFile())))
                { props.load(is); }

                return new PropertiesConfigSource.Parse(props, Collections.<DelayedLogItem>emptyList());
            }
            catch (FileNotFoundException e)
            { throw handleExceptionIndicatingFileNotFound( identifier, requiredConfig, e ); }
            catch (NoSuchFileException e)
            { throw handleExceptionIndicatingFileNotFound( identifier, requiredConfig, e ); }
        }
        else
            throw new IllegalArgumentException("FileUrlPropertiesConfigSource accepts only identifiers beginning with 'file:', found " + identifier);
    }

    private Exception handleExceptionIndicatingFileNotFound(String identifier, boolean requiredConfig, Exception e)
    {
        if (requiredConfig)
            return new InsecureConfigurationException("Existence of the file specified by '" + identifier +"' is required for this configuration, but the file does not exist.");
        else
            return e;
    }
}

