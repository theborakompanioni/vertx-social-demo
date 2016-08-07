package com.github.theborakompanioni.util;

import com.google.common.base.Charsets;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static java.util.Objects.requireNonNull;

public final class InputStreams {
    private InputStreams() {
        throw new UnsupportedOperationException();
    }

    public static JsonObject readAsJson(InputStream is) {
        try {
            return new JsonObject(readAsString(is));
        } catch (DecodeException e) {
            throw new IllegalArgumentException("InputStream contains invalid json");
        }
    }

    public static String readAsString(InputStream is) {
        requireNonNull(is);
        try (Scanner scanner = new Scanner(is, Charsets.UTF_8.name()).useDelimiter("\\A")) {
            return scanner.next();
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("InputStream is empty");
        }
    }
}
