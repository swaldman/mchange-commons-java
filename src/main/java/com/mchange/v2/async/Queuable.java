package com.mchange.v2.async;

public interface Queuable extends AsynchronousRunner
{
    public RunnableQueue asRunnableQueue();
}
