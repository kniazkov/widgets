/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**
 * An entity that can be clicked by the user.
 * This interface represents interactive elements such as buttons, icons, or clickable text.
 * It allows associating a {@link Controller <PointerEvent>} that defines the behavior
 * to execute when a click event occurs.
 */
public interface HandlesPointerEvents extends HandlesEvents {
    /**
     * Registers a controller that will be invoked when a pointer
     * click event occurs on this element.
     *
     * @param ctrl the controller to execute when the element is clicked
     */
    default void onClick(Controller<PointerEvent> ctrl) {
        this.setController(Event.CLICK, ctrl);
        this.subscribeToEvent(Event.CLICK);
    }

    /**
     * Registers a controller that will be invoked when the pointer
     * enters the element’s area (hover start).
     *
     * @param ctrl the controller to execute on pointer enter
     */
    default void onPointerEnter(Controller<PointerEvent> ctrl) {
        this.setController(Event.POINTER_ENTER, ctrl);
        this.subscribeToEvent(Event.POINTER_ENTER);
    }

    /**
     * Registers a controller that will be invoked when the pointer
     * leaves the element’s area (hover end).
     *
     * @param ctrl the controller to execute on pointer leave
     */
    default void onPointerLeave(Controller<PointerEvent> ctrl) {
        this.setController(Event.POINTER_LEAVE, ctrl);
        this.subscribeToEvent(Event.POINTER_LEAVE);
    }
}
