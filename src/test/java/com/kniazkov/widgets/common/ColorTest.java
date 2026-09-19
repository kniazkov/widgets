/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Regression tests for configuration colors and CSS-style concrete color syntax.
 */
public final class ColorTest {
    /**
     * parsesHexIncludingLogoColorAndAlphaLast.
     */
    @Test
    public void parsesHexIncludingLogoColorAndAlphaLast() {
        assertColor(new Color(192, 147, 92), "#C0935C", " #c0935c ");
        assertColor(new Color(170, 187, 204), "#abc", "#AABBCC");
        assertColor(new Color(170, 187, 204, 221), "#abcd", "#aabbccdd");
        assertColor(new Color(192, 147, 92, 128), "#C0935C80");
        assertColor(new Color(255, 255, 255, 0), "#fff0", "#ffffff00");
        assertColor(Color.WHITE, "#fff", "#ffff", "#ffffffff");
        assertEquals(0x80c0935c, Color.fromString("#c0935c80").pack());
    }

    /**
     * parsesRgbNumbersPercentagesAndSeparators.
     */
    @Test
    public void parsesRgbNumbersPercentagesAndSeparators() {
        assertColor(new Color(192, 147, 92), "rgb(192,147,92)", "RGB(192 147 92)",
            "rgba(192, 147, 92)", " rgb(\t192\n147 92) ", "rgb(1.92e2 147 92)");
        assertColor(new Color(255, 128, 0), "rgb(100%,50%,0%)", "rgb(100% 127.5 0)");
        assertColor(new Color(255, 0, 127, 128), "rgba(255,0,127,.5)",
            "rgb(255 0 127 / 50%)", "RGBA(255 0 127/.5)", "rgb(255,0,127,50%)");
        assertColor(new Color(255, 0, 128, 255), "rgba(300,-2,127.5,200%)");
        assertColor(new Color(255, 0, 128, 0), "rgb(200% -20% 50% / -1)");
    }

    /**
     * convertsHslAndWrapsAngles.
     */
    @Test
    public void convertsHslAndWrapsAngles() {
        assertColor(Color.RED, "hsl(0,100%,50%)", "hsl(360 100% 50%)",
            "hsl(-360deg 100% 50%)", "hsla(1turn 100% 50%)");
        assertColor(new Color(0, 255, 0), "hsl(120 100% 50%)", "hsl(480 100% 50%)");
        assertColor(Color.BLUE, "hsl(-120 100% 50%)");
        assertColor(Color.CYAN, "hsl(200grad 100% 50%)", "hsl(.5turn 100% 50%)",
            "hsl(3.141592653589793rad 100% 50%)");
        assertColor(Color.YELLOW, "hsl(60 100% 50%)");
        assertColor(new Color(255, 0, 255), "hsl(300 100% 50%)");
        assertColor(Color.GRAY, "hsl(20 0% 50%)");
        assertColor(new Color(48, 143, 48, 128), "hsla(120,50%,37.5%,50%)");
        assertColor(new Color(255, 0, 0, 128), "HSL(0 200% 50% / .5)");
        assertColor(Color.WHITE, "hsl(12 30% 200%)");
    }

    /**
     * preservesNamesAndClampingRegardlessOfLocale.
     */
    @Test
    public void preservesNamesAndClampingRegardlessOfLocale() {
        final Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertColor(Color.PINK, "pink", "Pink");
            assertColor(Color.LIGHT_GRAY, "light_gray", "LightGray", "LIGHTGREY");
            assertColor(Color.GREEN, "green");
            assertColor(Color.TRANSPARENT, "transparent");
            assertColor(Color.WHITE, "rgb(300,300,300)");
            assertColor(Color.RED, "rgba(255,-5,0,2)");
        } finally {
            Locale.setDefault(previous);
        }
    }

    /**
     * invalidInputFallsBackWithoutAcceptingPartialTokens.
     */
    @Test
    public void invalidInputFallsBackWithoutAcceptingPartialTokens() {
        assertColor(Color.BLACK, null, "", " ", "unknown", "#12", "#12345", "#1234567",
            "#123456789", "#ggg", "#１２３", "#abc garbage", "0xC0935C",
            "rgb(1,2,3,)", "rgb(1,2,)", "rgb(1 2 3 4)", "rgb(1,2 3)",
            "rgb(1,2,3 / .5)", "rgb(1 2 3 /)", "rgb(1 2 3 / .5 / .2)",
            "rgb(1,2%,3)", "rgb(1 2 NaN)", "rgb(1 2 Infinity)", "rgb(1 2 1e999)",
            "rgb(0x1p2 0 0)", "rgb(1f 2 3)", "rgb(1. 2 3)", "rgb(1 2 3) junk",
            "rgb (1 2 3)", "rgb(1 2 3))", "rgba(1,2,3,NaN)",
            "hsl(0 100 50)", "hsl(0 100% 50% /)", "hsl(1foo 100% 50%)",
            "hsl(NaN 100% 50%)", "hsl(0 10%% 50%)", "currentColor", "var(--brand)");
    }

    /**
     * hexValuesSurviveRenderingAndImagePacking.
     */
    @Test
    public void hexValuesSurviveRenderingAndImagePacking() {
        final Color color = Color.fromString("#C0935C");
        assertEquals("rgb(192,147,92)", color.toString());
        assertEquals(0xffc0935c, color.pack());
        assertEquals(new Color(192, 147, 92).toJsonObject().toString(),
            color.toJsonObject().toString());
        assertEquals(new Color(192, 147, 92).hashCode(), color.hashCode());
    }

    /**
     * Checks equivalent spellings against independently constructed channels.
     */
    private static void assertColor(final Color expected, final String... inputs) {
        for (final String input : inputs) {
            assertEquals(input, expected, Color.fromString(input));
        }
    }
}
