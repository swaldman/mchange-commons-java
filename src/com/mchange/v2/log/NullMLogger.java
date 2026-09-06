package com.mchange.v2.log;

import java.util.ResourceBundle;

public class NullMLogger implements MLogger
{
    private final static MLogger INSTANCE = new NullMLogger();
    
    public static MLogger instance() 
    { return INSTANCE; }
    
    private final static String NAME = "NullMLogger";
    
    private NullMLogger()
    {}
    
    @Deprecated
    public void addHandler(Object h) throws SecurityException
    {}

    public void config(String msg)
    {}

    public void entering(String srcClass, String srcMeth)
    {}

    public void entering(String srcClass, String srcMeth, Object param)
    {}

    public void entering(String srcClass, String srcMeth, Object[] params)
    {}

    public void exiting(String srcClass, String srcMeth)
    {}

    public void exiting(String srcClass, String srcMeth, Object result)
    {}

    public void fine(String msg)
    {}

    public void finer(String msg)
    {}

    public void finest(String msg)
    {}

    @Deprecated
    public Object getFilter()
    { return null; }

    @Deprecated
    public Object[] getHandlers()
    { return null; }

    @Deprecated
    public MLevel getLevel()
    { return MLevel.OFF; }

    public String getName()
    { return NAME; }

    @Deprecated
    public ResourceBundle getResourceBundle()
    { return null; }

    @Deprecated
    public String getResourceBundleName()
    { return null; }

    @Deprecated
    public boolean getUseParentHandlers()
    { return false; }

    public void info(String msg)
    {}

    public boolean isLoggable(MLevel l)
    { return false; }

    public void log(MLevel l, String msg)
    {}

    public void log(MLevel l, String msg, Object param)
    {}

    public void log(MLevel l, String msg, Object[] params)
    {}

    public void log(MLevel l, String msg, Throwable t)
    {}

    public void logp(MLevel l, String srcClass, String srcMeth, String msg)
    {}

    public void logp(MLevel l, String srcClass, String srcMeth, String msg,
                    Object param)
    {}

    public void logp(MLevel l, String srcClass, String srcMeth, String msg,
                    Object[] params)
    {}

    public void logp(MLevel l, String srcClass, String srcMeth, String msg,
                    Throwable t)
    {}

    public void logrb(MLevel l, String srcClass, String srcMeth, String rb,
                    String msg)
    {}

    public void logrb(MLevel l, String srcClass, String srcMeth, String rb,
                    String msg, Object param)
    {}

    public void logrb(MLevel l, String srcClass, String srcMeth, String rb,
                    String msg, Object[] params)
    {}

    public void logrb(MLevel l, String srcClass, String srcMeth, String rb,
                    String msg, Throwable t)
    {}

    @Deprecated
    public void removeHandler(Object h) throws SecurityException
    {}

    @Deprecated
    public void setFilter(Object java14Filter) throws SecurityException
    {}

    @Deprecated
    public void setLevel(MLevel l) throws SecurityException
    {}

    @Deprecated
    public void setUseParentHandlers(boolean uph)
    {}

    public void severe(String msg)
    {}

    public void throwing(String srcClass, String srcMeth, Throwable t)
    {}

    public void warning(String msg)
    {}
}
