package com.mchange.v2.util;

import java.util.*;

/**
 *  Rewrites a {@link Properties} so that it can be written to a log or a diagnostic report
 *  without disclosing the credentials configuration routinely carries.
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
 *  <h3>The asymmetry that licenses heuristics</h3>
 *
 *  <p>The two ways of being wrong cost very different amounts. Masking something harmless costs
 *  one diagnostic value. Failing to mask a credential discloses it, to everyone who ever reads
 *  the log, with no way to recall it. So the substring tests here are deliberately loose, and
 *  <code>pass</code> matching <code>bypass</code> or <code>compass</code> is an accepted price
 *  rather than a defect.</p>
 *
 *  <p>Loose, but not unbounded, because the opposite failure is real too: a masker that returned
 *  <code>****</code> for everything would disclose nothing and be worthless. The retentions are
 *  as much a part of the contract as the maskings. Two are worth naming, because the obvious
 *  substrings would have destroyed them. A bare <code>key</code> would mask this library's own
 *  configuration -- <code>whitelistBaseKey</code>, <code>overrideWhitelistKey</code> and their
 *  kind are not secrets -- and a bare <code>user</code> would mask <code>user.home</code> and
 *  <code>user.dir</code>, which are exactly what someone debugging a <code>file:</code> config
 *  source or a HOCON <code>${user.home}</code> substitution needs to see. Hence the narrow
 *  compounds (<code>apikey</code>, <code>accesskey</code>) and the specific
 *  <code>c3p0.user</code>.</p>
 *
 *  <h3>Two mechanisms, because neither subsumes the other</h3>
 *
 *  <ul>
 *    <li><b>Key names.</b> A key whose name contains a sensitive substring has its whole value
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
 *    <li><b>A secret in a URL path is out of reach.</b> A Slack webhook is
 *        <code>https://hooks.slack.com/services/T000/B000/XXXX</code> -- the credential is a
 *        path segment, not userinfo and not a parameter. Masking paths wholesale would destroy
 *        the diagnostic value of every JDBC URL, so such values are caught only when their key
 *        name gives them away.</li>
 *    <li><b>Name heuristics are a denylist.</b> A credential under a key named
 *        <code>foo.bar</code> is disclosed. Nothing here can know otherwise.</li>
 *    <li><b>Masking is not redaction of context.</b> The output still shows which keys are set,
 *        and for a URL still shows the scheme, host, port and path. That is the point -- those
 *        answer the questions a dump is asked -- but it is disclosure, and a host name can
 *        itself be sensitive.</li>
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
 *  is final. The published substring sets are unmodifiable, because a caller able to empty them
 *  could disable masking for the whole process. {@link #shouldMaskValue} is public and
 *  overridable, and a {@link Transformer} is handed the masker that is calling it, so a subclass
 *  that redefines what counts as sensitive redefines it for the transformers too.</p>
 */
public class PropertiesMasker
{
    public final static PropertiesMasker DEFAULT;

    public final static Set<String> DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS;

    /** As above, but tested against URL parameter names. See DANGEROUS_URL_VALUE_TRANSFORMER. */
    public final static Set<String> DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_SUBSTRINGS;

    /**
     *  URL parameter names matched WHOLE rather than as substrings, for credential parameters
     *  whose names are too short to be safe substrings. See DANGEROUS_URL_VALUE_TRANSFORMER.
     */
    public final static Set<String> DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_EXACT_NAMES;

    /**
     *  Masks credentials carried <i>inside</i> a URL-shaped value, which key-name tests cannot
     *  reach: a JDBC URL lives under a key called something like <code>jdbcUrl</code>, matching
     *  no sensitive substring, while its value may carry an account and password in either of
     *  two places.
     *
     *  <ul>
     *    <li>userinfo -- <code>scheme://user:password@host:port/path</code></li>
     *    <li>parameters -- <code>scheme://host/db?user=u&amp;password=p</code>, and the
     *        <code>;</code>-separated form SQL Server-style drivers use, which carries its
     *        parameters directly after the authority with no <code>?</code> at all.</li>
     *  </ul>
     *
     *  <p>Everything not itself a secret is retained: the scheme, host, port, path, every
     *  parameter name, the fragment, and the structure throughout. A masked userinfo still
     *  shows whether a password accompanied the account (<code>****:****</code>) or not
     *  (<code>****</code>), because "was a password supplied here" is usually the question
     *  being asked of a dump.</p>
     *
     *  <p>This is deliberately NOT built on {@link java.net.URI}. A JDBC URL is opaque to it --
     *  the scheme is <code>jdbc</code> and the remainder is the scheme-specific part -- so
     *  getUserInfo(), getHost() and getQuery() all return null for precisely the values that
     *  most need masking.</p>
     */
    public final static Transformer DANGEROUS_URL_VALUE_TRANSFORMER = new Transformer()
    {
        @Override
        public boolean transforms( PropertiesMasker masker, String key, String value )
        {
            // This transformer's own policy, not a rule of the class: a transformer claiming a
            // value suppresses key-name masking for that key, and we decline to use that
            // liberty. A key named 'foo.password' says nothing about what else its value might
            // carry, so partially revealing it is a worse answer than masking it entirely.
            // A transformer with better knowledge of its values may legitimately decide
            // otherwise.
            if ( value == null || key == null || masker.shouldMaskValue( key ) )
                return false;

            // Claim only values actually changed, so a URL carrying no credentials is left to
            // the ordinary path rather than silently passing through a transformer.
            String masked = maskUrlCredentials( value );
            return masked != null && !masked.equals( value );
        }

        @Override
        public String transformValue( PropertiesMasker masker, String key, String value )
        {
            String masked = maskUrlCredentials( value );
            return masked == null ? value : masked;
        }
    };

