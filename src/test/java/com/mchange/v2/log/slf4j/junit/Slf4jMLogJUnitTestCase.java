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

package com.mchange.v2.log.slf4j.junit;

import com.mchange.v2.log.*;
import com.mchange.v2.log.slf4j.*;
import junit.framework.TestCase;
import org.slf4j.*;

/*
 *  Note that the loggers required by these tests are configured in src/test/resources/logback.xml
 */
public final class Slf4jMLogJUnitTestCase extends TestCase 
{
    final static String TRACE_LOGGER = "TraceLogger";
    final static String DEBUG_LOGGER = "DebugLogger";
    final static String INFO_LOGGER  = "InfoLogger";
    final static String WARN_LOGGER  = "WarnLogger";
    final static String ERROR_LOGGER = "ErrorLogger";

    Slf4jMLog smlog;

    @Override
    protected void setUp() throws Exception
    {
	smlog = new Slf4jMLog();
	Logger warn = LoggerFactory.getLogger(WARN_LOGGER);
	assert( warn.isWarnEnabled() && !warn.isInfoEnabled() );

	Logger trace = LoggerFactory.getLogger(TRACE_LOGGER);
	assert( warn.isTraceEnabled() && !warn.isDebugEnabled() );
    }

    public void testTraceLoggerLevel()
    { assertEquals("SLF4J TRACE is expected to map to MLevel.FINEST.", MLevel.FINEST, smlog.getMLogger(TRACE_LOGGER).getLevel() ); }

    public void testDebugLoggerLevel()
    { assertEquals("SLF4J DEBUG is expected to map to MLevel.FINER.", MLevel.FINER, smlog.getMLogger(DEBUG_LOGGER).getLevel() ); }

    public void testInfoLoggerLevel()
    { assertEquals("SLF4J INFO is expected to map to MLevel.INFO.", MLevel.INFO, smlog.getMLogger(INFO_LOGGER).getLevel() ); }

    public void testWarnLoggerLevel()
    { assertEquals("SLF4J WARN is expected to map to MLevel.WARNING.", MLevel.WARNING, smlog.getMLogger(WARN_LOGGER).getLevel() ); }

    public void testErrorLoggerLevel()
    { assertEquals("SLF4J ERROR is expected to map to MLevel.SEVERE.", MLevel.SEVERE, smlog.getMLogger(ERROR_LOGGER).getLevel() ); }

    public void testErrorLoggerLoggability()
    {
	MLogger logger = smlog.getMLogger(ERROR_LOGGER);

	assertFalse("SLF4J " + ERROR_LOGGER + " should not be loggable to MLevel.OFF", logger.isLoggable( MLevel.OFF ) );
	assertFalse("SLF4J " + ERROR_LOGGER + " should be loggable to MLevel.ALL",     logger.isLoggable( MLevel.ALL ) );

	assertTrue("SLF4J " + ERROR_LOGGER + " should be loggable to MLevel.SEVERE.",       logger.isLoggable( MLevel.SEVERE ) );
	assertFalse("SLF4J " + ERROR_LOGGER + " should not be loggable to MLevel.WARNING.", logger.isLoggable( MLevel.WARNING ) );
	assertFalse("SLF4J " + ERROR_LOGGER + " should not be loggable to MLevel.INFO.",    logger.isLoggable( MLevel.INFO ) );
	assertFalse("SLF4J " + ERROR_LOGGER + " should not be loggable to MLevel.FINE.",    logger.isLoggable( MLevel.FINE ) );
	assertFalse("SLF4J " + ERROR_LOGGER + " should not be loggable to MLevel.FINE.",    logger.isLoggable( MLevel.FINER ) );
	assertFalse("SLF4J " + ERROR_LOGGER + " should not be loggable to MLevel.FINEST",   logger.isLoggable( MLevel.FINEST ) );
    }

    public void testWarnLoggerLoggability()
    {
	MLogger logger = smlog.getMLogger(WARN_LOGGER);

	assertFalse("SLF4J " + WARN_LOGGER + " should not be loggable to MLevel.OFF", logger.isLoggable( MLevel.OFF ) );
	assertFalse("SLF4J " + WARN_LOGGER + " should be loggable to MLevel.ALL",     logger.isLoggable( MLevel.ALL ) );

	assertTrue("SLF4J " + WARN_LOGGER + " should be loggable to MLevel.SEVERE.",     logger.isLoggable( MLevel.SEVERE ) );
	assertTrue("SLF4J " + WARN_LOGGER + " should be loggable to MLevel.WARNING.",    logger.isLoggable( MLevel.WARNING ) );
	assertFalse("SLF4J " + WARN_LOGGER + " should not be loggable to MLevel.INFO.",  logger.isLoggable( MLevel.INFO ) );
	assertFalse("SLF4J " + WARN_LOGGER + " should not be loggable to MLevel.FINE.",  logger.isLoggable( MLevel.FINE ) );
	assertFalse("SLF4J " + WARN_LOGGER + " should not be loggable to MLevel.FINER.", logger.isLoggable( MLevel.FINER ) );
	assertFalse("SLF4J " + WARN_LOGGER + " should not be loggable to MLevel.FINEST", logger.isLoggable( MLevel.FINEST ) );
    }

