package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

/**
 *  A security-sensitive boolean whose <i>System properties</i> may tighten it at runtime but
 *  never loosen it, while supplied configuration is honored per call in both directions.
 *
 *  <p><b>Not currently used.</b> The implementation in service is
 *  {@link SealedSystemPropertiesBooleanProperty}, which differs only in refusing to consult
 *  System properties after the seal at all. Kept because the difference is a real one and may
 *  yet be wanted; see below.</p>
 *
 *  <h3>The policy</h3>
 *
 *  <p>This is the intermediate position between {@link EarliestOrStrongestBooleanProperty},
 *  which ratchets everything, and {@link SealedSystemPropertiesBooleanProperty}, which ratchets
 *  nothing and simply reads System properties as of the seal. Here the only retained state is
 *  one boolean -- whether System properties have ever asserted the safe value -- and everything
 *  else is a function of the current call. Once they have, nothing may loosen the flag.
 *  Otherwise the supplied configuration decides, then the sealed System property, then the
 *  declared default.</p>
 *
 *  <p>It gets the distinction between the two sources right, which is what the full ratchet got
 *  wrong: a PropertiesConfig is an argument, so its influence ends with the call that carried
 *  it, and withdrawing it reverts to the default rather than leaving the last supplied value in
 *  force.</p>
 *
 *  <h3>Why it is not what we settled on</h3>
 *
 *  <p>Nothing here is unsound; it lost to symmetry. Whitelists took the simpler treatment --
 *  tracking a floor beneath runtime changes to whitelist-related System properties was more
 *  machinery than the threat justified, and {@link SealedSystemPropertiesWhitelistManager} is
 *  three lines with no state -- and two policies for two kinds of security configuration is a
 *  burden every reader pays. The question it invites, <i>why does my runtime -D change affect
 *  this flag but not that whitelist?</i>, has no good answer beyond an accident of
 *  implementation difficulty.</p>
 *
 *  <h3>When it might be right after all</h3>
 *
 *  <p>It hedges against a seal taken too early. If a flag is consulted before a deployment has
 *  finished configuring itself, the sealed implementation will never hear the correct setting,
 *  whereas this one still honors a later assertion of the safe value. That is a narrow case, and
 *  the remedy we prefer is an explicit {@link SealedSystemProperties#seal} at a point the
 *  deployment chooses -- but a setting whose deployments cannot be relied on to seal might be
 *  better served here.</p>
 */
class SystemOnlyStrengthensBooleanProperty extends AbstractBooleanProperty
{
    //MT: protected by this' lock
    boolean systemEverStrong       = false;
    boolean warnedSystemEverStrong = false;

    public SystemOnlyStrengthensBooleanProperty(String property, boolean strongest, boolean defaultValue)
    { super(property, strongest, defaultValue); }

    public SystemOnlyStrengthensBooleanProperty(String property, boolean strongest)
    { this(property, strongest, strongest); }

    @Override
    public synchronized boolean getValue(PropertiesConfig pcfg, MLogger logger, Details[] outHolder)
    {
        boolean out;

        Boolean earlySys    = parseEarlySys(logger);
        Boolean currentSys  = parseCurrentSys(logger);
        Boolean currentPcfg = parseCurrentConfig(pcfg,logger);

        // retained: has any system-properties observation ever been the safe value?
        if ( strongestBoolean.equals(earlySys) || strongestBoolean.equals(currentSys) )
        {
            systemEverStrong = true;
            if (!warnedSystemEverStrong && logger.isLoggable(MLevel.WARNING))
            {
                logger.log( MLevel.WARNING,
                             "We've seen System property '" + property + "' take its most secure value (" + strongest +
                             "). We mistrust changes to System properties that would weaken security settings, so this " +
                             "setting will remain enforced regardless of any future changes to System properties or weaker " +
                             "settings in other configuration." );
                warnedSystemEverStrong = true;
            }
        }

        if ( systemEverStrong )                       out = configured(strongest);         // system pinned it; nothing may loosen
        else if ( currentPcfg != null )               out = configured(currentPcfg);       // this call's config decides
        else if ( currentSys != null )                out = configured(currentSys);
        else                                          out = configureUnconfigured( logger );

        if (outHolder != null)
            outHolder[0] = new Details( earlySys, currentSys, currentPcfg );
        return out;
    }

    private boolean configured(boolean out)
    {
        this.warnedUnconfiguredWeakDefault = false;
        return out;
    }
}
