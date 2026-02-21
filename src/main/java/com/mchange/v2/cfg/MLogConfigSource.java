package com.mchange.v2.cfg;

import java.util.List;

/**
 * Intended solely for use by com.mchange.v2.log.MLogConfig;
 */
public final class MLogConfigSource
{
    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    { return ConfigUtils.readVmConfig( defaultResources, preemptingResources, delayedLogItemsOut); }

    private MLogConfigSource()
    {}
}

