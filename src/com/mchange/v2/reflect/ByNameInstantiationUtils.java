package com.mchange.v2.reflect;

import java.util.*;
import java.lang.reflect.InvocationTargetException;

import com.mchange.v2.log.*;

import com.mchange.v2.cfg.PropertiesConfig;

import static com.mchange.v2.cfg.PropertiesConfigUtils.securitySensitiveFalseBiasedLookupSyspropsPropertiesConfig;
import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig;

public final class ByNameInstantiationUtils
{
    final static MLogger logger = MLog.getLogger( ByNameInstantiationUtils.class );

    public final static String BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX     = "com.mchange.v2.reflect.by-name-instantiation.whitelist";
    public final static String BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY = "com.mchange.v2.reflect.by-name-instantiation.enforce-whitelist";

    private final static boolean DEFAULT_ENFORCE_WHITELIST = false;

    public static Object instantiateByName(String fqcn, PropertiesConfig pcfg)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationNotPermittedException
    {
        checkWarnThrowForInstantiateByName(fqcn, pcfg);
        return doInstantiate(fqcn);
    }

    /**
     * In some contexts, instantiation by name doesn't happen in a single step.
     * This does all the whitelist-checking and warning we want to support, and throws if we would not
     * permit the operation. You can safely instantiateNyNameUnguarded(...) if this function succeeds.
     */
    public static void checkWarnThrowForInstantiateByName(String fqcn, PropertiesConfig pcfg) throws InstantiationNotPermittedException
    {
        Set<String> whitelist = collectWhitelistSyspropsPropertiesConfig(pcfg);
        boolean nameOkay;
        if (whitelist.contains(fqcn))
            nameOkay = true;
        else if (whitelist.size() == 1 && whitelist.contains("*"))
            nameOkay = true;
        else
            nameOkay = false;

        if (!nameOkay)
        {
            String pcfgEnforceWhitelistStr = pcfg == null ? null : pcfg.getProperty(BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY);
            String syspropsEnforceWhitelistStr = System.getProperty(BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY);

            Boolean pcfgEnforceWhitelist = parseEnforceWhitelist(pcfgEnforceWhitelistStr);
            Boolean syspropsEnforceWhitelist = parseEnforceWhitelist(syspropsEnforceWhitelistStr);

            boolean enforce;
            boolean explicit;
            if (pcfgEnforceWhitelist == null && syspropsEnforceWhitelist == null)
            {
                if (logger.isLoggable(MLevel.WARNING))
                    logger.log(MLevel.WARNING, "No interpretable value set for '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "' set, currently defaulting to '" + DEFAULT_ENFORCE_WHITELIST +"'. THIS MAY CHANGE IN FUTURE RELEASES.");
                enforce = DEFAULT_ENFORCE_WHITELIST;
                explicit = false;
            }
            else
            {
                if (pcfgEnforceWhitelist == null)
                {
                    enforce = syspropsEnforceWhitelist.booleanValue();
                    explicit = true;
                }
                else if (syspropsEnforceWhitelist == null)
                {
                    enforce = pcfgEnforceWhitelist.booleanValue();
                    explicit = true;
                }
                else if (pcfgEnforceWhitelist.booleanValue() || syspropsEnforceWhitelist.booleanValue())
                {
                    enforce = true;
                    explicit = true;

                    if (logger.isLoggable(MLevel.WARNING))
                    {
                        if (pcfgEnforceWhitelist.booleanValue() != syspropsEnforceWhitelist.booleanValue())
                            logger.log(
                               MLevel.WARNING,
                               "Differing values of '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "' were found between system properties and other configuration. " +
                               "This security-sensitive key is true-biased. Since the value was 'true' in one source of configuration, the disagreement has been resolved to 'true' " +
                               "and the whitelist will be enforced. To eliminate these annoying log messages, please resolve the disagreement between System properties and other config."
                            );
                    }
                }
                else
                {
                    enforce = false;
                    explicit = true;
                }
            }

            if (enforce) // we already know fqcn is not in the whitelist
                throw new InstantiationNotPermittedException("By-name instantiation of '" + fqcn + "' not permitted. The class is not in the enforced whitelist defined by '" + BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX + "' and its subkeys. Whitelist: " + whitelist);
            else
            {
                if (!explicit && logger.isLoggable(MLevel.WARNING))
                    logger.log(
                       MLevel.WARNING,
                       "Instantiating '" + fqcn + "' by name despite its absence from '" + BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX + "' or a subkey, " +
                       "and despite no explicit suppression of whitelist enforcement via '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "=false'. " +
                       "This may be blocked in future releases. If you mean for '" + fqcn + "' to be instantiated by name, please add it to the whitelist, " +
                       "or else explicitly suppress enforcement of the whitelist. Current whitelist: " + whitelist
                    );
            }
        }
    }

    public static Object instantiateByNameUnguarded(String fqcn)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    { return doInstantiate(fqcn); }

    public static Object instantiateByNameUnguarded(String fqcn, Class<?> preloaded)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalArgumentException
    { return doInstantiate(fqcn, preloaded); }

    public static Set<String> currentWhitelist(PropertiesConfig pcfg)
    { return Collections.unmodifiableSet( collectWhitelistSyspropsPropertiesConfig(pcfg) ); }

    private static Object doInstantiate(String fqcn)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    { return Class.forName(fqcn).getDeclaredConstructor().newInstance(); }

    /**
     * For where a class is already loaded, clz.getName() must equal fqcn
     */
    private static Object doInstantiate(String fqcn, Class<?> clz)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if (!clz.getName().equals(fqcn))
            throw new IllegalArgumentException("Class " + clz + " must share a fully-qualified name with given fqcn: " + fqcn);
        return Class.forName(fqcn).getDeclaredConstructor().newInstance();
    }

    private static Boolean parseEnforceWhitelist(String val)
    {
        if (val == null)
            return null;
        else
        {
            if (val.equalsIgnoreCase("true"))
                return Boolean.TRUE;
            else if (val.equalsIgnoreCase("false"))
                return Boolean.FALSE;
            else
            {
                if (logger.isLoggable(MLevel.WARNING))
                    logger.log(MLevel.WARNING, "Found uninterpretable value for '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "', '" + val + "'.");
                return null;
            }
        }
    }

    private static Set<String> collectWhitelistSyspropsPropertiesConfig(PropertiesConfig pcfg)
    {
        Set<String> allWhitelistKeys = new HashSet<>();
        if (pcfg != null) allWhitelistKeys.addAll( pcfg.getPropertiesByPrefix(BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX).stringPropertyNames() );
        for (String k : System.getProperties().stringPropertyNames())
            if (k.startsWith(BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX))
                allWhitelistKeys.add(k);

        return narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig( allWhitelistKeys, pcfg, logger );
    }

    private ByNameInstantiationUtils()
    {}
}
