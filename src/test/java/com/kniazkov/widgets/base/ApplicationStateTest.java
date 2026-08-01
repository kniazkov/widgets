/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Reproduces state-management and watchdog races in {@link Application}. */
public class ApplicationStateTest {
    /** A failed page factory must not leave a half-created client in the registry. */
    @Test
    public void failedPageCreationRollsBackTheClient() throws Exception {
        final Application application = BaseTestSupport.application((root, context) -> {
            throw new IllegalStateException("page setup failed");
        });

        try {
            application.createClient("/", new PageContext());
            fail("The page exception should reach the caller");
        } catch (final IllegalStateException expected) {
            assertTrue(
                "The failed client remains registered",
                BaseTestSupport.clients(application).isEmpty()
            );
        }
    }

    /** A synchronization between the watchdog's check and removal must keep the client alive. */
    @Test
    public void watchdogRechecksAClientThatWasRenewedBeforeRemoval() throws Exception {
        final Application application = BaseTestSupport.application((root, context) -> { });
        final Client client = new Client();
        client.timer = 0;
        final RenewDuringWatchdogMap clients = new RenewDuringWatchdogMap(application, client);
        BaseTestSupport.replaceClients(application, clients);

        assertTrue(BaseTestSupport.tickWatchdog(application));

        assertNotNull(
            "The watchdog removed a client after a concurrent synchronization renewed it",
            clients.get(client.getId())
        );
    }

    /** A concurrent explicit kill must not crash the watchdog when it reaches the same client. */
    @Test
    public void explicitKillDuringWatchdogRemovalDoesNotCrashTheWatchdog() throws Exception {
        final Application application = BaseTestSupport.application((root, context) -> { });
        final Client client = new Client();
        client.timer = 0;
        final KillDuringWatchdogMap clients = new KillDuringWatchdogMap(application, client);
        BaseTestSupport.replaceClients(application, clients);

        try {
            assertTrue(BaseTestSupport.tickWatchdog(application));
        } catch (final InvocationTargetException error) {
            fail("A concurrent kill crashed the watchdog: " + error.getCause());
        }
    }

    /** Registry that hides manual-test entries from the application's real timer thread. */
    private abstract static class ControlledClientMap extends ConcurrentHashMap<UId, Client> {
        @Override
        public Set<Map.Entry<UId, Client>> entrySet() {
            if (Thread.currentThread().getName().startsWith("Timer-")) {
                return Collections.emptySet();
            }
            return super.entrySet();
        }
    }

    /** Injects a synchronization when the watchdog starts its atomic expiration operation. */
    private static final class RenewDuringWatchdogMap extends ControlledClientMap {
        private final Application application;
        private final UId target;
        private boolean renewed;

        RenewDuringWatchdogMap(final Application application, final Client client) {
            this.application = application;
            this.target = client.getId();
            this.put(this.target, client);
        }

        @Override
        public Client computeIfPresent(
            final UId key,
            final BiFunction<? super UId, ? super Client, ? extends Client> remappingFunction
        ) {
            if (!this.renewed && this.target.equals(key)) {
                this.renewed = true;
                this.application.synchronize(
                    this.target,
                    Collections.emptyMap(),
                    new JsonObject()
                );
            }
            return super.computeIfPresent(key, remappingFunction);
        }

        @Override
        public Client remove(final Object key) {
            if (!this.renewed && this.target.equals(key)) {
                this.renewed = true;
                this.application.synchronize(
                    this.target,
                    Collections.emptyMap(),
                    new JsonObject()
                );
            }
            return super.remove(key);
        }
    }

    /** Injects an explicit kill when the watchdog starts its atomic expiration operation. */
    private static final class KillDuringWatchdogMap extends ControlledClientMap {
        private final Application application;
        private final UId target;
        private boolean killing;

        KillDuringWatchdogMap(final Application application, final Client client) {
            this.application = application;
            this.target = client.getId();
            this.put(this.target, client);
        }

        @Override
        public Client computeIfPresent(
            final UId key,
            final BiFunction<? super UId, ? super Client, ? extends Client> remappingFunction
        ) {
            if (!this.killing && this.target.equals(key)) {
                this.killing = true;
                this.application.killClient(this.target);
            }
            return super.computeIfPresent(key, remappingFunction);
        }

        @Override
        public Client remove(final Object key) {
            if (!this.killing && this.target.equals(key)) {
                this.killing = true;
                this.application.killClient(this.target);
            }
            return super.remove(key);
        }
    }
}
