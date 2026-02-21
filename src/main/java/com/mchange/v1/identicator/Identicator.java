package com.mchange.v1.identicator;

/** 
 *  Identicators should be immutable (sharable).
 *  Cloned collections share identicators 
 */
public interface Identicator
{
    public boolean identical(Object a, Object b);
    public int     hash(Object o);
}
