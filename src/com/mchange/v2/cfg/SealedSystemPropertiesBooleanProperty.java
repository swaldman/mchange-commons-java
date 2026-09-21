package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

public class SealedSystemPropertiesBooleanProperty extends AbstractBooleanProperty
{
    //MT: protected by this' lock
    boolean uninitialized = true;
    Boolean sealedSys;

    public SealedSystemPropertiesBooleanProperty(String property, boolean strongest, boolean defaultValue)
    { super(property, strongest, defaultValue); }

    public SealedSystemPropertiesBooleanProperty(String property, boolean strongest)
    { this(property, strongest, strongest); }

    @Override
    public synchronized boolean getValue(PropertiesConfig pcfg, MLogger logger, Details[] outHolder)
    {
        boolean out;

        if (uninitialized)
        {
            sealedSys = parseEarlySys(logger);
            uninitialized = false;
        }
        Boolean currentPcfg = parseCurrentConfig(pcfg,logger);
        if (strongestBoolean.equals(sealedSys) || strongestBoolean.equals(currentPcfg))
            out = configured(strongest);
        else if (sealedSys != null || currentPcfg != null) // at least one is set, and any set are to weakest
            out = configured(!strongest);
        else
            out = configureUnconfigured(logger);

        if (outHolder != null)
            outHolder[0] = new Details( sealedSys, null, currentPcfg );
        return out;
    }

    private boolean configured(boolean out)
    {
        this.warnedUnconfiguredWeakDefault = false;
        return out;
    }
}
