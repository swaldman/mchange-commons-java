package com.mchange.v2.cfg;

import java.io.*;
import java.util.*;

import java.net.URL;

import static com.mchange.v2.cfg.DelayedLogItem.Level;

/**
 *  How a library teaches this one to read an identifier form it has never heard of.
 *
 *  <h3>Why this exists</h3>
 *
 *  <p>{@link MConfig} understands a fixed set of identifier forms: classloader resource paths,
 *  {@code file:} URLs, {@code hocon:} paths, and the two System-property pseudo-paths. Adding a
 *  form used to mean adding a branch here, which is how c3p0's XML format came to live inside
 *  this library &mdash; a layering inversion that was apologized for in a comment and kept in
 *  step by hand. A library may now declare its own readers instead, and this library knows
 *  nothing of them.</p>
 *
 *  <h3>Declaring one</h3>
 *
 *  <p>Ship a {@code mcfg-extra-properties-config-sources.txt} resource at the root of a jar or
 *  classpath directory, one fully-qualified class name per line. Blank lines are ignored, as is
 *  anything from a {@code #} to the end of a line, so a name may carry a trailing comment.
 *  Each named class must extend this one and must have a <b>public no-argument
 *  constructor</b>, because instances are created reflectively, by name.</p>
 *
 *  <p>Each declared resolver is offered an identifier that nothing built in claims. It returns a
 *  {@link PropertiesConfigSource} if it recognizes the identifier, or null to pass the question
 *  on to the next resolver. An identifier no resolver claims is reported at WARNING and
 *  contributes no configuration; it is not fatal.</p>
 *
 *  <p>A resolver's source may implement {@link VetoableConfig}, so an extension can make an
 *  identifier refusable. Vetoability is a property of the source rather than of how the
 *  identifier is spelled, and so is discovered by resolving: {@link MConfig.AsProvided} will
 *  decline such an identifier exactly as it declines a {@code file:} one, rather than risking a
 *  refusal it promises callers cannot happen.</p>
 *
 *  <h3>The two orderings, which are opposite</h3>
 *
 *  <p>Resolvers are <b>first-found</b>. Across resources, the one earliest on the CLASSPATH is
 *  consulted first, per the JVM's usual rule for a contested resource; within one resource, the
 *  earliest line is consulted first. The first resolver to claim an identifier settles how it is
 *  read, so later declarations cannot displace earlier ones.</p>
 *
 *  <p>That is the opposite of how the resource paths <i>themselves</i> compose, where <b>later
 *  wins</b> &mdash; a path later in a config source list takes precedence, because the last
 *  value written is the one in force. The distinction is worth holding onto: one is a question
 *  of who decides how to read a name, the other of whose value survives.</p>
 *
 *  <h3>Lifetime, and why there is no refresh</h3>
 *
 *  <p>The set of resolvers is resolved <b>once per ClassLoader</b> and then fixed. A resource
 *  that appears afterwards is not picked up, and there is deliberately no way to ask for a
 *  rescan.</p>
 *
 *  <p>That is a decision rather than an omission. This is a dispatch table, not configuration:
 *  it settles how an identifier is <i>interpreted</i>, where configuration settles what a
 *  setting's value is. {@code MConfig}'s cached readers key on the identifier string as a stable
 *  name, so a table that could change mid-JVM would let one string mean different things at
 *  different moments. It would also make resolution unstable <i>within</i> a single read, which
 *  resolves each identifier twice &mdash; once to ask whether it is vetoable, once to read it.
 *  {@code java.util.ServiceLoader} caches providers and requires an explicit {@code reload()} for
 *  the same reasons; a reload here could be added later, as a deliberate API, if a real need
 *  appears.</p>
 *
 *  <p>Because instances are shared and cached, implementations <b>must be stateless and safe for
 *  concurrent use</b>, so that any instance is interchangeable with any other. Anything a
 *  resolver needs for the duration of one call belongs in a local variable.</p>
 */
public abstract class ExtraPropertiesConfigSources
{
    public final static String MCFG_EXTRA_PROPERTIES_CONFIG_SOURCES = "mcfg-extra-properties-config-sources.txt";

    // MT: protected by class' lock
    private static List<ExtraPropertiesConfigSources> RESOURCES_CONFIGURED = null;
    private static List<DelayedLogItem> resourcesConfiguredCheckLogItems = null; // cached RESOURCES_CONFIGURED can legit be null, but the log items list never will be after a check, so we gate on it

