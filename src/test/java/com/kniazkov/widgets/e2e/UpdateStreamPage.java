/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.protocol.Update;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Own-application fixture simulating an unsupported update between two ordinary updates.
 */
final class UpdateStreamPage {
    /**
     * Static fixture only.
     */
    private UpdateStreamPage() {
    }

    /**
     * Creates a fresh model and a test-only fault control.
     *
     * @return integration fixture
     */
    static Page create() {
        return (root, context) -> {
            final AtomicInteger count = new AtomicInteger();
            final FixtureText label = new FixtureText();
            final Button increment = new Button("Increment model");
            increment.onClick(event -> label.setText("Model count: " + count.incrementAndGet()));
            final Button fault = new Button("Simulate unsupported update");
            fault.onClick(event -> label.failBetweenUpdates());
            root.add(new Section(increment, fault));
            root.add(new Section(label));
        };
    }

    /**
     * Text widget with a deliberately synthetic wire command for QA.
     */
    private static final class FixtureText extends TextWidget {
        /**
         * Creates the snapshot label.
         */
        FixtureText() {
            super("Model count: 0");
        }

        /**
         * Sends one valid prefix, the unsupported command and a marker that must not apply.
         */
        void failBetweenUpdates() {
            this.setText("Valid prefix applied");
            this.pushUpdate(new UnsupportedUpdate(this.getId()));
            this.setText("Trailing update must not be applied");
        }
    }

    /**
     * A synthetic unknown action. It has no protocol handler or application-side behavior.
     */
    private static final class UnsupportedUpdate extends Update {
        /**
         * @param widget identifier of the fixture label
         */
        UnsupportedUpdate(final RMId widget) {
            super(widget);
        }

        @Override
        protected String getAction() {
            return "fixture unsupported update";
        }

        @Override
        public Update clone() {
            return new UnsupportedUpdate(this.getWidgetId());
        }
    }
}
