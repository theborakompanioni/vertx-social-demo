package com.github.theborakompanioni.config;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.pac4j.core.authorization.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.http.client.direct.ParameterClient;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.GitHubClient;
import org.pac4j.oauth.client.TwitterClient;
import com.github.theborakompanioni.authorizer.UsernameAuthorizer;
import rx.Observable;

import static java.util.Objects.requireNonNull;

public class Pac4jFactory {

    public static final String AUTHORIZER_ADMIN = "admin";
    public static final String AUTHORIZER_TEST_USER = "test";
    private static final Logger LOG = LoggerFactory.getLogger(Pac4jFactory.class);
    private final String baseUrl;
    private final JsonObject oauthConfig;

    public Pac4jFactory(final JsonObject jsonConf) {
        this.baseUrl = requireNonNull(jsonConf.getString("baseUrl"), "'baseUrl' must be present");
        this.oauthConfig = requireNonNull(jsonConf.getJsonObject("oauth"), "'oauth' must be present");
    }

    static FacebookClient facebookClient(final JsonObject jsonConf) {
        final String id = jsonConf.getString("id");
        final String secret = jsonConf.getString("secret");
        return new FacebookClient(id, secret);
    }

    static TwitterClient twitterClient(final JsonObject jsonConf) {
        final String id = jsonConf.getString("id");
        final String secret = jsonConf.getString("secret");
        return new TwitterClient(id, secret);
    }

    static GitHubClient githubClient(final JsonObject jsonConf) {
        final String id = jsonConf.getString("id");
        final String secret = jsonConf.getString("secret");
        return new GitHubClient(id, secret);
    }

    static Client createClient(String name, JsonObject config) {
        if ("facebook".equals(name)) {
            return facebookClient(config);
        } else if ("github".equals(name)) {
            return githubClient(config);
        } else if ("twitter".equals(name)) {
            return twitterClient(config);
        } else if ("jsonwebtoken".equals(name)) {
            return jwtParameterClient(config);
        }
        throw new IllegalArgumentException("client '" + name + "' not available");
    }

    public Config config() {
        final Observable<String> clientNames = Observable.just("facebook", "github", "twitter", "jsonwebtoken");

        final Client[] configuredClients = clientNames
                .zipWith(clientNames.map(oauthConfig::getJsonObject), Pair::of)
                .filter(pair -> pair.getRight() != null)
                .map(pair -> createClient(pair.getLeft(), pair.getRight()))
                .toList()
                .toBlocking()
                .first().toArray(new Client[]{});


        final Clients clients = new Clients(baseUrl + "/oauth_callback", configuredClients);
        final Config config = new Config(clients);

        config.addAuthorizer(AUTHORIZER_ADMIN, new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
        config.addAuthorizer(AUTHORIZER_TEST_USER, new UsernameAuthorizer("theborakompanioni"));

        LOG.info("Config created " + config.toString());
        return config;
    }

    static ParameterClient jwtParameterClient(JsonObject conf) {
        requireNonNull(conf, "jsonwebtoken config must be present");

        // REST authent with JWT for a token passed in the url as the token parameter
        ParameterClient parameterClient = new ParameterClient("token", new JwtAuthenticator(conf.getString("salt")));
        parameterClient.setSupportGetRequest(true);
        parameterClient.setSupportPostRequest(false);
        return parameterClient;
    }

    public JwtGenerator<CommonProfile> jwtGenerator() {
        JsonObject jwtConf = oauthConfig.getJsonObject("jsonwebtoken");
        requireNonNull(jwtConf, "jsonwebtoken config must be present");

        String salt = requireNonNull(jwtConf.getString("salt"));

        boolean isJavaCryptoInstalled = false;
        final JwtGenerator<CommonProfile> generator = new JwtGenerator<>(salt, isJavaCryptoInstalled);
        return generator;
    }
}
