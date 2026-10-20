package com.mchange.v2.cfg;

import java.util.*;
import com.mchange.v2.log.*;

import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestStringSetFromStringListSyspropsPropertiesConfig;
import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.modifiableNarrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.modifiableNarrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.commaSeparatedStringListToModifiableSet;

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
 *        predates this scheme. It does not take its turn in a precedence order at all:
 *        whatever the two families above resolved to, the deprecated key
 *        <b>intersects</b> with it. Where it is the only thing configured, that
 *        intersection is itself, so a deployment still carrying only this key resolves
 *        exactly as it always did. Its continued use is nagged about on every
 *        resolution.</li>
 *  </ul>
 *
 *  <p>So precedence proper is override, then the union: the override replaces the union,
 *  and the union is consulted only if no override is set. Present-but-empty counts as
 *  present in both: an operator who set a key to nothing chose deny-all, and falling
 *  through would silently widen it. An empty deprecated key reaches the same end by
 *  intersecting everything away.</p>
 *
 *  <h3>Why the deprecated key narrows rather than decides</h3>
 *
 *  <p>The obvious alternative is for it to decide: while present, be the whole whitelist,
 *  and have the other two families ignored. The argument for that is real -- honoring them
 *  alongside it could widen a whitelist an existing deployment had already narrowed, and
 *  the whole point of keeping this key is that such a deployment keeps what it had.
 *  Intersection meets that argument, since it cannot widen anything, and avoids a hazard
 *  deciding would introduce.</p>
 *
 *  <p>The hazard is that <i>which key governs</i> sits outside the per-key intersection
 *  between the two sources described below. That intersection ensures neither source can
 *  widen a key the other also set; it says nothing about a key appearing where no key
 *  appeared before. So a deprecated key introduced into the supplied config could
 *  displace an <code>overrideWhitelist</code> an operator had pinned in System
 *  properties, and displace it with a <i>wider</i> list -- the one thing the whole scheme
 *  exists to prevent. Narrowing closes that: the deprecated key can subtract from the
 *  effective whitelist, never add to it, wherever it was written and whoever wrote it.</p>
 *
 *  <p>The cost is paid by a deployment that configures both families at once, which gets
 *  the intersection rather than the deprecated key whole -- deny-all, if the two are
 *  disjoint. That is a misconfiguration being resolved toward denial, consistent with how
 *  this class resolves every other conflict, and it is warned about specifically rather
 *  than left to surface as an unexplained empty whitelist. Note that no released version
 *  ever resolved these keys any other way: the supported families and this key's handling
 *  of them arrive together, so no existing configuration changes meaning.</p>
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
 *  <p>Note that there are now two intersections, and they are not the same rule. This one
 *  is <i>across the two sources, within one key</i>, and applies to every key. The other
 *  is <i>across key families</i>: the deprecated key against whatever the supported keys
 *  resolved to. A whitelist can be narrowed by both in one resolution.</p>
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
 *        dissenting, and must not veto a wildcard another key vouched for.</p>
 *
 *        <p>The same protection covers the cross-family intersection. A lone
 *        <code>*</code> surviving the deprecated key's narrowing is honored only where
 *        the deprecated list is what the effective whitelist already was -- otherwise
 *        <code>{A,*}</code> and <code>{B,*}</code> in the two families would again
 *        synthesize an accept-everything neither side wrote.</p></li>
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
 *  Where the deprecated key narrowed a whitelist the supported keys built, both are
 *  reported: the source is {@link WhitelistInfo.Source#DEPRECATED}, since that is the
 *  key whose presence changed the answer, and the returned keys name every key that
 *  actually contributed, so a reader is not sent to the wrong half of a configuration.
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
public class WhitelistManager
{
    final static String WILDCARD = "*";
    final static String DENY_ALL = "[]";

    final static Set<String> ACCEPT_ANY_WHITELIST;
    final static Set<WhitelistInfo.Source> ACCEPTABLE_WHITELIST_SOURCES;
    static
    {
        Set<String> tmp = new HashSet<String>();
        tmp.add(WILDCARD);
        ACCEPT_ANY_WHITELIST = Collections.unmodifiableSet(tmp);

        Set<WhitelistInfo.Source> tmp1 = new HashSet<WhitelistInfo.Source>();
        tmp1.add(WhitelistInfo.Source.MAIN_WHITELIST);
        tmp1.add(WhitelistInfo.Source.OVERRIDE);
        tmp1.add(WhitelistInfo.Source.DEPRECATED);
        ACCEPTABLE_WHITELIST_SOURCES = Collections.unmodifiableSet(tmp1);
    }

    public static boolean actuallyConfiguredWhitelist(WhitelistInfo info)
    { return ACCEPTABLE_WHITELIST_SOURCES.contains(info.getSource()); }

    public static boolean whitelistIsDisabled(WhitelistInfo info)
    { return info.getWhitelist().equals(ACCEPT_ANY_WHITELIST) && actuallyConfiguredWhitelist(info); }

    private static boolean _whitelistAccepts(WhitelistInfo info, String fqcn)
    { return whitelistIsDisabled(info) || info.getWhitelist().contains(fqcn); }


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

    private synchronized boolean warnSupported(PropertiesConfig cachedSyspropsConfig, PropertiesConfig pcfg)
    {
        ConfigSnapshot check = new ConfigSnapshot(cachedSyspropsConfig, pcfg, whitelistBaseKey);
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
    { return collectWhitelistInfoSyspropsPropertiesConfig(null, pcfg, logger); }

    public boolean whitelistAccepts(String fqcn, PropertiesConfig pcfg, MLogger logger)
    {
        WhitelistInfo info = this.collectWhitelistInfoSyspropsPropertiesConfig( pcfg, logger);
        return _whitelistAccepts( info, fqcn );
    }

    /**
     * @return null if there is no reason why not! that is, if the whitelist
     *         is good and the item is whitelisted.
     */
    public String whitelistWhyNot(String fqcn, PropertiesConfig pcfg, MLogger logger, String ofWhatPhrase)
    {
        WhitelistInfo info = this.collectWhitelistInfoSyspropsPropertiesConfig( pcfg, logger);
        if (actuallyConfiguredWhitelist(info))
        {
            if (!whitelistIsDisabled(info) && !info.getWhitelist().contains(fqcn))
            {
                return
                    "The whitelist of " + ofWhatPhrase + " does not contain class '" + fqcn + "'. " +
                    "Please add that class to comma-separated list at config key '" + whitelistBaseKey + "' (and/or subkeys) if " +
                    "you trust this class. " +
                    "(If this denial is unexpected, note that if you have set the same whitelist key or subkey in both System properties and other config to distinct values, only the INTERSECTION becomes whitelisted.) " +
                    "Current whitelist: " + info + " -- " + "Missing class: " + fqcn;
            }
            else
                return null; // if the whitelist is disabled or it contains fqcn, then all good, no reason why not!
        }
        else
        {
            if (info.getSource() == WhitelistInfo.Source.MISSING)
                return
                    "No whitelist is set for " + ofWhatPhrase + ". If you trust the class, please set '" + whitelistBaseKey +
                    "' (and/or subkeys) to a comma-separated list that includes '" + fqcn +
                    "'. " +
                    "No classes are currently whitelisted. -- Missing class: " + fqcn;
            else
            {
                // at present this can't happen, but in case the WhitelistInfo.Source enum grows
                return
                    "The whitelist of " + ofWhatPhrase + " was derived from an unexpected source, and will not be honored. Current whitelist: " + info +
                    "; source " + info.getSource();
            }
        }
    }

    /**
     * @param syspropsConfig null means use the cuurrent system properties
     */
    protected WhitelistInfo collectWhitelistInfoSyspropsPropertiesConfig(PropertiesConfig cachedSyspropsConfig, PropertiesConfig pcfg, MLogger logger)
    {
        Set<String> whitelist = null;

        Set<String> fromKeys = null;

        WhitelistInfo.Source source = null;

        Boolean shouldWarnSupported = null;

        boolean crossListVeto = false;

        Set<String> override = modifiableNarrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( cachedSyspropsConfig, overrideWhitelistKey, pcfg, DENY_ALL, logger);
        if (override != null)
        {
            if (logger.isLoggable(MLevel.WARNING))
            {
                // staying on override whitelists is supported, we warn just once
                if (shouldWarnSupported == null)
                    shouldWarnSupported = Boolean.valueOf( warnSupported(cachedSyspropsConfig,pcfg) );
                if (shouldWarnSupported.booleanValue())
                {
                    logger.log(MLevel.WARNING,
                               "The whitelist that would have been built from '" + whitelistBaseKey +
                               "' and its subkeys has been overridden by '" + overrideWhitelistKey +
                               "'. The overridden whitelist that will be in effect is " + override + ".");
                }
            }
            whitelist = override;
            fromKeys = Collections.singleton(overrideWhitelistKey);
            source = WhitelistInfo.Source.OVERRIDE;

            if (deprecatedKey != null && checkRawKeyForToken(cachedSyspropsConfig, deprecatedKey, DENY_ALL, pcfg))
            {
                whitelist = new HashSet<String>();
                crossListVeto = true;
                warnTokenInKey(deprecatedKey, "override whitelist '" + overrideWhitelistKey + "'", logger);
            }

            Set<String> blockers = whitelistBaseKey == null ? null : findRawSubkeysWithToken(cachedSyspropsConfig, whitelistBaseKey, DENY_ALL, pcfg);
            if (blockers != null)
            {
                whitelist = new HashSet<String>();
                crossListVeto = true;
                warnTokenInSubKeys(blockers, "override whitelist '" + overrideWhitelistKey + "'", logger);
            }
        }
        else
        {
            Set<String> allWhitelistKeys = computeAllSubkeys( cachedSyspropsConfig, whitelistBaseKey, pcfg );
            Set<String> noOverride = modifiableNarrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( cachedSyspropsConfig, allWhitelistKeys, pcfg, DENY_ALL, logger );
            whitelist = noOverride;
            fromKeys = allWhitelistKeys;
            source = WhitelistInfo.Source.MAIN_WHITELIST;

            if (deprecatedKey != null && checkRawKeyForToken(cachedSyspropsConfig, deprecatedKey, DENY_ALL, pcfg))
            {
                whitelist = new HashSet<String>();
                crossListVeto = true;
                warnTokenInKey(deprecatedKey, whitelistBaseKey + " and subkeys", logger);
            }

            if (overrideWhitelistKey != null && checkRawKeyForToken(cachedSyspropsConfig, overrideWhitelistKey, DENY_ALL, pcfg))
            {
                whitelist = new HashSet<String>();
                crossListVeto = true;
                warnTokenInKey(overrideWhitelistKey, whitelistBaseKey + " and subkeys", logger);
            }
        }


        Set<String> deprecatedWhitelist = null;

        // if a deprecated whitelist exists, we use it, but with a big warning to migrate, and intersecting with any otherwise specified whitelist
        if (deprecatedKey != null)
        {
            deprecatedWhitelist = modifiableNarrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( cachedSyspropsConfig, deprecatedKey, pcfg, DENY_ALL, logger );
            if (deprecatedWhitelist != null)
            {
                if (whitelist == null)
                    whitelist = deprecatedWhitelist;
                else
                {
                    Set<String> oldWhitelist = new HashSet<String>(whitelist);
                    whitelist.retainAll(deprecatedWhitelist);

                    if (logger.isLoggable(MLevel.WARNING))
                    {
                        logger.log(MLevel.WARNING,
                                   "A whitelist is specified both with currently supported keys ('" +
                                   whitelistBaseKey + "', its subkeys, or alternatively '" + overrideWhitelistKey +
                                   "') and with deprecated key '" + deprecatedKey +
                                   "'. Erring on the side of caution, when both new and deprecated config are provided, only the *intersection* of the two whitelists is retained in " +
                                   "the effective whitelist."
                                   );
                        if (whitelist.isEmpty() && !oldWhitelist.isEmpty())
                            logger.log(MLevel.WARNING,
                                       "There was no intersection between the whitelist specified by currently supported config " + oldWhitelist +
                                       " and the whitelist specified by deprecated config " + deprecatedWhitelist +
                                       ", so the effective whitelist, defined by the intersection, is now empty and all checks against it will be denied!"
                                       );
                    }
                }

                if (fromKeys == null)
                    fromKeys = Collections.singleton(deprecatedKey);
                else
                {
                    Set<String> oldFromKeys = fromKeys;
                    fromKeys = new HashSet<String>();
                    fromKeys.addAll(oldFromKeys);
                    fromKeys.add(deprecatedKey);
                }
                source = WhitelistInfo.Source.DEPRECATED;

                if (logger.isLoggable(MLevel.WARNING))
                {
                    // staying on a deprecated key is not supported, we're content to nag
                    logger.log(
                      MLevel.WARNING,
                      "Deprecated whitelist key '" + deprecatedKey + "' found. Please remove this from your configuration and migrate to '" + whitelistBaseKey + "' or any subkey for defining whitelist elements. " +
                      "Please note that as long as this key is present in your configuration, it will remain effective, and will narrow any whitelist that might have been specified by '" +
                      whitelistBaseKey + "', its subkeys, or alternatively '" + overrideWhitelistKey + "'. " +
                      "Effective whitelist: " + whitelist
                    );
                }

                if (overrideWhitelistKey != null && checkRawKeyForToken(cachedSyspropsConfig, overrideWhitelistKey, DENY_ALL, pcfg))
                {
                    whitelist = new HashSet<String>();
                    crossListVeto = true;
                    warnTokenInKey(overrideWhitelistKey, "deprecated key '" + deprecatedKey + "'", logger);
                }

                Set<String> blockers = whitelistBaseKey == null ? null : findRawSubkeysWithToken(cachedSyspropsConfig, whitelistBaseKey, DENY_ALL, pcfg);
                if (blockers != null)
                {
                    whitelist = new HashSet<String>();
                    crossListVeto = true;
                    warnTokenInSubKeys(blockers, "deprecated key '" + deprecatedKey + "'", logger);
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
                        shouldWarnSupported = Boolean.valueOf( warnSupported(cachedSyspropsConfig,pcfg) );
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
                if (sz > 1 && logger.isLoggable(MLevel.WARNING))
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
                          "The " + whitelistDescriptor + " contains an '" + WILDCARD + "' entry, but it is not unique and will be removed and ignored. " +
                          "To disable whitelist enforcement, '" + WILDCARD + "' must be the whitelist's only element. " +
                          "Please either remove the '" + WILDCARD + "' element, or ensure that it is unique in the effective whitelist: " +
                          whitelist
                        );
                    }
                    whitelist.remove(WILDCARD); // if a wildcard is not the unique element, we remove it, so it can't do mischief via intersections later.
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
                        whitelist = doubleCheckApparentWildcardWhitelistBaseKey( cachedSyspropsConfig, whitelistBaseKey, pcfg, whitelist, logger );
                        break;
                    case DEPRECATED:
                        whitelist = doubleCheckApparentWildcardDeprecatedKey( cachedSyspropsConfig, deprecatedKey, deprecatedWhitelist, pcfg, whitelist, logger );
                        break;
                    case OVERRIDE:
                        whitelist = doubleCheckApparentWildcardWhitelistSimpleKey( cachedSyspropsConfig, overrideWhitelistKey, pcfg, whitelist, logger );
                        break;
                    case MISSING:
                        throw new RuntimeException("Huh? Missing configuration should never have produced an apparent wildcard whitelist!!!");
                    default:
                        throw new RuntimeException("Unexpected WhitelistInfo.Source: " + source);
                    }

                }
            }
            return new WhitelistInfo(whitelist,fromKeys,source);
        }
        else
            return new WhitelistInfo(new HashSet<String>(),Collections.<String>emptySet(),WhitelistInfo.Source.MISSING);
    }

    private Set<String> doubleCheckApparentWildcardDeprecatedKey( PropertiesConfig cachedSyspropsConfig, String key, Set<String> deprecatedWhitelist, PropertiesConfig pcfg, Set<String> uncheckedWhitelist, MLogger logger )
    {
        // if the deprecated key is present, then the only scenario in which we accept a wildcard is if it's the sole entry in that key's whitelist
        if (uncheckedWhitelist.equals(deprecatedWhitelist))
        {
            // now we still have to make sure the wildcard is not a result of an intersection across sources within deprecated key
            return doubleCheckApparentWildcardWhitelistSimpleKey( cachedSyspropsConfig, key, pcfg, uncheckedWhitelist, logger );
        }
        else
            return new HashSet<String>();
    }
    private Set<String> doubleCheckApparentWildcardWhitelistSimpleKey( PropertiesConfig cachedSyspropsConfig, String key, PropertiesConfig pcfg, Set<String> uncheckedWhitelist, MLogger logger )
    {
        if (pcfg == null) // no conflict possible, only system properties
            return uncheckedWhitelist;
        else
        {
            String syspropsWhitelist = cachedSyspropsConfig == null ? System.getProperty( key ) : cachedSyspropsConfig.getProperty( key );
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
    private Set<String> doubleCheckApparentWildcardWhitelistBaseKey( PropertiesConfig cachedSyspropsConfig, String baseKey, PropertiesConfig pcfg, Set<String> uncheckedWhitelist, MLogger logger )
    {
        if (pcfg == null) // no conflict possible, only system properties
            return uncheckedWhitelist;
        else
        {
            Set<String> subkeys = computeAllSubkeys(cachedSyspropsConfig, baseKey, pcfg);

            Set<String> out = new HashSet<>( uncheckedWhitelist );
            for (String key : subkeys)
            {
                Set<String> check = narrowestStringSetFromStringListSyspropsPropertiesConfig( key, pcfg, logger );
                // if a key turned up null or empty, it contributed nothing to the resulting whitelist but shouldn't be taken to
                // mean a deny-all, empty whitelist when it is not the unique key, but part of a base-key composite
                if (check != null && !check.isEmpty())
                    out.retainAll( doubleCheckApparentWildcardWhitelistSimpleKey( cachedSyspropsConfig, key, pcfg, uncheckedWhitelist, logger ) );
            }
            return out;
        }
    }

    private Set<String> computeAllSubkeys( PropertiesConfig cachedSyspropsConfig, String baseKey, PropertiesConfig pcfg )
    {
        Set<String> allSubkeys = new HashSet<>();
        if (pcfg != null) allSubkeys.addAll( pcfg.getPropertiesByPrefix(baseKey).stringPropertyNames() );
        if (cachedSyspropsConfig != null)
            allSubkeys.addAll( cachedSyspropsConfig.getPropertiesByPrefix(baseKey).stringPropertyNames() );
        else
        {
            for (String k : System.getProperties().stringPropertyNames())
                if (k.equals(baseKey) || k.startsWith(baseKey + "."))
                    allSubkeys.add(k);
        }
        return allSubkeys;
    }

    private boolean checkRawKeyForToken(PropertiesConfig cachedSyspropsConfig, String rawKey, String token, PropertiesConfig pcfg)
    {
        String fromSysprops = cachedSyspropsConfig == null ? System.getProperty(rawKey) : cachedSyspropsConfig.getProperty(rawKey);

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

    protected Set<String> findRawSubkeysWithToken( PropertiesConfig cachedSyspropsConfig, String baseKey, String token, PropertiesConfig pcfg)
    {
        Set<String> out = null;
        Set<String> subkeys = computeAllSubkeys(cachedSyspropsConfig, baseKey, pcfg);
        for( String subkey : subkeys )
            if (checkRawKeyForToken(cachedSyspropsConfig, subkey, token, pcfg))
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
        case DEPRECATED:
            return source.getIdentifier() + " defined by the intersection of deprecated key " + effectiveKeyDescription(source) + " and currently supported config";
        default:
            return source.getIdentifier() + " defined by key " + effectiveKeyDescription(source);
        }
    }
}
