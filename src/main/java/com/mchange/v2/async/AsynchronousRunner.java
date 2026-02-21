package com.mchange.v2.async;

import com.mchange.v1.util.ClosableResource;

public interface AsynchronousRunner extends ClosableResource
{
    public void postRunnable(Runnable r);


    /**
     * Finish with this AsynchronousRunner, and clean-up
     * any Threads or resources it may hold.
     *
     * @param skip_remaining_tasks Should be regarded as
     *        a hint, not a guarantee. If true, pending,
     *        not-yet-performed tasks will be skipped,
     *        if possible.
     *        Currently executing tasks may or 
     *        may not be interrupted. If false, all
     *        previously scheduled tasks will be 
     *        completed prior to clean-up. The method
     *        returns immediately regardless.
     */ 
    public void close( boolean skip_remaining_tasks );

    /**
     * Clean-up resources held by this asynchronous runner
     * as soon as possible. Remaining tasks are skipped if possible,
     * and any tasks executing when close() is called may
     * or may not be interrupted. Equivalent to close( true ).
     */
    public void close();
}
