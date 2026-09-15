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
    public static Set<String> narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( String propStyleKey, PropertiesConfig pcfg, String alwaysRetainToken, MLogger logger )
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
            // note: sysPropSet, a newly constructed Set, will be mutated and returned
            //       it will no longer faithfully represent the whitelist from sysprops
            //       after the mutation.
            Set<String> sysPropSet = commaSeparatedStringListToModifiableSet( rawSysProp );
            Set<String> propsConfigSet = commaSeparatedStringListToModifiableSet( rawPropsConfigProp );

            if (sysPropSet.equals(propsConfigSet))
                return Collections.unmodifiableSet(sysPropSet);
            else
            {
                boolean readd = sysPropSet.contains(alwaysRetainToken) || propsConfigSet.contains(alwaysRetainToken);
                Set<String> tmp = sysPropSet; // just because it becomes awkward to read this as sysPropSet
                tmp.retainAll(propsConfigSet);
                if (readd) tmp.add(alwaysRetainToken);
                Set<String> out = Collections.unmodifiableSet(tmp);

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
    {
        int len = keys.size();
        String[] keysArray = keys.toArray(new String[len]);
        Set<String> out = null;
        for (int i = 0; i < len; ++i)
        {
            String key = keysArray[i];
            Set<String> valuesForKey = narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( key, pcfg, "[]", logger );
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


    /**
     *  Resolves a whitelist of Strings -- in practice fully-qualified class names -- from
     *  System properties and an optional {@link PropertiesConfig}, under one shape shared by
     *  every whitelist in this library.
     *
     *  <h3>The keys</h3>
     *
     *  <p>A manager is built from one base key, and derives the rest:</p>
     *
     *  <ul>
     *    <li><code>&lt;base&gt;.whitelist</code>, <b>and any subkey of it</b>. These
     *        <i>union</i>: independent layers of an application can each contribute without
     *        having to know what the others wrote, which is the point of the scheme. The
     *        naked key counts as one of them. So a library may ship
     *        <code>&lt;base&gt;.whitelist.mylib=com.example.Mine</code> while a deployment
     *        writes <code>&lt;base&gt;.whitelist=com.example.Theirs</code>, and both take
     *        effect.</li>
     *    <li><code>&lt;base&gt;.overrideWhitelist</code>, which <b>replaces</b> that union
     *        rather than adding to it, for a deployment that wants to discard what its
     *        libraries contributed.</li>
     *    <li>an optional deprecated key, named in full at construction, for a whitelist that
     *        predates this scheme. While it is present it is the whole whitelist and the two
     *        families above are ignored -- honoring them alongside it could only widen a
     *        whitelist an existing deployment had already narrowed. Its continued use is
     *        nagged about on every resolution.</li>
     *  </ul>
     *
     *  <p>Precedence is deprecated key, then override, then the union; each is consulted only
     *  if the previous is absent. Present-but-empty counts as present in all three: an
     *  operator who set a key to nothing chose deny-all, and falling through would silently
     *  widen it.</p>
     *
     *  <h3>Two sources, and why they intersect</h3>
     *
     *  <p>Each key is resolved from both System properties and the supplied config. Where
     *  only one names it, that value stands. Where <i>both</i> do and they disagree, the
     *  result is their <b>intersection</b>, and a warning says so: a whitelist is a security
     *  control, so a name only one source vouches for is not vouched for. Note this makes
     *  the two sources peers rather than one overriding the other, and note also that it
     *  constrains only keys both sources set -- a key written in just one place is taken
     *  whole, so this rule narrows disagreement, it does not make either source untrusted.</p>
     *
     *  <h3>The two sentinels</h3>
     *
     *  <p>Two entries mean something other than a class name. They resolve conflicts in
     *  opposite directions, and both resolve toward denial.</p>
     *
     *  <ul>
     *    <li><code>*</code> -- accept anything, <b>only when it is the whitelist's sole
     *        element</b>. A <code>*</code> sharing a whitelist with real entries is reported
     *        as configured but is not a wildcard, or one layer's <code>*</code> would
     *        silently open a gate another layer had narrowed. Callers apply that test
     *        themselves: this class hands back what was configured and warns.
     *
     *        <p>A lone <code>*</code> is additionally re-checked against the raw values that
     *        produced it. <code>{A,*}</code> from one source intersected with
     *        <code>{B,*}</code> from the other yields exactly <code>{*}</code> -- so two
     *        sources that each meant to <i>narrow</i> would otherwise combine into
     *        accept-everything. A wildcard nobody actually asked for becomes deny-all. Keys
     *        that contributed nothing are exempt from that check: a blank key is silent, not
     *        dissenting, and must not veto a wildcard another key vouched for.</p></li>
     *    <li><code>[]</code> -- deny everything. Present in <b>any</b> key of <b>any</b> of
     *        the three families above, in <b>either</b> source, it empties the whitelist,
     *        regardless of the precedence that would otherwise have ignored that key. It is
     *        the veto the additive union would otherwise lack: layers could widen but never
     *        narrow.
     *
     *        <p>It is deliberately exempt from the intersection rule. Intersection is the
     *        conservative resolution for <i>permissions</i>, because dropping one narrows --
     *        but <code>[]</code> is a <i>denial</i>, and dropping a denial widens. Without
     *        the exemption, <code>&lt;key&gt;=[]</code> set by an operator would be
     *        annihilated by any disagreeing value for the same key in the other source,
     *        leaving other keys' entries in force: the veto would degrade to a no-op.</p></li>
     *  </ul>
     *
     *  <p>An empty whitelist reached by accident draws a warning naming both sentinels. One
     *  reached by an explicit <code>[]</code> does not -- the operator already did the thing
     *  that warning would advise.</p>
     *
     *  <h3>What comes back</h3>
     *
     *  <p>{@link #collectWhitelistInfoSyspropsPropertiesConfig} returns a
     *  {@link WhitelistInfo}, never null, carrying a Set that is never null and the
     *  {@link WhitelistInfo.Source} that decided it. The source is worth attending to twice
     *  over. Diagnostics should name the key actually in force rather than the subkeys an
     *  override discarded -- {@link #makeWhitelistDescriptor} renders exactly that phrase.
     *  And {@link WhitelistInfo.Source#MISSING} distinguishes <i>no whitelist is
     *  configured</i> from a configured deny-all, which is the distinction a caller needs if
     *  it means to insist that one be configured rather than silently denying everything.</p>
     *
     *  <p>Every correction described here -- the <code>[]</code> veto, the refusal of a
     *  spurious wildcard -- is computed outside any {@link MLogger#isLoggable} guard. They
     *  are security decisions and must not depend on log level; only the warnings about them
     *  are conditional.</p>
     *
     *  <p>Resolution happens per call, so configuration changes are picked up without
     *  invalidating anything. The one piece of retained state is a snapshot used to warn only
     *  once, rather than on every resolution, about configurations that are supported but
     *  worth mentioning; it is guarded by this instance's monitor.</p>
     */
    public static class WhitelistManager
    {
        private final static String WILDCARD = "*";
        private final static String DENY_ALL = "[]";

        private final static Set<String> ACCEPT_ANY_WHITELIST;
        static
        {
            Set<String> tmp = new HashSet<String>();
            tmp.add(WILDCARD);
            ACCEPT_ANY_WHITELIST = Collections.unmodifiableSet(tmp);
        }

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
            Set<String> whitelist = null;

            WhitelistInfo.Source source = null;

            Boolean shouldWarnSupported = null;

            boolean crossListVeto = false;

            // if a deprecated whitelist exists, we use it, but with a big warning to migrate
            if (deprecatedKey != null)
            {
                Set<String> deprecatedWhitelist = narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( deprecatedKey, pcfg, DENY_ALL, logger );
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

                    if (overrideWhitelistKey != null && checkRawKeyForToken(overrideWhitelistKey, DENY_ALL, pcfg))
                    {
                        whitelist = new HashSet<String>();
                        crossListVeto = true;
                        warnTokenInKey(overrideWhitelistKey, "deprecated key '" + deprecatedKey + "'", logger);
                    }

                    Set<String> blockers = whitelistBaseKey == null ? null : findRawSubkeysWithToken(whitelistBaseKey, DENY_ALL, pcfg);
                    if (blockers != null)
                    {
                        whitelist = new HashSet<String>();
                        crossListVeto = true;
                        warnTokenInSubKeys(blockers, "deprecated key '" + deprecatedKey + "'", logger);
                    }
                }
            }

            if (whitelist == null)
            {
                Set<String> override = narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( overrideWhitelistKey, pcfg, DENY_ALL, logger);
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

                    if (deprecatedKey != null && checkRawKeyForToken(deprecatedKey, DENY_ALL, pcfg))
                    {
                        whitelist = new HashSet<String>();
                        crossListVeto = true;
                        warnTokenInKey(deprecatedKey, "override whitelist '" + overrideWhitelistKey + "'", logger);
                    }

                    Set<String> blockers = whitelistBaseKey == null ? null : findRawSubkeysWithToken(whitelistBaseKey, DENY_ALL, pcfg);
                    if (blockers != null)
                    {
                        whitelist = new HashSet<String>();
                        crossListVeto = true;
                        warnTokenInSubKeys(blockers, "override whitelist '" + overrideWhitelistKey + "'", logger);
                    }
                }
                else
                {
                    Set<String> allWhitelistKeys = computeAllSubkeys( whitelistBaseKey, pcfg );
                    Set<String> noOverride = narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( allWhitelistKeys, pcfg, DENY_ALL, logger );
                    whitelist = noOverride;
                    source = WhitelistInfo.Source.MAIN_WHITELIST;

                    if (deprecatedKey != null && checkRawKeyForToken(deprecatedKey, DENY_ALL, pcfg))
                    {
                        whitelist = new HashSet<String>();
                        crossListVeto = true;
                        warnTokenInKey(deprecatedKey, whitelistBaseKey + " and subkeys", logger);
                    }

                    if (overrideWhitelistKey != null && checkRawKeyForToken(overrideWhitelistKey, DENY_ALL, pcfg))
                    {
                        whitelist = new HashSet<String>();
                        crossListVeto = true;
                        warnTokenInKey(overrideWhitelistKey, whitelistBaseKey + " and subkeys", logger);
                    }
                }
            }

            // various warnings that may need to be emitted about a whitelist, but only if there is one
            if (whitelist != null)
            {
                int sz = whitelist.size();
                String whitelistDescriptor = null;
                if (!crossListVeto && sz == 0) // if it was a cross-list veto, we've already warned and described the true condition
                {
                    if (logger.isLoggable(MLevel.WARNING))
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
                               "If that is not intended, please configure entries in the whitelist, or a unique entry '" + WILDCARD + "' to allow all. " +
                               "If an empty, deny-all whitelist is intended, declare it explicitly with an entry of '" + DENY_ALL + "'."
                            );
                        }
                    }
                }
                else if (whitelist.contains(DENY_ALL))
                {
                    if (sz > 1 &&logger.isLoggable(MLevel.WARNING))
                    {

                        // whitelists containing multiple values and also "[]" are misconfigurations,
                        // the behavior they require is well-defined, but we can nag
                        if (whitelistDescriptor == null ) whitelistDescriptor = makeWhitelistDescriptor(source);
                       logger.log(
                          MLevel.WARNING,
                          "The " + whitelistDescriptor + " contains an explicit '" + DENY_ALL + "' entry, as well as other entries: " + whitelist +
                          " The other entries will be ignored. '" + DENY_ALL + "' trumps everything and means an empty, deny-all whitelist."
                       );
                    }
                    whitelist = new HashSet<String>();
                }
                else if (whitelist.contains(WILDCARD))
                {
                    if (sz > 1)
                    {
                        if (logger.isLoggable(MLevel.WARNING))
                        {
                            // whitelists containing multiple values and also '*' are misconfigurations,
                            // not supported, we are content to nag
                            if (whitelistDescriptor == null ) whitelistDescriptor = makeWhitelistDescriptor(source);
                            logger.log(
                              MLevel.WARNING,
                              "The " + whitelistDescriptor + " contains an '" + WILDCARD + "' entry, but it is not unique and will be ignored. " +
                              "To disable whitelist enforcement, '" + WILDCARD + "' must be the whitelist's only element. " +
                              "Please either remove the '" + WILDCARD + "' element, or ensure that it is unique in the effective whitelist: " +
                              whitelist
                            );
                        }
                    }
                    else
                    {
                        // the whitelist seems to specify a unique wildcard, but that could
                        // also result from keys in different sources (sysprops, other config)
                        // each specifying distinct lists whose intersection in '*'
                        //
                        // in that case, we don't accept the wildcard, and so are left conservatively
                        // with an empty whitelist, deny everything.
                        switch (source) {
                        case MAIN_WHITELIST:
                            whitelist = doubleCheckApparentWildcardWhitelistBaseKey( whitelistBaseKey, pcfg, whitelist, logger );
                            break;
                        case DEPRECATED:
                            whitelist = doubleCheckApparentWildcardWhitelistSimpleKey( deprecatedKey, pcfg, whitelist, logger );
                            break;
                        case OVERRIDE:
                            whitelist = doubleCheckApparentWildcardWhitelistSimpleKey( overrideWhitelistKey, pcfg, whitelist, logger );
                            break;
                        case MISSING:
                            throw new RuntimeException("Huh? Missing configuration should never have produced an apparent wildcard whitelist!!!");
                        default:
                            throw new RuntimeException("Unexpected WhitelistInfo.Source: " + source);
                        }

                    }
                }
                return new WhitelistInfo(whitelist,source);
            }
            else
                return new WhitelistInfo(new HashSet<String>(),WhitelistInfo.Source.MISSING);
        }

        private Set<String> doubleCheckApparentWildcardWhitelistSimpleKey( String key, PropertiesConfig pcfg, Set<String> uncheckedWhitelist, MLogger logger )
        {
            if (pcfg == null) // no conflict possible, only system properties
                return uncheckedWhitelist;
            else
            {
                String syspropsWhitelist = System.getProperty( key );
                String pcfgWhitelist     = pcfg.getProperty( key );
                if (syspropsWhitelist == null) // if syspropsWhitelist is null, the '*' whitelist was uniquely specified in pcfg
                    return uncheckedWhitelist;
                else if (pcfgWhitelist == null) // if pcfgWhitelist is null, the '*' whitelist was uniquely specified in System properties
                    return uncheckedWhitelist;
                else // the whitelist is specified in both places. make sure both are simple '*' whitelists
                {
                    boolean syspropsAcceptAll = ACCEPT_ANY_WHITELIST.equals(commaSeparatedStringListToModifiableSet(syspropsWhitelist));
                    boolean pcfgAcceptAll     = ACCEPT_ANY_WHITELIST.equals(commaSeparatedStringListToModifiableSet(pcfgWhitelist));

                    if (syspropsAcceptAll && pcfgAcceptAll)
                        return uncheckedWhitelist;
                    else
                    {
                        if (logger.isLoggable(MLevel.WARNING))
                        {
                            logger.log(
                                   MLevel.WARNING,
                                   "Although the whitelist for key '" + key + "' resolves to '" + WILDCARD + "' (wildcard, meaning accept all), " +
                                   "that result is the intersection of inconsistent whitelist values [from sysprops: '" + syspropsWhitelist +
                                   "'; from other config: '" + pcfgWhitelist + "']. This specification cannot be safely interpreted as a " +
                                   "wildcard and no other elements survive, so the whitelist becomes DENY ALL. Please resolve the inconsistent " +
                                   "specification, and note that '" + WILDCARD + "' is only supported when it is the sole element of a whitelist."
                            );
                        }
                        return new HashSet<String>();
                    }
                }
            }
        }

        // if ANY key is a dubiously specified intersection, we shout and deny all
        private Set<String> doubleCheckApparentWildcardWhitelistBaseKey( String baseKey, PropertiesConfig pcfg, Set<String> uncheckedWhitelist, MLogger logger )
        {
            if (pcfg == null) // no conflict possible, only system properties
                return uncheckedWhitelist;
            else
            {
                Set<String> subkeys = computeAllSubkeys(baseKey, pcfg);

                Set<String> out = new HashSet<>( uncheckedWhitelist );
                for (String key : subkeys)
                {
                    Set<String> check = narrowestStringSetFromStringListSyspropsPropertiesConfig( key, pcfg, logger );
                    // if a key turned up null or empty, it contributed nothing to the resulting whitelist but shouldn't be taken to
                    // mean a deny-all, empty whitelist when it is not the unique key, but part of a base-key composite
                    if (check != null && !check.isEmpty())
                        out.retainAll( doubleCheckApparentWildcardWhitelistSimpleKey( key, pcfg, uncheckedWhitelist, logger ) );
                }
                return out;
            }
        }

        private Set<String> computeAllSubkeys( String baseKey, PropertiesConfig pcfg )
        {
            Set<String> allSubkeys = new HashSet<>();
            if (pcfg != null) allSubkeys.addAll( pcfg.getPropertiesByPrefix(baseKey).stringPropertyNames() );
            for (String k : System.getProperties().stringPropertyNames())
                if (k.equals(baseKey) || k.startsWith(baseKey + "."))
                    allSubkeys.add(k);
            return allSubkeys;
        }

        private boolean checkRawKeyForToken(String rawKey, String token, PropertiesConfig pcfg)
        {
            String fromSysprops = System.getProperty(rawKey);

            if (fromSysprops != null && commaSeparatedStringListToModifiableSet(fromSysprops).contains(token))
                return true;
            else if (pcfg != null)
            {
                String fromConfig  = pcfg.getProperty(rawKey);
                return fromConfig != null && commaSeparatedStringListToModifiableSet(fromConfig).contains(token);
            }
            else
                return false;
        }

        private Set<String> findRawSubkeysWithToken( String baseKey, String token, PropertiesConfig pcfg)
        {
            Set<String> out = null;
            Set<String> subkeys = computeAllSubkeys(baseKey, pcfg);
            for( String subkey : subkeys )
                if (checkRawKeyForToken(subkey, token, pcfg))
                {
                    if (out == null) out = new HashSet<String>();
                    out.add(subkey);
                }
            return out;
        }

        private void warnTokenInKey( String inKey, String configuringFrom, MLogger logger )
        {
            if (logger.isLoggable(MLevel.WARNING))
            {
                logger.log(
                    MLevel.WARNING,
                    "Deny-all token '" + DENY_ALL + "' found in '" + inKey + "'. " +
                    "The whitelist will be empty. " +
                    "Even though we are configuring from " + configuringFrom + ", " + 
                    "a deny-all token anywhere in supported config trumps all other config. " +
                    "Remove the deny-all token from key '" + inKey + "' to restore the rest of the whitelist."
                );
            }
        }

        private void warnTokenInSubKeys( Set<String> blockers, String configuringFrom, MLogger logger )
        {
            if (logger.isLoggable(MLevel.WARNING))
            {
                logger.log(
                   MLevel.WARNING,
                   "Deny-all token '" + DENY_ALL + "' found in whitelist keys " + blockers + ". " +
                   "The whitelist will be empty. " +
                   "Even though we are configuring from " + configuringFrom + ", " +
                   "a deny-all token anywhere in supported config trumps all other config. " +
                   "Remove the deny-all token from " + blockers + " to restore the rest of the whitelist."
                );
            }
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
