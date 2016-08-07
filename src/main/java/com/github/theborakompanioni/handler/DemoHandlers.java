package com.github.theborakompanioni.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import io.vertx.ext.web.templ.TemplateEngine;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.handler.impl.ApplicationLogoutHandler;
import org.pac4j.vertx.handler.impl.Pac4jAuthHandlerOptions;
import org.pac4j.vertx.handler.impl.RequiresAuthenticationHandler;

import java.util.Optional;
import java.util.function.BiConsumer;

public class DemoHandlers {
    private static TemplateEngine engine = HandlebarsTemplateEngine.create();

    public static Handler<RoutingContext> addUserProfileToContext() {
        String key = "userProfile";
        return rc -> {

            if (rc.get(key) == null) {
                final Optional<CommonProfile> profile = getUserProfile(rc);
                rc.put(key, profile.orElse(null));
            }

            rc.next();
        };
    }

    public static Handler<RoutingContext> indexHandler() {
        return rc -> {
            rc.put("name", "Vert.x Web");
            engine.render(rc, "templates/index.hbs", res -> {
                if (res.succeeded()) {
                    rc.response().end(res.result());
                } else {
                    rc.fail(res.cause());
                }
            });
        };
    }

    public static Handler<RoutingContext> authHandler(final Vertx vertx,
                                                      final Config config,
                                                      final Pac4jAuthProvider provider,
                                                      final Pac4jAuthHandlerOptions options) {
        return new RequiresAuthenticationHandler(vertx, config, provider, options);
    }

    public static Handler<RoutingContext> logoutHandler() {
        return new ApplicationLogoutHandler();
    }

    public static Handler<RoutingContext> protectedIndexHandler() {
        return generateProtectedIndex((rc, buf) -> rc.response().end(buf));
    }

    public static Handler<RoutingContext> jwtGenerator(JwtGenerator<CommonProfile> generator) {
        return rc -> {
            final Optional<CommonProfile> profile = getUserProfile(rc);
            String token = profile.map(generator::generate).orElse("YOU_MUST_ME_LOGGED_IN");
            rc.put("token", token);

            engine.render(rc, "templates/jwt.hbs", res -> {
                if (res.succeeded()) {
                    rc.response().end(res.result());
                } else {
                    rc.fail(res.cause());
                }
            });
        };
    }

    public static Handler<RoutingContext> generateProtectedIndex(final BiConsumer<RoutingContext, Buffer> generatedContentConsumer) {
        return rc -> {
            engine.render(rc, "templates/protectedIndex.hbs", res -> {
                if (res.succeeded()) {
                    generatedContentConsumer.accept(rc, res.result());
                } else {
                    rc.fail(res.cause());
                }
            });
        };
    }

    public static Handler<RoutingContext> notFoundHandler() {
        return ctx -> {
            ctx.fail(404);
        };
    }

    public static Handler<RoutingContext> failureHandler() {
        boolean displayExceptionDetails = true;
        return ErrorHandler.create(displayExceptionDetails);
    }

    private static Optional<CommonProfile> getUserProfile(final RoutingContext rc) {
        final ProfileManager<CommonProfile> profileManager = new VertxProfileManager<>(new VertxWebContext(rc));
        return Optional.ofNullable(profileManager.get(true));
    }
}
