/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Reproducible in-process empty synchronization probe, not an HTTP capacity claim.
 * Run with -Dwidgets.pollingBenchmark=true -Dtest=PollingBenchmarkTest test.
 */
public final class PollingBenchmarkTest {
    /**
     * CPU and response payload cost for independent clients with 50 text widgets each.
     */
    @Test
    public void measuresEmptySynchronization() {
        if (!Boolean.getBoolean("widgets.pollingBenchmark")) {
            return;
        }
        for (final int count : new int[] {1, 100, 1000}) {
            final List<Client> clients = new ArrayList<>();
            final List<Map<String, String>> requests = new ArrayList<>();
            try {
                for (int index = 0; index < count; index++) {
                    final Client client = new Client();
                    clients.add(client);
                    final Section content = new Section();
                    for (int item = 0; item < 50; item++) {
                        content.add(new TextWidget("Benchmark " + item));
                    }
                    client.getRootWidget().add(content);
                    final Map<String, String> request = new HashMap<>();
                    request.put("events", "[]");
                    request.put("updateEpoch", client.updateEpoch());
                    final JsonObject initial = new JsonObject();
                    client.synchronize(request, initial);
                    request.put("lastUpdate", "#" + initial.get("updateThrough").intValue());
                    client.synchronize(request, new JsonObject());
                    requests.add(request);
                }
                for (int warmup = 0; warmup < 10; warmup++) {
                    for (int index = 0; index < count; index++) {
                        clients.get(index).synchronize(requests.get(index), new JsonObject());
                    }
                }
                final var bean = ManagementFactory.getThreadMXBean();
                final long before = bean.getCurrentThreadCpuTime();
                final long started = System.nanoTime();
                long bytes = 0;
                for (int round = 0; round < 30; round++) {
                    for (int index = 0; index < count; index++) {
                        final JsonObject response = new JsonObject();
                        clients.get(index).synchronize(requests.get(index), response);
                        assertEquals(0, response.get("updates").arrayValueOrThrow().size());
                        bytes += response.toString().getBytes(StandardCharsets.UTF_8).length;
                    }
                }
                System.out.printf(java.util.Locale.ROOT,
                    "POLLING clients=%d requests=%d bytes=%d cpu_ms=%.3f wall_ms=%.3f%n",
                    count, 30 * count, bytes, (bean.getCurrentThreadCpuTime() - before) / 1e6,
                    (System.nanoTime() - started) / 1e6);
            } finally {
                clients.forEach(Client::close);
            }
        }
    }
}
