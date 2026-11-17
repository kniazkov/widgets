/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Accessible list belonging to one UploadManager. Rows are maintained in the browser.
 */
public final class UploadList extends Panel {
    /**
     * Creates an empty upload list.
     */
    public UploadList() {
        super();
    }

    @Override
    public String getType() {
        return "upload list";
    }
}
