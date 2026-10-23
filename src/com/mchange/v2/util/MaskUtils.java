package com.mchange.v2.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *  The policy of value masking, held apart from any one place it is applied: which names are
 *  taken to be sensitive, and how a credential carried <i>inside</i> a URL is concealed.
 *  Stateless, and with no dependence on {@link java.util.Properties}, so a caller holding a
 *  single String can reach it. {@link PropertiesMasker} is this same policy applied across a
 *  whole <code>Properties</code>, and carries the account of why masking is wanted at all.
 *
 *  <p>The separation earns its keep because the places that most need masking are not all
 *  property dumps. A <code>toString()</code> that reports a JDBC URL, an exception message
 *  naming the URL that failed to parse, a log line reporting a JNDI environment -- each holds
 *  one value and wants one answer, and should not have to assemble a <code>Properties</code>
 *  to get it.</p>
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
 *  <code>******</code> for everything would disclose nothing and be worthless. The retentions are
 *  as much a part of the contract as the maskings. Two are worth naming, because the obvious
 *  substrings would have destroyed them. <code>key</code> and <code>user</code> are substrings
 *  of a wide variety of useful-to-debug configuration. <code>user</code> would mask <code>user.home</code> and
 *  <code>user.dir</code>, which are exactly what someone debugging a <code>file:</code> config
 *  source or a HOCON <code>${user.home}</code> substitution needs to see. Hence the narrow
 *  compounds (<code>apikey</code>, <code>accesskey</code>). We don't consider <code>user</code>
 *  sensitive, and don't mask it here (although users are welcome to override
 *  <code>public boolean shouldMaskValue(String key)</code> in our sibling <code>PropertiesMasker</code>
 *  to mask anything they wish).
 *
 *  <h3>What we do not do</h3>
 *
 *  <ul>
 *    <li><b>A secret in a URL path is out of reach.</b> A Slack webhook is
 *        <code>https://hooks.slack.com/services/T000/B000/XXXX</code> -- the credential is a
 *        path segment, not userinfo and not a parameter. Masking paths wholesale would destroy
 *        the diagnostic value of every JDBC URL, so such values are caught only when their key
 *        name gives them away, which is a judgement this class cannot make from the value
 *        alone.</li>
 *    <li><b>Name heuristics are a denylist.</b> A credential under a name containing no
 *        sensitive substring is disclosed. Nothing here can know otherwise.</li>
 *  </ul>
 */
public final class MaskUtils
{
    /**
     *  Names containing any of these, normalized, are taken to name a secret. Tested against
     *  configuration keys by {@link PropertiesMasker#shouldMaskValue}. Unmodifiable, because a
     *  caller able to empty it could disable masking for the whole process.
     */
    public final static Set<String> DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS;

    /** As above, but tested against URL parameter names. See {@link #maskUrlCredentials}. */
    public final static Set<String> DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_SUBSTRINGS;

    /**
     *  URL parameter names matched WHOLE rather than as substrings, for credential parameters
     *  whose names are too short to be safe substrings. See {@link #maskUrlCredentials}.
     */
    public final static Set<String> DEFAULT_MASKABLE_LOWERCASE_URL_PARAM_EXACT_NAMES;

    // Order within this block matters: the URL parameter set is derived from the key-name set,
    // which is why all three live here rather than beside the code that consults each.
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
        DEFAULT_MASKABLE_LOWERCASE_SUBSTRINGS = Collections.unmodifiableSet(tmp);

        // the same notions of sensitivity, tested against URL parameter names rather than config keys,
        // diverging only where noted below.
        Set<String> tmpUrl = new HashSet<>( tmp );

