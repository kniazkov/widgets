/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.model.ImageSourceModel;
import com.kniazkov.widgets.model.Model;

/**
 * Widget representing an image backed by an {@link ImageSource}.
 * <p>
 * The image source is stored in a {@link Model} and bound to the
 * {@link Property#IMAGE_SOURCE} property.
 */
public class ImageWidget extends BaseImageWidget {
    /**
     * Creates a new image initialized with the specified source.
     *
     * @param source the image source to use
     */
    public ImageWidget(final ImageSource source) {
        this.setSourceModel(new ImageSourceModel(source));
    }

    /**
     * Creates a new image initialized with a hyperlink string.
     *
     * @param href the raw hyperlink pointing to the image
     */
    public ImageWidget(final String href) {
        this.setSourceModel(new ImageSourceModel(ImageSource.fromHyperlink(href)));
    }

    @Override
    public String getType() {
        return "image";
    }

    /**
     * Returns the model storing this image's {@link ImageSource}.
     *
     * @return the model bound to {@code Property.IMAGE_SOURCE}
     */
    public Model<ImageSource> getSourceModel() {
        return this.getModel(State.ANY, Property.IMAGE_SOURCE);
    }

    /**
     * Sets the model storing this image's {@link ImageSource}.
     *
     * @param model the model to bind to {@code Property.IMAGE_SOURCE}
     */
    public void setSourceModel(final Model<ImageSource> model) {
        this.setModel(State.ANY, Property.IMAGE_SOURCE, model);
    }

    /**
     * Returns the current image source.
     *
     * @return the {@link ImageSource} stored in the source model
     */
    public ImageSource getSource() {
        return this.getSourceModel().getData();
    }

    /**
     * Updates the image source.
     *
     * @param source the new {@link ImageSource}
     */
    public void setSource(final ImageSource source) {
        this.getSourceModel().setData(source);
    }

    /**
     * Updates the image source using a hyperlink string.
     *
     * @param href the raw hyperlink pointing to the new image
     */
    public void setSource(final String href) {
        this.getSourceModel().setData(ImageSource.fromHyperlink(href));
    }
}
