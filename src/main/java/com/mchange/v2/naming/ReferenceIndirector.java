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

package com.mchange.v2.naming;

import java.io.InvalidObjectException;
import java.io.IOException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;
import com.mchange.v2.ser.Indirector;
import com.mchange.v2.ser.IndirectlySerialized;

public class ReferenceIndirector implements Indirector
{
    final static MLogger logger = MLog.getLogger( ReferenceIndirector.class );

    Name      name;
    Name      contextName;
    Hashtable environmentProperties;

    public Name getName()
    { return name; }

    public void setName( Name name )
    { this.name = name; }

    public Name getNameContextName()
    { return contextName; }

    public void setNameContextName( Name contextName )
    { this.contextName = contextName; }

    public Hashtable getEnvironmentProperties()
    { return environmentProperties; }

    public void setEnvironmentProperties( Hashtable environmentProperties )
    { this.environmentProperties = environmentProperties; }

    public IndirectlySerialized indirectForm( Object orig ) throws Exception
    { 
	Reference ref = ((Referenceable) orig).getReference();
	return new ReferenceSerialized( ref, name, contextName, environmentProperties );
    }

    private static class ReferenceSerialized implements IndirectlySerialized
    {
	Reference   reference;
	Name        name;
	Name        contextName;
	Hashtable   env;

	ReferenceSerialized( Reference   reference,
			     Name        name,
			     Name        contextName,
			     Hashtable   env )
	{
	    this.reference = reference;
	    this.name = name;
	    this.contextName = contextName;
	    this.env = env;
	}


	public Object getObject() throws ClassNotFoundException, IOException
	{
	    try
		{
		    Context initialContext;
		    if ( env == null )
			initialContext = new InitialContext();
		    else
			initialContext = new InitialContext( env );

		    Context nameContext = null;
		    if ( contextName != null )
			nameContext = (Context) initialContext.lookup( contextName );

		    return ReferenceableUtils.referenceToObject( reference, name, nameContext, env ); 
		}
	    catch (NamingException e)
		{
		    //e.printStackTrace();
		    if ( logger.isLoggable( MLevel.WARNING ) )
			logger.log( MLevel.WARNING, "Failed to acquire the Context necessary to lookup an Object.", e );
		    throw new InvalidObjectException( "Failed to acquire the Context necessary to lookup an Object: " + e.toString() );
		}
	}
    }
}
