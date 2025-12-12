package com.kniazkov.widgets.images;

import com.kniazkov.widgets.common.Color;
import java.net.URLEncoder;

public class CircleProgressBarCreator {
    private final int width;
    private final int height;
    private final int diameter;
    private final Color bgColor;
    private final Color circleColor;
    private final Color progressColor;
    private final ImageSource[] sources;

    public CircleProgressBarCreator(final int width, final int height, final int diameter,
            final Color bgColor, final Color circleColor, final Color progressColor) {
        this.width = width;
        this.height = height;
        this.diameter = diameter;
        this.bgColor = bgColor;
        this.circleColor = circleColor;
        this.progressColor = progressColor;
        this.sources = new ImageSource[101];
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
            int centerX = width / 2;
            int centerY = height / 2;

            int circleRadius = diameter / 2;

            int strokeWidth = Math.max(1, circleRadius / 8);
            int progressRadius = circleRadius - strokeWidth / 2;

            // Calculate progress arc
            double circumference = 2 * Math.PI * progressRadius;
            double progressLength = ((double) percent / 100.0) * circumference;
            double dashOffset = circumference - progressLength;

            StringBuilder svg = new StringBuilder();
            svg.append("<svg xmlns='http://www.w3.org/2000/svg' ")
               .append("width='").append(width).append("' ")
               .append("height='").append(height).append("' ")
               .append("viewBox='0 0 ").append(width).append(" ").append(height).append("'>");

            if (bgColor.getAlpha() == 0) {
                svg.append("<rect width='100%' height='100%' fill='transparent'/>");
            } else {
                svg.append("<rect width='100%' height='100%' fill='").append(bgColor.toString()).append("'/>");
            }

            svg.append("<circle cx='").append(centerX)
               .append("' cy='").append(centerY)
               .append("' r='").append(progressRadius)
               .append("' fill='none' stroke='").append(circleColor.toString())
               .append("' stroke-width='").append(strokeWidth).append("'/>");

            if (percent > 0) {
                if (percent < 5) {
                    progressLength = Math.max(circumference * 0.05, strokeWidth * 2);
                    dashOffset = circumference - progressLength;
                }

                svg.append("<circle cx='").append(centerX)
                   .append("' cy='").append(centerY)
                   .append("' r='").append(progressRadius)
                   .append("' fill='none' stroke='").append(progressColor.toString())
                   .append("' stroke-width='").append(strokeWidth)
                   .append("' stroke-linecap='round")
                   .append("' stroke-dasharray='").append(progressLength).append(" ").append(circumference)
                   .append("' stroke-dashoffset='").append(dashOffset)
                   .append("' transform='rotate(-90 ").append(centerX).append(" ").append(centerY).append(")'/>");
            }

            // Percentage text (center of circle)
            int fontSize = Math.max(8, circleRadius / 2);
            int textX = centerX;
            int textY = centerY + fontSize / 3;

            // Choose text color with good contrast
            String textColor = circleColor.toString();

            svg.append("<text x='").append(textX)
               .append("' y='").append(textY)
               .append("' text-anchor='middle'")
               .append(" font-family='Arial, sans-serif' font-size='").append(fontSize)
               .append("' fill='").append(textColor)
               .append("' font-weight='bold'>")
               .append(percent).append("%")
               .append("</text>");

            svg.append("</svg>");

            // URL encode and return as data URL
            String encodedSvg = URLEncoder.encode(svg.toString())
                .replaceAll("\\+", "%20");
            return "data:image/svg+xml," + encodedSvg;
        }
    }
}
