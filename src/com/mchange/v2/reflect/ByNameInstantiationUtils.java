package com.mchange.v2.reflect;

import java.util.*;
import java.lang.reflect.InvocationTargetException;

import com.mchange.v2.log.*;

import com.mchange.v2.cfg.PropertiesConfig;

import static com.mchange.v2.cfg.PropertiesConfigUtils.ConfigSnapshot;
import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestStringSetFromStringListSyspropsPropertiesConfig;
import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig;

public final class ByNameInstantiationUtils
{
    final static MLogger logger = MLog.getLogger( ByNameInstantiationUtils.class );


    private final static String BY_NAME_INSTANTIATION_COMMON_KEY_PFX         = "com.mchange.v2.reflect.by-name-instantiation";
    private final static String BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX      = "com.mchange.v2.reflect.by-name-instantiation.whitelist";
    private final static String BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY  = "com.mchange.v2.reflect.by-name-instantiation.enforce-whitelist";
    private final static String BY_NAME_INSTANTIATION_OVERRIDE_WHITELIST_KEY = "com.mchange.v2.reflect.by-name-instantiation.override-whitelist";

    private final static boolean DEFAULT_ENFORCE_WHITELIST = false;

    //MT: protected by class' monitor
    private static ConfigSnapshot warnedOverride   = null;
    private static ConfigSnapshot warnedStraySplat = null;

    private synchronized static boolean warnOnOverride(PropertiesConfig pcfg)
    {
        ConfigSnapshot check = new ConfigSnapshot(pcfg, BY_NAME_INSTANTIATION_COMMON_KEY_PFX);
        if (check.equals(warnedOverride))
            return false;
        else
        {
            warnedOverride = check;
            return true;
        }
    }

    private synchronized static boolean warnOnStraySplat(PropertiesConfig pcfg)
    {
        ConfigSnapshot check = new ConfigSnapshot(pcfg, BY_NAME_INSTANTIATION_COMMON_KEY_PFX);
        if (check.equals(warnedStraySplat))
            return false;
        else
        {
            warnedStraySplat = check;
            return true;
        }
    }

    public static Object instantiateByNameGated(String fqcn, PropertiesConfig pcfg)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationNotPermittedException
    {
        checkWarnThrowForInstantiateByNameGated(fqcn, pcfg);
        return doInstantiate(fqcn);
    }

