/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.controller.EventTestData;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Carousel;
import com.kniazkov.widgets.view.CheckBox;
import com.kniazkov.widgets.view.DropDownList;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Property;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.State;
import com.kniazkov.widgets.view.TextWidget;
import com.kniazkov.widgets.view.Widget;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for authenticated but malicious browser events.
 */
public final class ServerEventValidationTest {
    /**
     * Output text and disabled/hidden ancestors cannot be used to invoke controllers.
     */
    @Test
    public void rejectsOutputAndInactiveControls() {
        final Client client = new Client();
        final TextWidget output = new TextWidget("original");
        final Button button = new Button("Run");
        final Section section = new Section(output, button);
        client.getRootWidget().add(section);
        final AtomicInteger calls = new AtomicInteger();
        output.setController(Event.TEXT_INPUT, value -> calls.incrementAndGet());
        button.onClick(value -> calls.incrementAndGet());
        reject(client, output, "text input", "{\"text\":\"forged\"}", "unsupportedEvent");
        assertEquals("original", output.getText());
        button.disable();
        reject(client, button, "click", EventTestData.pointer().toString(), "inactiveWidget");
        button.enable();
        section.hide();
        reject(client, button, "click", EventTestData.pointer().toString(), "inactiveWidget");
        section.show();
        section.getModel(State.ANY, Property.DISABLED).setData(true);
        reject(client, button, "click", EventTestData.pointer().toString(), "inactiveWidget");
        assertEquals(0, calls.get());
        section.getModel(State.ANY, Property.DISABLED).setData(false);
        assertFalse(send(client, button, "click", EventTestData.pointer().toString())
            .containsKey("rejectedEvents"));
        assertEquals(1, calls.get());
    }

    /**
     * Wrong types, extra fields, overlong strings and unknown event names cannot mutate input.
     */
    @Test
    public void rejectsMalformedInputAndPreservesValidInput() {
        final Client client = new Client();
        final InputField field = new InputField("before");
        client.getRootWidget().add(new Section(field));
        final AtomicInteger calls = new AtomicInteger();
        field.onTextInput(value -> calls.incrementAndGet());
        for (final String data : new String[]{"null", "42", "[]", "{}",
                "{\"text\":null}", "{\"text\":42}",
                "{\"text\":[]}", "{\"text\":{},\"extra\":true}",
                "{\"text\":\"x\",\"extra\":true}",
                "{\"text\":\"" + "x".repeat(65537) + "\"}"}) {
            reject(client, field, "text input", data, "invalidPayload");
        }
        reject(client, field, "not an event", "{}", "unknownEvent");
        assertEquals("before", field.getText());
        assertEquals(0, calls.get());
        send(client, field, "text input", "{\"text\":\"" + "я".repeat(65536) + "\"}");
        assertEquals(65536, field.getText().length());
        send(client, field, "text input", "{\"text\":\"\"}");
        assertEquals("", field.getText());
        assertEquals(2, calls.get());
    }

    /**
     * Selection range and integer validation happens before mutation or application callbacks.
     */
    @Test
    public void rejectsInvalidSelectionsAndChecks() {
        final Client client = new Client();
        final DropDownList list = new DropDownList("a", "b");
        final Carousel carousel = new Carousel("a.png", "b.png");
        final CheckBox check = new CheckBox();
        client.getRootWidget().add(new Section(list, carousel, check));
        final AtomicInteger calls = new AtomicInteger();
        list.onSelect(value -> calls.incrementAndGet());
        carousel.onSelect(value -> calls.incrementAndGet());
        check.setController(Event.CHECK, value -> calls.incrementAndGet());
        for (final Widget<?> target : new Widget<?>[]{list, carousel}) {
            for (final String value : new String[]{"-2", "2", "1.5", "2147483648", "null",
                    "\"1\"", "true", "{}", "1e100", "1.0000000000000000001"}) {
                reject(client, target, "select", "{\"index\":" + value + "}", "invalidPayload");
            }
        }
        reject(client, carousel, "select", "{\"index\":-1}", "invalidPayload");
        reject(client, check, "check", "{\"state\":\"true\"}", "invalidPayload");
        assertEquals(-1, list.getSelectedIndex());
        assertEquals(0, carousel.getSelectedIndex());
        assertFalse(check.isChecked());
        assertEquals(0, calls.get());
        send(client, list, "select", "{\"index\":1}");
        send(client, carousel, "select", "{\"index\":1}");
        send(client, check, "check", "{\"state\":true}");
        assertEquals(3, calls.get());
    }

    /**
     * A business-invalid email remains normal input, not a protocol rejection.
     */
    @Test
    public void keepsBusinessValidationSeparate() {
        final Client client = new Client();
        final InputField input = new InputField();
        final var email = new com.kniazkov.widgets.model.EmailModel("valid@example.com");
        input.setTextModel(email);
        client.getRootWidget().add(new Section(input));
        final AtomicInteger calls = new AtomicInteger();
        input.onTextInput(value -> calls.incrementAndGet());
        final JsonObject response = send(client, input, "text input", "{\"text\":\"unfinished\"}");
        assertFalse(response.containsKey("rejectedEvents"));
        assertFalse(email.isValid());
        assertEquals("unfinished", email.getData());
        assertEquals(1, calls.get());
    }

