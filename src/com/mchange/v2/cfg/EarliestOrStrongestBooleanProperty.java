package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

public class EarliestOrStrongestBooleanProperty extends AbstractBooleanProperty
{
    //MT: protected by this' lock
    Boolean last = null;

    public EarliestOrStrongestBooleanProperty(String property, boolean strongest, boolean defaultValue)
    { super(property, strongest, defaultValue); }

    public EarliestOrStrongestBooleanProperty(String property, boolean strongest)
    { this(property, strongest, strongest); }

    @Override
    public synchronized boolean getValue(PropertiesConfig pcfg, MLogger logger, Details[] outHolder)
    {
        Boolean out;

        Boolean earlySys    = null;
        Boolean currentSys  = null;
        Boolean currentPcfg = null;
        if (!strongestBoolean.equals(last)) // we have to check
        {
            earlySys    = parseEarlySys(logger);
            currentSys  = parseCurrentSys(logger);
            currentPcfg = parseCurrentConfig(pcfg,logger);

            if (strongestBoolean.equals(earlySys) || checkWarnUpdateToStrongest("the latest system properties", currentSys, logger) || checkWarnUpdateToStrongest("the latest configuration", currentPcfg, logger))
                out = strongestBoolean;
            else if (last == null) // first pass
            {
                boolean unconfigured = (earlySys == null && currentSys == null && currentPcfg == null);
                if (unconfigured)
                    out = configureUnconfigured(logger);
                else
                {
                    // we are explicitly configured, but not to strongest (because the first if would have caught that!),
                    // so we must be explicitly configured to the weaker value. that's fine, a deployer's explicit choice, 
                    // so we don't warn.
                    out = !strongest;
                }
            }
            else
            {
                // it's not our first rodeo, we HAVE BEEN configured to the weaker and never the stronger value,
                // we are not explicitly configured to the stronger value now.
                //
                // Several possibilities remain. We might have a cached earlySys that used to
                // reflect a sysprop set to weak, and that is gone. earlySys can't be strong, or
                // we'd have caught it in the first if. We don't want to set in stone a cached,
                // earlySysprop setting to weak. So let's just see what happened recently.
                boolean unconfiguredRecently = (currentSys == null && currentPcfg == null);
                if (unconfiguredRecently)
                    out = configureUnconfigured(logger);
                else
                    out = last; // not first pass, we don't update to stronger, we stay as we were, which is last == !strongest
            }
        }
        else
        {
            if (outHolder != null)
            {
                earlySys    = parseEarlySys(logger);
                currentSys  = parseCurrentSys(logger);
                currentPcfg = parseCurrentConfig(pcfg, logger);
            }
            out = last;
        }

        last = out;
        if (outHolder != null)
            outHolder[0] = new Details( earlySys, currentSys, currentPcfg );
        return out.booleanValue();
    }

    private boolean checkWarnUpdateToStrongest(String identifier, Boolean currentValue, MLogger logger)
    {
        boolean out = strongestBoolean.equals(currentValue);
        if (last != null && out)
        {
            String prefix = (defaultValue == strongest ? strongest + "-biased s" : "S");
            String message = prefix + "ecurity-sensitive property '" + property + "' has been updated to its strongest value, " + strongest + " by " + identifier;
            if (logger.isLoggable(MLevel.WARNING))
                logger.log(MLevel.WARNING, message);
            if (logger.isLoggable(MLevel.FINE))
                logger.log(MLevel.FINE, "Stack trace of update of security-sensitive property '" + property + "':", new Exception(message));
        }
        return out;
    }
}
