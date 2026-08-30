package com.mchange.v2.lang;

public final class ThreadGroupUtils
{
    public static ThreadGroup rootThreadGroup()
    {
	ThreadGroup tg = Thread.currentThread().getThreadGroup(); 
	ThreadGroup ptg = tg.getParent();
	while (ptg != null)
	    {
		tg = ptg;
		ptg = tg.getParent();
	    }
	return tg;
    }

    private ThreadGroupUtils()
    {}
}
