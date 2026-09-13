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

    /**
     * for lists that can be grown additively across multiple keys,
     * useful for whitelists that multiple layers of an application may need to add to,
     * saves awkwardly trying to continually append
     *
     * builds a whitelist formed by union across keys, intersection between system properties and config within
     *
     * an empty set means an empty whitelist, forbids everything
     *
     * pcfg can be null
     *
     * @return set of elements of the composite whitelist, null if none of the keys are present
     */
    public static Set<String> narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig( Set<String> keys, PropertiesConfig pcfg, MLogger logger )
    {
        int len = keys.size();
        String[] keysArray = keys.toArray(new String[len]);
        Set<String> out = null;
        for (int i = 0; i < len; ++i)
        {
            String key = keysArray[i];
            Set<String> valuesForKey = narrowestStringSetFromStringListSyspropsPropertiesConfig( key, pcfg, logger );
            if (valuesForKey != null)
            {
                if (out == null) out = new HashSet<>();
                if (logger.isLoggable(MLevel.FINE))
                    logger.log(MLevel.FINE, "Building Set<String>, adding for key '" + key + "' values: " + valuesForKey);
                out.addAll(valuesForKey);
            }
        }
        return out;
    }

    public static class WhitelistInfo
    {
        public enum Source {
            MAIN_WHITELIST("whitelist"), OVERRIDE("override whitelist"), DEPRECATED("deprecated whitelist"), MISSING("missing whitelist");

            final String identifier;

            Source(String identifier)
            { this.identifier = identifier; }

            public String getIdentifier() { return identifier; }
        }

        final Set<String> whitelist;
        final Source source;

        public Set<String> getWhitelist() { return whitelist; }
        public Source      getSource()    { return source; }

        public WhitelistInfo(Set<String> whitelist, Source source)
        {
            if (whitelist == null)
                throw new IllegalArgumentException("A WhitelistInfo must describe a non-null whitelist; null provided instead.");
            if (source == null)
                throw new IllegalArgumentException("A WhitelistInfo must provide a non-null source; null provided instead.");

            this.whitelist = whitelist;
            this.source = source;
        }

        private boolean _equals(WhitelistInfo other)
        { return this.whitelist.equals(other.whitelist) && this.source.equals(other.source); }

        @Override
        public boolean equals(Object o)
        { return this == o || ((o instanceof WhitelistInfo) && _equals((WhitelistInfo) o)); }

        @Override
        public int hashCode()
        { return whitelist.hashCode() ^ source.hashCode(); }

        @Override
        public String toString()
        {
            switch (source)
            {
            case MISSING:
                return source.getIdentifier();
            default:
                return source.getIdentifier() + ": " + whitelist;
            }
        }
    }


    public static class WhitelistManager
    {
        final String baseKey;
        final String deprecatedKey;

        final String whitelistBaseKey;
        final String overrideWhitelistKey;

        //MT: protected by this' monitor
        private ConfigSnapshot warnedSupported = null; // override and empty whitelists are supported configurations, should only warn once, not nag

        public WhitelistManager(String baseKey, String deprecatedKey)
        {
            this.baseKey = baseKey;
            this.deprecatedKey = deprecatedKey;
            this.whitelistBaseKey = baseKey + ".whitelist";
            this.overrideWhitelistKey = baseKey + ".overrideWhitelist";
        }

        private synchronized boolean warnSupported(PropertiesConfig pcfg)
        {
            ConfigSnapshot check = new ConfigSnapshot(pcfg, whitelistBaseKey);
            if (check.equals(warnedSupported))
                return false;
            else
            {
                warnedSupported = check;
                return true;
            }
        }

        public String getTopLevelBaseKey()      { return baseKey; }
        public String getDeprecatedKey()        { return deprecatedKey; }
        public String getWhitelistBaseKey()     { return whitelistBaseKey; }
        public String getOverrideWhitelistKey() { return overrideWhitelistKey; }

        public WhitelistInfo collectWhitelistInfoSyspropsPropertiesConfig(PropertiesConfig pcfg, MLogger logger)
        {
            Set<String> whitelist          = null;

            WhitelistInfo.Source source = null;

            Boolean shouldWarnSupported = null;

            // if a deprecated whitelist exists, we use it, but with a big warning to migrate
            if (deprecatedKey != null)
            {
                Set<String> deprecatedWhitelist = narrowestStringSetFromStringListSyspropsPropertiesConfig( deprecatedKey, pcfg, logger );
                if (deprecatedWhitelist != null)
                {
                    whitelist = deprecatedWhitelist;
                    source = WhitelistInfo.Source.DEPRECATED;

                    if (logger.isLoggable(MLevel.WARNING))
                    {
                        // staying on a deprecated key is not supported, we're content to nag
                        logger.log(
                          MLevel.WARNING,
                          "Deprecated whitelist key '" + deprecatedKey + "' found. Please remove this from your configuration and migrate to '" + whitelistBaseKey + "' or any subkey for defining whitelist elements. " +
                          "Please note that as long as this key is present in your configuration, it will remain effective, and '" + whitelistBaseKey + "', its subkeys, and '" + overrideWhitelistKey + "' will all be ignored! " +
                          "Effective whitelist: " + whitelist
                        );
                    }
                }
            }

            if (whitelist == null)
            {
                Set<String> override = narrowestStringSetFromStringListSyspropsPropertiesConfig( overrideWhitelistKey, pcfg, logger);
                if (override != null)
                {
                    if (logger.isLoggable(MLevel.WARNING))
                    {
                        // staying on override whitelists is supported, we warn just once
                        if (shouldWarnSupported == null)
                            shouldWarnSupported = Boolean.valueOf( warnSupported(pcfg) );
                        if (shouldWarnSupported.booleanValue())
                        {
                            logger.log(MLevel.WARNING,
                                       "The whitelist that would have been built from '" + whitelistBaseKey +
                                       "' and its subkeys has been overridden by '" + overrideWhitelistKey +
                                       "'. The overridden whitelist that will be in effect is " + override + ".");
                        }
                    }
                    whitelist = override;
                    source = WhitelistInfo.Source.OVERRIDE;
                }
                else
                {
                    Set<String> allWhitelistKeys = new HashSet<>();
                    if (pcfg != null) allWhitelistKeys.addAll( pcfg.getPropertiesByPrefix(whitelistBaseKey).stringPropertyNames() );
                    for (String k : System.getProperties().stringPropertyNames())
                        if (k.startsWith(whitelistBaseKey))
                            allWhitelistKeys.add(k);

                    Set<String> noOverride = narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig( allWhitelistKeys, pcfg, logger );
                    whitelist = noOverride;
                    source = WhitelistInfo.Source.MAIN_WHITELIST;
                }
            }

            // various warnings that may need to be emitted about a whitelist, but only if there is one
            if (whitelist != null)
            {
                if (logger.isLoggable(MLevel.WARNING))
                {
                    int sz = whitelist.size();
                    String whitelistDescriptor = null;
                    if (sz == 0)
                    {
                        // empty whitelists meaning deny-all are supported, we warn just once
                        if (shouldWarnSupported == null)
                            shouldWarnSupported = Boolean.valueOf( warnSupported(pcfg) );
                        if (shouldWarnSupported.booleanValue())
                        {
                            // null check isn't useful here, but we might someday add more warnings and it's very cheap
                            if (whitelistDescriptor == null ) whitelistDescriptor = makeWhitelistDescriptor(source);
                            logger.log(
                               MLevel.WARNING,
                               "The " + whitelistDescriptor + " contains no entries, an empty whitelist, which means deny-all. " +
                               "If that is not intended, please configure entries in the whitelist, or a unique entry '*' to allow all."
                            );
                        }
                    }
                    else if (whitelist.contains("*") && sz > 1)
                    {
                        // whitelists containing multiple values and also '*' are misconfigurations,
                        // not supported, we are content to nag
                        if (whitelistDescriptor == null ) whitelistDescriptor = makeWhitelistDescriptor(source);
                        logger.log(
                          MLevel.WARNING,
                          "The " + whitelistDescriptor + " contains an '*' entry, but it is not unique and will be ignored. " +
                          "To disable whitelist enforcement, '*' must be the whitelist's only element. " +
                          "Please either remove the '*' element, or ensure that it is unique in the effective whitelist: " +
                          whitelist
                        );
                    }
                }
                return new WhitelistInfo(whitelist,source);
            }
            else
                return new WhitelistInfo(new HashSet<String>(),WhitelistInfo.Source.MISSING);
        }

        private String effectiveKeyDescription(WhitelistInfo.Source source)
        {
            switch (source) {
            case MAIN_WHITELIST:
                return "'" + whitelistBaseKey + "' and its subkeys";
            case DEPRECATED:
                return "'" + deprecatedKey + "'";
            case OVERRIDE:
                return "'" + overrideWhitelistKey + "'";
            case MISSING:
                return "<no configuration key>";
            default:
                throw new RuntimeException("Unexpected WhitelistInfo.Source: " + source);
            }
        }

        public String makeWhitelistDescriptor(WhitelistInfo.Source source)
        {
            switch (source) {
            case MISSING:
                return source.getIdentifier() + " (none configured; define one at '" + whitelistBaseKey + "' or a subkey)";
            default:
                return source.getIdentifier() + " defined by key " + effectiveKeyDescription(source);
            }
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
