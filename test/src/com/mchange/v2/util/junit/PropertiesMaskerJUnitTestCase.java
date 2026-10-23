package com.mchange.v2.util.junit;

import junit.framework.TestCase;

import com.mchange.v2.util.MaskUtils;
import com.mchange.v2.util.PropertiesMasker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 *  PropertiesMasker, which exists so that a dump of configuration can be useful without being
 *  dangerous. Two mechanisms, deliberately separate:
 *
 *  <ul>
 *    <li><b>Key names.</b> A key whose name contains a sensitive substring has its whole value
 *        replaced. The test is case-insensitive and separator-insensitive, because properties
 *        spell the same concept as <code>apiKey</code>, <code>api.key</code>,
 *        <code>api-key</code> and <code>api_key</code> interchangeably.</li>
 *    <li><b>Values.</b> A Transformer may rewrite a value that key names cannot judge. The one
 *        supplied handles URLs, where credentials hide in userinfo and in parameters under a key
 *        -- <code>jdbcUrl</code> -- that is not sensitive by name at all.</li>
 *  </ul>
 *
 *  <h3>What these tests are mostly defending</h3>
 *
 *  <p>Masking fails in two directions and they are not symmetric. Failing to mask leaks a
 *  credential; masking too much costs one diagnostic value. So the cases below assert the
 *  retentions as carefully as the maskings -- a masker that masked every value for
 *  everything would pass every leak test here and be useless.</p>
 *
 *  <p>The retentions that matter most are this library's own non-secret names. Its configuration
 *  is full of keys ending in <code>Key</code> (<code>whitelistBaseKey</code>,
 *  <code>overrideWhitelistKey</code>), and <code>user.home</code> and <code>user.dir</code> are
 *  exactly what someone debugging a <code>file:</code> config source or a HOCON
 *  <code>${user.home}</code> substitution needs to see. A bare <code>key</code> or
 *  <code>user</code> substring would have been the obvious thing to mask and would have gutted
 *  the dump.</p>
 */
public final class PropertiesMaskerJUnitTestCase extends TestCase
{
    /** Spelled once, in MaskUtilsJUnitTestCase.testTheMaskIsSixAsterisks. */
    private final static String MASK = MaskUtils.MASK;

    private PropertiesMasker masker;

    @Override
    protected void setUp()
    { masker = new PropertiesMasker(); }   // the default: URL transformer wired in, no prefix restriction

    private String maskedValue( String key, String value )
    {
        Properties raw = new Properties();
        raw.setProperty( key, value );
        return masker.masked( raw ).getProperty( key );
    }

    /** The URL transformer alone, bypassing key-name masking. */
    private String transformed( String value )
    {
        String key = "harmless.url";   // not sensitive by name, so the transformer gets its turn
        return maskedValue( key, value );
    }

    // ---------------------------------------------------------------- key names

    public void testObviouslySensitiveKeysAreMasked()
    {
        String[] keys = { "foo.password", "foo.secret", "foo.token", "foo.auth", "foo.credentials",
                          "foo.pwd", "foo.bearerToken", "foo.salt", "foo.privateKey", "foo.sessionId",
                          "foo.apiKey", "aws.accessKeyId", "azure.storage.accountKey", "notify.webhook" };
        for ( String key : keys )
            assertEquals( "should have been masked: " + key, MASK, maskedValue( key, "s3cret" ) );
    }

    /** Case is irrelevant. This was once broken by a computed-but-unused lowercased key. */
    public void testKeyMatchingIsCaseInsensitive()
    {
        for ( String key : new String[] { "foo.password", "foo.PASSWORD", "foo.Password", "FOO.PaSsWoRd" } )
            assertEquals( key, MASK, maskedValue( key, "s3cret" ) );
    }

    /**
     *  Separators are irrelevant too, which is the whole reason the test normalizes rather than
     *  merely lowercasing: properties conventionally use dots, so a substring of
     *  <code>apikey</code> would otherwise match only the camel-cased spelling.
     */
    public void testKeyMatchingIsSeparatorInsensitive()
    {
        for ( String key : new String[] { "myapp.apiKey", "myapp.api.key", "myapp.api-key", "myapp.api_key" } )
            assertEquals( key, MASK, maskedValue( key, "s3cret" ) );
    }

