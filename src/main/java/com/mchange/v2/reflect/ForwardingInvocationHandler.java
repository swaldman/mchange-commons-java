package com.mchange.v2.reflect;

import java.lang.reflect.*;

public class ForwardingInvocationHandler implements InvocationHandler
{
    Object inner;

    public ForwardingInvocationHandler(Object inner)
    { this.inner = inner; }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable
    { return m.invoke( inner, args ); }
}
