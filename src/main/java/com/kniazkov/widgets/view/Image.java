/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.ImageSource;
import com.kniazkov.widgets.model.ImageSourceModel;
import com.kniazkov.widgets.model.Model;

public class Image extends BaseImage {
    public Image(final ImageSource source) {
        this.setSourceModel(new ImageSourceModel(source));
    }

    public Image(final String href) {
        this.setSourceModel(new ImageSourceModel(ImageSource.fromHyperlink(href)));
    }

    @Override
    public String getType() {
        return "image";
    }

    public Model<ImageSource> getSourceModel() {
        return this.getModel(State.ANY, Property.IMAGE_SOURCE);
    }

    public void setSourceModel(final Model<ImageSource> model) {
        this.setModel(State.ANY, Property.IMAGE_SOURCE, model);
    }

    public ImageSource getSource() {
        return this.getSourceModel().getData();
    }

    public void setSource(final ImageSource source) {
        this.getSourceModel().setData(source);
    }

    public void setSource(final String href) {
        this.getSourceModel().setData(ImageSource.fromHyperlink(href));
    }
}
