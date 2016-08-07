package com.github.theborakompanioni.verticle;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SessionHandlerRegexTest {

    @Test
    public void testRegexpExcludesCorrectly() {
        final Pattern pattern = pattern();
        assertThat(pattern.matcher("/no-session/index.html").matches(), is(false));
        assertThat(pattern.matcher("/rest-jwt/api/v1/test_request").matches(), is(false));
    }

    @Test
    public void testRegexpIncludesCorrectly() {
        final Pattern pattern = pattern();
        assertThat(pattern.matcher("/oauth/callback").matches(), is(true));
        assertThat(pattern.matcher("/github/index.html").matches(), is(true));
        assertThat(pattern.matcher("/githubadmin/index.html").matches(), is(true));
        assertThat(pattern.matcher("/githubtest/index.html").matches(), is(true));
        assertThat(pattern.matcher("/facebook/index.html").matches(), is(true));
        assertThat(pattern.matcher("/twitter/index.html").matches(), is(true));
        assertThat(pattern.matcher("/protected/index.html").matches(), is(true));
    }

    Pattern pattern() {
        // This mimics the "starts with match" behaviour of regex route matching in vert.x
        return Pattern.compile(DemoServerVerticle.SESSION_HANDLER_REGEXP);
    }
}
