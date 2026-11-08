package com.mchange.v2.reflect;

import java.io.*;
import java.util.Set;

import com.mchange.v2.cfg.WhitelistInfo;

/**
 *  Signals that the by-name use of a class was refused because its name is not in the
 *  effective whitelist, and whitelist enforcement is in effect.
 *
 *  <p>This is a <i>policy</i> refusal, not a failure to find or construct a class.
 *  Nothing was loaded. {@link ByNameInstantiationUtils#checkWarnThrowForInstantiateByNameGated}
 *  decides on the name alone, ahead of any <code>Class.forName(..)</code>, precisely because
 *  loading a class runs its static initializers &mdash; so a load by an untrusted name is
 *  already an execution of untrusted code, whether or not an instance is ever constructed.</p>
 *
 *  <p>Code that catches this alongside {@link ClassNotFoundException} or the reflective
 *  construction exceptions should distinguish it from them. Those report a class that is
 *  missing or broken, which is a mistake to be corrected; this reports a name that was
 *  understood and declined. In particular, responding by retrying through
 *  {@link ByNameInstantiationUtils#instantiateByNameUngated} would reintroduce exactly the
 *  hazard the whitelist exists to close.</p>
 *
 *  <p>Note that enforcement is <i>off</i> by default as of this writing, so an unwhitelisted
 *  name ordinarily provokes a warning and proceeds, rather than raising this. A handler for
 *  this exception is therefore handling a configuration an operator has explicitly opted into
 *  &mdash; and a default that is expected to change in a future release.</p>
 *
 *  <h3>Why it carries the whitelist rather than describing it</h3>
 *
 *  <p>A refusal is only actionable if its recipient can say what was refused, and against what.
 *  All of that could have gone into {@link #getMessage}, but a whitelist of any size buries the
 *  one sentence that tells an operator what to do, and the full list is wanted far less often
 *  than it would be produced. So the message states the refusal, and the particulars are carried
 *  as structure, for each handler to report as much of as it needs: a log line may want only
 *  {@link #getRefused}, while a diagnostic dump may want the whole {@link #getWhitelistInfo}.</p>
 *
 *  <h3>Serialization, and why it is worth the trouble</h3>
 *
 *  <p>Java serialization has been a long-running source of security trouble, and this library
 *  reaches for it reluctantly. It is supported deliberately here, because a
 *  {@link Throwable} is serializable whether or not its author thinks about it, and carrying
 *  fields on one that cannot be serialized does not produce a slightly degraded exception &mdash;
 *  it destroys the exception.</p>
 *
 *  <p>Consider how this one travels. c3p0 wraps a refused <code>driverClass</code> into a
 *  <code>SQLException</code> handed to application code, with this exception as its cause.
 *  <code>SQLException</code> is serializable, and exception cause chains cross process boundaries
 *  routinely: JMX, RMI, a remote invocation in an application server, some logging appenders.
 *  Serializing a chain serializes every link, so one non-serializable member fails the whole
 *  write with a {@link java.io.NotSerializableException} naming an implementation class the
 *  operator never heard of. The refusal they needed to read would be replaced by an unrelated I/O
 *  error, in exactly the remote, hard-to-reproduce setting where a clear message is worth most.
 *  That is the failure this support exists to prevent, and it was observed before the support was
 *  added, not imagined.</p>
 *
 *  <p>The serialized form is written explicitly and carries its own version short, so that fields
 *  may be added later without silently misreading streams written by an earlier release; an
 *  unrecognized version is refused rather than guessed at. <code>readObject</code> re-runs the
 *  constructor's own validation, because the fields below are required and a stream would
 *  otherwise be a way to manufacture an instance the constructor refuses &mdash; the serialized
 *  form can represent a null that no caller can pass.</p>
 *
 *  <p><b>Being well-formed is not being trustworthy.</b> Deserialization bypasses constructors, so
 *  a deserialized instance is only ever a <i>report</i> of a refusal, never evidence of one: a
 *  forged or tampered stream can name any class and claim any whitelist. That is acceptable only
 *  because nothing consults this object to decide anything. The gate
 *  ({@link ByNameInstantiationUtils#checkWarnThrowForInstantiateByNameGated}) resolves the
 *  whitelist itself, from live configuration, every time it is asked; this exception merely
 *  describes a decision already taken elsewhere. Code that ever begins to <i>trust</i> what it
 *  reads from one of these &mdash; treating {@link #getWhitelist} as authority to proceed, say
 *  &mdash; would turn a diagnostic into a security decision made from attacker-supplied bytes.
 *  Don't.</p>
 *
 *  @see ByNameInstantiationUtils#checkWarnThrowForInstantiateByNameGated(String,com.mchange.v2.cfg.PropertiesConfig,Set)
 *  @see ByNameInstantiationUtils#instantiateByNameGated
 */
public final class InstantiationNotPermittedException extends Exception
{
    String refused;
    WhitelistInfo whitelistInfo;

    /**
     *  The fully-qualified class name whose by-name use was refused.
     *
     *  <p>This is the name as it was supplied &mdash; the thing an operator must whitelist, or
     *  correct &mdash; and not a loaded {@link Class}, since nothing was loaded.</p>
     *
     *  @return the refused name, never <code>null</code>.
     */
    public String getRefused()
    { return refused; }

