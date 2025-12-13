package com.kniazkov.widgets.images;

import com.kniazkov.widgets.common.Color;
import java.net.URLEncoder;

public class CircleProgressBarCreator {
    private final int width;
    private final int height;
    private int diameter;
    private Color bgColor = Color.TRANSPARENT;
    private Color circleColor = Color.LIGHT_GRAY;
    private Color progressColor = Color.BLACK;

    private final ImageSource[] sources;

    public CircleProgressBarCreator(final int width, final int height) {
        this.width = width;
        this.height = height;
        this.diameter = Math.min(width, height) / 3;
        this.sources = new ImageSource[101];
    }

    public void setDiameter(int value) {
        int maxDiam = Math.min(this.width, this.height);
        this.diameter = Math.min(maxDiam, value);
    }

    public void setBgColor(Color value) {
        this.bgColor = value;
    }

    public void setCircleColor(Color value) {
        this.circleColor = value;
    }

    public void setProgressColor(Color value) {
        this.progressColor = value;
    }

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

    private class ProgressBar implements ImageSource {
        final int percent;

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
            // URL encode and return as data URL
            String svg = URLEncoder.encode(
                header + background + circle + progress + footer
            ).replaceAll("\\+", "%20");
            return "data:image/svg+xml," + svg;
        }
    }
}
