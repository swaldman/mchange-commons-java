package com.mchange.v2.cfg;

import java.util.*;
import com.mchange.v2.log.*;

import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestStringSetFromStringListSyspropsPropertiesConfig;
import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.modifiableNarrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.modifiableNarrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.commaSeparatedStringListToModifiableSet;

/**
 *  A whitelist that may narrow after startup but never widen -- a ratchet over <i>both</i>
 *  System properties and supplied configuration.
 *
 *  <p><b>Not currently used.</b> The implementation in service is
 *  {@link SealedSystemPropertiesWhitelistManager}. Kept for the reasons below, which are mostly
 *  about what this can express that sealing cannot.</p>
 *
 *  <h3>The policy</h3>
 *
 *  <p>The whitelist is fixed at its first lookup and thereafter re-resolved only against the
 *  keys that produced it -- which is what stops a newly added subkey from being a back door,
 *  since the additive subkey scheme would otherwise let one widen the result. The re-resolution
 *  is intersected with what was latched, so entries may leave but never arrive. Both sentinels
 *  keep their meanings: a lone wildcard narrows to a concrete list when configuration supplies
 *  one, a later widening to a wildcard is ignored, and a deny-all token anywhere empties the
 *  whitelist irreversibly.</p>
 *
 *  <p>Getting that right took some care. Set intersection alone mishandles the wildcard in both
 *  directions -- <code>{*}</code> against <code>{A}</code> is empty, as is <code>{A}</code>
 *  against <code>{*}</code> -- so the most permissive configuration would silently become the
 *  most restrictive. And a non-unique wildcard must be stripped at resolution, or narrowing
 *  <code>{*,A}</code> against <code>{*,B}</code> would leave exactly <code>{*}</code>, turning
 *  two deployments that each meant to restrict into accept-everything.</p>
 *
 *  <h3>Why it is not what we settled on</h3>
 *
 *  <p>The same objection as {@link EarliestOrStrongestBooleanProperty}: ratcheting supplied
 *  configuration conflates a parameter with an ambient channel, and breaks the overloads that
 *  accept a config per call. Beyond that, the retained whitelist is expensive in ways sealing is
 *  not. An unconfigured first lookup latches deny-all permanently, so a library that consults a
 *  gate before a deployment has finished configuring can brick it; additions to a latched key,
 *  and whole new subkeys, are ignored in silence; and the result depends on lookup history,
 *  which a shared-JVM test suite can only manage by reaching in and resetting state.</p>
 *
 *  <p>{@link SealedSystemPropertiesWhitelistManager} closes the same channel -- runtime property
 *  mutation cannot widen a whitelist, because runtime System properties are not read -- while
 *  holding no state at all. What it gives up is runtime <i>narrowing</i> through System
 *  properties, which is not a workflow we support and which application configuration can do
 *  instead.</p>
 *
 *  <h3>When it might be right after all</h3>
 *
 *  <p>If a whitelist ever needs to be tightenable at runtime through System properties -- an
 *  operator hardening a live JVM without a restart -- this is the shape that allows it safely.
 *  Nothing in service today needs that.</p>
 */
class EarliestOrNarrowestWhitelistManager extends WhitelistManager
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
