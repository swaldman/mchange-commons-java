package com.mchange.v3.nio;

import java.nio.ByteBuffer;

public final class ByteBufferUtils
{
    public static byte[] newArray( ByteBuffer bb )
    {
	if ( bb.hasArray() ) {
	    return (byte[]) bb.array().clone();
	} else {
	    byte[] out = new byte[bb.remaining()];
	    bb.get(out);
	    return out;
	}
    }

    private ByteBufferUtils()
    {}
}
