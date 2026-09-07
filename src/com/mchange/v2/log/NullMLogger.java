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
    
    @Override
    @Deprecated
    public void addHandler(Object h) throws SecurityException
    {}

    @Override
    public void config(String msg)
    {}

    @Override
    public void entering(String srcClass, String srcMeth)
    {}

    @Override
    public void entering(String srcClass, String srcMeth, Object param)
    {}

    @Override
    public void entering(String srcClass, String srcMeth, Object[] params)
    {}

    @Override
    public void exiting(String srcClass, String srcMeth)
    {}

    @Override
    public void exiting(String srcClass, String srcMeth, Object result)
    {}

    @Override
    public void fine(String msg)
    {}

    @Override
    public void finer(String msg)
    {}

    @Override
    public void finest(String msg)
    {}

    @Override
    @Deprecated
    public Object getFilter()
    { return null; }

    @Override
    @Deprecated
    public Object[] getHandlers()
    { return null; }

    @Override
    @Deprecated
    public MLevel getLevel()
    { return MLevel.OFF; }

    @Override
    public String getName()
    { return NAME; }

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
    public boolean getUseParentHandlers()
    { return false; }

    @Override
    public void info(String msg)
    {}

    @Override
    public boolean isLoggable(MLevel l)
    { return false; }

    @Override
    public void log(MLevel l, String msg)
    {}

    @Override
    public void log(MLevel l, String msg, Object param)
    {}

    @Override
    public void log(MLevel l, String msg, Object[] params)
    {}

    @Override
    public void log(MLevel l, String msg, Throwable t)
    {}

    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg)
    {}

    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg,
                    Object param)
    {}

    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg,
                    Object[] params)
    {}

    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg,
                    Throwable t)
    {}

    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb,
                    String msg)
    {}

    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb,
                    String msg, Object param)
    {}

    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb,
                    String msg, Object[] params)
    {}

    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb,
                    String msg, Throwable t)
    {}

    @Override
    @Deprecated
    public void removeHandler(Object h) throws SecurityException
    {}

    @Override
    @Deprecated
    public void setFilter(Object java14Filter) throws SecurityException
    {}

    @Override
    @Deprecated
    public void setLevel(MLevel l) throws SecurityException
    {}

    @Override
    @Deprecated
    public void setUseParentHandlers(boolean uph)
    {}

    @Override
    public void severe(String msg)
    {}

    @Override
    public void throwing(String srcClass, String srcMeth, Throwable t)
    {}

    @Override
    public void warning(String msg)
    {}
}
