package com.kniazkov.widgets.images;

import com.kniazkov.widgets.common.Color;
import java.util.Arrays;

/**
 * A factory for creating circular progress bar images as SVG graphics.
 * <p>
 * This class generates a series of SVG images representing progress bars where
 * progress is visualized as a circular arc. The images are cached for efficiency
 * and can be customized in terms of size, colors, and appearance.
 */
public class CircleProgressBarCreator {
    /**
     * Width of the generated image in pixels.
     */
    private final int width;

    /**
     * Height of the generated image in pixels.
     */
    private final int height;

    /**
     * Diameter of the progress circle within the image.
     */
    private int diameter;

    /**
     * Ring line thickness.
     */
    private int lineWidth = 2;

    /**
     * Background color of the progress bar image.
     */
    private Color bgColor = Color.TRANSPARENT;

    /**
     * Color of the background circle (the empty track).
     */
    private Color circleColor = Color.LIGHT_GRAY;

    /**
     * Color of the progress arc (the filled portion).
     */
    private Color progressColor = Color.BLACK;

    /**
     * Cache of generated image sources indexed by percentage (0-100).
     */
    private final ImageSource[] sources;

    /**
     * Cached animated waiting indicator.
     */
    private ImageSource waitingSource;

    /**
     * Constructs a new progress bar creator with specified dimensions.
     *
     * @param width the width of the generated images in pixels
     * @param height the height of the generated images in pixels
     */
    public CircleProgressBarCreator(final int width, final int height) {
        this.width = width;
        this.height = height;
        this.diameter = Math.min(width, height) / 4;
        this.sources = new ImageSource[101];
    }

    /**
     * Clears cached generated image sources.
     */
    private void clearCache() {
        Arrays.fill(this.sources, null);
        this.waitingSource = null;
    }

    /**
     * Sets the diameter of the progress circle.
     * The diameter is automatically limited to fit within the image bounds.
     *
     * @param value the desired diameter in pixels
     */
    public void setDiameter(int value) {
        int maxDiam = Math.min(this.width, this.height);
        this.diameter = Math.max(1, Math.min(maxDiam, value));
        clearCache();
    }

    /**
     * Sets the ring line thickness.
     *
     * @param value the desired line thickness in pixels
     */
    public void setLineWidth(int value) {
        this.lineWidth = Math.max(1, value);
        clearCache();
    }

    /**
     * Sets the background color of the progress bar image.
     *
     * @param value the background color
     */
    public void setBgColor(Color value) {
        this.bgColor = value;
        clearCache();
    }

    /**
     * Sets the color of the background circle (the track).
     *
     * @param value the circle color
     */
    public void setCircleColor(Color value) {
        this.circleColor = value;
        clearCache();
    }

    /**
     * Sets the color of the progress arc (the filled portion).
     *
     * @param value the progress color
     */
    public void setProgressColor(Color value) {
        this.progressColor = value;
        clearCache();
    }

    /**
     * Returns an image source for the specified progress percentage.
     * Images are cached internally for efficient reuse.
     *
     * @param percent the progress percentage (0-100, values outside this range are clamped)
     * @return an ImageSource representing the progress bar at the given percentage
     */
    public ImageSource getImageSource(int percent) {
        if (percent < 0) {
            percent = 0;
        } else if (percent > 100) {
            percent = 100;
        }
        ImageSource source = this.sources[percent];
        if (source == null) {
            source = new ProgressBar(percent);
            this.sources[percent] = source;
        }
        return source;
    }

    /**
     * Returns an animated image source representing an indeterminate progress indicator.
     * The indicator is a rotating open ring whose gap size changes continuously.
     *
     * @return animated waiting ImageSource
     */
    public ImageSource getWaitingImageSource() {
        if (this.waitingSource == null) {
            this.waitingSource = new WaitingIndicator();
        }
        return this.waitingSource;
    }

