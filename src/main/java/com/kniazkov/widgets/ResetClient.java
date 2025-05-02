/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Instruction that initiates a full client reset.
 * <p>
 *     This instruction is used to clear the current client-side state and reinitialize
 *     everything from scratch. It is typically sent when the server determines that the UI
 *     must be rebuilt entirely.
 * </p>
 */
class ResetClient extends Instruction {
    /**
     * Constructs a new reset instruction.
     * <p>
     *     The widget ID is set to {@link UId#INVALID}, since this instruction is not tied
     *     to a specific widget.
     * </p>
     */
    ResetClient() {
        super(UId.INVALID);
    }

    @Override
    protected String getAction() {
        return "reset";
    }
}
