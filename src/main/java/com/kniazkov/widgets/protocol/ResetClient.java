/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.widgets.common.UId;

/**
 * Instruction that initiates a full client reset.
 */
public class ResetClient extends Update {
    /**
     * Constructs a new reset instruction.
     */
    public ResetClient() {
        super(UId.INVALID);
    }

    @Override
    public Update clone() {
        return new ResetClient();
    }

    @Override
    protected String getAction() {
        return "reset";
    }
}
