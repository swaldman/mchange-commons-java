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

package com.mchange.v2.log.log4j2;

import static com.mchange.v2.log.LogUtils.createMessage;
import static com.mchange.v2.log.LogUtils.formatMessage;

import com.mchange.v2.log.LogUtils;
import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;
import com.mchange.v2.log.NullMLogger;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.message.ObjectMessage;

public final class Log4j2MLog extends MLog
{

    final static String CHECK_CLASS = "org.apache.logging.log4j.LogManager";

    public Log4j2MLog() throws ClassNotFoundException
    {
        Class.forName(Log4j2MLog.CHECK_CLASS);
    }

    @Override
    public MLogger getMLogger(String name)
    {
        Logger lg = LogManager.getLogger(name);
        if(lg == null)
        {
            fallbackWarn(" with name '" + name + "'");
            return NullMLogger.instance();
        } else return new Log4jMLogger(lg);
    }

    @Override
    public MLogger getMLogger(Class cl)
    {
        Logger lg = LogManager.getLogger(cl);
        if(lg == null)
        {
            fallbackWarn(" for class '" + cl.getName() + "'");
            return NullMLogger.instance();
        } else return new Log4jMLogger(lg);
    }

    @Override
    public MLogger getMLogger()
    {
        Logger lg = LogManager.getRootLogger();
        if(lg == null)
        {
            fallbackWarn(" (root logger)");
            return NullMLogger.instance();
        } else return new Log4jMLogger(lg);
    }

    private void fallbackWarn(String subst)
    {
        MLog.getLogger()
	    .warning("Could not create or find log4j Logger" + subst + ". " + "Using NullMLogger. All messages sent to this" + "logger will be silently ignored. You might want to fix this.");
    }

    private final static class Log4jMLogger implements MLogger
    {
        final static String FQCN = Log4jMLogger.class.getName();

        // protected by this' lock
        MLevel myLevel = null;

        final Logger logger;

        Log4jMLogger(Logger logger)
        {
            this.logger = logger;
        }

        private static MLevel guessMLevel(Level lvl)
        {
            if(lvl == null) return null;
            else if(lvl == Level.ALL) return MLevel.ALL;
            else if(lvl == Level.TRACE) return MLevel.FINEST;
            else if(lvl == Level.DEBUG) return MLevel.FINER;
            else if(lvl == Level.ERROR) return MLevel.SEVERE;
            else if(lvl == Level.FATAL) return MLevel.SEVERE;
            else if(lvl == Level.INFO) return MLevel.INFO;
            else if(lvl == Level.OFF) return MLevel.OFF;
            else if(lvl == Level.WARN) return MLevel.WARNING;
            else throw new IllegalArgumentException("Unknown level: " + lvl);
        }

        private static Level level(MLevel lvl)
        {
            if(lvl == null) return null;
            else if(lvl == MLevel.ALL) return Level.ALL;
            else if(lvl == MLevel.CONFIG) return Level.FINE;
            else if(lvl == MLevel.FINE) return Level.DEBUG;
            else if(lvl == MLevel.FINER) return Level.DEBUG;
            else if(lvl == MLevel.FINEST) return Level.TRACE;
            else if(lvl == MLevel.INFO) return Level.INFO;
            else if(lvl == MLevel.OFF) return Level.OFF;
            else if(lvl == MLevel.SEVERE) return Level.ERROR;
            else if(lvl == MLevel.WARNING) return Level.WARN;
            else throw new IllegalArgumentException("Unknown MLevel: " + lvl);
        }

        @Override
        public ResourceBundle getResourceBundle()
        {
            return null;
        }

        @Override
        public String getResourceBundleName()
        {
            return null;
        }

        @Override
        public void setFilter(Object java14Filter) throws SecurityException
        {
            warning("setFilter() not supported by MLogger " + this.getClass().getName());
        }

        @Override
        public Object getFilter()
        {
            return null;
        }

        private void log(Level lvl, Object msg, Throwable t)
        {
            if(this.logger.isEnabled(lvl))
            {
                ((org.apache.logging.log4j.core.Logger) this.logger)
                        .logMessage(Log4jMLogger.FQCN, lvl, null, new ObjectMessage(msg), t);
            }
        }

        @Override
        public void log(MLevel l, String msg)
        {
            log(level(l), msg, null);
        }

        @Override
        public void log(MLevel l, String msg, Object param)
        {
            log(level(l), (msg != null ? MessageFormat.format(msg, new Object[]{param}) : null), null);
        }

        @Override
        public void log(MLevel l, String msg, Object[] params)
        {
            log(level(l), (msg != null ? MessageFormat.format(msg, params) : null), null);
        }

