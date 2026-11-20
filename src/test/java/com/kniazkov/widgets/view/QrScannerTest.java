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
        event.addString("text", "2\n" + "x".repeat(2048));
        assertEquals("invalidPayload", Event.TEXT_INPUT.rejectionReason(scanner, event).orElseThrow());
        scanner.handleEvent(Event.TEXT_INPUT, event);
        assertEquals(List.of("product"), received);
        assertTrue(Event.TEXT_INPUT.rejectionReason(new Panel(), event).isPresent());
        root.remove(scanner);
        event.addString("text", "2\nclosed");
        scanner.handleEvent(Event.TEXT_INPUT, event);
        assertEquals(List.of("product"), received);
    }
}