    /**
     *  The retentions. A masker that failed these would be safe and worthless, and the first two
     *  are this library's own configuration keys.
     */
    public void testNonSensitiveKeysAreRetained()
    {
        String[][] retained = {
            { "whitelistBaseKey",     "com.mchange.v2.naming.objectFactory.whitelist" },
            { "overrideWhitelistKey", "com.mchange.v2.naming.objectFactory.overrideWhitelist" },
            { "c3p0.maxPoolSize",     "30" },
            { "user.home",            "/home/someone" },
            { "user.dir",             "/srv/app" },
            { "java.version",         "11.0.2" },
        };
        for ( String[] kv : retained )
            assertEquals( "should have been left alone: " + kv[0], kv[1], maskedValue( kv[0], kv[1] ) );
    }

    /**
     *  c3p0's account name is masked while the JVM's user.* properties are not. That tension is
     *  why the key set names 'c3p0.user' specifically rather than carrying a bare 'user'.
     */
    /**
     *  No key naming an account is masked any more, c3p0's own included. A username identifies
     *  rather than authorizes, so the asymmetry that licenses loose masking -- a leaked
     *  credential cannot be recalled, a masked diagnostic costs one value -- does not apply to
     *  it. And with per-user pools, "which user does this DataSource act as" is the question
     *  being asked of the dump.
     *
     *  <p>Before 0.15.0 c3p0 masked <code>user</code> alongside <code>password</code>, and
     *  <code>c3p0.user</code> was a named entry in the key-name set for exactly that reason.</p>
     */
    public void testNoKeyNamingAnAccountIsMasked()
    {
        assertEquals( "appuser", maskedValue( "c3p0.user", "appuser" ) );
        assertEquals( "appuser", maskedValue( "user", "appuser" ) );
        assertEquals( "/home/someone", maskedValue( "user.home", "/home/someone" ) );
        assertEquals( "someone", maskedValue( "user.name", "someone" ) );
    }

    /** A masked key is still reported as present: that it was set is usually the question. */
    public void testMaskingPreservesTheKeyItself()
    {
        Properties raw = new Properties();
        raw.setProperty( "foo.password", "s3cret" );
        Properties out = masker.masked( raw );

        assertTrue( "the key must survive masking", out.stringPropertyNames().contains( "foo.password" ) );
        assertFalse( "the secret must not survive", out.getProperty( "foo.password" ).contains( "s3cret" ) );
    }

    public void testShouldMaskValueAgreesWithWhatMaskedDoes()
    {
        for ( String key : new String[] { "foo.password", "myapp.api.key", "c3p0.maxPoolSize", "user.home" } )
            assertEquals( key,
                          masker.shouldMaskValue( key ),
                          MASK.equals( maskedValue( key, "some-value" ) ) );
    }

    // ---------------------------------------------------------------- URL values

    public void testQueryParameterCredentialsAreMasked()
    {
        assertEquals( "jdbc:postgresql://db:5432/prod?user=app&password=" + MASK + "&ssl=true",
                      transformed( "jdbc:postgresql://db:5432/prod?user=app&password=hunter2&ssl=true" ) );
    }

    /**
     *  SQL Server-style URLs carry their parameters directly after the authority, separated by
     *  ';' and with no '?' at all, so ';' has to be a parameter delimiter and not just a path
     *  character.
     */
    public void testSemicolonSeparatedParametersAreMasked()
    {
        assertEquals( "jdbc:sqlserver://host:1433;databaseName=prod;user=sa;password=" + MASK + ";encrypt=true",
                      transformed( "jdbc:sqlserver://host:1433;databaseName=prod;user=sa;password=S3cr3t;encrypt=true" ) );
    }

    /** The account survives, the password does not. Position says which is which. */
    public void testUserInfoCredentialsAreMasked()
    {
        assertEquals( "postgresql://app:" + MASK + "@db:5432/prod",
                      transformed( "postgresql://app:hunter2@db:5432/prod" ) );
    }

    /**
     *  A masked userinfo still distinguishes an account with a password from an account without
     *  one, because "was a password supplied here" is usually what a dump is being asked.
     */
    /**
     *  "Was a password supplied here" is usually the question being asked, and the two cases stay
     *  distinguishable: one component masks to a bare mask, two components keep the account and
     *  mask the second.
     *
     *  <p>So the account is shown precisely where position proves it IS an account. See
     *  testASingleComponentUserInfoIsMaskedWhole for why the other case does not.</p>
     */
    public void testUserInfoMaskingRetainsWhetherAPasswordWasPresent()
    {
        assertEquals( "https://" + MASK + "@example.com/path",
                      transformed( "https://app@example.com/path" ) );
        assertEquals( "https://app:" + MASK + "@example.com/path",
                      transformed( "https://app:hunter2@example.com/path" ) );
    }