    final static String MASK = "****";

    private final static String SCHEME_SEP = "://";

    /** Case-insensitive substring test, shared by key names and URL parameter names. */
    /**
     *  Substring test, normalized on both sides exactly as shouldMaskValue(..) normalizes, so
     *  that a parameter name and a config key are judged by one standard. Without the
     *  normalization "apikey" would miss the dotted and hyphenated spellings that dominate in
     *  properties files.
     */
    static boolean maskableName( String name, Set<String> lowercaseSubstrings )
    {
        String normalized = lowercaseLettersDigitsOnly( name );
        for ( String substring : lowercaseSubstrings )
            if ( normalized.indexOf( lowercaseLettersDigitsOnly( substring ) ) >= 0 )
                return true;
        return false;
    }

    /** Whole-name test, for names too short to be safe substrings. */
    static boolean maskableNameExactly( String name, Set<String> lowercaseNames )
    {
        String normalized = lowercaseLettersDigitsOnly( name );
        for ( String exact : lowercaseNames )
            if ( normalized.equals( lowercaseLettersDigitsOnly( exact ) ) )
                return true;
        return false;
    }

    /**
     *  @return the value with any embedded credentials masked, or null if the value is not
     *          URL-shaped at all. Never throws: a malformed URL is treated as prose.
     */
    static String maskUrlCredentials( String value )
    {
        int schemeSep = value.indexOf( SCHEME_SEP );
        if ( schemeSep < 0 ) return null;

        int authorityStart = schemeSep + SCHEME_SEP.length();

        // The authority ends at the first delimiter that can follow it. ';' is among them
        // because SQL Server-style URLs begin their parameters there, with no '?'.
        int authorityEnd = authorityStart;
        while ( authorityEnd < value.length() && "/?;#".indexOf( value.charAt( authorityEnd ) ) < 0 )
            ++authorityEnd;

        StringBuilder out = new StringBuilder( value.length() + 16 );
        out.append( value, 0, authorityStart );
        out.append( maskUserInfo( value.substring( authorityStart, authorityEnd ) ) );
        out.append( maskParameters( value.substring( authorityEnd ) ) );
        return out.toString();
    }

    /** Masks the userinfo of an authority, retaining whether a password accompanied the account. */
    private static String maskUserInfo( String authority )
    {
        // A host cannot contain '@', so the last one separates userinfo from the host. Taking the
        // last rather than the first tolerates an unescaped '@' inside a password, which real
        // configuration files contain even though a URL should percent-encode it.
        int at = authority.lastIndexOf( '@' );
        if ( at < 0 ) return authority;

        String userinfo    = authority.substring( 0, at );
        String fromAtOn    = authority.substring( at );   // retains the '@' and all that follows

        return ( userinfo.indexOf( ':' ) < 0 ? MASK : MASK + ':' + MASK ) + fromAtOn;
    }

    /**
     *  Masks the values of sensitively-named parameters in everything following the authority,
     *  leaving path segments, parameter names and any fragment untouched. Handles '?', '&' and
     *  ';' as parameter delimiters, which also catches a path-borne ';jsessionid=' .
     */
    private static String maskParameters( String afterAuthority )
    {
        if ( afterAuthority.length() == 0 ) return afterAuthority;

        // A fragment carries no parameters we mean to interpret; copy it through untouched.
        int hash = afterAuthority.indexOf( '#' );
        String body     = hash < 0 ? afterAuthority : afterAuthority.substring( 0, hash );
        String fragment = hash < 0 ? ""             : afterAuthority.substring( hash );

        StringBuilder out = new StringBuilder( body.length() + 16 );
        int i = 0;
        while ( i < body.length() )
        {
            char c = body.charAt( i );
            out.append( c );
            ++i;
            if ( "?&;".indexOf( c ) < 0 ) continue;       // ordinary path character

            // Stop at any delimiter, '?' included: a '?' can follow a path-borne matrix
            // parameter, and treating it as part of the value would swallow the query string.
            int end = i;
            while ( end < body.length() && "?&;".indexOf( body.charAt( end ) ) < 0 )
                ++end;
            out.append( maskParameter( body.substring( i, end ) ) );
            i = end;
        }
        return out.append( fragment ).toString();
    }

