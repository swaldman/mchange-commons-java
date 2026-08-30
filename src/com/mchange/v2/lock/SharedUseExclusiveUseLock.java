package com.mchange.v2.lock;

public interface SharedUseExclusiveUseLock
{
    public void acquireShared() throws InterruptedException;

    public void relinquishShared();

    public void acquireExclusive() throws InterruptedException;

    public void relinquishExclusive();
}