    /**
     *  THE ONE EXCEPTION TO ACCOUNTS NOT BEING MASKED, and the reasoning is worth keeping
     *  because the exception looks inconsistent with the rest of the policy.
     *
     *  <p>Elsewhere a name identifies the thing being judged: a key called <code>user</code>, a
     *  parameter called <code>user</code>. A userinfo has no names, only positions -- it is
     *  <code>user[:password]</code> -- so with two components position proves which is the
     *  secret and the account is shown, while with one component nothing proves anything. And a
     *  single-component userinfo is often not an account at all but a bearer token:
     *  <code>https://ghp_xxxx@github.com/org/repo.git</code> is how a personal access token is
     *  embedded in a git remote.</p>
     *
     *  <p>So the asymmetry decides it, the same asymmetry that licenses the loose substring
     *  tests: masking an account costs one diagnostic value, disclosing a token cannot be
     *  undone. Err toward overmasking exactly here, where the evidence runs out.</p>
     */
    public void testASingleComponentUserInfoIsMaskedWhole()
    {
        assertEquals( "https://" + MASK + "@github.com/org/repo.git",
                      transformed( "https://ghp_notreallyatoken@github.com/org/repo.git" ) );

        // ... while two components still show the account, so this is about the ambiguity and
        // not a retreat from the username policy.
        assertEquals( "https://app:" + MASK + "@github.com/org/repo.git",
                      transformed( "https://app:ghp_notreallyatoken@github.com/org/repo.git" ) );
    }

    /** An unescaped '@' in a password is invalid but common; taking the LAST '@' tolerates it. */
    public void testUnescapedAtSignInPasswordIsStillMasked()
    {
        String out = transformed( "postgresql://app:p@sswor@d@db:5432/prod" );
        assertEquals( "postgresql://app:" + MASK + "@db:5432/prod", out );
        assertFalse( out.contains( "sswor" ) );
    }

    /** Scheme, host, port, path, parameter names and fragment all survive. */
    public void testEverythingNotSecretIsRetained()
    {
        String out = transformed( "scheme://u:p@host:99/a/b?token=xyz&page=4#frag" );
        assertTrue( out, out.startsWith( "scheme://" ) );
        assertTrue( out, out.contains( "@host:99/a/b" ) );
        assertTrue( "parameter names are retained", out.contains( "token=" ) && out.contains( "page=4" ) );
        assertTrue( "the fragment is retained", out.endsWith( "#frag" ) );
        assertFalse( "the token value is not", out.contains( "xyz" ) );
    }

    /** A path-borne matrix parameter is masked, and must not swallow the query string after it. */
    public void testMatrixParameterIsMaskedWithoutSwallowingTheQueryString()
    {
        assertEquals( "http://h/path;jsessionid=" + MASK + "?token=" + MASK + "&page=4",
                      transformed( "http://h/path;jsessionid=ABC123?token=xyz&page=4" ) );
    }

    // ---------------------------------------------------------------- pre-signed URLs

    //  In a pre-signed URL the signature is not a byproduct, it is the capability: possession of
    //  the URL grants the access it describes until it expires. That is why 'signature' is in the
    //  URL parameter set but NOT in the key-name set -- a signature held in a config value is
    //  generally harmless, since publishing one does not reveal a signing key.

    public void testAwsPreSignedSignatureIsMasked()
    {
        String out = transformed( "https://b.s3.amazonaws.com/k?X-Amz-Algorithm=AWS4-HMAC-SHA256"
                                + "&X-Amz-Credential=AKIA123&X-Amz-Date=20260101T000000Z"
                                + "&X-Amz-Expires=900&X-Amz-SignedHeaders=host&X-Amz-Signature=deadbeefcafe" );
        assertFalse( "the signature authorizes; it must go: " + out, out.contains( "deadbeefcafe" ) );
        assertTrue( "SignedHeaders is a list of header names, and is worth seeing: " + out,
                    out.contains( "X-Amz-SignedHeaders=host" ) );
        assertTrue( "expiry describes the grant rather than authorizing it: " + out,
                    out.contains( "X-Amz-Expires=900" ) );
    }

    /** Azure names its signature 'sig', which is matched as a whole name rather than a substring. */
    public void testAzureSasSignatureIsMasked()
    {
        String out = transformed( "https://acct.blob.core.windows.net/c/b?sv=2022-11-02&ss=b&sp=r"
                                + "&se=2026-01-01T00:00:00Z&sig=AbCdEf123" );
        assertTrue( out, out.contains( "sig=" + MASK ) );
        assertFalse( out, out.contains( "AbCdEf123" ) );
        assertTrue( "scope and expiry are retained: " + out,
                    out.contains( "sv=2022-11-02" ) && out.contains( "sp=r" ) && out.contains( "se=2026-01-01T00:00:00Z" ) );
    }