    /**
     *  The whitelist against which the refused name was tested.
     *
     *  <p>This is the <i>effective</i> whitelist at the moment of refusal, not the raw union of
     *  whatever the configuration keys name. It has already been narrowed by every rule
     *  {@link com.mchange.v2.cfg.WhitelistManager} applies: the intersection of System properties
     *  against the supplied configuration, the intersection of a deprecated key against the
     *  supported ones, the removal of a <code>*</code> that is not a whitelist's sole element, and
     *  any <code>[]</code> veto. So a name's absence here is the reason for the refusal even where
     *  some configuration key does list it, and the explanation of why lies in the keys reported
     *  by {@link #getWhitelistInfo}.</p>
     *
     *  <p>The returned set is unmodifiable.</p>
     *
     *  @return the effective whitelist, never <code>null</code>. It may be <i>empty</i>, which is
     *          a whitelist that denies everything rather than an absent one; a refusal always
     *          records what it was refused against, so there is no absent case to distinguish.
     *
     *  @see WhitelistInfo#getWhitelist
     */
    public Set<String> getWhitelist()
    { return whitelistInfo.getWhitelist(); }

    /**
     *  The whole record of the whitelist resolution that produced this refusal: the effective
     *  whitelist, the configuration keys it was computed from, and which family of keys governed
     *  &mdash; the main whitelist and its subkeys, an override whitelist, a deprecated key, or
     *  none at all.
     *
     *  <p>This is the accessor to prefer for diagnostics. {@link #getWhitelist} answers only what
     *  the whitelist contained, which says nothing about where it came from; and where a
     *  whitelist is surprising, the contributing keys are usually the answer, since any subkey
     *  may add to it and either source may narrow it.</p>
     *
     *  <p>{@link WhitelistInfo#toString} renders all of that as a single line, and is embedded
     *  verbatim in messages elsewhere in this library and in c3p0, so prefer it to assembling
     *  your own rendering.</p>
     *
     *  @return the resolution record, never <code>null</code>. A handler may read it, and read
     *          through it, without guarding: c3p0 composes a SQLException message straight from
     *          this, and an NPE raised in the catch handler of a security gate would replace the
     *          refusal with an unrelated failure.
     */
    public WhitelistInfo getWhitelistInfo()
    { return whitelistInfo; }

    /**
     *  @param message a description of the refusal, for {@link #getMessage}. Callers are
     *         encouraged to state what was refused and that enforcement was in effect, and to
     *         leave the whitelist itself out, so that a handler can reach for
     *         {@link #getWhitelistInfo} when it wants that detail and is not made to log it when
     *         it does not.
     *
     *  @param refused the fully-qualified class name that was refused. Required: a refusal that
     *         cannot say what it refused is not actionable, so <code>null</code> is rejected
     *         rather than carried.
     *
     *  @param whitelistInfo the whitelist resolution that refused it. Required, for the same
     *         reason and one more &mdash; callers compose messages directly from it, so it is
     *         guaranteed here rather than guarded at every use.
     *
     *  @param cause an underlying cause, or <code>null</code>. A whitelist refusal ordinarily has
     *         none, since nothing was attempted; a cause here would describe a failure of the
     *         resolution itself rather than of any instantiation.
     *
     *  @throws IllegalArgumentException if <code>refused</code> or <code>whitelistInfo</code> is
     *          <code>null</code>.
     */
    public InstantiationNotPermittedException(String message, String refused, WhitelistInfo whitelistInfo, Throwable cause)
    {
        super( message, cause );
        this.refused = refused;
        this.whitelistInfo = whitelistInfo;
        validate();
    }

    private void validate() throws IllegalArgumentException
    {
        if (refused == null)
            throw new IllegalArgumentException("refused cannot be null. You must supply a non-null fully-qualified class name as refused.");
        else if (whitelistInfo == null)
            throw new IllegalArgumentException("whitelistInfo cannot be null. You must supply a non-null WhitelistInfo.");
    }

    private void validateOnDeserialization() throws InvalidObjectException
    {
        try { validate(); }
        catch (IllegalArgumentException e)
        { throw (InvalidObjectException) new InvalidObjectException( e.getMessage() ).initCause( e ); }
    }

    /**
     *  Equivalent to
     *  {@link #InstantiationNotPermittedException(String,String,WhitelistInfo,Throwable)} with no
     *  cause, which is the ordinary case: the name was refused before anything was attempted, so
     *  there is nothing underlying to report.
     */
    public InstantiationNotPermittedException(String message, String refused, WhitelistInfo whitelistInfo)
    { this( message, refused, whitelistInfo, null ); }

    // serialization stuff
    private static final long serialVersionUID = 1;
    private static final short VERSION = 0x0001;

    private void writeObject( ObjectOutputStream oos ) throws IOException
    {
        oos.writeShort( VERSION );
        oos.writeObject( refused );
        oos.writeObject( whitelistInfo );
    }

    private void readObject( ObjectInputStream ois ) throws IOException, ClassNotFoundException
    {
        short version = ois.readShort();
        switch (version)
        {
        case VERSION:
            refused = (String) ois.readObject();
            whitelistInfo = (WhitelistInfo) ois.readObject();
            validateOnDeserialization();
            break;
        default:
            throw new IOException("Unsupported Serialized Version: " + version);
        }
    }
}
