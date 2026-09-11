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
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationVetoedException
    {
        Set<String> whitelist = collectWhitelistSyspropsPropertiesConfig(pcfg);
        boolean nameOkay;
        if (whitelist.contains(fqcn))
            nameOkay = true;
        if (whitelist.size() == 1 && whitelist.contains("*"))
            nameOkay = true;
        else
            nameOkay = false;

        if (nameOkay)
            return instantiate(fqcn);
        else
        {
            String pcfgEnforceWhitelistStr = pcfg.getProperty(BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY);
            String syspropsEnforceWhitelistStr = System.getProperty(BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY);

            Boolean pcfgEnforceWhitelist = parseEnforceWhitelist(pcfgEnforceWhitelistStr);
            Boolean syspropsEnforceWhitelist = parseEnforceWhitelist(syspropsEnforceWhitelistStr);

            boolean enforce;
            if (pcfgEnforceWhitelist == null && syspropsEnforceWhitelist == null)
            {
                if (logger.isLoggable(MLevel.WARNING))
                    logger.log(MLevel.WARNING, "No interpretable '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "' set, currently defaulting to '" + DEFAULT_ENFORCE_WHITELIST +"'. THIS MAY CHANGE IN FUTURE RELEASES.");
                enforce = DEFAULT_ENFORCE_WHITELIST;
            }
            else if (pcfgEnforceWhitelist.booleanValue() || syspropsEnforceWhitelist.booleanValue())
                enforce = true;
            else
                enforce = false;

            if (enforce) // we already know fqcn is not in the whitelist
                throw new InstantiationVetoedException("By-name instantiation of '" + fqcn + "' vetoed. The class is not in whitelist: " + whitelist);
            else
            {
                if (logger.isLoggable(MLevel.WARNING))
                    logger.log(
                       MLevel.WARNING,
                       "Instantiating '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "' by name despite its absence from '" + BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX + "' or a subkey. " +
                       "This may be blocked in future releases. If you mean for '" + fqcn + "' to be instantiated by name, please add it to the whitelist."
                    );
                return instantiate(fqcn);
            }
        }
    }

    private static Object instantiate(String fqcn)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    { return Class.forName(fqcn).getDeclaredConstructor().newInstance(); }

    private static Boolean parseEnforceWhitelist(String val)
    {
        if (val == null)
            return DEFAULT_ENFORCE_WHITELIST;
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
        Properties allWhitelistProperties = pcfg.getPropertiesByPrefix(BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX);
        return narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig( allWhitelistProperties.stringPropertyNames(), pcfg, logger );
    }

    private ByNameInstantiationUtils()
    {}
}
