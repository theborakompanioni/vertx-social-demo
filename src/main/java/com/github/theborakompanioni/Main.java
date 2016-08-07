package com.github.theborakompanioni;


import com.google.common.base.Throwables;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import com.github.theborakompanioni.util.InputStreams;
import com.github.theborakompanioni.verticle.MainVerticle;

import java.io.IOException;
import java.io.InputStream;

public class Main extends Launcher {

    private static Vertx vertx;

    public static void main(String[] args) {
        vertx = Vertx.vertx(getOptions());
        vertx.deployVerticle(getVerticleClassName(), getDeploymentOptions());

        Runtime.getRuntime().addShutdownHook(getShutdownHook());
    }

    private static JsonObject getConfig() {
        String descriptorFile = "conf/my-conf.json";
        try {
            try (InputStream is = Main.class.getClassLoader().getResourceAsStream(descriptorFile)) {
                return InputStreams.readAsJson(is);
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static VertxOptions getOptions() {
        return new VertxOptions(getConfig());
    }


    private static String getVerticleClassName() {
        return MainVerticle.class.getName();
    }

    private static DeploymentOptions getDeploymentOptions() {
        return new DeploymentOptions()
                .setConfig(getConfig())
                .setWorker(false);
    }

    private static Thread getShutdownHook() {
        return new Thread() {
            public void run() {
                vertx.close(event -> {
                });
            }
        };
    }

}