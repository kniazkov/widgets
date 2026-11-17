/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**

 * A bounded command addressing a browser-local managed upload.

 */
public final class UploadCommand {
    /**
     * Creates an empty command for JSON decoding.
     */
    public UploadCommand() {
    }
    /**
     * Command: cancel, remove or retry.
     */
    public String action;
    /**
     * Original upload identifier.
     */
    public int fileId;
}
