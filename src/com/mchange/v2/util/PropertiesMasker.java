package com.mchange.v2.util;

import java.util.*;

/**
 *  Rewrites a {@link Properties} so that it can be written to a log or a diagnostic report
 *  without disclosing the credentials configuration routinely carries. The policy it applies --
 *  which names are sensitive, and how a URL-borne credential is concealed -- lives in
 *  {@link MaskUtils}, which is usable on its own where the thing to be masked is a single value
 *  rather than a whole <code>Properties</code>.
 *
 *  <h3>Why this exists</h3>
 *
 *  <p>A dump of resolved configuration is the most useful diagnostic this library can offer
 *  and the most dangerous thing it can print. Asked why a setting is not taking effect, the
 *  honest answer is usually "here is every value in force and where each came from" -- and the
 *  same dump carries whatever was passed as <code>-Dmyapp.db.password=...</code>, every
 *  credential in a properties file, and the account and password embedded in a JDBC URL. Logs
 *  are shipped to aggregators, pasted into issue reports and mailed to vendors, so a dump that
 *  is safe only because nobody has raised the log level is not safe.</p>
 *
 *  <p>c3p0 has long masked this where it knew exactly what it was holding: AuthMaskingProperties
 *  conceals two keys, <code>user</code> and <code>password</code>, because it is looking at a
 *  JDBC authentication map and nothing else. That approach does not survive generalization. A
 *  dump of System properties holds keys this library has never heard of, named by applications
 *  and libraries it will never see, so there is no list to consult. What is left is heuristics
 *  over names, plus inspection of values whose shape is known.</p>
 *
 *  <p>That the tests are heuristics, and deliberately loose ones, is argued in
 *  {@link MaskUtils} -- as is what they retain on purpose, which is as much a part of the
 *  contract as what they mask.</p>
 *
 *  <h3>Two mechanisms, because neither subsumes the other</h3>
 *
 *  <ul>
 *    <li><b>Key names.</b> A key whose name contains one of
 *        {@link MaskUtils#DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS} has its whole value
 *        replaced. Matching ignores case and ignores separators, so one entry covers
 *        <code>apiKey</code>, <code>api.key</code>, <code>api-key</code> and
 *        <code>api_key</code> -- necessary rather than tidy, since properties conventionally
 *        use dots and a naive <code>apikey</code> would have matched only the camel-cased
 *        spelling.</li>
 *    <li><b>Values.</b> A {@link Transformer} may rewrite a value the key name cannot judge.
 *        {@link #DANGEROUS_URL_VALUE_TRANSFORMER} is the case that motivated the interface: a
 *        JDBC URL carries an account and password inside a value filed under a key called
 *        something like <code>jdbcUrl</code>, which is not sensitive by any name test. No
 *        key-name heuristic can reach it, and no value heuristic can replace key-name
 *        masking.</li>
 *  </ul>
 *
 *  <p>Transformers get first refusal. They are consulted in turn, and the first to claim a
 *  value decides it outright -- key-name masking is then skipped for that key entirely. The
 *  ordering is deliberate, and it hands a transformer real discretion: one may claim a value
 *  whose key <i>is</i> sensitive by name, rescuing it from wholesale replacement in order to
 *  disclose the part that is safe. Showing the scheme, host and database of a connection URL
 *  while concealing only the credential is more useful than <code>****</code>, and a
 *  transformer is entitled to make that call.</p>
 *
 *  <p>The flip side is that a transformer can reveal what key-name masking would have hidden,
 *  so the judgement rests with whoever writes one. {@link #DANGEROUS_URL_VALUE_TRANSFORMER}
 *  declines to exercise the liberty: asked about a value whose key is already sensitive, it
 *  returns false and lets whole-value masking proceed, on the grounds that a key named
 *  <code>foo.password</code> tells you nothing about what else its value might carry. That is
 *  its own policy, not a rule of this class.</p>
 *
 *  <h3>What it does not do</h3>
 *
 *  <p>This reduces exposure. It does not eliminate it, and should not be described as though it
 *  did.</p>
 *
 *  <ul>
 *    <li><b>Masking is not redaction of context.</b> The output still shows which keys are set,
 *        and for a URL still shows the scheme, host, port and path. That is the point -- those
 *        answer the questions a dump is asked -- but it is disclosure, and a host name can
 *        itself be sensitive.</li>
 *    <li><b>The name heuristics are a denylist, and a value can hide a secret where no test
 *        reaches.</b> Both limits belong to the policy rather than to this class; they are
 *        set out in {@link MaskUtils}.</li>
 *  </ul>
 *
 *  <p>The stronger control is therefore {@code restrictToPrefix}, which <b>omits</b> every key
 *  outside the prefix from the output rather than merely declining to mask within it. A dump
 *  restricted to one library's own keys cannot leak another application's properties at all,
 *  whatever they are named, and the heuristics above then serve as defence in depth over a
 *  surface small enough to reason about. Prefer it where the audience for the dump is known.</p>
 *
 *  <h3>Mechanics</h3>
 *
 *  <p>An instance is immutable once constructed: the transformer set is copied, and the prefix
 *  is final. The substring sets it consults are published on {@link MaskUtils} and are
 *  unmodifiable there, because a caller able to empty them could disable masking for the whole
 *  process. {@link #shouldMaskValue} is public and overridable, and a {@link Transformer} is
 *  handed the masker that is calling it, so a subclass that redefines what counts as sensitive
 *  redefines it for the transformers too -- though only for key names: a transformer consulting
 *  the URL parameter sets directly, as {@link #DANGEROUS_URL_VALUE_TRANSFORMER} does through
 *  {@link MaskUtils}, is not reached by such an override.</p>
 */