    public void testGoogleSignedUrlSignaturesAreMasked()
    {
        String v2 = transformed( "https://storage.googleapis.com/b/o?GoogleAccessId=svc&Expires=1800000000&Signature=Zm9vYmFy" );
        assertFalse( v2, v2.contains( "Zm9vYmFy" ) );

        String v4 = transformed( "https://storage.googleapis.com/b/o?X-Goog-Algorithm=GOOG4-RSA-SHA256"
                               + "&X-Goog-Credential=svc&X-Goog-Expires=900&X-Goog-SignedHeaders=host&X-Goog-Signature=abc123" );
        assertFalse( v4, v4.contains( "abc123" ) );
        assertTrue( "V4 SignedHeaders retained: " + v4, v4.contains( "X-Goog-SignedHeaders=host" ) );
    }

    /**
     *  'sig' is matched whole precisely so these survive: normalized, "sig" occurs inside each of
     *  them, and a substring test would have masked all three.
     */
    public void testShortSignatureNameDoesNotMaskWordsContainingIt()
    {
        String out = transformed( "https://h/p?design=modern&assign=bob&signal=green&sig=SECRET" );
        assertTrue( out, out.contains( "design=modern" ) );
        assertTrue( out, out.contains( "assign=bob" ) );
        assertTrue( out, out.contains( "signal=green" ) );
        assertTrue( "and the real one is still masked: " + out, out.contains( "sig=" + MASK ) );
    }

    /** Parameter names are normalized exactly as keys are. */
    public void testParameterNameMatchingIsSeparatorInsensitive()
    {
        String out = transformed( "https://h/p?api.key=K1&api-key=K2&access_key=K3&plain=ok" );
        assertFalse( out, out.contains( "K1" ) || out.contains( "K2" ) || out.contains( "K3" ) );
        assertTrue( "an unremarkable parameter survives: " + out, out.contains( "plain=ok" ) );
    }

    // ---------------------------------------------------------------- when NOT to transform

    public void testUrlsWithoutCredentialsAreUntouched()
    {
        String url = "jdbc:postgresql://db:5432/prod?ssl=true&ApplicationName=svc";
        assertEquals( url, transformed( url ) );
    }

    public void testNonUrlValuesAreUntouched()
    {
        assertEquals( "jdbc:h2:mem:test", transformed( "jdbc:h2:mem:test" ) );
        assertEquals( "this is prose", transformed( "this is prose" ) );
        assertEquals( "30", transformed( "30" ) );
    }

    /**
     *  Transformers get first refusal, and claiming a value suppresses key-name masking for that
     *  key -- so a transformer CHOOSES whether to defer. This pins the choice the supplied URL
     *  transformer makes, which is to decline: a key named 'foo.password' says nothing about
     *  what else its value might carry, so partially revealing it is a worse answer than masking
     *  it whole. That is this transformer's policy rather than a rule of the class, and another
     *  transformer may legitimately decide otherwise.
     *
     *  <p>Every spelling, because the deferral once used a different notion of a sensitive name
     *  than shouldMaskValue(..) did, and the dotted spellings fell between them.</p>
     */
    public void testTheUrlTransformerDeclinesValuesUnderSensitiveKeys()
    {
        String url = "jdbc:postgresql://h/db?user=u&password=p";
        for ( String key : new String[] { "foo.password", "myapp.apiKey", "myapp.api.key", "aws.access.key.id" } )
            assertEquals( "a sensitive key must mask the whole value: " + key, MASK, maskedValue( key, url ) );
    }

    /** ...while an insensitive key leaves it free to do its finer-grained work. */
    public void testAnInsensitiveKeyLetsTheTransformerAct()
    {
        assertEquals( "jdbc:postgresql://h/db?user=u&password=" + MASK,
                      maskedValue( "c3p0.jdbcUrl", "jdbc:postgresql://h/db?user=u&password=p" ) );
    }

    // ---------------------------------------------------------------- construction and prefix

    /** The safe configuration is the default one: no argument needed to get URL masking. */
    public void testDefaultConstructorInstallsTheUrlTransformer()
    {
        assertEquals( "jdbc:postgresql://h/db?password=" + MASK,
                      new PropertiesMasker().masked( propsOf( "c3p0.jdbcUrl", "jdbc:postgresql://h/db?password=p" ) )
                                            .getProperty( "c3p0.jdbcUrl" ) );
    }

