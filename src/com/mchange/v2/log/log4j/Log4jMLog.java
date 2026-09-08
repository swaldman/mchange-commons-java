package com.mchange.v2.log.log4j;

import java.text.*;
import java.util.*;

import com.mchange.v2.log.*;

import org.apache.log4j.*;

import static com.mchange.v2.log.LogUtils.*;

public final class Log4jMLog extends MLog
{
    final static String CHECK_CLASS = "org.apache.log4j.Logger";

    public Log4jMLog() throws ClassNotFoundException
    { Class.forName( CHECK_CLASS ); }

    @Override
    public MLogger getMLogger(String name)
    {
        Logger lg = Logger.getLogger(name);
        if (lg == null)
        {
            fallbackWarn(" with name '" + name + "'");
            return NullMLogger.instance();
        }
        else
            return new Log4jMLogger( lg ); 
    }

    @Override
    public MLogger getMLogger(Class<?> cl)
    { 
        Logger lg = Logger.getLogger(cl);
        if (lg == null)
        {
            fallbackWarn(" for class '" + cl.getName() + "'");
            return NullMLogger.instance();
        }
        else
            return new Log4jMLogger( lg );
    }


    @Override
    public MLogger getMLogger()
    {
        Logger lg = Logger.getRootLogger();
        if (lg == null)
        {
            fallbackWarn(" (root logger)");
            return NullMLogger.instance();
        }
        else
            return new Log4jMLogger( lg ); 
    }
    
    private void fallbackWarn(String subst)
    {
        FallbackMLog.getLogger().warning("Could not create or find log4j Logger" + subst + ". " +
                                         "Using NullMLogger. All messages sent to this" +
                                         "logger will be silently ignored. You might want to fix this.");
    }

    private final static class Log4jMLogger implements MLogger
    {
        final static String FQCN = Log4jMLogger.class.getName();

        // protected by this' lock
        MLevel myLevel = null;
        
        final Logger logger;

        Log4jMLogger( Logger logger )
        { this.logger = logger; }

        private static MLevel guessMLevel(Level lvl)
        {
            if (lvl == null)
                return null;
            else if (lvl == Level.ALL)
                return MLevel.ALL;
            else if (lvl == Level.TRACE)
                return MLevel.FINEST;
            else if (lvl == Level.DEBUG)
                return MLevel.FINER;
            else if (lvl == Level.ERROR)
                return MLevel.SEVERE;
            else if (lvl == Level.FATAL)
                return MLevel.SEVERE;
            else if (lvl == Level.INFO)
                return MLevel.INFO;
            else if (lvl == Level.OFF)
                return MLevel.OFF;
            else if (lvl == Level.WARN)
                return MLevel.WARNING;
            else
                throw new IllegalArgumentException("Unknown level: " + lvl);
        }

        private static Level level(MLevel lvl)
        {
            if (lvl == null)
                return null;
            else if (lvl == MLevel.ALL)
                return Level.ALL;
            else if (lvl == MLevel.CONFIG)
                return Level.DEBUG;
            else if (lvl == MLevel.FINE)
                return Level.DEBUG;
            else if (lvl == MLevel.FINER)
                return Level.DEBUG;
            else if (lvl == MLevel.FINEST)
                return Level.TRACE;
            else if (lvl == MLevel.INFO)
                return Level.INFO;
            else if (lvl == MLevel.OFF)
                return Level.OFF;
            else if (lvl == MLevel.SEVERE)
                return Level.ERROR;
            else if (lvl == MLevel.WARNING)
                return Level.WARN;
            else
                throw new IllegalArgumentException("Unknown MLevel: " + lvl);
        }

        @Override
        @Deprecated
        public ResourceBundle getResourceBundle()
        { return null; }

        @Override
        @Deprecated
        public String getResourceBundleName()
        { return null; }

        @Override
        @Deprecated
        public void setFilter(Object java14Filter) throws SecurityException
        { warning("setFilter() not supported by MLogger " + this.getClass().getName()); }

        @Override
        @Deprecated
        public Object getFilter()
        { return null; }

        private void log(Level lvl, Object msg, Throwable t)
        { logger.log( FQCN, lvl, msg, t ); }

        @Override
        public void log(MLevel l, String msg)
        { log( level(l),  msg,  null); }

        @Override
        public void log(MLevel l, String msg, Object param)
        { log( level(l),  (msg!=null ? MessageFormat.format(msg, new Object[] { param }) : null),  null); }

        @Override
        public void log(MLevel l,String msg, Object[] params)
        { log( level(l),  (msg!=null ? MessageFormat.format(msg, params) : null),  null); }

