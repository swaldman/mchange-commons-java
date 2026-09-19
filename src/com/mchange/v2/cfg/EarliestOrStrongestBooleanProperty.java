package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

import java.util.HashSet;

public class EarliestOrStrongestBooleanProperty
{
    final String property;
    final boolean strongest;
    final Boolean strongestBoolean;
    final boolean defaultValue;

    Boolean last = null;
    HashSet<String> warned = new HashSet<String>();
    boolean warnedUnconfiguredWeakDefault = false;

    public EarliestOrStrongestBooleanProperty(String property, boolean strongest, boolean defaultValue)
    {
        this.property = property;
        this.strongest = strongest;
        this.strongestBoolean = Boolean.valueOf(strongest);
        this.defaultValue = defaultValue;
    }

    public EarliestOrStrongestBooleanProperty(String property, boolean strongest)
    { this(property, strongest, strongest); }

    public synchronized boolean getValue(PropertiesConfig pcfg, MLogger logger)
    {
        Boolean out;
        if (!strongestBoolean.equals(last)) // we have to check
        {
            Boolean earlySys    = parseValue( "early system properties", SealedSystemProperties.get().getProperty(property), logger );
            Boolean currentSys  = parseValue( "current system properties", System.getProperty(property), logger );
            Boolean currentPcfg = pcfg == null ? null : parseValue( "current configuration", pcfg.getProperty(property), logger );

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
            out = last;

        last = out;
        return out.booleanValue();
    }

    private boolean configureUnconfigured(MLogger logger)
    {
        if (!warnedUnconfiguredWeakDefault && defaultValue != strongest && logger.isLoggable(MLevel.WARNING))
        {
            logger.log(MLevel.WARNING,
                       "Security-sensitive property '" + property +
                       "' has not been set (or has been set to a value not interpretable as a boolean). " +
                       "We currently default to " + defaultValue +
                       ", the less secure value. THIS MAY CHANGE IN A FUTURE RELEASE. " +
                       "Please modify your configuration to tolerate the more secure value of " + strongest +
                        " and explicitly configure '" + property + "' to " + strongest +
                       ", or else explicitly configure '" + property + "' to " + defaultValue +
                       " and take responsibility for the risk.");
            warnedUnconfiguredWeakDefault = true;
        }
       return defaultValue;
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

    private Boolean parseValue(String identifier, String val, MLogger logger)
    {
        if (val == null)
            return null;
        else
        {
            val = val.trim(); // update in place is a bit icky, but convenient!
            if (val.equalsIgnoreCase("true"))
                return Boolean.TRUE;
            else if (val.equalsIgnoreCase("false"))
                return Boolean.FALSE;
            else
            {
                String warnedKey = (identifier.indexOf("ystem") >= 0 ? "System:" : "Configuration:") + val;
                if (logger.isLoggable(MLevel.WARNING) && !warned.contains(warnedKey))
                {
                    logger.log(MLevel.WARNING, "Found uninterpretable value for '" + property + "', '" + val + "' in " + identifier + ".");
                    warned.add(warnedKey);
                }
                return null;
            }
        }
    }
}
