package com.mchange.v2.cfg;

import java.util.*;

public class WhitelistInfo
{
    public enum Source {
        MAIN_WHITELIST("whitelist"), OVERRIDE("override whitelist"), DEPRECATED("deprecated whitelist"), MISSING("missing whitelist");

        final String identifier;

        Source(String identifier)
        { this.identifier = identifier; }

        public String getIdentifier() { return identifier; }
    }

    final Set<String> whitelist;
    final Set<String> fromKeys;
    final Source source;

    public Set<String> getWhitelist() { return whitelist; }
    public Set<String> getFromKeys()  { return fromKeys; }
    public Source      getSource()    { return source; }

    public WhitelistInfo(Set<String> whitelist, Set<String> fromKeys, Source source)
    {
        if (whitelist == null)
            throw new IllegalArgumentException("A WhitelistInfo must describe a non-null whitelist; null provided instead.");
        if (fromKeys == null)
            throw new IllegalArgumentException("A WhitelistInfo must provide a non-null set of the keys from which the whitelist was computed; null provided instead.");
        if (source == null)
            throw new IllegalArgumentException("A WhitelistInfo must provide a non-null source; null provided instead.");

        this.whitelist = Collections.unmodifiableSet(whitelist);
        this.fromKeys = Collections.unmodifiableSet(fromKeys);
        this.source = source;
    }

    private boolean _equals(WhitelistInfo other)
    { return this.whitelist.equals(other.whitelist) && this.fromKeys.equals(other.fromKeys) && this.source.equals(other.source); }

    @Override
    public boolean equals(Object o)
    { return this == o || ((o instanceof WhitelistInfo) && _equals((WhitelistInfo) o)); }

    @Override
    public int hashCode()
    { return whitelist.hashCode() ^ fromKeys.hashCode() ^ source.hashCode(); }

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
}
