package com.mchange.v1.util;

import java.util.*;

public class SomethingChangedEventSupport
{
    Object source;
    Vector listeners = new Vector();

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
	for (Enumeration e = listeners.elements(); e.hasMoreElements();)
	    {
		SomethingChangedListener al = (SomethingChangedListener) e.nextElement();
		al.somethingChanged(ae);
	    }
    }
}
