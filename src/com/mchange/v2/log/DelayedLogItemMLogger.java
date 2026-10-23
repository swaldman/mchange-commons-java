package com.mchange.v2.log;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.*;
import com.mchange.v2.cfg.DelayedLogItem;

import static com.mchange.v2.cfg.DelayedLogItem.Level;

class DelayedLogItemMLogger implements MLogger
{
    private final static Map<MLevel,Level> DLIL;

    static
    {
        Map<MLevel,Level> tmp = new HashMap<>();
        tmp.put(MLevel.ALL,Level.ALL);
        tmp.put(MLevel.CONFIG,Level.CONFIG);
        tmp.put(MLevel.FINE,Level.FINE);
        tmp.put(MLevel.FINER,Level.FINER);
        tmp.put(MLevel.FINEST,Level.FINEST);
        tmp.put(MLevel.INFO,Level.INFO);
        tmp.put(MLevel.OFF,Level.OFF);
        tmp.put(MLevel.SEVERE,Level.SEVERE);
        tmp.put(MLevel.WARNING,Level.WARNING);
        tmp.put(MLevel.DEBUG,Level.FINE);
        tmp.put(MLevel.TRACE,Level.FINEST);
        DLIL = Collections.unmodifiableMap(tmp);
    }

    private static Level dlil(MLevel mlevel)
    {
        Level out = DLIL.get(mlevel);
        if (out == null) out = Level.INFO; // we just default to INFO on unknown levels
        return out;
    }

    String               name;
    List<DelayedLogItem> sink;

    DelayedLogItemMLogger(String name, List<DelayedLogItem> sink)
    {
        this.name = name;
        this.sink = sink;
    }

    DelayedLogItemMLogger(String name)
    { this( name, new ArrayList<DelayedLogItem>() ); }

    public List<DelayedLogItem> getDelayedLogItems()
    { return sink; }

    private String formatrb(String srcClass, String srcMeth, String rbname, String msg, Object[] params)
    {
        ResourceBundle rb = ResourceBundle.getBundle( rbname );
        if (msg != null && rb != null)
            {
                String check = rb.getString( msg );
                if (check != null)
                    msg = check;
            }
        return format( srcClass, srcMeth, msg, params );
    }

    private String format(String srcClass, String srcMeth, String msg, Object[] params)
    {
        boolean add_parens = (srcMeth != null && ! srcMeth.endsWith(")"));

        StringBuilder sb = new StringBuilder(256);
        if (srcClass != null && srcMeth != null)
            {
                sb.append('[');
                sb.append( srcClass );
                sb.append( '.' );
                sb.append( srcMeth );
                if (add_parens)
                    sb.append("()");
                sb.append( ']' );
            }
        else if (srcClass != null)
            {
                sb.append('[');
                sb.append( srcClass );
                sb.append( ']' );
            }
        else if (srcMeth != null)
            {
                sb.append('[');
                sb.append( srcMeth );
                if (add_parens)
                    sb.append("()");
                sb.append( ']' );
            }
        if (msg == null) 
            {
                if (params != null)
                    {
                        sb.append("params: ");
                        for (int i = 0, len = params.length; i < len; ++i)
                            {
                                if (i != 0) sb.append(", ");
                                sb.append( params[i] );
                            }
                    }
            }
        else 
            {
                if (params == null)
                    sb.append( msg );
                else
                    {
                        MessageFormat mfmt = new MessageFormat( msg );
                        sb.append( mfmt.format( params ) );
                    }
            }

        return sb.toString();
    }

    private DelayedLogItem dli(Level l, String srcClass, String srcMeth, String msg, Object[] params, Throwable t)
    { return new DelayedLogItem(l, format(srcClass, srcMeth, msg, params), t); }

    private DelayedLogItem dlirb(Level l, String srcClass, String srcMeth, String rbname, String msg, Object[] params, Throwable t)
    { return new DelayedLogItem(l, formatrb(srcClass, srcMeth, rbname, msg, params), t); }

