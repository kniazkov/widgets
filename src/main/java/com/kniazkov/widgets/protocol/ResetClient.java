/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.widgets.common.UId;

/**
 * Instruction that initiates a full client reset.
 */
class ResetClient extends Update {
    /**
     * Constructs a new reset instruction.
     */
    ResetClient() {
        super(UId.INVALID);
    }

    @Override
    protected String getAction() {
        return "reset";
    }
}
