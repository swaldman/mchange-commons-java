package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

/**
 *  A security-sensitive String setting -- in practice a class name -- read from System
 *  properties as of the seal, from supplied configuration live, or from a default.
 *
 *  <p>The String member of the family that includes
 *  {@link SealedSystemPropertiesBooleanProperty} and
 *  {@link SealedSystemPropertiesWhitelistManager}, and it shares their reason for treating the
 *  two sources differently. System properties are an ambient channel any code in the process
 *  can write, unguarded since SecurityManager's removal, so they are read from
 *  {@link SealedSystemProperties} and later mutation is invisible. A {@link PropertiesConfig}
 *  is a parameter the application chose to pass, and implementations are read-only views of
 *  configuration the application manages, so it is honored per call. Either may be null.</p>
 *
 *  <h3>Precedence, and how it differs from the others</h3>
 *
 *  <p>A sealed System property wins; failing that, the supplied configuration; failing that,
 *  the default. That is a plain precedence rule, and it is <i>not</i> what the boolean and
 *  whitelist members do. Those resolve disagreement <i>conservatively</i> -- the safer of two
 *  answers stands, whichever source offered it -- because a boolean has a safe polarity and a
 *  whitelist has a narrower reading. A String has neither, so there is nothing to be
 *  conservative toward, and the question becomes simply whose word counts. The operator's
 *  does.</p>
 *
 *  <p>Do not read this as a weaker guarantee. Nothing an attacker writes to System properties
 *  after the seal is visible here at all, which is the property that matters; what is given up
 *  is only the ability to resolve a genuine disagreement toward safety, which for an opaque
 *  String is not a meaningful notion.</p>
 *
 *  <h3>Whitespace</h3>
 *
 *  <p>The intended values are class names handed to <code>Class.forName</code>, and
 *  <code>Properties.load</code> preserves trailing whitespace, so
 *  <code>someKey=com.example.Foo&nbsp;</code> in a properties file yields a name that cannot
 *  resolve -- failing as a ClassNotFoundException for a class that looks correct in the file.
 *  Each source is therefore passed through a {@link Whitespace} policy, chosen at
 *  construction:</p>
 *
 *  <ul>
 *    <li>{@link Whitespace#TRIM_BLANKS_ARE_NULL} (the default) trims, and treats a value that
 *        is blank after trimming as <i>absent</i>, so it falls through to the next source. This
 *        is what a class name wants: a stray <code>someKey=</code> line yields the default
 *        rather than <code>Class.forName("")</code>.</li>
 *    <li>{@link Whitespace#TRIM_BLANKS_ARE_EMPTY} trims but returns the empty String, for a
 *        setting where "explicitly nothing" differs from "unset".</li>
 *    <li>{@link Whitespace#NO_TRIM} returns what was configured, for a setting whose values may
 *        carry significant whitespace.</li>
 *  </ul>
 *
 *  <p>Because trimming happens at each source rather than once at the end, a blank System
 *  property yields to a real configured value rather than masking it.</p>
 *
 *  <h3>The default is used as-is</h3>
 *
 *  <p>A default is a literal supplied by the calling code, not configuration, so it is returned
 *  unprocessed -- the whitespace policy does not apply to it. A default that is blank or
 *  carries surrounding whitespace is therefore almost certainly an authoring mistake, and is
 *  reported once, at the first lookup, whether or not the default is ever actually used: it is
 *  a defect in code rather than a condition of the deployment, and the person who can fix it
 *  should hear about it either way. An empty default under
 *  {@link Whitespace#TRIM_BLANKS_ARE_EMPTY} is exempt, that policy having declared the empty
 *  String meaningful.</p>
 *
 *  <p>Falling back to the default at all is reported too, once, and again if configuration
 *  appears and is later withdrawn -- a deployment running on a default should know it.</p>
 */
public class SealedSystemPropertiesStringProperty
{
    public enum Whitespace { NO_TRIM, TRIM_BLANKS_ARE_EMPTY, TRIM_BLANKS_ARE_NULL }

    //MT: immutable post-constructor
    final String property;
    final String defaultValue;
    final Whitespace whitespace;

    //MT: protected by this' lock
    boolean mustWarnRiskyDefaultValue;
    boolean warnedSealed = false;
    boolean warnedDefault = false;

    private String trim(String raw)
    {
        String out;
        if (raw == null)
            out = null;
        else
        {
            switch (whitespace)
            {
            case NO_TRIM:
                out = raw;
                break;
            case TRIM_BLANKS_ARE_EMPTY:
                out = raw.trim();
                break;
            case TRIM_BLANKS_ARE_NULL:
                out = raw.trim();
                if (out.isEmpty())
                    out = null;
                break;
            default:
                throw new RuntimeException("Unexpected Whitespace value: " + whitespace);
            }
        }
        return out;
    }

    public SealedSystemPropertiesStringProperty(String property, String defaultValue, Whitespace whitespace)
    {
        this.property = property;
        this.defaultValue = defaultValue;
        this.whitespace = whitespace;

        this.mustWarnRiskyDefaultValue = (whitespace != Whitespace.NO_TRIM && defaultValue != null && ("".equals(defaultValue) || !defaultValue.equals(defaultValue.trim())));
    }

    public SealedSystemPropertiesStringProperty(String property, String defaultValue)
    { this( property, defaultValue, Whitespace.TRIM_BLANKS_ARE_NULL ); }

    public SealedSystemPropertiesStringProperty(String property)
    { this( property, null ); }

    public synchronized String getValue(PropertiesConfig pcfg, MLogger logger)
    {
        if (mustWarnRiskyDefaultValue && logger.isLoggable(MLevel.WARNING))
        {
            if (whitespace == Whitespace.TRIM_BLANKS_ARE_EMPTY && "".equals(defaultValue))
            {
                // skip warning in this one special case, empty string is a foreseen value
            }
            else
                logger.log(MLevel.WARNING, "Note that default value '" + defaultValue + "' has leading or trailing spaces or is empty. This value will be used AS-IS as the default, it will not be processed according to whitespace policy " + whitespace + ".");
            mustWarnRiskyDefaultValue = false;
        }

        String out = trim(SealedSystemProperties.get().getProperty(property));
        if (out == null)
        {
            out = trim(pcfg == null ? null : pcfg.getProperty(property));
            if (out == null)
            {
                out = defaultValue;
                if (defaultValue != null && !warnedDefault && logger.isLoggable(MLevel.WARNING))
                {
                    logger.log(MLevel.WARNING,
                               "No value available in sealed System properties or in current config for property '" +
                               property +
                               "'. Using default value '" + defaultValue + "'.");
                    warnedDefault = true;
                }
            }
            else
            {
                warnedDefault = false;
            }
        }
        else
        {
            if (!warnedSealed && logger.isLoggable(MLevel.INFO))
            {
                logger.log(MLevel.INFO, "Using sealed System property for '" + property + "'. Other config for this property will be ignored.");
                warnedSealed = true;
            }
        }
        return out;
    }
}
