package com.mchange.v2.cfg;

/**
 *  A {@link PropertiesConfigSource} that may refuse to supply configuration at all, rather than
 *  merely failing to find any.
 *
 *  <p>The distinction matters because these are different events. A source that cannot find its
 *  configuration is ordinary: the path is dropped, a log item is recorded, and the remaining
 *  sources are read. A source that <i>vetoes</i> is saying something stronger &mdash; that the
 *  configuration it found must not be used, and that proceeding as though it were simply absent
 *  would be wrong. {@link com.mchange.v2.cfg.FileUrlPropertiesConfigSource} vetoes, for example,
 *  when a file it was told to treat as private turns out to be readable by anyone.</p>
 *
 *  <p>A veto is signalled by throwing {@link ConfigVetoedException}, which is checked. Only a
 *  VetoableConfig should throw one.</p>
 *
 *  <p>Implementing this interface does not merely permit a veto; it changes who may use the
 *  source. Whether an identifier is vetoable is decided from the source class alone, before
 *  anything is read, so:</p>
 *
 *  <ul>
 *    <li>{@code MConfig.AsProvidedVetoable} accepts such identifiers. Its methods declare
 *    {@code throws ConfigVetoedException}, so a caller cannot use them without deciding what a
 *    veto should mean to the application.</li>
 *
 *    <li>{@code MConfig.AsProvided} refuses them outright, with an IllegalArgumentException
 *    naming the vetoable facade. Every path it reads was named by the caller in code, so this
 *    is a programmer error, caught before any file is opened. Note that the refusal follows from
 *    the source class, not from the particular identifier: an identifier is refused even when
 *    nothing about it could actually provoke a veto.</li>
 *
 *    <li>{@code MConfig.WithTraditionalDefaultSources} ignores the veto, dropping that one source
 *    with a warning and reading the rest. Its resource paths may come from resource-path text
 *    files the application never wrote, so a veto there reflects an end user's configuration
 *    choice, and must not be able to abort a read that never opted in.</li>
 *  </ul>
 *
 *  <p>See {@link MConfig} for the model as a whole, and {@link PropertiesConfigSource} for the
 *  requirements every source must meet.</p>
 */
public interface VetoableConfig extends PropertiesConfigSource
{}
