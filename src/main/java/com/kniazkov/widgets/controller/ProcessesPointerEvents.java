/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import com.kniazkov.json.JsonObject;

/**
 * An entity that can be clicked by the user.
 * This interface represents interactive elements such as buttons, icons, or clickable text.
 * It allows associating a {@link Controller <PointerEvent>} that defines the behavior
 * to execute when a click event occurs.
 */
public interface ProcessesPointerEvents {
    /**
     * Registers a controller that will be invoked when a pointer
     * click event occurs on this element.
     *
     * @param ctrl the controller to execute when the element is clicked
     */
    void onClick(Controller<PointerEvent> ctrl);

    /**
     * Registers a controller that will be invoked when the pointer
     * enters the element’s area (hover start).
     *
     * @param ctrl the controller to execute on pointer enter
     */
    void onPointerEnter(Controller<PointerEvent> ctrl);

    /**
     * Registers a controller that will be invoked when the pointer
     * leaves the element’s area (hover end).
     *
     * @param ctrl the controller to execute on pointer leave
     */
    void onPointerLeave(Controller<PointerEvent> ctrl);

    /**
     * Parses a {@link PointerEvent} from a given JSON object.

     * @param object the JSON object representing a pointer event
     * @return the deserialized {@code PointerEvent} instance
     */
    static PointerEvent parsePointerEvent(final JsonObject object) {
        return object.toJavaObject(PointerEvent.class);
    }
}
