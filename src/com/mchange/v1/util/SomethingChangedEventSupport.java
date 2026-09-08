package com.mchange.v1.util;

import java.util.*;

public class SomethingChangedEventSupport
{
    Object source;
    Vector<SomethingChangedListener> listeners = new Vector<SomethingChangedListener>();

    public SomethingChangedEventSupport(Object source)
    {this.source = source;}

    public synchronized void addSomethingChangedListener(SomethingChangedListener mlistener)
    {
	if (! listeners.contains(mlistener))
	    listeners.addElement(mlistener);
    }

    public synchronized void removeSomethingChangedListener(SomethingChangedListener mlistener)
    {listeners.removeElement(mlistener);}

    public synchronized void fireSomethingChanged()
    {
	SomethingChangedEvent ae = new SomethingChangedEvent(source);
	for (Enumeration<SomethingChangedListener> e = listeners.elements(); e.hasMoreElements();)
	    {
		SomethingChangedListener al = e.nextElement();
		al.somethingChanged(ae);
	    }
    }
}
