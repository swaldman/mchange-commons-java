package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

/**
 *  A security-sensitive boolean that may tighten after startup but never loosen -- a ratchet
 *  over <i>both</i> System properties and supplied configuration.
 *
 *  <p><b>Not currently used.</b> Kept because the policy is coherent and may prove to be what
 *  some setting wants; see the note on what it costs, below. The implementations in service are
 *  {@link SealedSystemPropertiesBooleanProperty} and, for whitelists,
 *  {@link SealedSystemPropertiesWhitelistManager}.</p>
 *
 *  <h3>The policy</h3>
 *
 *  <p>Each flag declares which polarity is safe -- an enforcement switch is safe when true, a
 *  permission switch when false. The sealed snapshot of System properties is the baseline. Once
 *  any source asserts the safe value the flag latches there and stops consulting configuration;
 *  until then it keeps looking, because that is the state in which a later tightening still
 *  needs to be noticed. Removing configuration reverts to the declared default, which is a
 *  tightening when the default is the safe value and a no-op otherwise.</p>
 *
 *  <p>The invariant is that the value never moves from the safe side to the less safe side,
 *  whatever an operator does afterward. It holds structurally rather than case by case: the only
 *  path that can lower a value is reachable only from inside the guard that skips a flag already
 *  at its safe value.</p>
 *
 *  <h3>Why it is not what we settled on</h3>
 *
 *  <p>Ratcheting <i>supplied configuration</i> turned out to be the wrong call. System properties
 *  are an ambient channel any code in the process can write, unguarded since SecurityManager's
 *  removal, and that is what justifies refusing later changes. A PropertiesConfig is a parameter
 *  the application chooses to pass, and implementations are read-only views of configuration the
 *  application manages; it is not a channel an attacker reaches without already controlling the
 *  call site. Treating the two alike broke the overloads that accept a config per call, whose
 *  signatures promise that <i>this</i> call's configuration decides -- under the ratchet the
 *  parameter was honored only on the first consultation in the JVM, after which a caller opting
 *  into a capability was silently refused.</p>
 *
 *  <p>The latch also has costs paid by every deployment, not only the ones under attack. An
 *  unconfigured first lookup latches deny-all for the life of the JVM, so configuration arriving
 *  later is ignored; a transient deny-all cannot be undone without a restart; and the retained
 *  answer makes the result depend on which lookups happened earlier, which is a hard thing to
 *  reason about and an awkward thing to test -- a shared-JVM suite has to reach in and reset it.
 *  {@link SealedSystemPropertiesBooleanProperty} has none of that, because it retains nothing.</p>
 *
 *  <h3>When it might be right after all</h3>
 *
 *  <p>The one thing this offers that sealing does not is recovery from a seal taken too early:
 *  a deployment that asserts the safe value after the snapshot was captured is still heard. A
 *  setting for which that matters more than per-call configuration would want this rather than
 *  the sealed implementation. {@link SystemOnlyStrengthensBooleanProperty} is the intermediate
 *  position, ratcheting System properties alone.</p>
 */
class EarliestOrStrongestBooleanProperty extends AbstractBooleanProperty
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
