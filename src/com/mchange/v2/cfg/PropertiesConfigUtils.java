package com.mchange.v2.cfg;

import java.util.*;

import com.mchange.v2.log.*;

import com.mchange.v2.lang.ObjectUtils;
import com.mchange.v2.util.IterableUtils;

public class PropertiesConfigUtils
{
    public static class ConfigSnapshot
    {
        Properties pConfigProperties;
        Properties systemProperties;

        /**
         *  relevantPrefix can be "" if you want to warn on ANY change to pcfg. Properties from pcfg by prefix should be fast
         *
         *  we always warn on any change to sysprops because clone() is gonna be faster than iterating to check a subset.
         */
        public ConfigSnapshot(PropertiesConfig pcfg, String relevantPrefix)
        {
            this.pConfigProperties = pcfg == null ? null : (Properties) pcfg.getPropertiesByPrefix(relevantPrefix).clone();
            this.systemProperties = (Properties) System.getProperties().clone();
        }

        public ConfigSnapshot(PropertiesConfig pcfg)
        { this( pcfg, "" ); }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof ConfigSnapshot)
            {
                ConfigSnapshot other = (ConfigSnapshot) o;
                return
                    ObjectUtils.eqOrBothNull(this.pConfigProperties,other.pConfigProperties) &&
                    ObjectUtils.eqOrBothNull(this.systemProperties,other.systemProperties);
            }
            else
                return false;
        }

        @Override
        public int hashCode()
        {return ObjectUtils.hashOrZero(pConfigProperties) ^ ObjectUtils.hashOrZero(systemProperties);}
    }

    // pcfg can be null
    public static boolean securitySensitiveFalseBiasedLookupSyspropsPropertiesConfig( String propStyleKey, PropertiesConfig pcfg, String whatWillBeDisabled, MLogger logger )
    {
        String systemPropertiesBasedShouldSupportStr = System.getProperty( propStyleKey );
        Boolean systemPropertiesBasedShouldSupport = systemPropertiesBasedShouldSupportStr == null ? null : Boolean.valueOf( systemPropertiesBasedShouldSupportStr );

        Boolean pcfgBasedShouldSupport;
        if ( pcfg != null )
        {
            String pcfgBasedShouldSupportStr = pcfg.getProperty( propStyleKey );
            pcfgBasedShouldSupport = pcfgBasedShouldSupportStr == null ? null : Boolean.valueOf( pcfgBasedShouldSupportStr );
        }
        else
            pcfgBasedShouldSupport = null;

        boolean out;
        if ( Boolean.FALSE.equals( systemPropertiesBasedShouldSupport ) )
        {
            if (Boolean.TRUE.equals(pcfgBasedShouldSupport))
            {
                if ( logger.isLoggable( MLevel.WARNING ) )
                    logger.log(
                       MLevel.WARNING,
                       "Security-sensitive property '" + propStyleKey +
                       "' has been set to 'false' in System properties. Disabling this functionality in System properties conservatively " +
                       "OVERRIDES any configuration of this property set elsewhere, regardless of any alternative prioritization of system properties you may have configured. " +
                       "Please resolve the inconsistency of configuration." +
                       whatWillBeDisabled + " will be disabled!"
                    );
            }
            out = false;
        }
        else if ( Boolean.TRUE.equals( systemPropertiesBasedShouldSupport ) )
        {
            if ( Boolean.FALSE.equals( pcfgBasedShouldSupport ) )
            {
                if ( logger.isLoggable( MLevel.WARNING ) )
                    logger.log(
                       MLevel.WARNING,
                       "Security-sensitive property '" + propStyleKey +
                       "' has been set to 'true' in System properties, however it has been set to 'false' in other configuration supplied. Disabling this functionality in  " +
                       "supplied configuration overrides permission granted in System properties. " +
                       "Please resolve the inconsistency of configuration." +
                       whatWillBeDisabled + " will be disabled!"
                    );
                out = false;
            }
            else // System prop is explicitly set to true, MConfig value is either unset or set to true
            {
                out = true;
            }
        }
        else // property unset in System properties, defer to pcfg, only support if explicitly set to true there
        {
            out = Boolean.TRUE.equals( pcfgBasedShouldSupport );
        }
        return out;
    }


    // pcfg can be null
    //
    // returns null iff the key is unavailable from either source
    public static Set<String> narrowestStringSetFromStringListSyspropsPropertiesConfig( String propStyleKey, PropertiesConfig pcfg, MLogger logger )
    {
        String rawSysProp = System.getProperty( propStyleKey );
        String rawPropsConfigProp = pcfg == null ? null : pcfg.getProperty( propStyleKey );

        if (rawSysProp == null && rawPropsConfigProp == null)
            return null;
        else if (rawSysProp != null && rawPropsConfigProp == null)
            return commaSeparatedStringListToSet( rawSysProp );
        else if (rawSysProp == null && rawPropsConfigProp != null)
            return commaSeparatedStringListToSet( rawPropsConfigProp );
        else
        {
            Set<String> sysPropSet = commaSeparatedStringListToModifiableSet( rawSysProp );
            Set<String> propsConfigSet = commaSeparatedStringListToModifiableSet( rawPropsConfigProp );

            if (sysPropSet.equals(propsConfigSet))
                return Collections.unmodifiableSet(sysPropSet);
            else
            {
                sysPropSet.retainAll(propsConfigSet);
                Set<String> out = Collections.unmodifiableSet(sysPropSet);

                if ( logger.isLoggable( MLevel.WARNING ) )
                    logger.log(
                        MLevel.WARNING,
                        "Inconsistent values of '" + propStyleKey + "' were found in System properties and the provided configuration. " +
                        "We are conservatively using the *intersection* of those values. " +
                        "Value in System properties: '" + rawSysProp + "'; Value in PropertiesConfig: '" + rawPropsConfigProp + "'; " +
                        "Value of intersection: '" + IterableUtils.joinAsString(",",out)
                    );

                return out;
            }
        }
    }

    // for lists that can be grown additively across multiple keys,
    // useful for whitelists that multiple layers of an application may need to add to,
    // saves awkwardly trying to continually append
    public static Set<String> narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig( Set<String> keys, PropertiesConfig pcfg, MLogger logger )
    {
        int len = keys.size();
        String[] keysArray = keys.toArray(new String[len]);
        Set<String> out = new HashSet<>();
        for (int i = 0; i < len; ++i)
        {
            String key = keysArray[i];
            Set<String> valuesForKey = narrowestStringSetFromStringListSyspropsPropertiesConfig( key, pcfg, logger );
            if (valuesForKey != null)
            {
                if (logger.isLoggable(MLevel.FINE))
                    logger.log(MLevel.FINE, "Building Set<String>, adding for key '" + key + "' values: " + valuesForKey);
                out.addAll(valuesForKey);
            }
        }
        return out;
    }

    static abstract class WhitelistManager
    {
        String baseKey;
        String legacyKey;

        String whitelistBaseKey;
        String overrideWhitelistKey;

        //MT: protected by this' monitor
        private ConfigSnapshot warnedOverride = null; // override is a supported configuration, should only warn once, not nag

        private synchronized boolean warnOnOverride(PropertiesConfig pcfg)
        {
            ConfigSnapshot check = new ConfigSnapshot(pcfg, whitelistBaseKey);
            if (check.equals(warnedOverride))
                return false;
            else
            {
                warnedOverride = check;
                return true;
            }
        }

        public WhitelistManager(String baseKey, String legacyKey)
        {
            this.baseKey = baseKey;
            this.legacyKey = legacyKey;
            this.whitelistBaseKey = baseKey + ".whitelist";
            this.overrideWhitelistKey = baseKey + ".overrideWhitelist";
        }

        public Set<String> collectWhitelistSyspropsPropertiesConfig(PropertiesConfig pcfg, MLogger logger)
        {
            Set<String> out          = null;
            String whitelistModifier = null;
            String effectiveKey      = null;

            // if a legacy whitelist exists, we use it, but with a big warning to migrate
            if (legacyKey != null)
            {
                Set<String> legacyWhitelist = narrowestStringSetFromStringListSyspropsPropertiesConfig( legacyKey, pcfg, logger );
                if (legacyWhitelist != null && legacyWhitelist.size() > 0)
                {
                    out = legacyWhitelist;
                    whitelistModifier = "deprecated ";
                    effectiveKey = legacyKey;

                    if (logger.isLoggable(MLevel.WARNING))
                    {
                        logger.log(
                          MLevel.WARNING,
                          "Deprecated whitelist key '" + legacyKey + "' found. Please remove this from your configuration and migrate to '" + whitelistBaseKey + "' or any subkey for defining whitelist elements. " +
                          "Please note that as long as this key is present in your configuration, it will remain effective, and '" + whitelistBaseKey + "', its subkeys, and '" + overrideWhitelistKey + "' will all be ignored! " +
                          "Effective whitelist: " + out
                        );
                    }
                }
            }

            if (out == null)
            {
                Set<String> override = narrowestStringSetFromStringListSyspropsPropertiesConfig( overrideWhitelistKey, pcfg, logger);
                if (override != null)
                {
                    if (logger.isLoggable(MLevel.WARNING) && warnOnOverride(pcfg))
                        logger.log(MLevel.WARNING,
                                   "The whitelist that would have been built from '" + whitelistBaseKey +
                                   "' and its subkeys has been overridden by '" + overrideWhitelistKey +
                                   "'. The overridden whitelist that will be in effect is " + override + ".");
                    out = override;
                    whitelistModifier = "override ";
                    effectiveKey = overrideWhitelistKey;
                }
                else
                {
                    Set<String> allWhitelistKeys = new HashSet<>();
                    if (pcfg != null) allWhitelistKeys.addAll( pcfg.getPropertiesByPrefix(whitelistBaseKey).stringPropertyNames() );
                    for (String k : System.getProperties().stringPropertyNames())
                        if (k.startsWith(whitelistBaseKey))
                            allWhitelistKeys.add(k);

                    Set<String> noOverride = narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig( allWhitelistKeys, pcfg, logger );
                    out = noOverride;
                    whitelistModifier = "";
                    effectiveKey = whitelistBaseKey;
                }
            }

            if (logger.isLoggable(MLevel.WARNING) && out.contains("*") && out.size() > 1)
            {
                String whitelistDescriptor =
                    whitelistModifier + "whitelist defined by key '" + effectiveKey + "'" +
                    (effectiveKey == whitelistBaseKey ? " and its subkeys" : "");
                logger.log(
                  MLevel.WARNING,
                  "The " + whitelistDescriptor + " contains an '*' entry, but it is not unique and will be ignored. " +
                  "To disable whitelist enforcement, '*' must be the whitelist's only element. " +
                  "Please either remove the '*' element, or ensure that it is unique in the effective whitelist: " +
                  out
                );
            }
            return out;
        }
    }

    public static Set<String> commaSeparatedStringListToModifiableSet( String csList )
    {
        if ("".equals(csList.trim()))
            return new HashSet<String>();
        else
        {
            String[] items = csList.split("\\s*,\\s*");
            return new HashSet<String>(Arrays.asList(items));
        }
    }

    public static Set<String> commaSeparatedStringListToSet( String csList )
    { return Collections.unmodifiableSet(commaSeparatedStringListToModifiableSet(csList)); }

    private PropertiesConfigUtils()
    {}
}
