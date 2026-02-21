package com.mchange.v2.codegen.bean;

public interface ResolvedClassInfo extends ClassInfo
{
    public Class[] getInterfaces();
    public Class[] getSuperclass();
}