public class PropertiesMasker
{
    /**
     *  Brings {@link MaskUtils#maskUrlCredentials} into a <code>Properties</code> dump, so that
     *  a connection URL filed under an innocuous key is not disclosed whole. What it conceals
     *  and what it retains are described there; what belongs here is when it declines.
     *
     *  <p>It claims a value only when masking actually changed it, so a URL carrying no
     *  credentials is left to the ordinary path rather than passing silently through a
     *  transformer. And it declines any key that is sensitive by name, forgoing the discretion
     *  described above: a key named <code>foo.password</code> says nothing about what else its
     *  value might carry, so partially revealing it is a worse answer than masking it entirely.
     *  That is this transformer's policy, not a rule of the class.</p>
     */
    public final static Transformer DANGEROUS_URL_VALUE_TRANSFORMER = new Transformer()
    {
        @Override
        public boolean transforms( PropertiesMasker masker, String key, String value )
        {
            // Decline a key already sensitive by name, forgoing a transformer's liberty to
            // override that. Reasoning in the javadoc above.
            if ( value == null || key == null || masker.shouldMaskValue( key ) )
                return false;

            // Claim only values actually changed.
            String masked = MaskUtils.maskUrlCredentials( value );
            return masked != null && !masked.equals( value );
        }

        @Override
        public String transformValue( PropertiesMasker masker, String key, String value )
        {
            String masked = MaskUtils.maskUrlCredentials( value );
            return masked == null ? value : masked;
        }
    };

    // MUST be declared after DANGEROUS_URL_VALUE_TRANSFORMER. Static initializers run in
    // textual order, and the no-arg constructor builds its transformer set from that field.
    public final static PropertiesMasker DEFAULT = new PropertiesMasker();

    public interface Transformer
    {
        public boolean transforms(PropertiesMasker masker, String key, String value);
        public String  transformValue(PropertiesMasker masker, String key, String value);
    }

    // MT: Immutable post-construction
    private final Set<Transformer> transformers;
    private final String restrictToPrefix;

    public PropertiesMasker(Set<Transformer> transformers, String restrictToPrefix)
    {
        this.transformers     = new HashSet<>(transformers);
        this.restrictToPrefix = restrictToPrefix;
    }

    public PropertiesMasker(Set<Transformer> transformers)
    { this(transformers,""); }

    public PropertiesMasker()
    { this(Collections.singleton(DANGEROUS_URL_VALUE_TRANSFORMER)); }

    // intended to be overridden as desired
    public boolean shouldMaskValue(String key)
    {
        for(String substring : MaskUtils.DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS)
            if (MaskUtils.lowercaseLettersDigitsOnly(key).indexOf(MaskUtils.lowercaseLettersDigitsOnly(substring)) >= 0)
                return true;
        return false;
    }

    public Properties masked(Properties raw)
    {
        Properties out = new Properties();

        outer:
        for (String key : raw.stringPropertyNames())
        {
            if (key.startsWith(restrictToPrefix))
            {
                for (Transformer xf : transformers)
                {
                    String value = raw.getProperty(key);
                    if (xf.transforms(this,key,value))
                     {
                         out.setProperty(key, xf.transformValue(this,key,value));
                        continue outer;
                     }
                }
                if (shouldMaskValue(key))
                    out.setProperty(key,MaskUtils.MASK);
                else
                    out.setProperty(key,raw.getProperty(key));
            }
        }

        return out;
    }

}
