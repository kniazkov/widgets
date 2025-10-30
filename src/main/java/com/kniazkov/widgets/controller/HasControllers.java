package com.kniazkov.widgets.controller;

public interface HasControllers {
    <T> Controller<T> getController(Event<T> event);

    <T> void setController(Event<T> event, Controller<T> controller);
}
