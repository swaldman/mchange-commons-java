package com.mchange.v2.async;

/**
 *  RunnableQueues guarantee that tasks will be
 *  executed in order, where other AsynchronousRunners
 *  may not.
 */
public interface RunnableQueue extends AsynchronousRunner
{}
