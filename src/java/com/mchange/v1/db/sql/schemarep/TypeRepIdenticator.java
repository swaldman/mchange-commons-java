/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
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


package com.mchange.v1.db.sql.schemarep;

import java.util.Arrays;
import com.mchange.v1.identicator.Identicator;

public class TypeRepIdenticator implements Identicator
{
    private final static TypeRepIdenticator INSTANCE = new TypeRepIdenticator();

    public static TypeRepIdenticator getInstance()
    { return INSTANCE; }

    private TypeRepIdenticator()
    {}

    public boolean identical(Object a, Object b)
    {
	if (a == b)
	    return true;

	TypeRep aa = (TypeRep) a;
	TypeRep bb = (TypeRep) b;

	return 
	    aa.getTypeCode() == bb.getTypeCode() &&
	    Arrays.equals( aa.getTypeSize(), bb.getTypeSize() );
    }

    public int hash(Object o)
    {
	TypeRep tr = (TypeRep) o;
	int out = tr.getTypeCode();

	int[] szArray = tr.getTypeSize();
	if (szArray != null)
	    {
		int len = szArray.length;
		for (int i = 0; i < len; ++i)
		    out ^= szArray[i];
		out ^= len;
	    }
	return out;
    }
}
