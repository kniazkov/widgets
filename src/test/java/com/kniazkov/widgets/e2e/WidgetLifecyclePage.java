/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Link;
import com.kniazkov.widgets.view.Panel;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manual and automated browser fixture for temporary detach and terminal disposal.
 */
final class WidgetLifecyclePage {
    /**
     * Number of child nodes in each disposal batch.
     */
    private static final int BATCH_SIZE = 60;

    /**
     * Prevents construction of the fixture factory.
     */
    private WidgetLifecyclePage() {
        /*
         * Static entry point only.
         */
    }

    /**
     * Creates a page where two independent controls exercise the respective removal modes.
     *
     * @return browser acceptance fixture
     */
    static Page create() {
        return (root, context) -> {
            final Panel holder = new Panel();
            final Section inputSection = new Section(new InputField());
            holder.add(inputSection);
            final TextWidget state = new TextWidget("Input attached");
            final Button detach = new Button("Detach input");
            detach.onClick(event -> {
                inputSection.remove();
                state.setText("Input detached");
            });
            final Button attach = new Button("Attach input");
            attach.onClick(event -> {
                if (inputSection.getParent().isEmpty()) {
                    holder.add(inputSection);
                    state.setText("Input attached");
                }
            });
            final Panel batchHolder = new Panel();
            final AtomicInteger completed = new AtomicInteger();
            final TextWidget batchState = new TextWidget("Batches disposed: 0");
            final Button mount = new Button("Mount disposable batch");
            mount.onClick(event -> {
                if (batchHolder.getChildCount() == 0) {
                    final Section batch = new Section();
                    for (int index = 0; index < BATCH_SIZE; index++) {
                        batch.add(new TextWidget("Disposable child " + (index + 1)));
                    }
                    batchHolder.add(batch);
                    batchState.setText("Batch mounted");
                }
            });
            final Button dispose = new Button("Dispose batch");
            dispose.onClick(event -> {
                if (batchHolder.getChildCount() > 0) {
                    batchHolder.getChild(0).dispose();
                    batchState.setText("Batches disposed: " + completed.incrementAndGet());
                }
            });
            root.add(new Section(new TextWidget("Widget lifecycle fixture")));
            root.add(new Section(detach, attach, state));
            root.add(holder);
            root.add(new Section(mount, dispose, batchState));
            root.add(batchHolder);
            root.add(new Section(new Link("Next lifecycle page", "/catalog")));
        };
    }
}
