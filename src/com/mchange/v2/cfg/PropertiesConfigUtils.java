package com.mchange.v2.cfg;

import java.util.*;

import com.mchange.v2.log.*;

import com.mchange.v2.lang.ObjectUtils;
import com.mchange.v2.util.IterableUtils;

public class PropertiesConfigUtils
{
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
    static Set<String> modifiableNarrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( PropertiesConfig cachedSyspropsConfig, String propStyleKey, PropertiesConfig pcfg, String alwaysRetainToken, MLogger logger )
    {
        String rawSysProp = cachedSyspropsConfig == null ? System.getProperty( propStyleKey ) : cachedSyspropsConfig.getProperty( propStyleKey );
        String rawPropsConfigProp = pcfg == null ? null : pcfg.getProperty( propStyleKey );

        if (rawSysProp == null && rawPropsConfigProp == null)
            return null;
        else if (rawSysProp != null && rawPropsConfigProp == null)
            return commaSeparatedStringListToModifiableSet( rawSysProp );
        else if (rawSysProp == null && rawPropsConfigProp != null)
            return commaSeparatedStringListToModifiableSet( rawPropsConfigProp );
        else
        {
            // note: sysPropSet, a newly constructed Set, will be mutated and returned
            //       it will no longer faithfully represent the whitelist from sysprops
            //       after the mutation.
            Set<String> sysPropSet = commaSeparatedStringListToModifiableSet( rawSysProp );
            Set<String> propsConfigSet = commaSeparatedStringListToModifiableSet( rawPropsConfigProp );

            if (sysPropSet.equals(propsConfigSet))
                return sysPropSet;
            else
            {
                boolean readd = sysPropSet.contains(alwaysRetainToken) || propsConfigSet.contains(alwaysRetainToken);
                Set<String> tmp = sysPropSet; // just because it becomes awkward to read this as sysPropSet
                tmp.retainAll(propsConfigSet);
                if (readd) tmp.add(alwaysRetainToken);
                Set<String> out = tmp;

                if ( logger.isLoggable( MLevel.WARNING ) )
                    logger.log(
                        MLevel.WARNING,
                        "Inconsistent values of '" + propStyleKey + "' were found in System properties and the provided configuration. " +
                        "We are conservatively using the *intersection* of those values. " +
                        "Value in System properties: '" + rawSysProp + "'; Value in PropertiesConfig: '" + rawPropsConfigProp + "'; " +
                        "Value of intersection: '" + IterableUtils.joinAsString(",",out) + "'"
                    );

                return out;
            }
        }
    }

    private static Set<String> unmodifiableOrNull(Set<String> in)
    { return in == null ? null : Collections.unmodifiableSet( in ); }

    // pcfg can be null
    //
    // returns null iff the key is unavailable from either source
    public static Set<String> narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( String propStyleKey, PropertiesConfig pcfg, String alwaysRetainToken, MLogger logger )
    { return unmodifiableOrNull(modifiableNarrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( null, propStyleKey, pcfg, alwaysRetainToken, logger )); }

    // pcfg can be null
    //
    // returns null iff the key is unavailable from either source
    public static Set<String> narrowestStringSetFromStringListSyspropsPropertiesConfig( String propStyleKey, PropertiesConfig pcfg, MLogger logger )
    { return narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( propStyleKey, pcfg, null, logger ); }

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
    public static Set<String> narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( Set<String> keys, PropertiesConfig pcfg, String alwaysRetainToken, MLogger logger )
    { return unmodifiableOrNull( modifiableNarrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( null, keys, pcfg, alwaysRetainToken, logger ) ); }

    // pcfg can be null
    //
    // returns null iff the key is unavailable from either source
    static Set<String> modifiableNarrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( PropertiesConfig cachedSyspropsConfig, Set<String> keys, PropertiesConfig pcfg, String alwaysRetainToken, MLogger logger )
    {
        int len = keys.size();
        String[] keysArray = keys.toArray(new String[len]);
        Set<String> out = null;
        for (int i = 0; i < len; ++i)
        {
            String key = keysArray[i];
            Set<String> valuesForKey = modifiableNarrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( cachedSyspropsConfig, key, pcfg, alwaysRetainToken, logger );
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
    { return narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken(keys, pcfg, null, logger); }



    public static Set<String> commaSeparatedStringListToModifiableSet( String csList )
    {
        if ("".equals(csList.trim()))
            return new HashSet<String>();
        else
        {
            String[] items = csList.trim().split("\\s*,\\s*");
            return new HashSet<String>(Arrays.asList(items));
        }
    }

    public static Set<String> commaSeparatedStringListToSet( String csList )
    { return Collections.unmodifiableSet(commaSeparatedStringListToModifiableSet(csList)); }

    private PropertiesConfigUtils()
    {}
}
