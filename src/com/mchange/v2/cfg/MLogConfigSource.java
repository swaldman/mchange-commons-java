package com.mchange.v2.cfg;

import java.util.List;

/**
 * Was intended solely for use by com.mchange.v2.log.MLogConfig, which no longer calls it:
 * MLogConfig now goes directly to {@link MConfig.WithTraditionalDefaultSources}.
 *
 * @deprecated Use {@link MConfig.WithTraditionalDefaultSources} directly. This class survives
 *             only so that any outside caller that found it keeps compiling.
 */
@Deprecated
public final class MLogConfigSource
{
    /**
     * @deprecated The vmConfig APIs are confusing. Use readUncachedClassloaderResourceConfig(...)
     */
    @Deprecated
    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    { return ConfigUtils.readVmConfig( defaultResources, preemptingResources, delayedLogItemsOut); }

    private MLogConfigSource()
    {}
}
