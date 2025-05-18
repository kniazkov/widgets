/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * An entity that can be clicked by the user.
 * <p>
 *     This interface represents interactive elements such as buttons, icons, or clickable text.
 *     It allows associating a {@link Controller} that defines the behavior to execute
 *     when a click event occurs.
 * </p>
 */
public interface ProcessesPointerEvents {
    /**
     * Sets a controller that will be invoked when the entity is clicked.
     *
     * @param ctrl The controller to run on click
     */
    void onClick(TypedController<PointerEvent> ctrl);

    void onPointerEnter(TypedController<PointerEvent> ctrl);

    void onPointerLeave(TypedController<PointerEvent> ctrl);

    /**
     * Parses a {@link PointerEvent} from a given JSON object.
     * @param object The JSON object representing a pointer event
     * @return The deserialized {@code PointerEvent} instance
     */
    static PointerEvent parsePointerEvent(final JsonObject object) {
        return object.toJavaObject(PointerEvent.class);
    }
}
