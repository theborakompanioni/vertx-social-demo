package com.github.theborakompanioni.verticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import rx.Observable;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();

        Observable.just(new DeploymentOptions().setConfig(this.config()))
                .flatMap(options -> vertx.deployVerticleObservable(DemoServerVerticle.class.getName(), options))
                .subscribe(string -> {
                    LOG.info("Demo server verticle deployed with deployment id '" + string + "'");
                    startFuture.complete();
                });
    }
}