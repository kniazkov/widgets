/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.protocol.Update;
import com.kniazkov.widgets.view.Widget;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Shared reflection helpers for tests of package-private base infrastructure.
 */
final class BaseTestSupport {
    /**
     * A lifetime that keeps the real watchdog away from ordinary test clients.
     */
    private static final long TEST_CLIENT_LIFETIME = 60_000;

    /**
     * Prevents construction of the reflection helper class.
     */
    private BaseTestSupport() {
        /*
         * Utility class
         */
    }

    /**
     * Creates an application that is ready to create clients without starting an HTTP server.
     *
     * @param page index page
     * @return configured application
     */
    static Application application(final Page page) {
        final Application application = new Application(page);
        final Options options = new Options.Builder()
            .setClientLifetime(TEST_CLIENT_LIFETIME)
            .build();
        application.setOptions(options);
        return application;
    }

    /**
     * Returns the private client registry.
     *
     * @param application application under test
     * @return live registry
     * @throws ReflectiveOperationException if the implementation shape changed
     */
    @SuppressWarnings("unchecked")
    static Map<RMId, Client> clients(final Application application)
            throws ReflectiveOperationException {
        final Field field = Application.class.getDeclaredField("clients");
        field.setAccessible(true);
        return (Map<RMId, Client>) field.get(application);
    }

    /**
     * Replaces the client registry with an instrumented map.
     *
     * @param application application under test
     * @param clients replacement registry
     * @throws ReflectiveOperationException if the implementation shape changed
     */
    static void replaceClients(final Application application, final Map<RMId, Client> clients)
            throws ReflectiveOperationException {
        final Field field = Application.class.getDeclaredField("clients");
        field.setAccessible(true);
        field.set(application, clients);
    }

    /**
     * Replaces a client's pending-update set with an instrumented set.
     *
     * @param client client under test
     * @param updates replacement set
     * @throws ReflectiveOperationException if the implementation shape changed
     */
    static void replaceUpdates(final Client client, final Set<Update> updates)
            throws ReflectiveOperationException {
        final Field field = Client.class.getDeclaredField("updates");
        field.setAccessible(true);
        field.set(client, updates);
    }

    /**
     * Returns a widget's private pending-update collection.
     *
     * @param widget widget under test
     * @return live update collection
     * @throws ReflectiveOperationException if the implementation shape changed
     */
    @SuppressWarnings("unchecked")
    static Collection<Update> updates(final Widget<?> widget)
            throws ReflectiveOperationException {
        final Field field = Widget.class.getDeclaredField("updates");
        field.setAccessible(true);
        return (Collection<Update>) field.get(widget);
    }

    /**
     * Executes one watchdog pass synchronously so tests can control the interleaving.
     *
     * @param application application whose watchdog should run
     * @return the watchdog's continuation flag
     * @throws ReflectiveOperationException if the implementation shape changed or the tick failed
     */
    static boolean tickWatchdog(final Application application)
            throws ReflectiveOperationException {
        final Class<?> type = Class.forName(
            "com.kniazkov.widgets.base.Application$Watchdog"
        );
        final Constructor<?> constructor = type.getDeclaredConstructor(Application.class);
        constructor.setAccessible(true);
        final Object watchdog = constructor.newInstance(application);
        final Method tick = type.getDeclaredMethod("tick");
        tick.setAccessible(true);
        return (Boolean) tick.invoke(watchdog);
    }
}