    /**
     * @return null iff there are no resource files configured, empty list if there are files but no classes
     */
    public static synchronized List<ExtraPropertiesConfigSources> getResourceConfigured(List<DelayedLogItem> delayedLogItemsOut)
    {
        if (resourcesConfiguredCheckLogItems == null)
        {
            resourcesConfiguredCheckLogItems = new ArrayList<>();
            RESOURCES_CONFIGURED = _getResourceConfigured(resourcesConfiguredCheckLogItems);
        }
        delayedLogItemsOut.addAll(resourcesConfiguredCheckLogItems);
        return RESOURCES_CONFIGURED;
    }

    /**
     * @return null iff there are no resource files configured, empty list if there are files but no classes
     */
    private static List<ExtraPropertiesConfigSources> _getResourceConfigured(List<DelayedLogItem> delayedLogItemsOut)
    {
        List<ExtraPropertiesConfigSources> out = null;

        List<String> fqcns = getResourceConfiguredClassNames(delayedLogItemsOut);

        if (fqcns != null)
        {
            out = new ArrayList<>();
            for (String fqcn : fqcns)
            {
                try
                {
                    // we don't gate, because the fqcns are specified in trusted config
                    // can't use ByNameInstantiationUtils.instantiateByNameUngated(fqcn) 'cuz it hits maybe-not-initialized logging
                    // so we go old-school.
                    ExtraPropertiesConfigSources epcs = (ExtraPropertiesConfigSources) Class.forName(fqcn).getDeclaredConstructor().newInstance();
                    out.add( epcs );
                }
                catch (Exception e)
                {
                    DelayedLogItem warning = new DelayedLogItem(Level.WARNING,
                                                                "Could not instantiate '" + fqcn + "' as an com.mchange.v2.cfg.ExtraPropertiesConfigSources instance. " +
                                                                "An unexpected Exception occurred.",
                                                                e);
                    delayedLogItemsOut.add(warning);
                }
            }
        }

        return out;
    }

    /**
     * @return null iff there are no resource files configured, empty list if there are files but no classes
     */
    private static List<String> getResourceConfiguredClassNames(List<DelayedLogItem> delayedLogItemsOut)
    {
        List<String> out = null;

        try
        {
            ArrayList<URL> allResources = Collections.list(ExtraPropertiesConfigSources.class.getClassLoader().getResources(MCFG_EXTRA_PROPERTIES_CONFIG_SOURCES));

            int sz = allResources.size();

            if (sz > 0)
            {
                out = new ArrayList<>();

                for (int i = 0; i < sz; ++i)
                {
                    URL resource = allResources.get(i);
                    BufferedReader br = new BufferedReader( new InputStreamReader( resource.openStream(), "UTF8" ) );
                    try
                    {
                        List<String> tmp = new ArrayList<>();
                        String line;
                        while ((line = br.readLine()) != null)
                        {
                            String trimmed = line.trim();
                            if (trimmed.length() > 0 && !trimmed.startsWith("#"))
                            {
                                int cut_point = trimmed.indexOf("#");
                                String cut = cut_point >= 0 ? trimmed.substring(0,cut_point) : trimmed;
                                tmp.add(cut.trim()); // we better retrim, or else we'll have any whitespace that came before the comment!
                            }
                        }
                        out.addAll( tmp );
                    }
                    catch (Exception e)
                    { delayedLogItemsOut.add(new DelayedLogItem(Level.WARNING, "An Exception occurred while trying to load ExtraPropertiesConfigSources fully-qualified class names from '" + resource + "'.", e)); }
                    finally
                    {
                        try { if ( br != null ) br.close(); } //ensures closure of nested raw stream as well
                        catch (IOException e) 
                            { delayedLogItemsOut.add( new DelayedLogItem( Level.WARNING, "An IOException occurred while closing BufferedReader from resource path '" + MCFG_EXTRA_PROPERTIES_CONFIG_SOURCES + "'.", e ) ); }
                    }
                }
            }
        }
        catch (Exception e)
        { delayedLogItemsOut.add(new DelayedLogItem(Level.WARNING, "An unexpected Exception occurred while trying to load property config resources from '" + MCFG_EXTRA_PROPERTIES_CONFIG_SOURCES + "'.", e)); }

        return out;
    }

    public abstract PropertiesConfigSource propertiesConfigSourceForIdentifier(String identifier, List<DelayedLogItem> delayedLogItemsOut); 
}