    /**
     * In some contexts, instantiation by name doesn't happen in a single step.
     * This does all the whitelist-checking and warning we want to support, and throws if we would not
     * permit the operation. You can safely instantiateByNameUngated(...) if this function succeeds.
     */
    public static void checkWarnThrowForInstantiateByNameGated(String fqcn, PropertiesConfig pcfg) throws InstantiationNotPermittedException
    {
        Set<String> whitelist = collectWhitelistSyspropsPropertiesConfig(pcfg);
        boolean nameOkay;
        if (whitelist.contains(fqcn))
            nameOkay = true;
        else if (whitelist.size() == 1 && whitelist.contains("*"))
            nameOkay = true;
        else
            nameOkay = false;

        if (!nameOkay)
        {
            String pcfgEnforceWhitelistStr = pcfg == null ? null : pcfg.getProperty(BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY);
            String syspropsEnforceWhitelistStr = System.getProperty(BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY);

            Boolean pcfgEnforceWhitelist = parseEnforceWhitelist(pcfgEnforceWhitelistStr);
            Boolean syspropsEnforceWhitelist = parseEnforceWhitelist(syspropsEnforceWhitelistStr);

            boolean enforce;
            boolean explicit;
            if (pcfgEnforceWhitelist == null && syspropsEnforceWhitelist == null)
            {
                if (logger.isLoggable(MLevel.WARNING))
                    logger.log(MLevel.WARNING, "No interpretable value set for '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "' set, currently defaulting to '" + DEFAULT_ENFORCE_WHITELIST +"'. THIS MAY CHANGE IN FUTURE RELEASES.");
                enforce = DEFAULT_ENFORCE_WHITELIST;
                explicit = false;
            }
            else
            {
                if (pcfgEnforceWhitelist == null)
                {
                    enforce = syspropsEnforceWhitelist.booleanValue();
                    explicit = true;
                }
                else if (syspropsEnforceWhitelist == null)
                {
                    enforce = pcfgEnforceWhitelist.booleanValue();
                    explicit = true;
                }
                else if (pcfgEnforceWhitelist.booleanValue() || syspropsEnforceWhitelist.booleanValue())
                {
                    enforce = true;
                    explicit = true;

                    if (logger.isLoggable(MLevel.WARNING))
                    {
                        if (pcfgEnforceWhitelist.booleanValue() != syspropsEnforceWhitelist.booleanValue())
                            logger.log(
                               MLevel.WARNING,
                               "Differing values of '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "' were found between system properties and other configuration. " +
                               "This security-sensitive key is true-biased. Since the value was 'true' in one source of configuration, the disagreement has been resolved to 'true' " +
                               "and the whitelist will be enforced. To eliminate these annoying log messages, please resolve the disagreement between System properties and other config."
                            );
                    }
                }
                else
                {
                    enforce = false;
                    explicit = true;
                }
            }

            if (enforce) // we already know fqcn is not in the whitelist
                throw new InstantiationNotPermittedException("By-name instantiation of '" + fqcn + "' not permitted. The class is not in the enforced whitelist defined by '" + BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX + "' and its subkeys. Whitelist: " + whitelist);
            else
            {
                if (!explicit && logger.isLoggable(MLevel.WARNING))
                    logger.log(
                       MLevel.WARNING,
                       "Instantiating '" + fqcn + "' by name despite its absence from '" + BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX + "' or a subkey, " +
                       "and despite no explicit suppression of whitelist enforcement via '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "=false'. " +
                       "This may be blocked in future releases. If you mean for '" + fqcn + "' to be instantiated by name, please add it to the whitelist, " +
                       "or else explicitly suppress enforcement of the whitelist. Current whitelist: " + whitelist
                    );
            }
        }
    }

    /**
     *  Instantiates fqcn without consulting the whitelist at all. This is the bypass, and
     *  the whole point of the whitelist is that most by-name instantiation should not use it.
     *
     *  <p>It is appropriate only where the name cannot have been influenced by anything
     *  outside the application's control. In practice that means one of two things:</p>
     *
     *  <ul>
     *    <li>the name is fixed in the code -- a literal or a compile-time constant -- so
     *        there is nothing for a whitelist to decide; or</li>
     *    <li>the name has already been passed through
     *        {@link #checkWarnThrowForInstantiateByNameGated}, which performs the whole
     *        check and throws if the operation would not be permitted. Instantiating after
     *        that call returns normally is exactly as gated as
     *        {@link #instantiateByNameGated} would have been.</li>
     *    <li>the name was read from deployment configuration at the point of use -- a
     *        PropertiesConfig or System property lookup -- so the party who chose it is the
     *        same party who would have had to whitelist it. Gating there would only ask the
     *        configurer to authorize themselves.
     *
     *        <p>Note this turns on where the value in hand came from, not on whether a
     *        property of that name exists. A class name that is settable in configuration
     *        may also reach you as a property of a deserialized or dereferenced object, and
     *        at the point of instantiation the two are indistinguishable. If the value was
     *        carried on an object rather than read directly from configuration, it must be
     *        gated.</p></li>
     *  </ul>
     *
     *  <p>A name that reached you from a deserialized object, a dereferenced JNDI
     *  Reference, or any other channel an attacker might influence belongs in
     *  {@link #instantiateByNameGated} instead. Reaching for this method to quiet an
     *  {@link InstantiationNotPermittedException} converts a refusal into the vulnerability
     *  the refusal existed to prevent; the fix for that exception is to add the class to the
     *  whitelist.</p>
     */
    public static Object instantiateByNameUngated(String fqcn)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    { return doInstantiate(fqcn); }

    /**
     *  As {@link #instantiateByNameUngated(String)}, and subject to the same caution about
     *  when bypassing the whitelist is legitimate, but instantiating a Class already in
     *  hand rather than resolving fqcn afresh.
     *
     *  <p>Which Class object is used matters. A caller that loaded the class through some
     *  other ClassLoader -- a thread context ClassLoader, say, consulted because
     *  Class.forName had already failed -- holds a Class that a fresh Class.forName here
     *  could not find. This instantiates the Class it is given.</p>
     *
     *  <p>The two arguments must agree: preloaded.getName() must equal fqcn, and an
     *  IllegalArgumentException is thrown if it does not. Passing the name separately is
     *  not redundant -- it is what lets the caller gate on the same name it is about to
     *  instantiate, via {@link #checkWarnThrowForInstantiateByNameGated}, before calling
     *  this.</p>
     */
    public static Object instantiateByNameUngated(String fqcn, Class<?> preloaded)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalArgumentException
    { return doInstantiate(fqcn, preloaded); }

    public static Set<String> currentWhitelist(PropertiesConfig pcfg)
    { return Collections.unmodifiableSet( collectWhitelistSyspropsPropertiesConfig(pcfg) ); }

    private static Object doInstantiate(String fqcn)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    { return Class.forName(fqcn).getDeclaredConstructor().newInstance(); }

    /**
     * For where a class is already loaded, clz.getName() must equal fqcn
     */
    private static Object doInstantiate(String fqcn, Class<?> clz)
        throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if (!clz.getName().equals(fqcn))
            throw new IllegalArgumentException("Class " + clz + " must share a fully-qualified name with given fqcn: " + fqcn);
        return clz.getDeclaredConstructor().newInstance();
    }

