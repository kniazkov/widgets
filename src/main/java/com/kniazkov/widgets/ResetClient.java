/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Instruction that initiates a client reset.
 */
class ResetClient extends Instruction {
    /**
     * Constructor.
     */
    ResetClient() {
        super(UId.INVALID);
    }

    @Override
    protected @NotNull String getAction() {
        return "reset";
    }
}
