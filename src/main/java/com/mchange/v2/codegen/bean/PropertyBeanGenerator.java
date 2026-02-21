package com.mchange.v2.codegen.bean;

import java.io.*;

public interface PropertyBeanGenerator
{
    public void generate( ClassInfo info, Property[] props, Writer w ) throws IOException;
}