    /**
     * Invalid pointer structures and upload metadata are rejected before callbacks.
     */
    @Test
    public void validatesStructuredPayloads() {
        final Client client = new Client();
        final Button button = new Button("Run");
        final FileLoader loader = new FileLoader();
        client.getRootWidget().add(new Section(button, loader));
        final AtomicInteger calls = new AtomicInteger();
        button.onClick(value -> calls.incrementAndGet());
        loader.onSelect(value -> calls.incrementAndGet());
        reject(client, button, "click", "{}", "invalidPayload");
        for (final String key : new String[]{"position", "type", "primary", "buttons", "keys",
                "pressure"}) {
            final JsonObject data = EventTestData.pointer();
            data.addString(key, "bad");
            reject(client, button, "click", data.toString(), "invalidPayload");
        }
        final JsonObject data = EventTestData.pointer();
        data.addNumber("pressure", 2);
        reject(client, button, "click", data.toString(), "invalidPayload");
        for (final String metadata : new String[]{"{}",
                "{\"fileId\":1,\"name\":\"../x\",\"type\":\"\",\"size\":1,\"totalChunks\":1}",
                "{\"fileId\":1,\"name\":\"x\",\"type\":\"\",\"size\":1,\"totalChunks\":2}"}) {
            reject(client, loader, "upload", metadata, "invalidPayload");
        }
        assertEquals(0, calls.get());
    }

    /**
     * Later events recheck ancestry when an earlier controller disables or detaches a subtree.
     */
    @Test
    public void rechecksCurrentStateBetweenEvents() {
        for (final boolean detach : new boolean[]{false, true}) {
            final Client client = new Client();
            final Button first = new Button("first");
            final Button second = new Button("second");
            final Section section = new Section(first, second);
            client.getRootWidget().add(section);
            final AtomicInteger calls = new AtomicInteger();
            first.onClick(value -> {
                if (detach) {
                    section.remove();
                } else {
                    section.hide();
                }
            });
            second.onClick(value -> calls.incrementAndGet());
            final String events = "[" + event(first, "click", EventTestData.pointer().toString())
                + "," + event(second, "click", EventTestData.pointer().toString()) + "]";
            final JsonObject response = new JsonObject();
            client.synchronize(Map.of("events", events), response);
            assertEquals(0, calls.get());
            assertTrue(response.containsKey("rejectedEvents"));
        }
    }

    /**
     * Actual HTTP responses acknowledge rejected events without affecting server state.
     */
    @Test
    public void rejectsForgedInputOverAuthenticatedHttp() throws Exception {
        final TextWidget output = new TextWidget("original");
        final InputField input = new InputField("before");
        final Application app = new Application((root, context) ->
            root.add(new Section(output, input)));
        final var server = Server.start(app, new Options.Builder().setPort(0).build());
        try {
            final var browser = new ProtocolTestBrowser(server.getPort());
            final JsonObject channel = browser.create(null);
            final String forged = "[" + event(output, "text input", "{\"text\":\"forged\"}") + "]";
            final var response = browser.post(Map.of("action", "synchronize", "client",
                channel.get("id").stringValue(), "events", forged), browser.headers(channel));
            assertEquals(200, response.statusCode());
            assertTrue(ProtocolTestBrowser.json(response).containsKey("rejectedEvents"));
            assertEquals("original", output.getText());
            final var valid = browser.post(Map.of("action", "synchronize", "client",
                channel.get("id").stringValue(), "events", "["
                    + event(input, "text input", "{\"text\":\"after\"}") + "]"),
                browser.headers(channel));
            assertEquals(200, valid.statusCode());
            assertEquals("after", input.getText());
        } finally {
            server.stop();
        }
    }

    /**
     * Asserts the explicit rejection reason and consumption of the event identifier.
     */
    private static void reject(final Client client, final Widget<?> widget, final String type,
            final String payload, final String reason) {
        final JsonObject response = send(client, widget, type, payload);
        assertEquals(reason, response.get("rejectedEvents").arrayValueOrThrow().get(0)
            .objectValueOrThrow().get("reason").stringValue());
        assertEquals(response.get("lastEvent").stringValue(), response.get("rejectedEvents")
            .arrayValueOrThrow().get(0).objectValueOrThrow().get("id").stringValue());
    }

    /**
     * Sends a new monotonic event through the production synchronization dispatcher.
     */
    private static JsonObject send(final Client client, final Widget<?> widget, final String type,
            final String payload) {
        final JsonObject response = new JsonObject();
        client.synchronize(Map.of("events", "[" + event(widget, type, payload) + "]"), response);
        return response;
    }

    /**
     * Creates an event envelope with a real monotonically increasing ID.
     */
    private static JsonObject event(final Widget<?> widget, final String type,
            final String payload) {
        final JsonObject event = new JsonObject();
        event.addString("id", RMId.create().toString());
        event.addString("widget", widget.getId().toString());
        event.addString("type", type);
        try {
            event.put("data", Json.parse(payload));
        } catch (final com.kniazkov.json.JsonException error) {
            throw new AssertionError(error);
        }
        return event;
    }
}
