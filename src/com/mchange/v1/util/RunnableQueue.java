package com.mchange.v1.util;


/**
 * @deprecated use com.mchange.v2.async.RunnableQueue
 */
public interface RunnableQueue
{
    public void postRunnable(Runnable r);
}
