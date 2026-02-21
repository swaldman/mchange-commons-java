package com.mchange.v2.management;

import java.util.Arrays;

public final class OperationKey
{
    String   name;
    String[] signature;
    
    public OperationKey(String name, String[] signature)
    {
        this.name = name;
        this.signature = signature;
    }
    
    public boolean equals(Object o)
    {
        if (o instanceof OperationKey)
        {
            OperationKey oo = (OperationKey) o;
            return 
                (this.name.equals(oo.name)) &&
                (Arrays.equals(this.signature, oo.signature));
        }
        else
            return false;
    }
    
    public int hashCode()
    { return name.hashCode() ^ Arrays.hashCode(signature); }
}

