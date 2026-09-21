package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

public class SystemOnlyStrengthensBooleanProperty extends AbstractBooleanProperty
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
