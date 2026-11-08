package com.mchange.v2.reflect;

import java.util.*;
import java.lang.reflect.InvocationTargetException;

import com.mchange.v2.log.*;
import com.mchange.v2.cfg.*;

import static com.mchange.v2.lang.ObjectUtils.eqOrBothNull;
import static com.mchange.v2.lang.ObjectUtils.hashOrZero;

public final class ByNameInstantiationUtils
{
    final static MLogger logger = MLog.getLogger( ByNameInstantiationUtils.class );

    private final static String COMMON_KEY_PFX = "com.mchange.v2.reflect.byNameInstantiation";

    private final static SealedSystemPropertiesWhitelistManager whitelistManager = new SealedSystemPropertiesWhitelistManager( COMMON_KEY_PFX, null );

    public final static String ENFORCE_WHITELIST_KEY = whitelistManager.getTopLevelBaseKey() + ".enforceWhitelist";

    private final static SealedSystemPropertiesBooleanProperty _enforceWhiteList = new SealedSystemPropertiesBooleanProperty( ENFORCE_WHITELIST_KEY, true, false );

    /**
     *  A record that a warning about <code>fqcn</code>'s absence from the whitelist has already been
     *  emitted, under the configuration <code>pcfg</code>. These are the elements of the set callers
     *  hand to {@link #checkWarnThrowForInstantiateByNameGated(String,PropertiesConfig,Set)}, which
     *  both reads and writes them. Callers do not usually construct these themselves &mdash; they just
     *  supply an empty, mutable set for the gate to fill.
     *
     *  <p>Note that a <code>Warned</code> is keyed on the configuration as well as the class name, so
     *  that a configuration change which might have changed the whitelist warns afresh. See that
     *  method's documentation for what that implies in practice.</p>
     */
    public final static class Warned
    {
        final String fqcn;
        final PropertiesConfig pcfg;

        public Warned(String fqcn, PropertiesConfig pcfg)
        {
            this.fqcn = fqcn;
            this.pcfg = pcfg;
        }

        @Override
        public boolean equals(Object o) { return (o instanceof Warned) && equals((Warned) o); }
        public boolean equals(Warned w) { return eqOrBothNull(this.fqcn,w.fqcn) && eqOrBothNull(this.pcfg,w.pcfg); }

