package com.mchange.v2.async;

/**
 *  Note: All "reporting" values below are **optional**.
 *
 *  Implementations may return -1 for int values, empty string or (perhaps, be prepared) null for String values.
 *
 *  These methods are intended to provide optional reporting to users, for dashboards and debugging,
 *  They cannot be relied upon.
 */
public interface ThreadPoolReportingAsynchronousRunner extends AsynchronousRunner
{
    public int getThreadCount();
    public int getActiveCount();
    public int getIdleCount();
    public int getPendingTaskCount();
    public String getStatus();
    public String getStackTraces();
}
