package com.mchange.v3.decode;

/**
 * should have a no-arg constructor.
 */
public interface DecoderFinder
{
    public String decoderClassName( Object encoded ) throws CannotDecodeException;
}
