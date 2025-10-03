/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.protocol.Update;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;

public interface HasText extends View {
    Model<String> getTextModel();
    void setTextModel(Model<String> model);

    default String getText() {
        return this.getTextModel().getData();
    }

    default void setText(String text) {
        this.getTextModel().setData(text);
    }

    final class TextModelListener implements Listener<String> {
        private final Widget widget;

        public TextModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final String data) {
            final Update update = new Update(this.widget.getId()) {
                @Override
                protected String getAction() {
                    return "set text";
                }

                @Override
                protected void fillJsonObject(JsonObject json) {
                    json.addString("text", data);
                }
            };
            this.widget.update(update);
        }
    }
}