    /**
     * Internal class representing a single progress bar image.
     */
    private class ProgressBar extends SvgImageSource {
        /**
         * The progress percentage for this image (0-100).
         */
        final int percent;

        /**
         * Constructs a progress bar image for the specified percentage.
         *
         * @param percent the progress percentage (0-100)
         */
        private ProgressBar(final int percent) {
            this.percent = percent;
        }

        @Override
        protected String getSvg() {
            final int cX = width / 2;
            final int cY = height / 2;
            final int radius = Math.max(1, (diameter - lineWidth) / 2);

            final String header = String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">",
                width,
                height,
                width,
                height
            );

            final String background = String.format(
                "<rect width=\"%d\" height=\"%d\" fill=\"%s\"/>",
                width,
                height,
                bgColor.getAlpha() == 0 ? "transparent" : bgColor.toString()
            );

            final String circle = String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"none\" stroke=\"%s\" stroke-width=\"%d\"/>",
                cX,
                cY,
                radius,
                circleColor.toString(),
                lineWidth
            );

            final String progress;
            if (this.percent > 0) {
                final double circumference = 2 * Math.PI * radius;
                final double progressLength = ((double) this.percent / 100.0) * circumference;
                progress = String.format(
                    "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"none\" stroke=\"%s\" " +
                        "stroke-width=\"%d\" stroke-linecap=\"round\" " +
                        "stroke-dasharray=\"%f %f\" transform=\"rotate(-90 %d %d)\"/>",
                    cX,
                    cY,
                    radius,
                    progressColor.toString(),
                    lineWidth,
                    progressLength,
                    circumference,
                    cX,
                    cY
                );
            } else {
                progress = "";
            }

            final String footer = "</svg>";
            return header + background + circle + progress + footer;
        }
    }

    /**
     * Internal class representing an animated indeterminate waiting indicator.
     */
    private class WaitingIndicator extends SvgImageSource {
        @Override
        protected String getSvg() {
            final int cX = width / 2;
            final int cY = height / 2;
            final int radius = Math.max(1, (diameter - lineWidth) / 2);
            final double circumference = 2 * Math.PI * radius;

            final double dash1 = circumference * 0.92;
            final double gap1 = circumference * 0.08;

            final double dash2 = circumference * 0.55;
            final double gap2 = circumference * 0.45;

            final double dash3 = circumference * 0.25;
            final double gap3 = circumference * 0.75;

            return String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">" +
                    "<rect width=\"%d\" height=\"%d\" fill=\"%s\"/>" +
                    "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"none\" stroke=\"%s\" stroke-width=\"%d\" opacity=\"0.25\"/>" +
                    "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"none\" stroke=\"%s\" stroke-width=\"%d\" " +
                    "stroke-linecap=\"round\" stroke-dasharray=\"%f %f\" transform=\"rotate(-90 %d %d)\">" +
                    "<animateTransform attributeName=\"transform\" type=\"rotate\" " +
                    "from=\"-90 %d %d\" to=\"270 %d %d\" dur=\"1.1s\" repeatCount=\"indefinite\"/>" +
                    "<animate attributeName=\"stroke-dasharray\" " +
                    "values=\"%f %f; %f %f; %f %f; %f %f\" " +
                    "dur=\"1.4s\" repeatCount=\"indefinite\"/>" +
                    "</circle>" +
                    "</svg>",
                width,
                height,
                width,
                height,
                width,
                height,
                bgColor.getAlpha() == 0 ? "transparent" : bgColor.toString(),
                cX,
                cY,
                radius,
                circleColor.toString(),
                lineWidth,
                cX,
                cY,
                radius,
                progressColor.toString(),
                lineWidth,
                dash1,
                gap1,
                cX,
                cY,
                cX,
                cY,
                cX,
                cY,
                dash1, gap1,
                dash2, gap2,
                dash3, gap3,
                dash1, gap1
            );
        }
    }
}