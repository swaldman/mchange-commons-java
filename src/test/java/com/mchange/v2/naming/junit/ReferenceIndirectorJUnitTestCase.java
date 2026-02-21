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

package com.mchange.v2.naming.junit;

import java.io.IOException;
import java.util.*;
import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import junit.framework.TestCase;
import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.naming.AnyNameNameGuard;
import com.mchange.v2.naming.FirstComponentIsJavaIdentifierNameGuard;
import com.mchange.v2.naming.ReferenceIndirector;
import com.mchange.v2.naming.SecurityConfigKey;
import com.mchange.v2.ser.IndirectlySerialized;

public final class ReferenceIndirectorJUnitTestCase extends TestCase
{
    // ==========================================
    // Test helpers: ObjectFactory + Referenceable
    // Must be public static for Class.forName + newInstance to work
    // ==========================================

    public static final class SimpleObjectFactory implements ObjectFactory
    {
        public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment )
            throws Exception
        { return "SIMPLE"; }
    }

    static final String SIMPLE_FACTORY = SimpleObjectFactory.class.getName();

    /** A minimal Referenceable backed by SimpleObjectFactory. */
    public static final class TestReferenceable implements Referenceable
    {
        public Reference getReference() throws NamingException
        { return new Reference( TestReferenceable.class.getName(), SIMPLE_FACTORY, null ); }
    }

    // ==========================================
    // Helpers
    // ==========================================

    private static PropertiesConfig pcfg( String key, String value )
    {
        Properties p = new Properties();
        p.setProperty( key, value );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    private static PropertiesConfig pcfg( String key1, String val1, String key2, String val2 )
    {
        Properties p = new Properties();
        p.setProperty( key1, val1 );
        p.setProperty( key2, val2 );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    private static void restoreSystemProperty( String key, String savedValue )
    {
        if ( savedValue == null )
            System.clearProperty( key );
        else
            System.setProperty( key, savedValue );
    }

    /**
     * Use the given indirector to produce a ReferenceSerialized wrapping a TestReferenceable.
     */
    private static IndirectlySerialized makeReferenceSerialized( ReferenceIndirector indirector ) throws Exception
    { return indirector.indirectForm( new TestReferenceable() ); }

    // ==========================================
    // Getter / setter tests
    // ==========================================

    public void testGetNameDefaultNull()
    { assertNull( new ReferenceIndirector().getName() ); }

    public void testSetGetName() throws InvalidNameException
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        Name name = new CompositeName( "java:comp/env/myDS" );
        ri.setName( name );
        assertSame( name, ri.getName() );
    }

    public void testGetContextNameDefaultNull()
    { assertNull( new ReferenceIndirector().getNameContextName() ); }

    public void testSetGetContextName() throws InvalidNameException
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        Name ctxName = new CompositeName( "java:comp/env" );
        ri.setNameContextName( ctxName );
        assertSame( ctxName, ri.getNameContextName() );
    }

    public void testGetEnvironmentPropertiesDefaultNull()
    { assertNull( new ReferenceIndirector().getEnvironmentProperties() ); }

    public void testSetGetEnvironmentProperties()
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        Hashtable env = new Hashtable();
        env.put( "key", "value" );
        ri.setEnvironmentProperties( env );
        assertSame( env, ri.getEnvironmentProperties() );
    }

    // ==========================================
    // indirectForm() tests
    // ==========================================

    public void testIndirectFormReturnsNonNull() throws Exception
    { assertNotNull( makeReferenceSerialized( new ReferenceIndirector() ) ); }

    public void testIndirectFormReturnsIndirectlySerialized() throws Exception
    { assertTrue( makeReferenceSerialized( new ReferenceIndirector() ) instanceof IndirectlySerialized ); }

    public void testIndirectFormToStringContainsReferenceInfo() throws Exception
    {
        String str = makeReferenceSerialized( new ReferenceIndirector() ).toString();
        assertNotNull( str );
        // The ReferenceSerialized.toString() mentions "reference=", "name=", "contextName=", "env="
        assertTrue( "toString should contain 'reference'", str.toLowerCase().contains( "reference" ) );
    }

    // ==========================================
    // ReferenceSerialized.getObject() - security rejection tests
    // ==========================================

    /**
     * When the indirector was given a non-null environment and
     * acceptDeserializedInitialContextEnvironment is false (the default),
     * getObject() must throw IOException.
     */
    public void testGetObjectNonNullEnvRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setEnvironmentProperties( new Hashtable() );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        try
        {
            is.getObject( null );
            fail( "Expected IOException: non-null env rejected by default" );
        }
        catch (IOException e) { /* expected */ }
    }

    /**
     * When acceptDeserializedInitialContextEnvironment=true and a whitelist is provided,
     * an empty (but non-null) env is accepted and getObject() resolves the reference.
     */
    public void testGetObjectNonNullEnvPermittedByConfig() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setEnvironmentProperties( new Hashtable() );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg(
            SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, "true",
            SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY
        );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /**
     * "jdbc/DataSource" lacks a "java:" first component so is not explicitly local.
     * The default ApparentlyLocalNameGuard rejects it and getObject() must throw IOException.
     */
    public void testGetObjectNotExplicitlyLocalContextNameRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setNameContextName( new CompositeName( "jdbc/DataSource" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        try
        {
            is.getObject( null );
            fail( "Expected IOException: contextName rejected by default ApparentlyLocalNameGuard" );
        }
        catch (IOException e) { /* expected */ }
    }

    // ==========================================
    // ReferenceSerialized.getObject() - success tests
    // ==========================================

    /** Simplest success case: null env, null contextName, factory whitelisted in pcfg. */
    public void testGetObjectWithWhitelistedFactoryViaPcfg() throws Exception
    {
        IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /** No-arg getObject() uses the system-property whitelist when present. */
    public void testGetObjectNoArgWithWhitelistViaSysprop() throws Exception
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
            IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );
            assertEquals( "SIMPLE", is.getObject() );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * A local name (first component starts with "java:") set on the indirector
     * is acceptable by default and getObject() succeeds with a whitelisted factory.
     */
    public void testGetObjectWithLocalNameSetSucceeds() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "java:comp/env/myObj" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /**
     * "jdbc/DataSource" lacks a "java:" prefix so is not explicitly local.
     * The default ApparentlyLocalNameGuard rejects it; the NamingException is wrapped as IOException.
     */
    public void testGetObjectWithNotExplicitlyLocalNameRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "jdbc/DataSource" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
        try
        {
            is.getObject( cfg );
            fail( "Expected IOException: jdbc/DataSource rejected by default ApparentlyLocalNameGuard" );
        }
        catch (IOException e) { /* expected */ }
    }

    /**
     * "jdbc/DataSource" lacks a "java:" prefix so is rejected by the default guard,
     * but is accepted when FirstComponentIsJavaIdentifierNameGuard is configured
     * (its first component "jdbc" is a valid Java identifier).
     */
    public void testGetObjectWithNotExplicitlyLocalNamePermittedByConfig() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "jdbc/DataSource" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg(
            SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY,
            SecurityConfigKey.NAME_GUARD_CLASS_NAME, FirstComponentIsJavaIdentifierNameGuard.class.getName()
        );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /**
     * A contextName with scheme "ldap:" is genuinely non-local.
     * The default ApparentlyLocalNameGuard rejects it and getObject() must throw IOException.
     */
    public void testGetObjectNonLocalContextNameRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setNameContextName( new CompositeName( "ldap://example.com/ctx" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        try
        {
            is.getObject( null );
            fail( "Expected IOException: ldap:// contextName rejected by default ApparentlyLocalNameGuard" );
        }
        catch (IOException e) { /* expected */ }
    }

    /**
     * A name with scheme "ldap:" is genuinely non-local.
     * The default ApparentlyLocalNameGuard rejects it; the NamingException is wrapped as IOException.
     */
    public void testGetObjectWithNonLocalNameRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "ldap://example.com/myObj" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
        try
        {
            is.getObject( cfg );
            fail( "Expected IOException: ldap:// name rejected by default ApparentlyLocalNameGuard" );
        }
        catch (IOException e) { /* expected */ }
    }

    /**
     * A genuinely non-local "ldap://..." name is accepted when AnyNameNameGuard is configured,
     * and dereferencing succeeds with a whitelisted factory.
     */
    public void testGetObjectWithNonLocalNamePermittedByConfig() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "ldap://example.com/myObj" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg(
            SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY,
            SecurityConfigKey.NAME_GUARD_CLASS_NAME, AnyNameNameGuard.class.getName()
        );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /**
     * Without any whitelist configured (neither pcfg nor system property),
     * getObject() must fail because the mandatory whitelist is missing.
     * The NamingException is wrapped in an InvalidObjectException (an IOException subclass).
     */
    public void testGetObjectNoWhitelistFails() throws Exception
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.clearProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
            IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );
            // pass an empty pcfg that has no whitelist key
            PropertiesConfig cfg = MultiPropertiesConfig.fromProperties( "/test", new Properties() );
            is.getObject( cfg );
            fail( "Expected IOException: no whitelist configured" );
        }
        catch (IOException e) { /* expected: NamingException wrapped in InvalidObjectException */ }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * When the factory class is not in the whitelist, getObject() must fail.
     * The NamingException is wrapped in an InvalidObjectException (an IOException subclass).
     */
    public void testGetObjectFactoryNotInWhitelistFails() throws Exception
    {
        IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );
        // whitelist only contains a different factory
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "com.example.SomeOtherFactory" );
        try
        {
            is.getObject( cfg );
            fail( "Expected IOException: factory not in whitelist" );
        }
        catch (IOException e) { /* expected */ }
    }
}
