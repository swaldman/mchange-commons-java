package com.mchange.v2.cfg;

/**
 *  No config source could be found that knows how to read a resource identifier.
 *
 *  <p>Distinct from the identifier naming something that is not there, which is ordinary absence:
 *  a resource path may legitimately go unsatisfied, and {@link MConfig} skips it. This says
 *  something stronger &mdash; that nothing in the library, and nothing any
 *  {@link ExtraPropertiesConfigSources} declared on the CLASSPATH, recognizes the identifier at
 *  all. In practice it usually means a typo, or a jar that was expected to supply a reader and
 *  did not.</p>
 *
 *  <p>That distinction is the reason this type exists. Reading a list of resource paths records
 *  each path's outcome &mdash; read, vetoed, not found, or faulted &mdash; and an unreadable
 *  identifier belongs in the <b>fault</b> bucket rather than among the merely absent. Before this
 *  type, such an identifier arrived as a {@link java.io.FileNotFoundException} and was filed as
 *  absence, so a mistake in a deployment's configuration was reported as the benign case.</p>
 *
 *  <p>A {@link RuntimeException} because it is a programming or deployment error rather than a
 *  condition a caller should be made to handle at every call. It does not escape a read: it is
 *  caught per path, recorded against that path, and reported. Code that merely <i>asks</i> about
 *  an identifier rather than reading it &mdash; {@code ConfigUtils.pointsToVetoableConfig}, for
 *  instance &mdash; catches it and answers in the negative, leaving the complaint to whoever
 *  actually performs the read.</p>
 */
public class UnhandledConfigResourceIdentifierException extends RuntimeException
{
    public UnhandledConfigResourceIdentifierException(String message, Throwable cause)
    { super( message, cause ); }

    public UnhandledConfigResourceIdentifierException(String message)
    { this( message, null ); }
}
