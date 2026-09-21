/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonElement;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Action exceptions must reach the HTTP logging boundary unchanged.
 */
public final class ActionHandlerTest {
    /**
     * The original exception is not converted into a successful protocol response.
     */
    @Test
    public void propagatesFailure() {
        final IllegalStateException failure = new IllegalStateException("client failed");
        final Application app = BaseTestSupport.application((r, c) -> { });
        final ActionHandler handler = new ActionHandler(app) {
            @Override
            JsonElement process(final Map<String, String> data) {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> handler.process(Collections.emptyMap())));
    }
}
