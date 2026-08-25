package com.mchange.v2.cfg;

import java.util.*;
import java.io.FileNotFoundException;

/**
 *  A source of properties-style configuration, identified by a resource-path identifier.
 *
 *  <p>Implementations <b>must</b> provide a public no-argument constructor. The library
 *  constructs them reflectively, by name, when it selects a source for an identifier.</p>
 *
 *  <p>Implementations <b>must</b> also be stateless and safe for concurrent use. One instance
 *  per implementation class is created and then shared by every read, across every identifier
 *  and every calling thread, so an implementation that carries per-read state will see it
 *  corrupted by unrelated reads. Anything an implementation needs to remember for the duration
 *  of a single call belongs in a local variable, and anything it wants to report belongs in the
 *  {@link Parse} it returns.</p>
 *
 *  <p>An implementation that may refuse to supply configuration at all &mdash; rather than
 *  merely failing to find it &mdash; should implement {@link VetoableConfig} and signal the
 *  refusal by throwing {@link ConfigVetoedException}. Doing so is what allows a caller to opt
 *  in to that possibility, via {@code MConfig.AsProvidedVetoable}, and what keeps such a source
 *  from aborting reads that never agreed to handle it. See {@link MConfig} for the whole model.</p>
 */
public interface PropertiesConfigSource
{
    /**
     *  An Exception signifies this source cannot be parsed at all;
     *  it is a bad source. More local failures should be handled and
     *  reported in parse messages.
     *
     *  <p>A {@link java.io.FileNotFoundException} or {@link java.nio.file.NoSuchFileException}
     *  means only that nothing was found at this identifier: the path is dropped and the
     *  remaining sources are read, reported at FINE. Any other exception is reported at WARNING,
     *  and likewise drops only this path.</p>
     *
     *  <p>A {@link ConfigVetoedException} is different in kind: it does not mean "I could not
     *  read this", it means "this configuration must not be used". Only a {@link VetoableConfig}
     *  should throw one.</p>
     */
    public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception;

    public static class Parse
    {
	private Properties           properties;
	private List<DelayedLogItem> parseMessages;

	public Properties           getProperties()    { return properties; }
	public List<DelayedLogItem> getDelayedLogItems() { return parseMessages; }

	public Parse( Properties properties, List<DelayedLogItem> parseMessages )
	{
	    this.properties    = properties;
	    this.parseMessages = parseMessages;
	}
    }
}
