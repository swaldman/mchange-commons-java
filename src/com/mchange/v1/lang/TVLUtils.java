package com.mchange.v1.lang;

/**
 * Three-Valued Logic Utils -- utilities for treating
 * a Boolean variable as a three-state logical entity, with
 * states true, false, or unknown if the variable is null.
 */
public final class TVLUtils
{
    public final static boolean isDefinitelyTrue(Boolean check)
    { return (check != null && check.booleanValue()); }

    public final static boolean isDefinitelyFalse(Boolean check)
    { return (check != null && !check.booleanValue()); }

    public final static boolean isPossiblyTrue(Boolean check)
    { return (check == null || check.booleanValue()); }

    public final static boolean isPossiblyFalse(Boolean check)
    { return (check == null || !check.booleanValue()); }

    public final static boolean isUnknown(Boolean check)
    { return (check == null); }
}