        @Override
        public void log(MLevel l, String msg, Throwable t)
        { log( level(l),  msg,  t); }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg)
        { log( level(l),  createMessage( srcClass, srcMeth, msg),  null); }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param)
        { log( level(l),  createMessage( srcClass, srcMeth, (msg!=null ? MessageFormat.format(msg, new Object[] {param}) : null) ),  null); }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params)
        { log( level(l),  createMessage( srcClass, srcMeth, (msg!=null ? MessageFormat.format(msg, params) : null) ),  null); }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t)
        { log( level(l),  createMessage( srcClass, srcMeth, msg ),  t); }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg)
        { log( level(l),  createMessage( srcClass, srcMeth, formatMessage(rb, msg, null) ),  null); }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param)
        { log( level(l),  createMessage( srcClass, srcMeth, formatMessage(rb, msg, new Object[] { param } ) ),  null); }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params)
        { log( level(l),  createMessage( srcClass, srcMeth, formatMessage(rb, msg, params) ),  null); }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t)
        { log( level(l),  createMessage( srcClass, srcMeth, formatMessage(rb, msg, null) ),  t); }

        @Override
        public void entering(String srcClass, String srcMeth)
        { log( Level.TRACE,  createMessage( srcClass, srcMeth, "entering method." ),  null); }

        @Override
        public void entering(String srcClass, String srcMeth, Object param)
        { log( Level.TRACE,  createMessage( srcClass, srcMeth, "entering method... param: " + param.toString() ),  null); }

        @Override
        public void entering(String srcClass, String srcMeth, Object params[])
        { log( Level.TRACE,  createMessage( srcClass, srcMeth, "entering method... " + LogUtils.createParamsList( params ) ),  null); }

        @Override
        public void exiting(String srcClass, String srcMeth)
        { log( Level.TRACE,  createMessage( srcClass, srcMeth, "exiting method." ),  null); }

        @Override
        public void exiting(String srcClass, String srcMeth, Object result)
        { log( Level.TRACE,  createMessage( srcClass, srcMeth, "exiting method... result: " + result.toString() ),  null); }

        @Override
        public void throwing(String srcClass, String srcMeth, Throwable t)
        { log( Level.TRACE,  createMessage( srcClass, srcMeth, "throwing exception... " ),  t); }

        @Override
        public void severe(String msg)
        { log( Level.ERROR, msg,  null); }

        @Override
        public void warning(String msg)
        { log( Level.WARN, msg,  null); }

        @Override
        public void info(String msg)
        { log( Level.INFO, msg,  null); }

        @Override
        public void config(String msg)
        { log( Level.DEBUG, msg,  null); }

        @Override
        public void fine(String msg)
        { log( Level.DEBUG, msg,  null); }

        @Override
        public void finer(String msg)
        { log( Level.DEBUG, msg,  null); }

        @Override
        public void finest(String msg)
        { log( Level.TRACE, msg,  null); }

        @Override
        @Deprecated
        public synchronized void setLevel(MLevel l) throws SecurityException
        {
            logger.setLevel( level( l ) );
            myLevel = l;
        }

        @Override
        @Deprecated
        public synchronized MLevel getLevel()
        { 
            //System.err.println( logger.getLevel() );
            if (myLevel == null)
                myLevel = guessMLevel( logger.getLevel() );
            return myLevel;
        }

        @Override
        public boolean isLoggable(MLevel l)
        { 
            //System.err.println( "MLevel: " + l + "; isEnabledFor(): " + logger.isEnabledFor( level(l) ) + "; getLevel(): " + getLevel() +
            //"; MLog.getLogger().getLevel(): " + MLog.getLogger().getLevel());
            //new Exception("WHADDAFUC").printStackTrace();
            return logger.isEnabledFor( level(l) );
        }

        @Override
        public String getName()
        { return logger.getName(); }

        @Override
        @Deprecated
        public void addHandler(Object h) throws SecurityException
        { 
            if (! (h instanceof Appender))
                throw new IllegalArgumentException("The 'handler' " + h + " is not compatible with MLogger " + this); 
            logger.addAppender( (Appender) h ); 
        }

        @Override
        @Deprecated
        public void removeHandler(Object h) throws SecurityException
        {
            if (! (h instanceof Appender))
                throw new IllegalArgumentException("The 'handler' " + h + " is not compatible with MLogger " + this); 
            logger.removeAppender( (Appender) h ); 
        }

        @Override
        @Deprecated
        public Object[] getHandlers()
        {
            List<Object> tmp = new LinkedList<Object>();
            for (Enumeration<?> e = logger.getAllAppenders(); e.hasMoreElements(); )
                tmp.add( e.nextElement() );
            return tmp.toArray();
        }

        @Override
        @Deprecated
        public void setUseParentHandlers(boolean uph)
        { logger.setAdditivity( uph ); }

        @Override
        @Deprecated
        public boolean getUseParentHandlers()
        { return logger.getAdditivity(); }
    }
}
