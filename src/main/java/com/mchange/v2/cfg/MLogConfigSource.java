package com.mchange.v2.cfg;

import java.util.List;

/**
 * Intended solely for use by com.mchange.v2.log.MLogConfig;
 */
public final class MLogConfigSource
{
    public static MultiPropertiesConfig readUncachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    { return ConfigUtils.readUncachedClassloaderResourceConfig( defaultResources, preemptingResources, delayedLogItemsOut); }

    /**
     * @deprecated The vmConfig APIs are confusing. Use readUncachedClassloaderResourceConfig(...)
     */
    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    { return ConfigUtils.readVmConfig( defaultResources, preemptingResources, delayedLogItemsOut); }

    private MLogConfigSource()
    {}
}

