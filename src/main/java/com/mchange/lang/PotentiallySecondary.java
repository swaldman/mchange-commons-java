package com.mchange.lang;

/*
 * An interface to be implemented by Throwable objects
 * that may indicate a secondary problem, originated by the
 * more primary, nested, throwable
 */
public interface PotentiallySecondary
{
    public Throwable getNestedThrowable();
}