    private static String maskParameter( String nameAndValue )
    {
        int eq = nameAndValue.indexOf( '=' );
        if ( eq < 0 ) return nameAndValue;                 // a valueless parameter hides nothing

        String name = nameAndValue.substring( 0, eq );
        boolean sensitive = maskableName( name, DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_SUBSTRINGS )
                         || maskableNameExactly( name, DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_EXACT_NAMES );
        return sensitive ? name + '=' + MASK : nameAndValue;
    }

    public interface Transformer
    {
        public boolean transforms(PropertiesMasker masker, String key, String value);
        public String  transformValue(PropertiesMasker masker, String key, String value);
    }

    // note this block has to come after the definition of DANGEROUS_URL_VALUE_TRANSFORMER
    // or else we end up with a null in a broken PropertiesMasker.DEFAULT
    static
    {
        Set<String> tmp = new HashSet<>();
        tmp.add("secret");
        tmp.add("private");
        tmp.add("cred");
        tmp.add("pass");
        tmp.add("pwd");
        tmp.add("token");
        tmp.add("session");
        tmp.add("auth");
        tmp.add("bearer");
        tmp.add("salt");
        tmp.add("apikey");
        tmp.add("accesskey");
        tmp.add("accountkey");
        tmp.add("webhook");
        tmp.add("c3p0.user"); // we know that this one will hit pretty specifically
        DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS = Collections.unmodifiableSet(tmp);

        // The same notions of sensitivity, but tested against URL parameter NAMES rather than
        // config keys, so the two differ at exactly two entries. "c3p0.user" is a key-name test
        // and means nothing as a parameter name; plain "user" is the universal JDBC parameter
        // for the account, and is not caught by any substring above.
        Set<String> tmpUrl = new HashSet<>( tmp );
        tmpUrl.remove("c3p0.user");
        tmpUrl.add("user");

        // In a PRE-SIGNED url the signature is not a byproduct, it is the capability: possession
        // of the url grants the access it describes until it expires. So the signature parameter
        // is masked even though a signature in a config VALUE generally need not be -- publishing
        // a signature does not reveal a signing key, which is why "signature" is absent from the
        // key-name set above.
        //
        // "signature" as a substring covers AWS (X-Amz-Signature), Google V4 (X-Goog-Signature)
        // and Google V2 (Signature), and deliberately does NOT cover X-Amz-SignedHeaders, which
        // is a list of header names and worth seeing. The companion parameters of those schemes
        // are already covered: X-Amz-Credential by "cred", X-Amz-Security-Token by "token".
        tmpUrl.add("signature");
        DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_SUBSTRINGS = Collections.unmodifiableSet( tmpUrl );

        // Azure shared access signatures name theirs "sig", which is too short to use as a
        // substring: normalized, "sig" occurs inside "design", "assign" and "signal", all
        // plausible parameter names. Azure's other sas parameters -- sv, ss, sp, se and the
        // rest -- describe scope and expiry rather than authorizing anything, and are left
        // visible on purpose.
        Set<String> tmpUrlExact = new HashSet<>();
        tmpUrlExact.add("sig");
        DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_EXACT_NAMES = Collections.unmodifiableSet( tmpUrlExact );

        DEFAULT = new PropertiesMasker();
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

    public boolean shouldMaskValue(String key)
    {
        for(String substring : DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS)
            if (lowercaseLettersDigitsOnly(key).indexOf(lowercaseLettersDigitsOnly(substring)) >= 0)
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
                    out.setProperty(key,"****");
                else
                    out.setProperty(key,raw.getProperty(key));
            }
        }

        return out;
    }

    // we're presuming keys that we care about will not contain
    // Unicode "supplementary characters" not renderable in two bytes
    private static String lowercaseLettersDigitsOnly(String s)
    {
        String slc = s.toLowerCase();
        int len = slc.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; ++i)
        {
            char c = slc.charAt(i);
            if (Character.isLetterOrDigit(c))
                sb.append(c);
        }
        return sb.toString();
    }
}