    /** With no transformers at all, key-name masking still applies and URLs pass through whole. */
    public void testWithoutTransformersOnlyKeyMaskingApplies()
    {
        PropertiesMasker bare = new PropertiesMasker( Collections.<PropertiesMasker.Transformer>emptySet() );
        Properties out = bare.masked( propsOf( "c3p0.jdbcUrl", "jdbc:postgresql://h/db?password=p",
                                               "foo.password",  "s3cret" ) );
        assertEquals( "jdbc:postgresql://h/db?password=p", out.getProperty( "c3p0.jdbcUrl" ) );
        assertEquals( MASK, out.getProperty( "foo.password" ) );
    }

    /**
     *  restrictToPrefix OMITS non-matching keys rather than passing them through. That is the
     *  stronger behaviour -- a dump restricted to one library's keys cannot leak another
     *  application's properties at all -- and worth pinning, since "restrict" could as easily
     *  have meant "only mask within".
     */
    public void testRestrictToPrefixOmitsEverythingElse()
    {
        PropertiesMasker restricted =
            new PropertiesMasker( Collections.singleton( PropertiesMasker.DANGEROUS_URL_VALUE_TRANSFORMER ),
                                  "com.mchange." );
        Properties out = restricted.masked( propsOf( "com.mchange.v2.foo", "kept",
                                                     "com.mchange.v2.password", "s3cret",
                                                     "someone.elses.password", "theirs" ) );
        assertEquals( "kept", out.getProperty( "com.mchange.v2.foo" ) );
        assertEquals( MASK,   out.getProperty( "com.mchange.v2.password" ) );
        assertNull( "a key outside the prefix must not appear at all",
                    out.getProperty( "someone.elses.password" ) );
        assertEquals( 2, out.stringPropertyNames().size() );
    }

    /** Mutating the set a caller passed must not change what the masker does afterward. */
    public void testTransformerSetIsCopiedAtConstruction()
    {
        Set<PropertiesMasker.Transformer> supplied = new HashSet<PropertiesMasker.Transformer>();
        supplied.add( PropertiesMasker.DANGEROUS_URL_VALUE_TRANSFORMER );
        PropertiesMasker pm = new PropertiesMasker( supplied );

        supplied.clear();

        assertEquals( "the masker must not follow the caller's set",
                      "jdbc:postgresql://h/db?password=" + MASK,
                      pm.masked( propsOf( "c3p0.jdbcUrl", "jdbc:postgresql://h/db?password=p" ) )
                        .getProperty( "c3p0.jdbcUrl" ) );
    }

    /**
     *  DEFAULT is built during this class's static initialization, which makes it the one
     *  instance whose construction can observe a half-initialized class. It once did: it was
     *  constructed before DANGEROUS_URL_VALUE_TRANSFORMER was assigned, so it held a set
     *  containing null and threw NullPointerException on every call, while every locally
     *  constructed masker -- which is to say every other test here -- worked perfectly.
     */
    public void testDefaultInstanceBehavesLikeAFreshlyConstructedOne()
    {
        Properties raw = propsOf( "foo.password",  "s3cret",
                                  "c3p0.jdbcUrl",  "jdbc:postgresql://db:5432/prod?user=app&password=p",
                                  "c3p0.maxPoolSize", "30",
                                  "user.home",     "/home/someone" );

        Properties viaDefault = PropertiesMasker.DEFAULT.masked( raw );
        Properties viaFresh   = new PropertiesMasker().masked( raw );

        assertEquals( "DEFAULT must mask exactly as a freshly constructed masker does",
                      viaFresh, viaDefault );
    }

    /**
     *  ...and specifically that DEFAULT's transformer is live, since a DEFAULT holding no usable
     *  transformer would still agree with a fresh masker on key-name masking alone.
     */
    public void testDefaultInstanceHasAWorkingUrlTransformer()
    {
        assertEquals( "jdbc:postgresql://db:5432/prod?user=app&password=" + MASK,
                      PropertiesMasker.DEFAULT
                          .masked( propsOf( "c3p0.jdbcUrl", "jdbc:postgresql://db:5432/prod?user=app&password=p" ) )
                          .getProperty( "c3p0.jdbcUrl" ) );
    }

    private static Properties propsOf( String... keysAndValues )
    {
        Properties props = new Properties();
        for ( int i = 0; i < keysAndValues.length; i += 2 )
            props.setProperty( keysAndValues[i], keysAndValues[i + 1] );
        return props;
    }
}
