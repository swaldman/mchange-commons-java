package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

import com.mchange.v2.lang.ObjectUtils;

import java.util.HashSet;

abstract class AbstractBooleanProperty
{
    // MT: immutable post-constructor
    final String property;
    final boolean strongest;
    final Boolean strongestBoolean;
    final boolean defaultValue;

    // MT: protected by this' lock
    HashSet<String> warned = new HashSet<String>();
    boolean warnedUnconfiguredWeakDefault = false;

    public AbstractBooleanProperty(String property, boolean strongest, boolean defaultValue)
    {
        this.property = property;
        this.strongest = strongest;
        this.strongestBoolean = Boolean.valueOf(strongest);
        this.defaultValue = defaultValue;
    }

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

        /** @return null if current system property is discarded in decision-making */
        public Boolean getCurrentSystemPropertyIfConsulted() { return currentSystemProperty; }

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
                ObjectUtils.eqOrBothNull(this.currentConfigProperty, other.currentConfigProperty);
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

    public abstract boolean getValue(PropertiesConfig pcfg, MLogger logger, Details[] outHolder);

    Boolean parseEarlySys(MLogger logger)
    { return parseValue( "early system properties", SealedSystemProperties.get().getProperty(property), logger ); }

    Boolean parseCurrentSys(MLogger logger)
    { return parseValue( "current system properties", System.getProperty(property), logger ); }

    Boolean parseCurrentConfig(PropertiesConfig pcfg, MLogger logger)
    { return pcfg == null ? null : parseValue( "current configuration", pcfg.getProperty(property), logger ); }

    Boolean parseValue(String identifier, String val, MLogger logger)
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

    boolean configureUnconfigured(MLogger logger)
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
}
