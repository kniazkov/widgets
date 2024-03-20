/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * An instruction that is sent from server to client to display changes in widgets.
 */
interface Instruction {
    /**
     * Returns unique identifier of the instruction.
     * @return Unique identifier of the instruction
     */
    @NotNull UId getId();

    /**
     * Fills a JSON object with the data needed to execute the instruction on the client side.
     * @param json JSON object to be filled
     */
    void fillJsonObject(final @NotNull JsonObject json);
}
