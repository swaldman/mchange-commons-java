package com.mchange.v2.cfg;

import java.util.*;

import com.mchange.v2.log.*;

import com.mchange.v2.lang.ObjectUtils;
import com.mchange.v2.util.IterableUtils;

public class PropertiesConfigUtils
{
    public static class WarnedOn
    {
        Properties pConfigProperties;
        Properties systemProperties;

        /**
         *  relevantPrefix can be "" if you want to warn on ANY change to pcfg. Properties from pcfg by prefix should be fast
         *
         *  we always warn on any change to sysprops because clone() is gonna be faster than iterating to check a subset.
         */
        public WarnedOn(PropertiesConfig pcfg, String relevantPrefix)
        {
            this.pConfigProperties = pcfg == null ? null : (Properties) pcfg.getPropertiesByPrefix(relevantPrefix).clone();
            this.systemProperties = (Properties) System.getProperties().clone();
        }

        public WarnedOn(PropertiesConfig pcfg)
        { this( pcfg, "" ); }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof WarnedOn)
            {
                WarnedOn other = (WarnedOn) o;
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
