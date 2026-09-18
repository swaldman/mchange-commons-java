package com.mchange.v2.cfg;

import com.mchange.v2.log.*;

/**
 *  A snapshot of this JVM's System properties, taken once and never retaken, for
 *  configuration whose meaning must not change after an application has started.
 *
 *  <h3>Why this exists</h3>
 *
 *  <p>System properties are writable by any code in the process, at any time. That was once
 *  checked -- <code>System.setProperty</code> required a
 *  <code>PropertyPermission(..., "write")</code> under a SecurityManager -- but SecurityManager
 *  was deprecated by JEP 411 and permanently disabled by JEP 486, so on a current JVM there is
 *  no check at all. Security configuration read live from System properties therefore means
 *  only what it meant at the instant it was read, and can be rewritten afterward by anything
 *  running in the same process.</p>
 *
 *  <p>The platform solves this for itself the same way: the JVM keeps its own snapshot of the
 *  properties it was launched with, and several security-sensitive values (the serialization
 *  filter, the temporary directory, various TLS and RMI settings) are captured once and are
 *  simply not re-read. This class offers that to library and application code.</p>
 *
 *  <h3>When the snapshot is taken</h3>
 *
 *  <p>The snapshot is taken at the first call to {@link #get}, and there is exactly one for the
 *  lifetime of the JVM. Two consequences follow, and both matter:</p>
 *
 *  <ul>
 *    <li><b>Reading seals.</b> {@link #get} is not a passive accessor; the first call fixes the
 *        snapshot as a side effect. Any consumer anywhere in the process can therefore be the
 *        one that seals, simply by consulting configuration.</li>
 *    <li><b>Sealing is global.</b> One snapshot serves every consumer, so they cannot disagree
 *        about what the properties were -- but equally, whoever reads first decides the instant
 *        for everyone.</li>
 *  </ul>
 *
 *  <p>A deployment that wants to choose that instant should call {@link #seal} at the end of its
 *  own startup, once it has set every property it intends to set. {@link #seal} returns
 *  <code>true</code> if the call is what sealed the snapshot and <code>false</code> -- with a
 *  warning -- if something had already sealed it, which is worth attending to: it means the
 *  effective snapshot was taken at a moment the caller did not choose. {@link #isSealed} answers
 *  the same question without causing a seal.</p>
 *
 *  <h3>What this does and does not do</h3>
 *
 *  <p>It does not prevent anything. <code>System.setProperty</code> continues to work, and code
 *  that calls <code>System.getProperty</code> directly continues to see live values. What the
 *  snapshot provides is a source of configuration that a later write cannot alter, for the
 *  consumers that choose to read from it. Late writes are not rejected; they are invisible
 *  here.</p>
 *
 *  <p>Nor does this class decide policy. It holds the properties; what to do when current
 *  configuration disagrees with them belongs to the consumer.
 *  {@link PropertiesConfigUtils.EarliestOrNarrowestWhitelistManager} is the one in this library
 *  today: it honors later changes that <i>narrow</i> a whitelist and ignores those that would
 *  widen it.</p>
 *
 *  <p>{@link MConfig#sealSystemProperties}, {@link MConfig#getSealedSystemProperties} and
 *  {@link MConfig#isSealedSystemProperties} forward to these methods, for callers who reach for
 *  MConfig first.</p>
 *
 *  <h3>A constraint on logging</h3>
 *
 *  <p>Initializing <code>com.mchange.v2.log</code> must never consult this class. Sealing reads
 *  configuration and logs what it did, so a logger that sealed in order to initialize would
 *  re-enter its own initialization. That is why MLog resolves its backend with a direct
 *  reflective construction rather than through a whitelist-gated one, and why the logger here is
 *  obtained lazily rather than in a static initializer.</p>
 */
public final class SealedSystemProperties
{
    // setting up MLog / com.mchange.v2.log must never
    // touch this construct, or we'll provoke a potentially
    // dangerous cycle
    private static MLogger _logger = null;

    private synchronized static MLogger logger()
    {
        if (_logger == null ) _logger = MLog.getLogger( SealedSystemProperties.class );
        return _logger;
    }

    private static PropertiesConfig theProperties = null;

    public synchronized static PropertiesConfig get()
    {
        if (theProperties == null)
        {
            theProperties = MConfig.AsProvided.readUncachedClassloaderResourceConfig(new String[]{"/"});
            if (logger().isLoggable( MLevel.INFO ))
                logger().log(MLevel.INFO, "Sealed snapshot of System properties.");
            if (logger().isLoggable( MLevel.FINE ))
                logger().log(MLevel.FINE, "Logging stack trace of the call that sealed snapshot of System properties.", new Exception("Logging stack trace of the call that sealed snapshot of System properties."));
        }
        return theProperties;
    }

    public synchronized static boolean seal()
    {
        boolean unset = (theProperties == null);
        if (unset)
        {
            get();
            assert theProperties != null : "get() should have set theProperties, should not be null here!";
            return true;
        }
        else
        {
            if (logger().isLoggable( MLevel.WARNING ))
                logger().log(MLevel.WARNING, "An attempt to seal a snapshot of System properties did nothing, a prior snapshot has already been sealed.");
            if (logger().isLoggable( MLevel.FINE ))
                logger().log(
                  MLevel.FINE,
                  "Logging stack trace of the attempt to seal a snapshot of System properties when a snapshot had already been sealed.",
                  new Exception("Logging stack trace that attempt to seal a snapshot of System properties when a snapshot had already been sealed.")
                );
            return false;
        }
    }

    public synchronized static boolean isSealed()
    { return theProperties != null; }

    private SealedSystemProperties()
    {}
}
