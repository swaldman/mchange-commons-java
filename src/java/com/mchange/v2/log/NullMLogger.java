/*
 * Distributed as part of mchange-commons-java v.0.2.3.2
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


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

    public Object getFilter()
    { return null; }

    public Object[] getHandlers()
    { return null; }

    public MLevel getLevel()
    { return MLevel.OFF; }

    public String getName()
    { return NAME; }

    public ResourceBundle getResourceBundle()
    { return null; }

    public String getResourceBundleName()
    { return null; }

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

    public void removeHandler(Object h) throws SecurityException
    {}

    public void setFilter(Object java14Filter) throws SecurityException
    {}

    public void setLevel(MLevel l) throws SecurityException
    {}

    public void setUseParentHandlers(boolean uph)
    {}

    public void severe(String msg)
    {}

    public void throwing(String srcClass, String srcMeth, Throwable t)
    {}

    public void warning(String msg)
    {}
}