        // Bare "key" is excluded from the key-name set because it would mask this library's own
        // configuration -- whitelistBaseKey, overrideWhitelistKey and their kind. That argument
        // is about CONFIG KEYS and does not carry over to parameter names, where "?key=" is how
        // Google's APIs and SQLCipher pass a secret. It subsumes the apikey/accesskey/accountkey
        // compounds inherited above, harmlessly, and it will also claim keystore, sslkey and
        // keywords -- a path or a word list masked is the cheap error, by the asymmetry this
        // class is built on.
        tmpUrl.add("key");

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
    }

    public final static String MASK = "******"; // consistent with how c3p0's AuthMaskingProperties used to mask

    private final static String SCHEME_SEP = "://";

    /**
     *  Substring test, normalized on both sides exactly as
     *  {@link PropertiesMasker#shouldMaskValue} normalizes, so
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
     *  <p>A value is URL-shaped if it begins with a scheme, and an authority is not required.
     *  Parameters do not need one, and most embedded databases put their credentials exactly
     *  there -- <code>jdbc:h2:mem:db;USER=sa;PASSWORD=x</code>,
     *  <code>jdbc:derby:memory:db;user=app;password=x</code>,
     *  <code>jdbc:hsqldb:file:/db;user=SA;password=x</code>. Testing for
     *  <code>&#47;&#47;</code> asks after the authority and concludes from its absence that
     *  there is nothing to read, which is wrong for every one of those.</p>
     *
     *  <p>Everything not itself a secret is retained: the scheme, host, port, path, every
     *  parameter name, the fragment, and the structure throughout. A masked userinfo still shows
     *  whether a password accompanied the account, because "was a password supplied here" is
     *  usually the question being asked of a dump -- and shows the account itself where one did:
     *  <code>mickey:******</code> for a two-component userinfo, <code>******</code> for a
     *  one-component one.</p>
     *
     *  <p>The single-component form is masked whole on purpose, although accounts are not
     *  otherwise masked. Position is what distinguishes an account from a secret in a userinfo,
     *  and with one component there is nothing to distinguish: a bare userinfo is sometimes a
     *  bearer token rather than a username -- <code>https://TOKEN@github.com/org/repo.git</code>
     *  is how a personal access token is embedded in a git remote. Masking an account costs one
     *  diagnostic; disclosing a token cannot be undone.</p>
     *
     *  <p>This is deliberately NOT built on {@link java.net.URI}. A JDBC URL is opaque to it --
     *  the scheme is <code>jdbc</code> and the remainder is the scheme-specific part -- so
     *  getUserInfo(), getHost() and getQuery() all return null for precisely the values that
     *  most need masking.</p>
     *
     *  @return the value with any embedded credentials masked, or null if the value carries no
     *          scheme and so is not URL-shaped at all. A URL with nothing to mask comes back
     *          unchanged and equal to the input, which is how a caller tells "declined" from
     *          "nothing needed". Never throws: a malformed URL is treated as prose.
     */
    public static String maskUrlCredentials( String value )
    {
        if (value != null)
        {
            int schemeSep = value.indexOf( SCHEME_SEP );
            if ( schemeSep < 0 ) return maskSchemeOnlyUrlCredentials( value );

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
        else
            return null;
    }

    /**
     *  A value with a scheme but no authority. There is no userinfo to mask -- userinfo lives in
     *  an authority -- but the parameters do not need one, so everything after the scheme goes
     *  through the ordinary parameter scan.
     *
     *  <p>Oracle's thin driver stays a miss, and knowingly:
     *  <code>jdbc:oracle:thin:scott/tiger@//host:1521/db</code> writes the account and password
     *  as <code>user/password@</code>, behind no delimiter this recognizes. Reading an
     *  <code>@</code> without an authority to anchor it would mask path segments in every URL
     *  containing one, and that retention is worth more than this one driver's unusual
     *  spelling. Such a value is caught by its key name or not at all.</p>
     */
    private static String maskSchemeOnlyUrlCredentials( String value )
    {
        int schemeEnd = schemeEnd( value );
        if ( schemeEnd < 0 ) return null;

        return value.substring( 0, schemeEnd ) + maskParameters( value.substring( schemeEnd ) );
    }

    /**
     *  @return the index just past the ':' terminating an RFC 3986 scheme, or -1 if the value
     *          does not begin with one. This is what "URL-shaped" means where there is no
     *          authority to go by, and every JDBC URL has a scheme whatever follows it.
     */
    private static int schemeEnd( String value )
    {
        int colon = value.indexOf( ':' );
        if ( colon < 1 || !Character.isLetter( value.charAt( 0 ) ) ) return -1;
        for ( int i = 1; i < colon; ++i )
        {
            char c = value.charAt( i );
            if ( !Character.isLetterOrDigit( c ) && c != '+' && c != '-' && c != '.' ) return -1;
        }
        return colon + 1;
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

        int passwordColon = userinfo.indexOf( ':' );
        return ( passwordColon < 0 ? MASK : userinfo.substring(0,passwordColon) + ':' + MASK ) + fromAtOn;
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

    // we're presuming keys that we care about will not contain
    // Unicode "supplementary characters" not renderable in two bytes
    public static String lowercaseLettersDigitsOnly(String s)
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

    private MaskUtils() {}
}