    private static Boolean parseEnforceWhitelist(String val)
    {
        if (val == null)
            return null;
        else
        {
            if (val.equalsIgnoreCase("true"))
                return Boolean.TRUE;
            else if (val.equalsIgnoreCase("false"))
                return Boolean.FALSE;
            else
            {
                if (logger.isLoggable(MLevel.WARNING))
                    logger.log(MLevel.WARNING, "Found uninterpretable value for '" + BY_NAME_INSTANTIATION_ENFORCE_WHITELIST_KEY + "', '" + val + "'.");
                return null;
            }
        }
    }

    private static Set<String> collectWhitelistSyspropsPropertiesConfig(PropertiesConfig pcfg)
    {
        Set<String> override = narrowestStringSetFromStringListSyspropsPropertiesConfig( BY_NAME_INSTANTIATION_OVERRIDE_WHITELIST_KEY, pcfg, logger);

        Set<String> allWhitelistKeys = new HashSet<>();
        if (pcfg != null) allWhitelistKeys.addAll( pcfg.getPropertiesByPrefix(BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX).stringPropertyNames() );
        for (String k : System.getProperties().stringPropertyNames())
            if (k.startsWith(BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX))
                allWhitelistKeys.add(k);

        Set<String> noOverride = narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig( allWhitelistKeys, pcfg, logger );

        Set<String> out;
        String whitelistModifier;
        if (override != null)
        {
            if (logger.isLoggable(MLevel.WARNING) && warnOnOverride(pcfg))
                logger.log(MLevel.WARNING,
                           "The whitelist that would have been built from '" + BY_NAME_INSTANTIATION_WHITELIST_KEY_PFX +
                           "' and its subkeys has been overridden by '" + BY_NAME_INSTANTIATION_OVERRIDE_WHITELIST_KEY +
                           "'. The overridden whitelist that will be in effect is " + override +
                           ". The whitelist that is overridden and will now be ignored would have been " + noOverride);
            out = override;
            whitelistModifier = "override ";
        }
        else
        {
            out = noOverride;
            whitelistModifier = "";
        }

        if (logger.isLoggable(MLevel.WARNING) && out.contains("*") && out.size() > 1 && warnOnStraySplat(pcfg))
            logger.log(MLevel.WARNING, "The " + whitelistModifier + "whitelist contains an '*' entry, but it is not unique and will be ignored. To disable whitelist enforcement, '*' must be the whitelist's only element.");
        return out;
    }

    private ByNameInstantiationUtils()
    {}
}
