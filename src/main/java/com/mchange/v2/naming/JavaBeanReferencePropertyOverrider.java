package com.mchange.v2.naming;

import javax.naming.RefAddr;
import com.mchange.v2.cfg.PropertiesConfig;

public interface JavaBeanReferencePropertyOverrider
{
    public RefAddr overrideRefAddr(Class beanClass, PropertiesConfig pcfg, String propName, Class propType, Object val) throws Exception; // null means don't override encoding
    public Object  overrideDecodeRefAddr(Class beanClass, PropertiesConfig pcfg, String propName, Class propType, RefAddr refAddr) throws Exception; // null means don't override decoding
}
