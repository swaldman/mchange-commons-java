package com.mchange.v2.codegen.intfc;

public final class ReflectiveDelegationPolicy 
{
    public final static ReflectiveDelegationPolicy USE_MAIN_DELEGATE_INTERFACE = new ReflectiveDelegationPolicy();
    public final static ReflectiveDelegationPolicy USE_RUNTIME_CLASS           = new ReflectiveDelegationPolicy();
    
    Class delegateClass;

    private ReflectiveDelegationPolicy()
    {  this.delegateClass = null; }

    public ReflectiveDelegationPolicy(Class dc)
    { 
	if (dc == null)
	    throw new IllegalArgumentException("Class for reflective delegation cannot be null!");
	this.delegateClass = dc; 
    }

    public String toString()
    {
	if (this == USE_MAIN_DELEGATE_INTERFACE)
	    return "[ReflectiveDelegationPolicy: Reflectively delegate via the main delegate interface.]";
	else if (this == USE_RUNTIME_CLASS)
	    return "[ReflectiveDelegationPolicy: Reflectively delegate via the runtime class of the delegate object.]";
	else
	    return "[ReflectiveDelegationPolicy: Reflectively delegate via " + delegateClass.getName() + ".]";
    }
}