    public void testInfoLoggerLoggability()
    {
	MLogger logger = smlog.getMLogger(INFO_LOGGER);

	assertFalse("SLF4J " + INFO_LOGGER + " should not be loggable to MLevel.OFF", logger.isLoggable( MLevel.OFF ) );
	assertFalse("SLF4J " + INFO_LOGGER + " should be loggable to MLevel.ALL",     logger.isLoggable( MLevel.ALL ) );

	assertTrue("SLF4J " + INFO_LOGGER + " should be loggable to MLevel.SEVERE.",     logger.isLoggable( MLevel.SEVERE ) );
	assertTrue("SLF4J " + INFO_LOGGER + " should be loggable to MLevel.WARNING.",    logger.isLoggable( MLevel.WARNING ) );
	assertTrue("SLF4J " + INFO_LOGGER + " should be loggable to MLevel.INFO.",       logger.isLoggable( MLevel.INFO ) );
	assertFalse("SLF4J " + INFO_LOGGER + " should not be loggable to MLevel.FINE.",  logger.isLoggable( MLevel.FINE ) );
	assertFalse("SLF4J " + INFO_LOGGER + " should not be loggable to MLevel.FINER.", logger.isLoggable( MLevel.FINER ) );
	assertFalse("SLF4J " + INFO_LOGGER + " should not be loggable to MLevel.FINEST", logger.isLoggable( MLevel.FINEST ) );
    }

    public void testDebugLoggerLoggability()
    {
	MLogger logger = smlog.getMLogger(DEBUG_LOGGER);

	assertFalse("SLF4J " + DEBUG_LOGGER + " should not be loggable to MLevel.OFF", logger.isLoggable( MLevel.OFF ) );
	assertFalse("SLF4J " + DEBUG_LOGGER + " should be loggable to MLevel.ALL",     logger.isLoggable( MLevel.ALL ) );

	assertTrue("SLF4J " + DEBUG_LOGGER + " should be loggable to MLevel.SEVERE.",     logger.isLoggable( MLevel.SEVERE ) );
	assertTrue("SLF4J " + DEBUG_LOGGER + " should be loggable to MLevel.WARNING.",    logger.isLoggable( MLevel.WARNING ) );
	assertTrue("SLF4J " + DEBUG_LOGGER + " should be loggable to MLevel.INFO.",       logger.isLoggable( MLevel.INFO ) );
	assertTrue("SLF4J " + DEBUG_LOGGER + " should be loggable to MLevel.FINE.",       logger.isLoggable( MLevel.FINE ) );
	assertTrue("SLF4J " + DEBUG_LOGGER + " should be loggable to MLevel.FINER.",      logger.isLoggable( MLevel.FINER ) );
	assertFalse("SLF4J " + DEBUG_LOGGER + " should not be loggable to MLevel.FINEST", logger.isLoggable( MLevel.FINEST ) );
    }

    public void testTraceLoggerLoggability()
    {
	MLogger logger = smlog.getMLogger(TRACE_LOGGER);

	assertFalse("SLF4J " + TRACE_LOGGER + " should not be loggable to MLevel.OFF", logger.isLoggable( MLevel.OFF ) );
	assertFalse("SLF4J " + TRACE_LOGGER + " should be loggable to MLevel.ALL",     logger.isLoggable( MLevel.ALL ) );

	assertTrue("SLF4J " + TRACE_LOGGER + " should be loggable to MLevel.SEVERE.",  logger.isLoggable( MLevel.SEVERE ) );
	assertTrue("SLF4J " + TRACE_LOGGER + " should be loggable to MLevel.WARNING.", logger.isLoggable( MLevel.WARNING ) );
	assertTrue("SLF4J " + TRACE_LOGGER + " should be loggable to MLevel.INFO.",    logger.isLoggable( MLevel.INFO ) );
	assertTrue("SLF4J " + TRACE_LOGGER + " should be loggable to MLevel.FINE.",    logger.isLoggable( MLevel.FINE ) );
	assertTrue("SLF4J " + TRACE_LOGGER + " should be loggable to MLevel.FINER.",   logger.isLoggable( MLevel.FINER ) );
	assertTrue("SLF4J " + TRACE_LOGGER + " should be loggable to MLevel.FINEST",   logger.isLoggable( MLevel.FINEST ) );
    }
}

