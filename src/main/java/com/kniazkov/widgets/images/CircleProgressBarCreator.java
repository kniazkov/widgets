package com.kniazkov.widgets.images;

import com.kniazkov.widgets.common.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
     * Sets the diameter of the progress circle.
     * The diameter is automatically limited to fit within the image bounds.
     *
     * @param value the desired diameter in pixels
     */
    public void setDiameter(int value) {
        int maxDiam = Math.min(this.width, this.height);
        this.diameter = Math.min(maxDiam, value);
    }

    /**
     * Sets the background color of the progress bar image.
     *
     * @param value the background color
     */
    public void setBgColor(Color value) {
        this.bgColor = value;
    }

    /**
     * Sets the color of the background circle (the track).
     *
     * @param value the circle color
     */
    public void setCircleColor(Color value) {
        this.circleColor = value;
    }

    /**
     * Sets the color of the progress arc (the filled portion).
     *
     * @param value the progress color
     */
    public void setProgressColor(Color value) {
        this.progressColor = value;
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
        if (this.sources[percent] == null) {
            source = new ProgressBar(percent);
            this.sources[percent] = source;
        }
        return source;
    }

    /**
     * Internal class representing a single progress bar image.
     */
    private class ProgressBar implements ImageSource {
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
        public String toString() {
            final String header = String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" " +
                    "viewBox=\"0 0 %d %d\">",
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
            final int cX = width / 2;
            final int cY = height / 2;
            final int radius = diameter / 2;
            final String circle = String.format(
                "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"none\" stroke=\"%s\" " +
                    "stroke-width=\"2\"/>",
                cX,
                cY,
                radius,
                circleColor.toString()
            );
            final String progress;
            if (this.percent > 0) {
                final double circumference = 2 * Math.PI * radius;
                final double progressLength = ((double) this.percent / 100.0) * circumference;
                progress = String.format(
                    "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"none\" stroke=\"%s\" " +
                        "stroke-width=\"5\" stroke-linecap=\"round\" " +
                        "stroke-dasharray=\"%f %f\" " +
                        "transform=\"rotate(-90 %d %d)\"/>",
                    cX,
                    cY,
                    radius,
                    progressColor.toString(),
                    progressLength,
                    circumference,
                    cX,
                    cY
                );
            } else {
                progress = "";
            }
            final String footer = "</svg>";
            String svg = "";
            try {
                svg = URLEncoder.encode(
                    header + background + circle + progress + footer,
                    StandardCharsets.UTF_8.name()
                ).replaceAll("\\+", "%20");
            } catch (final UnsupportedEncodingException ignored) {
            }
            return "data:image/svg+xml," + svg;
        }
    }
}
