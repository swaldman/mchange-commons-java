/*
 * Distributed as part of mchange-commons-java v.0.2.1
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


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
