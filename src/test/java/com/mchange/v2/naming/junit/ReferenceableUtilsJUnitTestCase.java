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
import com.mchange.v2.naming.AnyNameNameGuard;
import com.mchange.v2.naming.ApparentlyLocalNameGuard;
import com.mchange.v2.naming.ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard;
import com.mchange.v2.naming.FirstComponentIsJavaIdentifierNameGuard;
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
    // AnyNameNameGuard
    // Accepts every name unconditionally.
    // ==========================================

    public void testAnyNameGuardAcceptsAnyString()
    {
        AnyNameNameGuard guard = new AnyNameNameGuard();
        assertTrue( guard.nameIsAcceptable( "" ) );
        assertTrue( guard.nameIsAcceptable( "java:comp/env" ) );
        assertTrue( guard.nameIsAcceptable( "ldap://example.com/myDS" ) );
        assertTrue( guard.nameIsAcceptable( "anything at all" ) );
    }

    public void testAnyNameGuardAcceptsAnyName() throws InvalidNameException
    {
        AnyNameNameGuard guard = new AnyNameNameGuard();
        assertTrue( guard.nameIsAcceptable( new CompositeName( "" ) ) );
        assertTrue( guard.nameIsAcceptable( new CompositeName( "java:comp/env" ) ) );
        assertTrue( guard.nameIsAcceptable( new CompositeName( "ldap://example.com" ) ) );
    }

    public void testAnyNameGuardDescriptionNonNull()
    { assertNotNull( new AnyNameNameGuard().onlyAcceptableWhen() ); }

    // ==========================================
    // ApparentlyLocalNameGuard
    // String: must start with "java:"
    // Name:   first component must start with "java:"
    // ==========================================

    public void testApparentlyLocalNameGuardStringLocal()
    {
        ApparentlyLocalNameGuard guard = new ApparentlyLocalNameGuard();
        assertTrue( guard.nameIsAcceptable( "java:comp/env/myDS" ) );
        assertTrue( guard.nameIsAcceptable( "java:" ) );
    }

    public void testApparentlyLocalNameGuardStringNonLocal()
    {
        ApparentlyLocalNameGuard guard = new ApparentlyLocalNameGuard();
        assertFalse( guard.nameIsAcceptable( "" ) );
        assertFalse( guard.nameIsAcceptable( "ldap://example.com" ) );
        assertFalse( guard.nameIsAcceptable( "jdbc/myDS" ) );
        assertFalse( guard.nameIsAcceptable( "jms/topic" ) );
    }

    public void testApparentlyLocalNameGuardNameLocalFirstComponent() throws InvalidNameException
    {
        ApparentlyLocalNameGuard guard = new ApparentlyLocalNameGuard();
        // CompositeName splits on "/", so first component of "java:comp/env" is "java:comp"
        assertTrue( guard.nameIsAcceptable( new CompositeName( "java:comp/env" ) ) );
        assertTrue( guard.nameIsAcceptable( new CompositeName( "java:" ) ) );
    }

    public void testApparentlyLocalNameGuardNameNonLocal() throws InvalidNameException
    {
        ApparentlyLocalNameGuard guard = new ApparentlyLocalNameGuard();
        assertFalse( guard.nameIsAcceptable( new CompositeName( "" ) ) );          // empty Name
        assertFalse( guard.nameIsAcceptable( new CompositeName( "ldap://example.com" ) ) ); // first comp "ldap:"
        assertFalse( guard.nameIsAcceptable( new CompositeName( "jdbc/myDS" ) ) ); // first comp "jdbc"
    }

    public void testApparentlyLocalNameGuardDescriptionNonNull()
    { assertNotNull( new ApparentlyLocalNameGuard().onlyAcceptableWhen() ); }

    // ==========================================
    // FirstComponentIsJavaIdentifierNameGuard
    // String: text before the first "/" must be a valid Java (qualified) name.
    // Name:   first component must be a valid Java name.
    // ==========================================

    public void testFirstComponentJavaIdentifierGuardStringValid()
    {
        FirstComponentIsJavaIdentifierNameGuard guard = new FirstComponentIsJavaIdentifierNameGuard();
        assertTrue( guard.nameIsAcceptable( "jdbc/myDS" ) );   // first comp = "jdbc"
        assertTrue( guard.nameIsAcceptable( "jms/topic" ) );   // first comp = "jms"
        assertTrue( guard.nameIsAcceptable( "jdbc" ) );        // no slash; whole string = "jdbc"
    }

    public void testFirstComponentJavaIdentifierGuardStringInvalid()
    {
        FirstComponentIsJavaIdentifierNameGuard guard = new FirstComponentIsJavaIdentifierNameGuard();
        // "java:comp" before "/" contains a colon → not a valid Java name
        assertFalse( guard.nameIsAcceptable( "java:comp/env" ) );
        // bare "java:" has a colon → invalid
        assertFalse( guard.nameIsAcceptable( "java:" ) );
        // "ldap:" before first "/" has colon → invalid
        assertFalse( guard.nameIsAcceptable( "ldap://example.com" ) );
        assertFalse( guard.nameIsAcceptable( "" ) );
    }

    public void testFirstComponentJavaIdentifierGuardNameValid() throws InvalidNameException
    {
        FirstComponentIsJavaIdentifierNameGuard guard = new FirstComponentIsJavaIdentifierNameGuard();
        // CompositeName splits on "/"; first component is "jdbc"
        assertTrue( guard.nameIsAcceptable( new CompositeName( "jdbc/myDS" ) ) );
        assertTrue( guard.nameIsAcceptable( new CompositeName( "jms" ) ) );
    }

    public void testFirstComponentJavaIdentifierGuardNameInvalid() throws InvalidNameException
    {
        FirstComponentIsJavaIdentifierNameGuard guard = new FirstComponentIsJavaIdentifierNameGuard();
        // First component of "java:comp/env" is "java:comp" → colon → invalid
        assertFalse( guard.nameIsAcceptable( new CompositeName( "java:comp/env" ) ) );
        // Empty Name
        assertFalse( guard.nameIsAcceptable( new CompositeName( "" ) ) );
    }

    public void testFirstComponentJavaIdentifierGuardDescriptionNonNull()
    { assertNotNull( new FirstComponentIsJavaIdentifierNameGuard().onlyAcceptableWhen() ); }

    // ==========================================
    // ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard
    // Accepts if EITHER ApparentlyLocalNameGuard OR
    // FirstComponentIsJavaIdentifierNameGuard accepts.
    // ==========================================

    public void testApparentlyLocalOrJavaIdentifierGuardStringAccepted()
    {
        ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard guard =
            new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard();
        // Accepted by ApparentlyLocal
        assertTrue( guard.nameIsAcceptable( "java:comp/env" ) );
        // Accepted by FirstComponentIsJavaIdentifier
        assertTrue( guard.nameIsAcceptable( "jdbc/myDS" ) );
        assertTrue( guard.nameIsAcceptable( "jms/topic" ) );
    }

    public void testApparentlyLocalOrJavaIdentifierGuardStringRejected()
    {
        ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard guard =
            new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard();
        // Neither guard accepts these
        assertFalse( guard.nameIsAcceptable( "ldap://example.com" ) );
        assertFalse( guard.nameIsAcceptable( "" ) );
    }

    public void testApparentlyLocalOrJavaIdentifierGuardNameAccepted() throws InvalidNameException
    {
        ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard guard =
            new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard();
        // First component "java:comp" starts with "java:" → ApparentlyLocal accepts
        assertTrue( guard.nameIsAcceptable( new CompositeName( "java:comp/env" ) ) );
        // First component "jdbc" is valid Java name → FirstComponentIsJavaIdentifier accepts
        assertTrue( guard.nameIsAcceptable( new CompositeName( "jdbc/myDS" ) ) );
    }

    public void testApparentlyLocalOrJavaIdentifierGuardNameRejected() throws InvalidNameException
    {
        ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard guard =
            new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard();
        // First component "ldap:" has colon → neither accepts
        assertFalse( guard.nameIsAcceptable( new CompositeName( "ldap://example.com" ) ) );
        assertFalse( guard.nameIsAcceptable( new CompositeName( "" ) ) );
    }

    public void testApparentlyLocalOrJavaIdentifierGuardDescriptionNonNull()
    { assertNotNull( new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard().onlyAcceptableWhen() ); }

    // ==========================================
    // assertAcceptableName
    // Default NameGuard is ApparentlyLocalNameGuard.
    // NAME_GUARD_CLASS_NAME config overrides the guard.
    // When pcfg is non-null, pcfg is consulted; otherwise the system property is consulted.
    // ==========================================

    /** Default guard (ApparentlyLocalNameGuard): local String passes. */
    public void testAssertAcceptableNameDefaultGuardStringLocalPasses() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            ReferenceableUtils.assertAcceptableName( "java:comp/env/myDS", null );
        }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** Default guard: non-local String throws NamingException. */
    public void testAssertAcceptableNameDefaultGuardStringNonLocalThrows()
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            ReferenceableUtils.assertAcceptableName( "ldap://example.com", null );
            fail( "Expected NamingException: non-local name rejected by default guard" );
        }
        catch (NamingException e) { /* expected */ }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** Default guard: Name with "java:" first component passes. */
    public void testAssertAcceptableNameDefaultGuardNameLocalPasses() throws NamingException, InvalidNameException
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            ReferenceableUtils.assertAcceptableName( new CompositeName( "java:comp/env" ), null );
        }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** Default guard: Name without "java:" first component throws NamingException. */
    public void testAssertAcceptableNameDefaultGuardNameNonLocalThrows() throws InvalidNameException
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            ReferenceableUtils.assertAcceptableName( new CompositeName( "ldap://example.com" ), null );
            fail( "Expected NamingException: non-local Name rejected by default guard" );
        }
        catch (NamingException e) { /* expected */ }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** Unknown type (not String, not Name) always throws NamingException. */
    public void testAssertAcceptableNameUnknownTypeThrows()
    {
        try
        {
            ReferenceableUtils.assertAcceptableName( Integer.valueOf(42), null );
            fail( "Expected NamingException: unknown type" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** AnyNameNameGuard configured via pcfg: any String passes. */
    public void testAssertAcceptableNameAnyGuardViaPcfgAcceptsAnyString() throws NamingException
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME, AnyNameNameGuard.class.getName() );
        ReferenceableUtils.assertAcceptableName( "ldap://example.com", cfg );
        ReferenceableUtils.assertAcceptableName( "", cfg );
        ReferenceableUtils.assertAcceptableName( "java:comp/env", cfg );
    }

    /** AnyNameNameGuard configured via pcfg: any Name passes. */
    public void testAssertAcceptableNameAnyGuardViaPcfgAcceptsAnyName() throws NamingException, InvalidNameException
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME, AnyNameNameGuard.class.getName() );
        ReferenceableUtils.assertAcceptableName( new CompositeName( "ldap://example.com" ), cfg );
        ReferenceableUtils.assertAcceptableName( new CompositeName( "" ), cfg );
    }

    /** AnyNameNameGuard configured via system property: any String passes. */
    public void testAssertAcceptableNameAnyGuardViaSysprop() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.setProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, AnyNameNameGuard.class.getName() );
            ReferenceableUtils.assertAcceptableName( "ldap://example.com", null );
            ReferenceableUtils.assertAcceptableName( "", null );
        }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** FirstComponentIsJavaIdentifierNameGuard via pcfg: "jdbc/..." passes, "java:..." throws. */
    public void testAssertAcceptableNameFirstComponentGuardViaPcfg() throws NamingException
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                     FirstComponentIsJavaIdentifierNameGuard.class.getName() );
        // "jdbc/myDS" first component "jdbc" is a valid Java name → passes
        ReferenceableUtils.assertAcceptableName( "jdbc/myDS", cfg );

        // "java:comp/env" first component "java:comp" has a colon → throws
        try
        {
            ReferenceableUtils.assertAcceptableName( "java:comp/env", cfg );
            fail( "Expected NamingException: 'java:comp/env' rejected by FirstComponentIsJavaIdentifier guard" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Configuring a non-existent class name throws NamingException (not InternalError). */
    public void testAssertAcceptableNameBadGuardClassThrows()
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                     "com.example.DoesNotExistNameGuard" );
        try
        {
            ReferenceableUtils.assertAcceptableName( "java:comp/env", cfg );
            fail( "Expected NamingException: non-existent NameGuard class" );
        }
        catch (NamingException e) { /* expected */ }
    }

    // ==========================================
    // referenceToObject – name-guard integration
    // referenceToObject calls assertAcceptableName when name != null.
    // ==========================================

    /** Non-null local name passes the default guard and dereferencing succeeds. */
    public void testReferenceToObjectWithLocalNameSucceeds() throws NamingException, InvalidNameException
    {
        String savedWl    = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        String savedGuard = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject(
                ref, new CompositeName( "java:comp/env/myDS" ), null, null );
            assertEquals( "ALPHA", result );
        }
        finally
        {
            restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, savedWl );
            restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, savedGuard );
        }
    }

    /** Non-null non-local name is rejected by the default guard → NamingException. */
    public void testReferenceToObjectWithNonLocalNameThrows() throws InvalidNameException
    {
        String savedWl    = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        String savedGuard = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            ReferenceableUtils.referenceToObject(
                ref, new CompositeName( "ldap://example.com/myDS" ), null, null );
            fail( "Expected NamingException: non-local name rejected by default guard" );
        }
        catch (NamingException e) { /* expected */ }
        finally
        {
            restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, savedWl );
            restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, savedGuard );
        }
    }

    /** Non-local name succeeds when AnyNameNameGuard is configured via pcfg. */
    public void testReferenceToObjectWithNonLocalNameAndAnyGuardSucceeds()
        throws NamingException, InvalidNameException
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                     AnyNameNameGuard.class.getName() );
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        Set whitelist = Collections.singleton( ALPHA_FACTORY );
        Object result = ReferenceableUtils.referenceToObject(
            ref, new CompositeName( "ldap://example.com/myDS" ), null, null, whitelist, cfg );
        assertEquals( "ALPHA", result );
    }

    // ==========================================
    // falseBiasedLookup logic (exercised via
    // supportReferenceRemoteFactoryClassLocation and
    // acceptDeserializedInitialContextEnvironment)
    // ==========================================

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
    // referenceToObject – whitelist
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