    @Override
    @Deprecated
    public void addHandler(Object h) throws SecurityException
    {}

    @Override
    public void config(String msg)
    { sink.add(new DelayedLogItem(Level.CONFIG,msg)); }

    @Override
    public void entering(String srcClass, String srcMeth)
    { sink.add(dli(Level.FINER, srcClass, srcMeth, "Entering method.", null, null)); }

    @Override
    public void entering(String srcClass, String srcMeth, Object param)
    { sink.add(dli(Level.FINER, srcClass, srcMeth, "Entering method with argument " + param, null, null)); }

    @Override
    public void entering(String srcClass, String srcMeth, Object[] params)
    {
        if (params == null)
            entering( srcClass, srcMeth );
        else
        {
            StringBuffer sb = new StringBuffer(128);
            sb.append("( ");
            for (int i = 0, len = params.length; i < len; ++i)
                {
                    if (i != 0) sb.append(", ");
                    sb.append( params[i] );
                }
            sb.append(" )");
            sink.add(dli(Level.FINER,srcClass, srcMeth, "Entering method with arguments " + sb.toString(),null,null)); 
        }
    }

    @Override
    public void exiting(String srcClass, String srcMeth)
    { sink.add(dli(Level.FINER, srcClass, srcMeth, "Exiting method.", null, null)); }

    @Override
    public void exiting(String srcClass, String srcMeth, Object result)
    { sink.add(dli(Level.FINER, srcClass, srcMeth, "Exiting method with result " + result, null, null)); }

    @Override
    public void fine(String msg)
    { sink.add(dli(Level.FINE,null,null,msg,null,null)); } 

    @Override
    public void finer(String msg)
    { sink.add(dli(Level.FINER,null,null,msg,null,null)); } 

    @Override
    public void finest(String msg)
    { sink.add(dli(Level.FINEST,null,null,msg,null,null)); } 

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
    { return name; }

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
    { sink.add(dli(Level.INFO,null,null,msg,null,null)); }

    @Override
    public boolean isLoggable(MLevel l) // we want every message, something else will filter
    { return true; }

    @Override
    public void log(MLevel l, String msg)
    { sink.add(dli(dlil(l),null,null,msg,null,null)); }

    @Override
    public void log(MLevel l, String msg, Object param)
    { sink.add(dli(dlil(l),null,null,msg,new Object[]{param},null)); }

    @Override
    public void log(MLevel l, String msg, Object[] params)
    { sink.add(dli(dlil(l),null,null,msg,params,null)); }

    @Override
    public void log(MLevel l, String msg, Throwable t)
    { sink.add(dli(dlil(l),null,null,msg,null,t)); }

    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg)
    { sink.add(dli(dlil(l),srcClass,srcMeth,msg,null,null)); }

    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param)
    { sink.add(dli(dlil(l),srcClass,srcMeth,msg,new Object[]{param},null)); }

    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params)
    { sink.add(dli(dlil(l),srcClass,srcMeth,msg,params,null)); }

    @Override
    public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t)
    { sink.add(dli(dlil(l),srcClass,srcMeth,msg,null,t)); }

    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg)
    { sink.add(dlirb(dlil(l),srcClass,srcMeth,rb,msg,null,null)); }


    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param)
    { sink.add(dlirb(dlil(l),srcClass,srcMeth,rb,msg,new Object[]{param},null)); }

    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params)
    { sink.add(dlirb(dlil(l),srcClass,srcMeth,rb,msg,params,null)); }

    @Override
    public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t)
    { sink.add(dlirb(dlil(l),srcClass,srcMeth,rb,msg,null,t)); }

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
    { sink.add(dli(Level.SEVERE,null,null,msg,null,null)); }

    @Override
    public void throwing(String srcClass, String srcMeth, Throwable t)
    { sink.add(dli(Level.FINE,srcClass,srcMeth,"Throwing exception.",null,t)); }

    @Override
    public void warning(String msg)
    { sink.add(dli(Level.WARNING,null,null,msg,null,null)); }
}
