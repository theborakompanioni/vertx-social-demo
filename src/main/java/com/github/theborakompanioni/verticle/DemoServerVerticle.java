package com.github.theborakompanioni.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import com.github.theborakompanioni.config.Pac4jFactory;
import com.github.theborakompanioni.handler.DemoHandlers;
import org.pac4j.vertx.handler.impl.CallbackHandler;
import org.pac4j.vertx.handler.impl.Pac4jAuthHandlerOptions;

public class DemoServerVerticle extends AbstractVerticle {
    static final String SESSION_HANDLER_REGEXP = "\\/((?!no-session\\/|rest-jwt\\/)).*";

    private static final Logger LOG = LoggerFactory.getLogger(DemoServerVerticle.class);
    private final Handler<RoutingContext> protectedIndexRenderer = DemoHandlers.protectedIndexHandler();
    private final Pac4jAuthProvider authProvider = new Pac4jAuthProvider(); // We don't need to instantiate this on demand

    private Config pac4jConfig;
    private JwtGenerator<CommonProfile> jwtGenerator;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        // need to add a json configuration file internally and ensure it's consumed by this verticle
        LOG.info("DemoServerVerticle: pac4jConfig is \n" + config().encodePrettily());
        Pac4jFactory pac4JFactory = new Pac4jFactory(config());
        this.pac4jConfig = pac4JFactory.config();
        this.jwtGenerator = pac4JFactory.jwtGenerator();
    }

    @Override
    public void start() {
        Router router = router();

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080);
    }

    private Router router() {
        Router router = Router.router(vertx);

        router.route().failureHandler(DemoHandlers.failureHandler());

        SessionStore sessionStore = LocalSessionStore.create(vertx);
        SessionHandler sessionHandler = SessionHandler.create(sessionStore);
        // Only use the following handlers where we want to use sessions - this is enforced by the regexp
        router.routeWithRegex(SESSION_HANDLER_REGEXP).handler(io.vertx.ext.web.handler.CookieHandler.create());
        router.routeWithRegex(SESSION_HANDLER_REGEXP).handler(sessionHandler);
        router.routeWithRegex(SESSION_HANDLER_REGEXP).handler(UserSessionHandler.create(authProvider));

        router.route().handler(DemoHandlers.addUserProfileToContext());

        // social-authenticated endpoints
        addProtectedEndpoint("/facebook/index.html", "FacebookClient", router);
        addProtectedEndpoint("/twitter/index.html", "TwitterClient", router);
        addProtectedEndpoint("/twitter-github/index.html", "TwitterClient,GitHubClient", router);
        addProtectedEndpoint("/github/index.html", "GitHubClient", router);
        addProtectedEndpoint("/githubadmin/index.html", "GitHubClient", Pac4jFactory.AUTHORIZER_ADMIN, router);
        addProtectedEndpoint("/githubtest/index.html", "GitHubClient", Pac4jFactory.AUTHORIZER_TEST_USER, router);

        // requires authentication endpoint without specific authenticator attached
        addProtectedEndpointWithoutImplicitAuthentication("/protected/index.html", router);

        // token authentication for "rest" endpoint(web services)
        addProtectedEndpoint("/rest-jwt/index.html", "ParameterClient", router);

        router.get("/").handler(DemoHandlers.indexHandler());
        router.get("/index.html").handler(DemoHandlers.indexHandler());

        CallbackHandler callbackHandler = new CallbackHandler(vertx, pac4jConfig);
        router.get("/oauth_callback").handler(callbackHandler); // This will deploy the callback handler
        router.post("/oauth_callback").handler(BodyHandler.create().setMergeFormAttributes(true));
        router.post("/oauth_callback").handler(callbackHandler);

        router.get("/logout").handler(DemoHandlers.logoutHandler());


        router.get("/jwt.html").handler(DemoHandlers.jwtGenerator(jwtGenerator));
        router.get("/*").handler(StaticHandler.create("static"));


        router.route().handler(DemoHandlers.notFoundHandler());
        return router;
    }

    private void addProtectedEndpointWithoutImplicitAuthentication(String url, Router router) {
        Pac4jAuthHandlerOptions options = new Pac4jAuthHandlerOptions().withClientName("");
        addProtectedEndpoint(url, router, options);
    }

    private void addProtectedEndpoint(String url, String clientNames, Router router) {
        Pac4jAuthHandlerOptions options = new Pac4jAuthHandlerOptions().withClientName(clientNames);
        addProtectedEndpoint(url, router, options);
    }

    private void addProtectedEndpoint(String url, String clientNames, String authName, Router router) {
        Pac4jAuthHandlerOptions options = new Pac4jAuthHandlerOptions().withClientName(clientNames)
                .withAuthorizerName(authName);

        addProtectedEndpoint(url, router, options);
    }

    private void addProtectedEndpoint(String url, Router router, Pac4jAuthHandlerOptions options) {
        router.get(url).handler(DemoHandlers.authHandler(vertx, pac4jConfig, authProvider,
                options));
        router.get(url).handler(DemoHandlers.addUserProfileToContext());
        router.get(url).handler(protectedIndexRenderer);
    }

}
