package com.mchange.v2.cfg;

import java.util.*;

import com.mchange.v2.lang.ObjectUtils;

public class ConfigSnapshot
{
    Properties systemProperties;
    Properties pConfigProperties;

    /**
     *  relevantPrefix can be "" if you want to warn on ANY change to pcfg. Properties from pcfg by prefix should be fast
     *
     *  we always warn on any change to sysprops because clone() is gonna be faster than iterating to check a subset.
     */
    ConfigSnapshot(PropertiesConfig cachedSyspropsConfig, PropertiesConfig pcfg, String relevantPrefix)
    {
        this.pConfigProperties = pcfg == null ? null : (Properties) pcfg.getPropertiesByPrefix(relevantPrefix).clone();
        this.systemProperties = (Properties) (cachedSyspropsConfig == null ? System.getProperties().clone() : cachedSyspropsConfig.getPropertiesByPrefix(relevantPrefix).clone());
    }

    public ConfigSnapshot(PropertiesConfig pcfg, String relevantPrefix)
    { this(null, pcfg, relevantPrefix); }

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
