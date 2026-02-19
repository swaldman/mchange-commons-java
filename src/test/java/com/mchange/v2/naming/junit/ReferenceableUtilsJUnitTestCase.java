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

import java.util.*;
import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import junit.framework.TestCase;
import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.naming.ReferenceableUtils;
import com.mchange.v2.naming.SecurityConfigKey;

public final class ReferenceableUtilsJUnitTestCase extends TestCase
{
    // ==========================================
    // Test ObjectFactory implementations
    // Must be public static for Class.forName + newInstance to work
    // ==========================================

    public static final class AlphaObjectFactory implements ObjectFactory
    {
        public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment )
            throws Exception
        { return "ALPHA"; }
    }

    public static final class BetaObjectFactory implements ObjectFactory
    {
        public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment )
            throws Exception
        { return "BETA"; }
    }

    // Binary names as used by Class.forName / class literals
    static final String ALPHA_FACTORY = AlphaObjectFactory.class.getName();
    static final String BETA_FACTORY  = BetaObjectFactory.class.getName();

    // ==========================================
    // Helpers
    // ==========================================

    private static Reference makeRef( String className, String factoryClassName )
    { return new Reference( className, factoryClassName, null ); }

    /** Build a PropertiesConfig with a single key/value pair. */
    private static PropertiesConfig pcfg( String key, String value )
    {
        Properties p = new Properties();
        p.setProperty( key, value );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    private static PropertiesConfig emptyPcfg()
    { return MultiPropertiesConfig.fromProperties( "/test", new Properties() ); }

    private static void restoreSystemProperty( String key, String savedValue )
    {
        if ( savedValue == null )
            System.clearProperty( key );
        else
            System.setProperty( key, savedValue );
    }

    // ==========================================
    // literalNullToNull
    // ==========================================

    public void testLiteralNullToNullWithNull()
    { assertNull( ReferenceableUtils.literalNullToNull( null ) ); }

    public void testLiteralNullToNullWithLiteralNull()
    { assertNull( ReferenceableUtils.literalNullToNull( "null" ) ); }

    public void testLiteralNullToNullPassthrough()
    { assertEquals( "hello", ReferenceableUtils.literalNullToNull( "hello" ) ); }

    public void testLiteralNullToNullEmptyString()
    { assertEquals( "", ReferenceableUtils.literalNullToNull( "" ) ); }

    // ==========================================
    // jndiNameIsLocal (String overload)
    // ==========================================

    public void testJndiNameIsLocalStringJavaPrefix()
    { assertTrue( ReferenceableUtils.jndiNameIsLocal( "java:comp/env/myDS" ) ); }

    public void testJndiNameIsLocalStringBarePrefix()
    { assertTrue( ReferenceableUtils.jndiNameIsLocal( "java:" ) ); }

    public void testJndiNameIsLocalStringNonLocal()
    { assertFalse( ReferenceableUtils.jndiNameIsLocal( "ldap://example.com/myDS" ) ); }

    public void testJndiNameIsLocalStringEmpty()
    { assertFalse( ReferenceableUtils.jndiNameIsLocal( "" ) ); }

    // ==========================================
    // jndiNameIsLocal (Name overload)
    // A Name whose first component starts with "java:" is considered local.
    // ==========================================

    public void testJndiNameIsLocalNameJavaPrefix() throws InvalidNameException
    { assertTrue( ReferenceableUtils.jndiNameIsLocal( new CompositeName( "java:comp/env/myDS" ) ) ); }

    public void testJndiNameIsLocalNameBareJavaColon() throws InvalidNameException
    { assertTrue( ReferenceableUtils.jndiNameIsLocal( new CompositeName( "java:" ) ) ); }

    public void testJndiNameIsLocalNameNonLocalUrl() throws InvalidNameException
    { assertFalse( ReferenceableUtils.jndiNameIsLocal( new CompositeName( "ldap://example.com/myDS" ) ) ); }

    public void testJndiNameIsLocalNameNoJavaPrefix() throws InvalidNameException
    { assertFalse( ReferenceableUtils.jndiNameIsLocal( new CompositeName( "comp/env" ) ) ); }

    public void testJndiNameIsLocalNameEmpty() throws InvalidNameException
    { assertFalse( ReferenceableUtils.jndiNameIsLocal( new CompositeName( "" ) ) ); }

    // ==========================================
    // nameLocalityIsAcceptable
    // ==========================================

    public void testNameLocalityAcceptableLocalString()
    {
        // "java:*" strings are always acceptable regardless of config
        assertTrue( ReferenceableUtils.nameLocalityIsAcceptable( "java:comp/env/myDS", null ) );
        assertTrue( ReferenceableUtils.nameLocalityIsAcceptable( "java:comp/env/myDS", emptyPcfg() ) );
    }

    public void testNameLocalityAcceptableNonLocalStringNoPermit()
    { assertFalse( ReferenceableUtils.nameLocalityIsAcceptable( "ldap://example.com", null ) ); }

    public void testNameLocalityAcceptableNonLocalStringPcfgFalse()
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "false" );
        assertFalse( ReferenceableUtils.nameLocalityIsAcceptable( "ldap://example.com", cfg ) );
    }

    public void testNameLocalityAcceptableNonLocalStringPcfgTrue()
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "true" );
        assertTrue( ReferenceableUtils.nameLocalityIsAcceptable( "ldap://example.com", cfg ) );
    }

    public void testNameLocalityAcceptableLocalNameNoPermit() throws InvalidNameException
    {
        // A Name whose first component starts with "java:" is local regardless of config
        Name name = new CompositeName( "java:comp/env" );
        assertTrue( ReferenceableUtils.nameLocalityIsAcceptable( name, null ) );
    }

    public void testNameLocalityAcceptableLocalNamePcfgFalse() throws InvalidNameException
    {
        // Local name is still acceptable even when permitNonlocalJndiNames=false
        PropertiesConfig cfg = pcfg( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "false" );
        Name name = new CompositeName( "java:comp/env" );
        assertTrue( ReferenceableUtils.nameLocalityIsAcceptable( name, cfg ) );
    }

    public void testNameLocalityAcceptableNonLocalNameNoPermit() throws InvalidNameException
    {
        // A non-java: Name is non-local; rejected without explicit permit
        Name name = new CompositeName( "ldap://example.com/myDS" );
        assertFalse( ReferenceableUtils.nameLocalityIsAcceptable( name, null ) );
    }

    public void testNameLocalityAcceptableNonLocalNameWithPermit() throws InvalidNameException
    {
        // A non-java: Name is accepted when permitNonlocalJndiNames=true
        PropertiesConfig cfg = pcfg( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "true" );
        Name name = new CompositeName( "ldap://example.com/myDS" );
        assertTrue( ReferenceableUtils.nameLocalityIsAcceptable( name, cfg ) );
    }

    public void testNameLocalityAcceptableUnknownTypeReturnsFalse()
    {
        // Unknown type → conservatively false
        assertFalse( ReferenceableUtils.nameLocalityIsAcceptable( Integer.valueOf(42), null ) );
    }

    // ==========================================
    // falseBiasedLookup logic (exercised via permitNonlocalJndiNames,
    // supportReferenceRemoteFactoryClassLocation, and
    // acceptDeserializedInitialContextEnvironment)
    // ==========================================

    // --- permitNonlocalJndiNames ---

    public void testPermitNonlocalDefaultFalse()
    { assertFalse( ReferenceableUtils.permitNonlocalJndiNames( null ) ); }

    public void testPermitNonlocalPcfgTrue()
    {
        assertTrue( ReferenceableUtils.permitNonlocalJndiNames(
            pcfg( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "true" ) ) );
    }

    public void testPermitNonlocalPcfgFalse()
    {
        assertFalse( ReferenceableUtils.permitNonlocalJndiNames(
            pcfg( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "false" ) ) );
    }

    public void testPermitNonlocalSyspropTruePcfgNull()
    {
        String saved = System.getProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES );
        try
        {
            System.setProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "true" );
            assertTrue( ReferenceableUtils.permitNonlocalJndiNames( null ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, saved ); }
    }

    public void testPermitNonlocalSyspropFalsePcfgNull()
    {
        String saved = System.getProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES );
        try
        {
            System.setProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "false" );
            assertFalse( ReferenceableUtils.permitNonlocalJndiNames( null ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, saved ); }
    }

    /** sysprop=false, pcfg=true → false  (sysprop false is unconditionally disabling) */
    public void testPermitNonlocalSyspropFalseOverridesPcfgTrue()
    {
        String saved = System.getProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES );
        try
        {
            System.setProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "false" );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "true" );
            assertFalse( ReferenceableUtils.permitNonlocalJndiNames( cfg ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, saved ); }
    }

    /** sysprop=true, pcfg=false → false  (pcfg false overrides even sysprop true) */
    public void testPermitNonlocalPcfgFalseOverridesSyspropTrue()
    {
        String saved = System.getProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES );
        try
        {
            System.setProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "true" );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "false" );
            assertFalse( ReferenceableUtils.permitNonlocalJndiNames( cfg ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, saved ); }
    }

    /** sysprop=true, pcfg=true → true */
    public void testPermitNonlocalSyspropTruePcfgTrue()
    {
        String saved = System.getProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES );
        try
        {
            System.setProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "true" );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, "true" );
            assertTrue( ReferenceableUtils.permitNonlocalJndiNames( cfg ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.PERMIT_NONLOCAL_JNDI_NAMES, saved ); }
    }

    // --- supportReferenceRemoteFactoryClassLocation ---

    public void testSupportRemoteFactoryDefaultFalse()
    { assertFalse( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation( null ) ); }

    public void testSupportRemoteFactoryPcfgTrue()
    {
        assertTrue( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation(
            pcfg( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, "true" ) ) );
    }

    public void testSupportRemoteFactoryPcfgFalse()
    {
        assertFalse( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation(
            pcfg( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, "false" ) ) );
    }

    public void testSupportRemoteFactorySyspropTruePcfgNull()
    {
        String saved = System.getProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION );
        try
        {
            System.setProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, "true" );
            assertTrue( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation( null ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, saved ); }
    }

    public void testSupportRemoteFactorySyspropFalsePcfgNull()
    {
        String saved = System.getProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION );
        try
        {
            System.setProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, "false" );
            assertFalse( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation( null ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, saved ); }
    }

    // --- acceptDeserializedInitialContextEnvironment ---

    public void testAcceptDeserializedDefaultFalse()
    { assertFalse( ReferenceableUtils.acceptDeserializedInitialContextEnvironment( null ) ); }

    public void testAcceptDeserializedPcfgTrue()
    {
        assertTrue( ReferenceableUtils.acceptDeserializedInitialContextEnvironment(
            pcfg( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, "true" ) ) );
    }

    public void testAcceptDeserializedPcfgFalse()
    {
        assertFalse( ReferenceableUtils.acceptDeserializedInitialContextEnvironment(
            pcfg( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, "false" ) ) );
    }

    public void testAcceptDeserializedSyspropTruePcfgNull()
    {
        String saved = System.getProperty( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT );
        try
        {
            System.setProperty( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, "true" );
            assertTrue( ReferenceableUtils.acceptDeserializedInitialContextEnvironment( null ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, saved ); }
    }

    // ==========================================
    // referenceToObject
    // ==========================================

    /** Explicit non-null whitelist containing the factory → succeeds */
    public void testReferenceToObjectExplicitWhitelist() throws NamingException
    {
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        Set whitelist = Collections.singleton( ALPHA_FACTORY );
        Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, whitelist );
        assertEquals( "ALPHA", result );
    }

    /** Explicit null whitelist (Set overload) → any factory accepted */
    public void testReferenceToObjectNullSetWhitelistAcceptsAny() throws NamingException
    {
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, (Set) null );
        assertEquals( "ALPHA", result );
    }

    /** Factory not in explicit whitelist → NamingException */
    public void testReferenceToObjectFactoryNotInWhitelistThrows()
    {
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        Set whitelist = Collections.singleton( BETA_FACTORY );
        try
        {
            ReferenceableUtils.referenceToObject( ref, null, null, null, whitelist );
            fail( "Expected NamingException: factory not in whitelist" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Null factoryClassName → NamingException regardless of whitelist */
    public void testReferenceToObjectNullFactoryClassThrows()
    {
        Reference ref = new Reference( "java.lang.String", null, null );
        try
        {
            ReferenceableUtils.referenceToObject( ref, null, null, null, (Set) null );
            fail( "Expected NamingException: null factory class name" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Whitelist supplied via PropertiesConfig */
    public void testReferenceToObjectWhitelistViaPcfg() throws NamingException
    {
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
        Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, cfg );
        assertEquals( "ALPHA", result );
    }

    /** No pcfg, no sysprop whitelist → NamingException (mandatory whitelist missing) */
    public void testReferenceToObjectNoWhitelistConfiguredThrows()
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.clearProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            ReferenceableUtils.referenceToObject( ref, null, null, null, (PropertiesConfig) null );
            fail( "Expected NamingException: no whitelist configured" );
        }
        catch (NamingException e) { /* expected */ }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /** Whitelist supplied via system property */
    public void testReferenceToObjectWhitelistViaSysprop() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, (PropertiesConfig) null );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * When sysprop and pcfg whitelists differ, the intersection is used.
     * sysprop = {ALPHA, BETA}, pcfg = {BETA} → intersection = {BETA}
     * ALPHA factory should be rejected; BETA factory should be accepted.
     */
    public void testReferenceToObjectWhitelistIntersection() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY + "," + BETA_FACTORY );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, BETA_FACTORY );

            // ALPHA is only in sysprop list, not in intersection → rejected
            Reference refAlpha = makeRef( "java.lang.String", ALPHA_FACTORY );
            try
            {
                ReferenceableUtils.referenceToObject( refAlpha, null, null, null, cfg );
                fail( "Expected NamingException: ALPHA not in intersection whitelist" );
            }
            catch (NamingException e) { /* expected */ }

            // BETA is in both → accepted
            Reference refBeta = makeRef( "java.lang.String", BETA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( refBeta, null, null, null, cfg );
            assertEquals( "BETA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * When sysprop and pcfg have identical whitelists, the result is that set
     * (no intersection narrowing, no warning).
     */
    public void testReferenceToObjectWhitelistSyspropAndPcfgAgree() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, cfg );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /** No-pcfg no-set overload uses mandatory whitelist from sysprop when present */
    public void testReferenceToObjectNoArgsOverloadUsesSysprop() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    // ==========================================
    // appendToReference / extractNestedReference (deprecated)
    // ==========================================

    public void testAppendAndExtractSingleNestedReference() throws NamingException
    {
        Reference inner = new Reference( "com.example.Foo", "com.example.FooFactory", null );
        inner.add( new StringRefAddr( "key1", "value1" ) );
        inner.add( new StringRefAddr( "key2", "value2" ) );

        Reference outer = new Reference( "com.example.Bar" );
        ReferenceableUtils.appendToReference( outer, inner );

        ReferenceableUtils.ExtractRec rec = ReferenceableUtils.extractNestedReference( outer, 0 );
        Reference extracted = rec.ref;

        assertEquals( "com.example.Foo", extracted.getClassName() );
        assertEquals( "com.example.FooFactory", extracted.getFactoryClassName() );
        // factoryClassLocation was null; appendToReference stores the literal "null"
        assertNull( ReferenceableUtils.literalNullToNull( extracted.getFactoryClassLocation() ) );
        assertEquals( 2, extracted.size() );
        assertEquals( "value1", extracted.get( "key1" ).getContent() );
        assertEquals( "value2", extracted.get( "key2" ).getContent() );
        // rec.index should point past all the appended entries
        assertEquals( outer.size(), rec.index );
    }

    /** Append two references in sequence; verify each can be extracted at the correct index. */
    public void testAppendAndExtractMultipleNestedReferences() throws NamingException
    {
        Reference inner1 = new Reference( "com.example.Foo", "com.example.FooFactory", null );
        inner1.add( new StringRefAddr( "foo-key", "foo-val" ) );

        Reference inner2 = new Reference( "com.example.Bar", "com.example.BarFactory", null );
        inner2.add( new StringRefAddr( "bar-key", "bar-val" ) );

        Reference outer = new Reference( "com.example.Outer" );
        ReferenceableUtils.appendToReference( outer, inner1 );
        ReferenceableUtils.appendToReference( outer, inner2 );

        ReferenceableUtils.ExtractRec rec1 = ReferenceableUtils.extractNestedReference( outer, 0 );
        assertEquals( "com.example.Foo", rec1.ref.getClassName() );
        assertEquals( "foo-val", rec1.ref.get( "foo-key" ).getContent() );

        ReferenceableUtils.ExtractRec rec2 = ReferenceableUtils.extractNestedReference( outer, rec1.index );
        assertEquals( "com.example.Bar", rec2.ref.getClassName() );
        assertEquals( "bar-val", rec2.ref.get( "bar-key" ).getContent() );

        assertEquals( outer.size(), rec2.index );
    }

    /** A reference with no RefAddrs round-trips correctly. */
    public void testAppendAndExtractEmptyNestedReference() throws NamingException
    {
        Reference inner = new Reference( "com.example.Empty", "com.example.EmptyFactory", null );
        Reference outer = new Reference( "com.example.Outer" );
        ReferenceableUtils.appendToReference( outer, inner );

        ReferenceableUtils.ExtractRec rec = ReferenceableUtils.extractNestedReference( outer, 0 );
        assertEquals( "com.example.Empty", rec.ref.getClassName() );
        assertEquals( 0, rec.ref.size() );
        assertEquals( outer.size(), rec.index );
    }
}