        @Override
        public void log(MLevel l, String msg, Throwable t)
        {
            log(level(l), msg, t);
        }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg)
        {
            log(level(l), createMessage(srcClass, srcMeth, msg), null);
        }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param)
        {
            log(level(l),
                createMessage(srcClass, srcMeth, (msg != null ? MessageFormat.format(msg, new Object[]{param}) : null)),
                null);
        }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params)
        {
            log(level(l),
                createMessage(srcClass, srcMeth, (msg != null ? MessageFormat.format(msg, params) : null)),
                null);
        }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t)
        {
            log(level(l), createMessage(srcClass, srcMeth, msg), t);
        }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg)
        {
            log(level(l), createMessage(srcClass, srcMeth, formatMessage(rb, msg, null)), null);
        }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param)
        {
            log(level(l), createMessage(srcClass, srcMeth, formatMessage(rb, msg, new Object[]{param})), null);
        }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params)
        {
            log(level(l), createMessage(srcClass, srcMeth, formatMessage(rb, msg, params)), null);
        }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t)
        {
            log(level(l), createMessage(srcClass, srcMeth, formatMessage(rb, msg, null)), t);
        }

        @Override
        public void entering(String srcClass, String srcMeth)
        {
            log(Level.DEBUG, createMessage(srcClass, srcMeth, "entering method."), null);
        }

        @Override
        public void entering(String srcClass, String srcMeth, Object param)
        {
            log(Level.DEBUG, createMessage(srcClass, srcMeth, "entering method... param: " + param.toString()), null);
        }

        @Override
        public void entering(String srcClass, String srcMeth, Object params[])
        {
            log(Level.DEBUG,
                createMessage(srcClass, srcMeth, "entering method... " + LogUtils.createParamsList(params)),
                null);
        }

        @Override
        public void exiting(String srcClass, String srcMeth)
        {
            log(Level.DEBUG, createMessage(srcClass, srcMeth, "exiting method."), null);
        }

        @Override
        public void exiting(String srcClass, String srcMeth, Object result)
        {
            log(Level.DEBUG, createMessage(srcClass, srcMeth, "exiting method... result: " + result.toString()), null);
        }

        @Override
        public void throwing(String srcClass, String srcMeth, Throwable t)
        {
            log(Level.DEBUG, createMessage(srcClass, srcMeth, "throwing exception... "), t);
        }

        @Override
        public void severe(String msg)
        {
            log(Level.ERROR, msg, null);
        }

        @Override
        public void warning(String msg)
        {
            log(Level.WARN, msg, null);
        }

        @Override
        public void info(String msg)
        {
            log(Level.INFO, msg, null);
        }

        @Override
        public void config(String msg)
        {
            log(Level.DEBUG, msg, null);
        }

        @Override
        public void fine(String msg)
        {
            log(Level.DEBUG, msg, null);
        }

        @Override
        public void finer(String msg)
        {
            log(Level.DEBUG, msg, null);
        }

        @Override
        public void finest(String msg)
        {
            log(Level.DEBUG, msg, null);
        }

        @Override
        public synchronized void setLevel(MLevel l) throws SecurityException
        {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(this.logger.getName());
            loggerConfig.setLevel(level(l));
            ctx.updateLoggers();
            this.myLevel = l;
        }

        @Override
        public synchronized MLevel getLevel()
        {
            //System.err.println( logger.getLevel() );
            if(this.myLevel == null) this.myLevel = guessMLevel(this.logger.getLevel());
            return this.myLevel;
        }

        @Override
        public boolean isLoggable(MLevel l)
        {
            //System.err.println( "MLevel: " + l + "; isEnabledFor(): " + logger.isEnabledFor( level(l) ) + "; getLevel(): " + getLevel() +
            //"; MLog.getLogger().getLevel(): " + MLog.getLogger().getLevel());
            //new Exception("WHADDAFUC").printStackTrace();
            return this.logger.isEnabled(level(l));
        }

        @Override
        public String getName()
        {
            return this.logger.getName();
        }

        @Override
        public void addHandler(Object h) throws SecurityException
        {
            if(!(h instanceof Appender))
                throw new IllegalArgumentException("The 'handler' " + h + " is not compatible with MLogger " + this);

            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(this.logger.getName());
            loggerConfig.addAppender((Appender) h, null, null);
            ctx.updateLoggers();
        }

        @Override
        public void removeHandler(Object h) throws SecurityException
        {
            if(!(h instanceof Appender))
                throw new IllegalArgumentException("The 'handler' " + h + " is not compatible with MLogger " + this);
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(this.logger.getName());
            loggerConfig.removeAppender(((Appender) h).getName());
            ctx.updateLoggers();
        }

        @Override
        public Object[] getHandlers()
        {
            List tmp = new LinkedList();

            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(this.logger.getName());

            for(String appenderName : loggerConfig.getAppenders().keySet())
                tmp.add(loggerConfig.getAppenders().get(appenderName));
            return tmp.toArray();
        }

        @Override
        public void setUseParentHandlers(boolean uph)
        {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(this.logger.getName());
            loggerConfig.setAdditive(uph);
            ctx.updateLoggers();
        }

        @Override
        public boolean getUseParentHandlers()
        {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(this.logger.getName());
            return loggerConfig.isAdditive();
        }
    }
}