        @Override
        public int hashCode() { return hashOrZero(fqcn) ^ hashOrZero(pcfg); }
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
     *
     * <p>Note that the check is a check of the <i>name</i>, so it is also the right gate in front of
     * a mere <code>Class.forName(..)</code>. Loading a class runs its static initializers, so a load
     * by an untrusted name is already an execution of untrusted code, whether or not an instance is
     * ever constructed.</p>
     *
     * @param fqcn the fully-qualified class name whose by-name use is proposed
     *
     * @param pcfg the configuration from which the whitelist and the enforcement flag are drawn, in
     *        addition to the sealed System properties snapshot. May be <code>null</code>, in which
     *        case only the snapshot is consulted.
     *
     * @param optionalAlreadyWarned may be <code>null</code>. When non-null, a caller-owned mutable set
     *        in which this method records the (fqcn, pcfg) pairs it has warned about, and which it
     *        consults to suppress a warning it has already emitted. Callers that reach this gate
     *        repeatedly for the same class &mdash; a <code>DataSource</code> re-checking on every
     *        connection checkout, say &mdash; should pass one, or the warning becomes a flood. Pass
     *        <code>null</code> to warn every time.
     *
     *        <p>The set is both read and written here, and is not internally synchronized, so a caller
     *        that shares it between threads must guard it. Passing a set owned by a lock already held
     *        at the call site is the simplest approach.</p>
     *
     *        <p>Suppression is keyed on <code>pcfg</code> as well as on <code>fqcn</code>, so that a
     *        configuration change which might have changed the whitelist warns afresh. Be aware of what
     *        that means in practice: <code>PropertiesConfig</code> implementations do not generally
     *        override <code>equals(..)</code>, so suppression ordinarily lasts only for as long as the
     *        caller hands in the same <code>PropertiesConfig</code> <i>instance</i>. A caller that
     *        builds a fresh config for each call will see no suppression at all, and will grow the set
     *        without bound. Callers wanting suppression should hold a stable reference &mdash; which is
     *        what a library-wide config accessor ordinarily returns.</p>
     *
     *        <p>Only the warn-and-continue path consults this set. When whitelist enforcement is on, a
     *        non-whitelisted name throws every time, as it must.</p>
     *
     * @throws InstantiationNotPermittedException if <code>fqcn</code> is not whitelisted and whitelist
     *         enforcement is in effect. Note that enforcement defaults to <i>off</i> for now, so an
     *         unwhitelisted name ordinarily warns rather than throwing. That default is expected to
     *         change in a future release.
     *
     * @see #checkWarnThrowForInstantiateByNameGated(String,PropertiesConfig)
     * @see #instantiateByNameUngated
     */
    public static void checkWarnThrowForInstantiateByNameGated(String fqcn, PropertiesConfig pcfg, Set<Warned> optionalAlreadyWarned) throws InstantiationNotPermittedException
    {
        WhitelistInfo info = collectWhitelistSyspropsPropertiesConfig(pcfg);
        Set<String> whitelist = info.getWhitelist();
        boolean nameOkay;

        if (whitelist.contains(fqcn))
            nameOkay = true;
        else if (whitelist.size() == 1 && whitelist.contains("*"))
            nameOkay = true;
        else
            nameOkay = false;

        if (!nameOkay)
        {
            String whitelistDescriptor = whitelistManager.makeWhitelistDescriptor(info.getSource());
            SealedSystemPropertiesBooleanProperty.Details[] holder = new SealedSystemPropertiesBooleanProperty.Details[1];
            if (_enforceWhiteList.getValue(pcfg, logger, holder))
            {
                throw new InstantiationNotPermittedException(
                    "By-name instantiation of '" + fqcn + "' not permitted. The class is not in the " + whitelistDescriptor + ", which is enforced.",
                    fqcn,
                    info
                );
            }
            else
            {
                SealedSystemPropertiesBooleanProperty.Details details = holder[0];
                if (logger.isLoggable(MLevel.WARNING))
                {
                    Warned warned = null;
                    if (optionalAlreadyWarned == null || !optionalAlreadyWarned.contains(warned = new Warned(fqcn, pcfg)))
                    {
                        if (details.isUnconfigured())
                            logger.log(
                               MLevel.WARNING,
                               "Instantiating '" + fqcn + "' by name despite its absence from the " + whitelistDescriptor + ", " +
                               "and despite no explicit suppression of whitelist enforcement via '" + ENFORCE_WHITELIST_KEY + "=false'. " +
                               "This may be blocked in future releases. If you mean for '" + fqcn + "' to be instantiated by name, please add it to the whitelist, " +
                               "or else explicitly suppress enforcement of the whitelist. Log at DEBUG/FINE to see the current whitelist."
                            );
                        else
                            logger.log(
                               MLevel.WARNING,
                               "Instantiating '" + fqcn + "' by name despite conflicts in the configuration of the " + whitelistDescriptor + ": " + details +
                               ". Log at DEBUG/FINE to see the current whitelist."
                            );
                        if (warned != null) optionalAlreadyWarned.add(warned); // warned not null implies optionalAlreadyWarned not null
                    }
                }
                if (logger.isLoggable(MLevel.FINE))
                    logger.log(MLevel.FINE, "'" + fqcn + "' not in '" + whitelistManager.getWhitelistBaseKey() + "' and would be refused if the whitelist were enforced. Current whitelist: " + whitelist);
            }
        }
    }

    /**
     *  Equivalent to {@link #checkWarnThrowForInstantiateByNameGated(String,PropertiesConfig,Set)} with
     *  no record of prior warnings, so that a non-whitelisted name warns on every call. Appropriate for
     *  gates reached once per object, or once per configuration load. A gate reached once per operation
     *  &mdash; per connection checkout, per request &mdash; should prefer the three-argument form.
     */
    public static void checkWarnThrowForInstantiateByNameGated(String fqcn, PropertiesConfig pcfg) throws InstantiationNotPermittedException
    { checkWarnThrowForInstantiateByNameGated( fqcn, pcfg, null ); }

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

    public static WhitelistInfo currentWhitelistInfo(PropertiesConfig pcfg)
    { return collectWhitelistSyspropsPropertiesConfig(pcfg); }

    public static boolean isEnforcingWhitelist(PropertiesConfig pcfg)
    { return _enforceWhiteList.getValue(pcfg, logger, null); }

    public static String getWhitelistBaseKey()
    { return whitelistManager.getWhitelistBaseKey(); }

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
                    logger.log(MLevel.WARNING, "Found uninterpretable value for '" + ENFORCE_WHITELIST_KEY + "', '" + val + "'.");
                return null;
            }
        }
    }

    private static WhitelistInfo collectWhitelistSyspropsPropertiesConfig(PropertiesConfig pcfg)
    { return whitelistManager.collectWhitelistInfoSyspropsPropertiesConfig(pcfg, logger); }

    private ByNameInstantiationUtils()
    {}
}
