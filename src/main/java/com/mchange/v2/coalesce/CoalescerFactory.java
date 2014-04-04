/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

package com.mchange.v2.coalesce;

public final class CoalescerFactory
{
    /**
     *  <p>Creates a "Coalescer" that coalesces Objects according to their
     *  equals() method. Given a set of n Objects among whom equals() would
     *  return true, calling coalescer.coalesce() in any order on any sequence 
     *  of these Objects will always return a single "canonical" instance.</p>
     *
     *  <p>This method creates a weak, synchronized coalesecer, safe for use
     *  by multiple Threads.</p>
     */
    public static Coalescer createCoalescer()
    { return createCoalescer( true, true ); }

    /**
     *  <p>Creates a "Coalescer" that coalesces Objects according to their
     *  equals() method. Given a set of n Objects among whom equals() would
     *  return true, calling coalescer.coalesce() in any order on any sequence 
     *  of these Objects will always return a single "canonical" instance.</p>
     *
     *  @param weak if true, the Coalescer will use WeakReferences to hold
     *              its canonical instances, allowing them to be garbage
     *              collected if they are nowhere in use.
     *
     *  @param synced if true, access to the Coalescer will be automatically
     *                synchronized. if set to false, then users must manually
     *                synchronize access.
     */
    public static Coalescer createCoalescer( boolean weak, boolean synced )
    { return createCoalescer( null, weak, synced ); }

    /**
     *  <p>Creates a "Coalescer" that coalesces Objects according to the
     *  checkCoalesce() method of a "CoalesceChecker". Given a set of 
     *  n Objects among whom calling cc.checkCoalesce() on any pair would
     *  return true, calling coalescer.coalesce() in any order on any sequence 
     *  of these Objects will always return a single "canonical" instance.
     *  This allows one to define immutable value Objects whose equals() 
     *  method is a mere identity test -- one can use a Coalescer in a 
     *  factory method to ensure that no two instances with the same values
     *  are made available to clients.</p>
     *
     * @param cc CoalesceChecker that will be used to determine whether two
     *           objects are equivalent and can be coalesced. [If cc is null, then two
     *           objects will be coalesced iff o1.equals( o2 ).]
     *
     *  @param weak if true, the Coalescer will use WeakReferences to hold
     *              its canonical instances, allowing them to be garbage
     *              collected if they are nowhere in use.
     *
     *  @param synced if true, access to the Coalescer will be automatically
     *                synchronized. if set to false, then users must manually
     *                synchronize access.
     */
    public static Coalescer createCoalescer( CoalesceChecker cc, boolean weak, boolean synced )
    {
	Coalescer out;
	if ( cc == null )
	    {
		out = ( weak ? 
			(Coalescer) new WeakEqualsCoalescer() : 
			(Coalescer) new StrongEqualsCoalescer() );
	    }
	else
	    {
		out = ( weak ? 
			(Coalescer) new WeakCcCoalescer( cc ) : 
			(Coalescer) new StrongCcCoalescer( cc ) );
	    }
	return ( synced ? new SyncedCoalescer( out ) : out );
    }

}
