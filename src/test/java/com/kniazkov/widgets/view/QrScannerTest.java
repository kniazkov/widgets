/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Проверяет сканер через полный протокол событий браузера.
 */
public class QrScannerTest {
    /**
     * Допускает текст сканера без замены JSON-конфигурации сырым QR-кодом.
     */
    @Test
    public void acceptsScannerEventsWithoutReplacingConfiguration() {
        final List<String> received = new ArrayList<>();
        final QrScanner scanner = new QrScanner("https://shop.example/", Map.of(), received::add);
        final RootWidget root = new RootWidget();
        root.add(scanner);
        final String configuration = scanner.getText();
        final JsonObject event = new JsonObject();
        event.addString("text", "1\nproduct");
        assertTrue(Event.TEXT_INPUT.rejectionReason(scanner, event).isEmpty());
        scanner.handleEvent(Event.TEXT_INPUT, event);
        scanner.handleEvent(Event.TEXT_INPUT, event);
        assertEquals(List.of("product"), received);
        assertEquals(configuration, scanner.getText());
        event.addString("text", "2\n" + "x".repeat(QrScanner.MAX_EVENT_LENGTH));
        assertEquals("invalidPayload",
            Event.TEXT_INPUT.rejectionReason(scanner, event).orElseThrow());
        scanner.handleEvent(Event.TEXT_INPUT, event);
        assertEquals(List.of("product"), received);
        assertTrue(Event.TEXT_INPUT.rejectionReason(new Panel(), event).isPresent());
        root.remove(scanner);
        event.addString("text", "2\nclosed");
        scanner.handleEvent(Event.TEXT_INPUT, event);
        assertEquals(List.of("product"), received);
    }
    /**
     * General mode preserves arbitrary text, Unicode and large QR payloads.
     * @throws com.kniazkov.json.JsonException if the configuration is malformed
     */
    @org.junit.Test
    public void acceptsGeneralQrText() throws com.kniazkov.json.JsonException {
        final java.util.List<String> received = new java.util.ArrayList<>();
        final QrScanner scanner = new QrScanner(java.util.Map.of(), received::add);
        final RootWidget root = new RootWidget(scanner);
        final String payload = "WIFI:T:WPA;S:Guest;P:sample;;\n" + "x".repeat(7000);
        final var event = new com.kniazkov.json.JsonObject();
        event.addString("text", "1\n" + payload);
        scanner.handleEvent(com.kniazkov.widgets.controller.Event.TEXT_INPUT, event);
        org.junit.Assert.assertEquals(java.util.List.of(payload), received);
        assertEquals("", com.kniazkov.json.Json.parse(scanner.getText())
            .objectValueOrThrow().get("prefix").stringValue());
        root.closePage();
    }

}
