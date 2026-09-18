/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.widgets.common.RMId;

/**
 * Instruction that initiates a discarding cached pages after an authentication change.
 */
public final class ClearPageCache extends Update {
    /**
     * Constructs a cache invalidation instruction.
     */
    public ClearPageCache() {
        super(RMId.INVALID);
    }

    @Override
    public Update clone() {
        return new ClearPageCache();
    }

    @Override
    protected String getAction() {
        return "clear page cache";
    }
}
