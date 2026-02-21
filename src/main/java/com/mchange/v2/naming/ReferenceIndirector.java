/*
 * Distributed as part of mchange-commons-java 0.2.11
 *
 * Copyright (C) 2015 Machinery For Change, Inc.
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
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;
import com.mchange.v2.naming.ReferenceableUtils;
import com.mchange.v2.ser.Indirector;
import com.mchange.v2.ser.IndirectlySerialized;
import com.mchange.v2.util.MapUtils;

public class ReferenceIndirector implements Indirector
{
    final static MLogger logger = MLog.getLogger( ReferenceIndirector.class );

    Name      name;
    Name      contextName;
    Hashtable environmentProperties;

    private static String envToString( Hashtable env )
    {
        if (env == null)
            return "null";
        else
            return "[" + MapUtils.joinEntriesIntoString(true,true,"->",",",env) + "]";
    }

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

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getClass().getName());
            sb.append("[reference=");
            sb.append(reference);
            sb.append("; name=");
            sb.append(name);
            sb.append("; contextName=");
            sb.append(contextName);
            sb.append("; env=");
            sb.append(envToString(env));
            sb.append("]");
            return sb.toString();
        }

	public Object getObject() throws ClassNotFoundException, IOException
        { return getObject(null); }

	public Object getObject(PropertiesConfig pcfg) throws ClassNotFoundException, IOException
	{
	    try
		{
		    Context initialContext;
		    if ( env == null )
			initialContext = new InitialContext();
		    else
                    {
                        if (ReferenceableUtils.acceptDeserializedInitialContextEnvironment(pcfg))
                            initialContext = new InitialContext( env );
                        else
                            throw new IOException(
                                "A value indirectly serialized as a reference includes a non-default (non-null) InitialContext environment " +
                                "by which the reference wishes to be looked up. " +
                                "InitialContext environment parameters can redirect lookups to untrusted remote servers " +
                                "and potentially lead to download and execution of malicious code. SecurityConfigKey '" +
                                SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT +
                                "' is set conservatively to false, so this operation is not supported. " +
                                "Indirectly serialized reference: " + this.toString()
                            );
                    }

		    Context nameContext = null;
		    if ( contextName != null )
                    {
                        try
                        {
                            ReferenceableUtils.assertAcceptableName( contextName, pcfg );
                            nameContext = (Context) initialContext.lookup( contextName );
                        }
                        catch (NamingException ne) // if the name is unacceptable, fail with an informative message.
                        { throw new IOException(ne.getMessage(), ne); }
                    }

                    try
                    { return ReferenceableUtils.referenceToObject( reference, name, nameContext, env, pcfg ); }
                    catch (NamingException ne)
                    {
                        throw new IOException(
                            "Failed to dereference reference " + reference +
                            "' under name '" + name + "' and nameContext '" + nameContext +
                            "' using environment: " + envToString(env),
                            ne
                        );
                    }
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
