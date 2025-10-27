/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;

/**
 * A {@link View} that displays stylable text.
 * Extends {@link HasText} to support additional text style attributes:
 * <ul>
 *   <li><b>Font face</b> — the typeface used to render the text ({@link FontFace})</li>
 *   <li><b>Font size</b> — the text size in CSS units ({@link FontSize})</li>
 *   <li><b>Font weight</b> — the text weight (e.g., 400, 700) ({@link FontWeight})</li>
 *   <li><b>Italic</b> — whether the text is rendered in italics ({@code boolean})</li>
 * </ul>
 */
public interface HasStyledText extends HasText {
}
