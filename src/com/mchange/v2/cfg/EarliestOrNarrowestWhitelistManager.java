package com.mchange.v2.cfg;

import java.util.*;
import com.mchange.v2.log.*;

import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestStringSetFromStringListSyspropsPropertiesConfig;
import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.modifiableNarrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.modifiableNarrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.commaSeparatedStringListToModifiableSet;

public class EarliestOrNarrowestWhitelistManager extends WhitelistManager
{
    //MT: protected by this' lock
    WhitelistInfo previousWhitelistInfo = null;

    public EarliestOrNarrowestWhitelistManager(String baseKey, String deprecatedKey)
    { super(baseKey, deprecatedKey); }

    @Override
    public synchronized WhitelistInfo collectWhitelistInfoSyspropsPropertiesConfig(PropertiesConfig pcfg, MLogger logger)
    {
        WhitelistInfo previous = previousWhitelistInfo == null ? collectWhitelistInfoSyspropsPropertiesConfig(SealedSystemProperties.get(), pcfg, logger) : previousWhitelistInfo;

        Set<String>          outWhitelist = new HashSet<>( previous.whitelist );
        Set<String>          outFromKeys  = previous.getFromKeys();
        WhitelistInfo.Source outSource    = previous.getSource();
        if (previous.getWhitelist().equals(ACCEPT_ANY_WHITELIST))
        {
            WhitelistInfo replacement = collectWhitelistInfoSyspropsPropertiesConfig(null, pcfg, logger); // we can only narrow from current config
            outWhitelist = new HashSet<>( replacement.whitelist ); // we need this one modifiable
            outFromKeys = replacement.getFromKeys();
            outSource = replacement.getSource();
        }
        else
        {
            Set<String> currentWhitelistFromEarliestKeys = new HashSet<>();
            for ( String key : previous.getFromKeys() )
            {
                Set<String> currentForKey = narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( key, pcfg, DENY_ALL, logger ); // needn't be modifiable
                if (currentForKey != null)
                    currentWhitelistFromEarliestKeys.addAll(currentForKey);
            }
            if (currentWhitelistFromEarliestKeys.equals(ACCEPT_ANY_WHITELIST))
            {
                outWhitelist = new HashSet<String>(previous.getWhitelist()); // we need this one modifiable
                outFromKeys = previous.getFromKeys(); // we do let fromKeys change when the effect of doing so might be to narrow
                outSource = previous.getSource();
            }
            else
            {
                if (currentWhitelistFromEarliestKeys.contains(DENY_ALL))
                    outWhitelist = new HashSet<String>(); // we need this one modifiable
                else
                {
                    outWhitelist = new HashSet<>( previous.whitelist ); // we need this one modifiable
                    outWhitelist.retainAll(currentWhitelistFromEarliestKeys);
                }

                outFromKeys = previous.getFromKeys();
                outSource = previous.getSource();
            }
        }

        WhitelistInfo out;
        if (outWhitelist.isEmpty()) // we don't have to recheck for DENY_ALL, we're denying all anyway
            out = new WhitelistInfo(outWhitelist, outFromKeys, outSource);
        else
        {
            // neither cached sysprops nor our earliest config contained a DENY_ALL token,
            // or we would be empty. But now we have to recheck if one has been introduced.
            Set<String> blockers = findRawSubkeysWithToken( null /* current sysprops */, this.getWhitelistBaseKey(), DENY_ALL, pcfg /* the current config */);
            if (blockers == null) blockers = new HashSet<String>();
            String dkey = this.getDeprecatedKey();
            String okey = this.getOverrideWhitelistKey();
            boolean deprecatedContainsDenyAll = (dkey == null ? false : keyUnderCurrentConfigContainsDenyAll(dkey, pcfg));
            boolean overrideContainsDenyAll   = (okey == null ? false : keyUnderCurrentConfigContainsDenyAll(okey, pcfg));
            if (deprecatedContainsDenyAll) blockers.add(dkey);
            if (overrideContainsDenyAll) blockers.add(okey);
            if (!blockers.isEmpty())
             {
                 outWhitelist.clear(); // we become a DENY_ALL whitelist...
                 if (logger.isLoggable(MLevel.WARNING))
                     logger.log( MLevel.WARNING, "A whitelist which previously accepted some classes has been disabled, made deny-all by the presence of '" + DENY_ALL + "' in the following keys: " + blockers );
             }

            out = new WhitelistInfo(outWhitelist, outFromKeys, outSource);
        }

        // we don't have to worry about narrowing down to a wildcard, because if a wildcard
        // had been part of a larger list when we computed our earlier config, it would have
        // been removed prior to completing that computation

        if (logger.isLoggable(MLevel.WARNING))
        {
            if (previousWhitelistInfo != null && !out.whitelist.equals(previousWhitelistInfo.whitelist))
            {
                logger.log(
                    MLevel.WARNING,
                    "A late narrowing of security configuration has been applied: the whitelist controlled by '" + getWhitelistBaseKey() +
                    "' narrowed from " + previousWhitelistInfo.whitelist + " to " + out.whitelist + ". Only narrowing changes are honored after first lookup; widening changes are silently ignored." 
                );
            }
        }

        previousWhitelistInfo = out;

        return out;
    }

    private boolean keyUnderCurrentConfigContainsDenyAll(String key, PropertiesConfig pcfg)
    {
        String fromCurrentSys = System.getProperty(key);
        if (fromCurrentSys != null && commaSeparatedStringListToModifiableSet( fromCurrentSys ).contains(DENY_ALL))
            return true;
        else if (pcfg != null)
        {
            String fromCurrentCfg = pcfg.getProperty(key);
            if (fromCurrentCfg != null && commaSeparatedStringListToModifiableSet( fromCurrentCfg ).contains(DENY_ALL))
                return true;
            else
                return false;
        }
        else
            return false;
    }
}
