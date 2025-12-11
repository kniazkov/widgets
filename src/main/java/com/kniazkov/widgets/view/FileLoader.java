/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;

import com.kniazkov.widgets.protocol.RequestNextChunk;
import java.util.Map;
import java.util.TreeMap;

/**
 * ...
 */
public class FileLoader extends Button {
    /**
     * ...
     */
    private final Map<Integer, UploadingFile> uploading = new TreeMap<>();

    /**
     * ...
     */
    private Controller<UploadingFile> onSelectCtrl = Controller.stub();

    /**
     * ...
     */
    public FileLoader() {
        super();
    }

    /**
     * ...
     * @param text
     */
    public FileLoader(final String text) {
        super(text);
    }

    /**
     * ...
     * @param style
     * @param text
     */
    public FileLoader(final ButtonStyle style, final String text) {
        super(style, text);
    }

    @Override
    public String getType() {
        return "file loader";
    }

    /**
     * ...
     * @param event
     */
    public void handleUploadEvent(final UploadEvent event) {
        UploadingFile file = this.uploading.get(event.fileId);
        if (file == null) {
            file = new UploadingFile(event);
            this.uploading.put(event.fileId, file);
            this.onSelectCtrl.handleEvent(file);
        } else {
            file.handleUploadEvent(event);
        }
        this.pushUpdate(new RequestNextChunk(this.getId()));
    }

    /**
     * ...
     * @param ctrl
     */
    public void onSelect(final Controller<UploadingFile> ctrl) {
        this.onSelectCtrl = ctrl;
    }
}
