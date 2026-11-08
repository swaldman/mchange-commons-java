package com.mchange.v2.cfg;

import java.io.*;
import java.util.*;

/**
 *  The outcome of one whitelist resolution: the effective whitelist, the configuration keys it was
 *  computed from, and which family of keys governed.
 *
 *  <p>It records a resolution rather than a configuration. The whitelist here is what
 *  {@link WhitelistManager} arrived at after every narrowing rule it applies, so it is not the
 *  union of what the named keys say, and that is the point of reporting the keys alongside it: when
 *  an effective whitelist is surprising, its provenance is usually the explanation.</p>
 *
 *  <p>Instances are immutable, and both sets they expose are unmodifiable. A
 *  <code>WhitelistInfo</code> cannot exist in a partly-specified state: all three components are
 *  required, and the constructor refuses null for any of them.</p>
 *
 *  <h3>Serialization, and why it is worth the trouble</h3>
 *
 *  <p>Java serialization has been a long-running source of security trouble, and this library
 *  reaches for it reluctantly. This class supports it for one concrete reason: it is carried by
 *  {@link com.mchange.v2.reflect.InstantiationNotPermittedException}, and an exception's fields
 *  must be serializable or the exception is not. See that class for why that matters &mdash; in
 *  short, a refusal travels as the cause of a <code>SQLException</code> into application code, and
 *  a cause chain containing one non-serializable link cannot be written at all, so the refusal
 *  would reach a remote reader as an unrelated I/O error.</p>
 *
 *  <p>Marking the field <code>transient</code> instead would have serialized without complaint and
 *  arrived empty, leaving a refusal able to say that a name was rejected but not against what
 *  &mdash; discarding the half that makes it actionable, and discarding it silently. So this class
 *  does the work itself.</p>
 *
 *  <p>Deserialization bypasses constructors, which is how serializable value classes acquire states
 *  their own invariants forbid. That is not tolerable in a class whose {@link #toString} every
 *  diagnostic path calls: a null <code>source</code> arriving off a stream would mean an NPE thrown
 *  from inside a log statement, while reporting a security refusal. So <code>readObject</code> runs
 *  the same validation the constructor runs, reporting a rejection as a
 *  {@link java.io.InvalidObjectException} because that is an {@link java.io.IOException} and
 *  deserializing code is written to catch those; and it re-applies the same hardening, so that a
 *  stream supplying a bare mutable set cannot leave an accessor documented as unmodifiable
 *  returning something modifiable. The serialized form carries its own version short, so fields may
 *  be added later without misreading older streams.</p>
 *
 *  <p><b>Validation makes a deserialized instance well-formed, not true.</b> Nothing stops a forged
 *  stream describing a whitelist that was never configured. That is acceptable only because this
 *  type is a record of a decision, never an input to one: whitelist enforcement resolves the
 *  whitelist afresh from live configuration at each gate, and never consults a
 *  <code>WhitelistInfo</code> it was handed. Code that begins to trust one it received over a wire
 *  would be making a security decision from attacker-supplied bytes.</p>
 *
 *  @see WhitelistManager
 *  @see com.mchange.v2.reflect.InstantiationNotPermittedException#getWhitelistInfo
 */
public class WhitelistInfo implements Serializable
{
    public enum Source {
        MAIN_WHITELIST("whitelist"), OVERRIDE("override whitelist"), DEPRECATED("deprecated whitelist"), MISSING("missing whitelist");

        final String identifier;

        Source(String identifier)
        { this.identifier = identifier; }

        public String getIdentifier() { return identifier; }
    }

    private Set<String> whitelist;
    private Set<String> fromKeys;
    private Source source;

    public Set<String> getWhitelist() { return whitelist; }
    public Set<String> getFromKeys()  { return fromKeys; }
    public Source      getSource()    { return source; }

    public WhitelistInfo(Set<String> whitelist, Set<String> fromKeys, Source source)
    {
        this.whitelist = whitelist;
        this.fromKeys = fromKeys;
        this.source = source;
        validateAndHarden();
    }

    private void validateAndHarden() throws IllegalArgumentException
    {
        if (whitelist == null)
            throw new IllegalArgumentException("A WhitelistInfo must describe a non-null whitelist; null provided instead.");
        if (fromKeys == null)
            throw new IllegalArgumentException("A WhitelistInfo must provide a non-null set of the keys from which the whitelist was computed; null provided instead.");
        if (source == null)
            throw new IllegalArgumentException("A WhitelistInfo must provide a non-null source; null provided instead.");
        whitelist = Collections.unmodifiableSet(whitelist);
        fromKeys = Collections.unmodifiableSet(fromKeys);
    }

    private void validateAndHardenOnDeserialization() throws InvalidObjectException
    {
        try { validateAndHarden(); }
        catch (IllegalArgumentException e)
        { throw (InvalidObjectException) new InvalidObjectException( e.getMessage() ).initCause( e ); }
    }

    private boolean _equals(WhitelistInfo other)
    { return this.whitelist.equals(other.whitelist) && this.fromKeys.equals(other.fromKeys) && this.source.equals(other.source); }

    @Override
    public boolean equals(Object o)
    { return this == o || ((o instanceof WhitelistInfo) && _equals((WhitelistInfo) o)); }

    @Override
    public int hashCode()
    { return whitelist.hashCode() ^ fromKeys.hashCode() ^ source.hashCode(); }

    // Note: We're embedding toString() in larger messages that expect this format (in C3P0Registry)
    //       Try not to mess with it
    @Override
    public String toString()
    {
        switch (source)
        {
        case MISSING:
            return source.getIdentifier();
        default:
            return source.getIdentifier() + ": " + whitelist + " (computed from keys: " + fromKeys + ")";
        }
    }

    // serialization stuff
    private static final long serialVersionUID = 1;
    private static final short VERSION = 0x0001;

    private void writeObject( ObjectOutputStream oos ) throws IOException
    {
        oos.writeShort( VERSION );
        oos.writeObject( whitelist );
        oos.writeObject( fromKeys );
        oos.writeObject( source );
    }

    @SuppressWarnings("unchecked")
    private void readObject( ObjectInputStream ois ) throws IOException, ClassNotFoundException
    {
        short version = ois.readShort();
        switch (version)
        {
        case VERSION:
            whitelist = (Set<String>) ois.readObject();
            fromKeys  = (Set<String>) ois.readObject();
            source    = (Source)      ois.readObject();
            validateAndHardenOnDeserialization();
            break;
        default:
            throw new IOException("Unsupported Serialized Version: " + version);
        }
    }
}
