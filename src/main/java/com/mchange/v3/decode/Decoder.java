package com.mchange.v3.decode;


/**
 * Decoders should have a no-arg constructor.
 */
public interface Decoder
{
    public Object decode( Object obj ) throws CannotDecodeException;
}
