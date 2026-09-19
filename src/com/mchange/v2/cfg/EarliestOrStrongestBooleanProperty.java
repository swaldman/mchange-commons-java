package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

import com.mchange.v2.lang.ObjectUtils;

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

    public static class Details
    {
        Boolean earlySystemProperty;
        Boolean currentSystemProperty;
        Boolean currentConfigProperty;

        Details(Boolean earlySystemProperty, Boolean currentSystemProperty, Boolean currentConfigProperty)
        {
            this.earlySystemProperty = earlySystemProperty;
            this.currentSystemProperty = currentSystemProperty;
            this.currentConfigProperty = currentConfigProperty;
        }

        public Boolean getEarlySystemProperty()   { return earlySystemProperty; }
        public Boolean getCurrentSystemProperty() { return currentSystemProperty; }
        public Boolean getCurrentConfigProperty() { return currentConfigProperty; }

        public boolean isUnconfigured() { return earlySystemProperty == null && currentSystemProperty == null && currentConfigProperty == null; }

        public boolean isCurrentExplicitUnconflicted()
        {
            if (currentSystemProperty == null && currentConfigProperty == null)
                return false;
            else if (currentSystemProperty != null && currentConfigProperty != null)
            {
                boolean noCurrentConflict = currentSystemProperty.equals(currentConfigProperty);
                return noCurrentConflict && (earlySystemProperty == null || earlySystemProperty.equals(currentSystemProperty));
            }
            else if (currentSystemProperty != null)
                return earlySystemProperty == null || earlySystemProperty.equals(currentSystemProperty);
            else if (currentConfigProperty != null)
                return earlySystemProperty == null || earlySystemProperty.equals(currentConfigProperty);
            else
                throw new RuntimeException("Huh? The cases we've checked, from which we've unconditionally returned should be exhaustive.");
        }

        @Override
        public boolean equals(Object o)
        { return this == o || (o instanceof Details && _equals((Details) o)); }

        private boolean _equals(Details other)
        {
            return
                ObjectUtils.eqOrBothNull(this.earlySystemProperty,   other.earlySystemProperty)   &&
                ObjectUtils.eqOrBothNull(this.currentSystemProperty, other.currentSystemProperty) &&
                ObjectUtils.eqOrBothNull(this.currentConfigProperty, other.earlySystemProperty);
        }

        @Override
        public int hashCode()
        { return ObjectUtils.hashOrZero(earlySystemProperty) << 2 ^ ObjectUtils.hashOrZero(currentSystemProperty) << 1 ^ ObjectUtils.hashOrZero(currentConfigProperty); }

        @Override
        public String toString()
        { return "[earlySystemProperty: " + earlySystemProperty + "; currentSystemProperty: " + currentSystemProperty + "; currentConfigProperty: " + currentConfigProperty + "]"; }
    }

    public String  getProperty()     { return property; }
    public boolean getStrongest()    { return strongest; }
    public boolean getDefaultValue() { return defaultValue; }

    public boolean getValue(PropertiesConfig pcfg, MLogger logger) { return getValue( pcfg, logger, null ); }

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

    private Boolean parseEarlySys(MLogger logger)
    { return parseValue( "early system properties", SealedSystemProperties.get().getProperty(property), logger ); }

    private Boolean parseCurrentSys(MLogger logger)
    { return parseValue( "current system properties", System.getProperty(property), logger ); }

    private Boolean parseCurrentConfig(PropertiesConfig pcfg, MLogger logger)
    { return pcfg == null ? null : parseValue( "current configuration", pcfg.getProperty(property), logger ); }

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
