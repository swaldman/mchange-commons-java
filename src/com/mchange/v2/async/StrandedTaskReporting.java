package com.mchange.v2.async;

import java.util.List;

public interface StrandedTaskReporting
{
    /**
     * makes available any tasks that were unperformed when
     * this AsynchronousRunner was closed, either explicitly
     * using close() or close( true ), or implicitly because
     * some failure or corruption killed the Object (most likely
     * a Thread interruption.
     *
     * @return null if the AsynchronousRunner is still alive, a List
     *  of Runnables otherwise, which will be empty only if all tasks
     *  were performed before the AsynchronousRunner shut down.
     */
    public List getStrandedTasks();
}
