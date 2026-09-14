/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.model.ImageSourceModel;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.protocol.SetCarouselNewTabHref;
import com.kniazkov.widgets.protocol.SetCarouselNewTabHrefs;
import com.kniazkov.widgets.protocol.SetCarouselSource;
import com.kniazkov.widgets.protocol.SetCarouselSources;
import com.kniazkov.widgets.protocol.SetProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A swipeable, non-circular sequence of images.
 *
 * <p>The number and order of positions are fixed at construction time. Every source is stored in
 * its own reactive {@link Model}; a source value or the model at an existing position may be
 * replaced later. The initially selected position is zero.</p>
 *
 * <p>{@link #onClick(com.kniazkov.widgets.controller.Controller)} reports taps and clicks.
 * {@link #onSelect(com.kniazkov.widgets.controller.Controller)} reports a successful swipe after
 * the selected-index model has been updated. Swiping beyond either end only produces a resisted
 * visual movement in the browser and does not change the selected position.</p>
 */
public class Carousel extends BaseImageWidget<CarouselStyle>
        implements HasSelectedIndex, HandlesPointerEvents {
    /**
     * Fixed ordered source bindings.
     */
    private final List<SourceBinding> sources;

    /**
     * Hyperlinks opened directly by the browser for the corresponding positions.
     * Empty values disable direct opening.
     */
    private final List<String> newTabHrefs;

    /**
     * Selected position binding. It starts at zero because an empty carousel is prohibited.
     */
    private final Binding<Integer> selectedIndex;

    /**
     * Returns the global default style.
     *
     * @return default carousel style
     */
    public static CarouselStyle getDefaultStyle() {
        return CarouselStyle.DEFAULT;
    }

    /**
     * Creates a carousel from one or more hyperlinks.
     *
     * @param firstHref first image hyperlink
     * @param otherHrefs remaining image hyperlinks
     */
    public Carousel(final String firstHref, final String... otherHrefs) {
        this(getDefaultStyle(), toSources(firstHref, otherHrefs));
    }

    /**
     * Creates a carousel from one or more image sources.
     *
     * @param firstSource first image source
     * @param otherSources remaining image sources
     */
    public Carousel(final ImageSource firstSource, final ImageSource... otherSources) {
        this(getDefaultStyle(), toModels(firstSource, otherSources));
    }

    /**
     * Creates a carousel from an ordered non-empty sequence of source models.
     *
     * @param sourceModels ordered image source models
     */
    public Carousel(final Iterable<Model<ImageSource>> sourceModels) {
        this(getDefaultStyle(), sourceModels);
    }

    /**
     * Creates a styled carousel from one or more hyperlinks.
     *
     * @param style widget style
     * @param firstHref first image hyperlink
     * @param otherHrefs remaining image hyperlinks
     */
    public Carousel(
        final CarouselStyle style,
        final String firstHref,
        final String... otherHrefs
    ) {
        this(style, toSources(firstHref, otherHrefs));
    }

    /**
     * Creates a styled carousel from one or more image sources.
     *
     * @param style widget style
     * @param firstSource first image source
     * @param otherSources remaining image sources
     */
    public Carousel(
        final CarouselStyle style,
        final ImageSource firstSource,
        final ImageSource... otherSources
    ) {
        this(style, toModels(firstSource, otherSources));
    }

    /**
     * Creates a styled carousel from an ordered non-empty sequence of source models.
     *
     * @param style widget style
     * @param sourceModels ordered image source models
     */
    @SuppressWarnings("this-escape")
    public Carousel(
        final CarouselStyle style,
        final Iterable<Model<ImageSource>> sourceModels
    ) {
        super(Objects.requireNonNull(style, "style"));
        final List<Model<ImageSource>> models = copyModels(sourceModels);
        if (models.isEmpty()) {
            throw new IllegalArgumentException("A carousel requires at least one image");
        }
        final List<String> serialized = new ArrayList<>(models.size());
        for (final Model<ImageSource> model : models) {
            serialized.add(model.getData().toString());
        }
        this.pushUpdate(new SetCarouselSources(this.getId(), serialized));
        final List<SourceBinding> bindings = new ArrayList<>(models.size());
        for (int index = 0; index < models.size(); index++) {
            bindings.add(new SourceBinding(index, models.get(index)));
        }
        this.sources = List.copyOf(bindings);
        this.newTabHrefs = new ArrayList<>(models.size());
        for (int index = 0; index < models.size(); index++) {
            this.newTabHrefs.add("");
        }
        this.selectedIndex = new Binding<>(new IntegerModel(0), index -> {
            this.validateSelectedIndex(index);
            this.pushUpdate(new SetProperty<>(
                this.getId(), State.ANY, Property.SELECTED_INDEX, index
            ));
        });
    }

    @Override
    public String getType() {
        return "carousel";
    }

    /**
     * Returns the fixed number of images.
     *
     * @return image count
     */
    public int getImageCount() {
        return this.sources.size();
    }

    /**
     * Returns the model bound to an image position.
     *
     * @param index image position
     * @return image source model
     */
    public Model<ImageSource> getSourceModel(final int index) {
        return this.sources.get(index).getModel();
    }

    /**
     * Returns an immutable snapshot of all source models in their fixed order.
     *
     * @return source models
     */
    public List<Model<ImageSource>> getSourceModels() {
        final List<Model<ImageSource>> result = new ArrayList<>(this.sources.size());
        for (final SourceBinding source : this.sources) {
            result.add(source.getModel());
        }
        return List.copyOf(result);
    }

    /**
     * Replaces the model at an existing image position.
     *
     * @param index image position
     * @param model replacement source model
     */
    public void setSourceModel(final int index, final Model<ImageSource> model) {
        this.sources.get(index).setModel(Objects.requireNonNull(model, "model"));
    }

    /**
     * Returns the current source at an image position.
     *
     * @param index image position
     * @return image source
     */
    public ImageSource getSource(final int index) {
        return this.getSourceModel(index).getData();
    }

    /**
     * Changes the source at an existing image position.
     *
     * @param index image position
     * @param source new image source
     */
    public void setSource(final int index, final ImageSource source) {
        this.getSourceModel(index).setData(Objects.requireNonNull(source, "source"));
    }

    /**
     * Changes the hyperlink at an existing image position.
     *
     * @param index image position
     * @param href new image hyperlink
     */
    public void setSource(final int index, final String href) {
        this.setSource(index, ImageSource.fromHyperlink(Objects.requireNonNull(href, "href")));
    }

    /**
     * Returns the hyperlink opened by a click at one carousel position.
     *
     * @param index carousel position
     * @return hyperlink, or an empty string when direct opening is disabled
     */
    public String getNewTabHref(final int index) {
        return this.newTabHrefs.get(index);
    }

    /**
     * Returns an immutable snapshot of all new-tab hyperlinks.
     *
     * @return hyperlinks in carousel order
     */
    public List<String> getNewTabHrefs() {
        return List.copyOf(this.newTabHrefs);
    }

    /**
     * Changes the hyperlink opened by a click at one carousel position.
     *
     * @param index carousel position
     * @param href hyperlink, or an empty string to disable direct opening
     */
    public void setNewTabHref(final int index, final String href) {
        this.sources.get(index);
        final String checked = Objects.requireNonNull(href, "href");
        this.newTabHrefs.set(index, checked);
        this.pushUpdate(new SetCarouselNewTabHref(this.getId(), index, checked));
    }

    /**
     * Replaces all hyperlinks opened by clicks. The number of hyperlinks must match
     * the fixed number of carousel images.
     *
     * @param hrefs hyperlinks in carousel order; empty values disable individual links
     */
    public void setNewTabHrefs(final List<String> hrefs) {
        final List<String> checked = List.copyOf(
            Objects.requireNonNull(hrefs, "hrefs")
        );
        if (checked.size() != this.sources.size()) {
            throw new IllegalArgumentException(
                "New-tab hyperlink count must match carousel image count"
            );
        }
        this.newTabHrefs.clear();
        this.newTabHrefs.addAll(checked);
        this.pushUpdate(new SetCarouselNewTabHrefs(this.getId(), checked));
    }

    @Override
    public Model<Integer> getSelectedIndexModel() {
        return this.selectedIndex.getModel();
    }

    @Override
    public void setSelectedIndexModel(final Model<Integer> model) {
        final Model<Integer> replacement = Objects.requireNonNull(model, "model");
        this.validateSelectedIndex(replacement.getData());
        this.selectedIndex.setModel(replacement);
    }

    @Override
    public int getSelectedIndex() {
        return this.selectedIndex.getModel().getData();
    }

    @Override
    public void setSelectedIndex(final int index) {
        this.validateSelectedIndex(index);
        this.selectedIndex.getModel().setData(index);
    }

    /**
     * Validates a position against the fixed source array.
     *
     * @param index selected position
     */
    private void validateSelectedIndex(final int index) {
        if (index < 0 || index >= this.sources.size()) {
            throw new IllegalArgumentException("Invalid carousel image index: " + index);
        }
    }

    /**
     * Converts hyperlink arguments to source models.
     *
     * @param firstHref first hyperlink
     * @param otherHrefs remaining hyperlinks
     * @return source models
     */
    private static List<Model<ImageSource>> toSources(
        final String firstHref,
        final String[] otherHrefs
    ) {
        final ImageSource first = ImageSource.fromHyperlink(
            Objects.requireNonNull(firstHref, "first href")
        );
        final ImageSource[] remaining = Arrays.stream(
            Objects.requireNonNull(otherHrefs, "other hrefs")
        ).map(href -> ImageSource.fromHyperlink(Objects.requireNonNull(href, "href")))
            .toArray(ImageSource[]::new);
        return toModels(first, remaining);
    }

    /**
     * Converts image source arguments to independent models.
     *
     * @param firstSource first image source
     * @param otherSources remaining image sources
     * @return source models
     */
    private static List<Model<ImageSource>> toModels(
        final ImageSource firstSource,
        final ImageSource[] otherSources
    ) {
        final ImageSource[] remaining = Objects.requireNonNull(otherSources, "other sources");
        final List<Model<ImageSource>> result = new ArrayList<>(remaining.length + 1);
        result.add(new ImageSourceModel(Objects.requireNonNull(firstSource, "first source")));
        for (final ImageSource source : remaining) {
            result.add(new ImageSourceModel(Objects.requireNonNull(source, "source")));
        }
        return result;
    }

    /**
     * Copies and validates model references from an arbitrary iterable.
     *
     * @param sourceModels source models
     * @return validated source model snapshot
     */
    private static List<Model<ImageSource>> copyModels(
        final Iterable<Model<ImageSource>> sourceModels
    ) {
        final List<Model<ImageSource>> result = new ArrayList<>();
        for (final Model<ImageSource> model
                : Objects.requireNonNull(sourceModels, "source models")) {
            final Model<ImageSource> checked = Objects.requireNonNull(model, "source model");
            Objects.requireNonNull(checked.getData(), "image source");
            result.add(checked);
        }
        return result;
    }

    /**
     * Keeps one stable carousel position bound to a replaceable source model.
     */
    private final class SourceBinding {
        /**
         * Stable image position.
         */
        private final int index;

        /**
         * Strong listener reference required by weak-listener models.
         */
        private final Listener<ImageSource> listener;

        /**
         * Currently bound source model.
         */
        private Model<ImageSource> model;

        /**
         * Binds one source without sending a redundant initial update.
         *
         * @param index image position
         * @param model initial source model
         */
        SourceBinding(final int index, final Model<ImageSource> model) {
            this.index = index;
            this.listener = source -> Carousel.this.pushUpdate(
                new SetCarouselSource(
                    Carousel.this.getId(),
                    this.index,
                    Objects.requireNonNull(source, "image source").toString()
                )
            );
            this.model = model;
            this.model.addListener(this.listener);
        }

        /**
         * Returns the current source model.
         *
         * @return current source model
         */
        Model<ImageSource> getModel() {
            return this.model;
        }

        /**
         * Rebinds this position and publishes the replacement value.
         *
         * @param replacement replacement source model
         */
        void setModel(final Model<ImageSource> replacement) {
            Objects.requireNonNull(replacement.getData(), "image source");
            if (this.model != replacement) {
                this.model.removeListener(this.listener);
                this.model = replacement;
                this.model.addListener(this.listener);
                this.listener.accept(replacement.getData());
            }
        }
    }
}
