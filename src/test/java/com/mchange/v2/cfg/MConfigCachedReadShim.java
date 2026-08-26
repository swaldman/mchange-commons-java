package com.mchange.v2.cfg;

import java.util.List;

/**
 *  Exposes the package-private cached readers on the MConfig facade to the test harness.
 *
 *  <p>The list-taking cached readers are deliberately not public: MConfig offers exactly one
 *  MLog-safe entry point per facade class -- the uncached, list-taking one -- and every other
 *  public form logs, which is what makes it unsafe to call while MLog is still coming up. The
 *  cached readers log from inside CSManager rather than accepting a list from a caller, so a
 *  public list-taking form would only invite the mistake this arrangement exists to prevent.</p>
 *
 *  <p>Tests still need to reach them, because a caller-supplied list is the only way to observe
 *  what a cached read reports. Living in com.mchange.v2.cfg, this class simply calls them; the
 *  alternative was getDeclaredMethod plus setAccessible in {@code CfgScenario.call}, which would
 *  have made every reflective call in the harness look privileged in order to serve three.</p>
 *
 *  <p>CfgScenario loads this class into the scenario ClassLoader -- see its {@code shimRoot()} --
 *  so that "package-private" resolves against the scenario's own copy of MConfig rather than the
 *  one on the ordinary test classpath. That is also why this class must stay dependency-free
 *  apart from the cfg package itself: the scenario ClassLoader cannot see anything else.</p>
 */
public final class MConfigCachedReadShim
{
    public static MultiPropertiesConfig traditionalCached( String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut )
    { return MConfig.WithTraditionalDefaultSources.readCachedClassloaderResourceConfig( defaultResources, preemptingResources, delayedLogItemsOut ); }

    public static MultiPropertiesConfig asProvidedCached( String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut )
    { return MConfig.AsProvided.readCachedClassloaderResourceConfig( defaultResources, preemptingResources, delayedLogItemsOut ); }

    public static MultiPropertiesConfig asProvidedVetoableCached( String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut )
        throws ConfigVetoedException
    { return MConfig.AsProvidedVetoable.readCachedClassloaderResourceConfig( defaultResources, preemptingResources, delayedLogItemsOut ); }

    private MConfigCachedReadShim() {}
}
