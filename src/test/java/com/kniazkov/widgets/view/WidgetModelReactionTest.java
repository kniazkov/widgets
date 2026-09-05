/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.model.Model;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests translation of model changes into widget protocol updates.
 */
public final class WidgetModelReactionTest {
    /**
     * Verifies the textWidgetsReactToTheirTextModels behavior.
     */
    @Test
    public void textWidgetsReactToTheirTextModels() {
        final List<Widget<?>> widgets = Arrays.<Widget<?>>asList(
            new TextWidget(),
            new ActiveText(),
            new Link(),
            new InputField(),
            new PasswordInput(),
            new TextArea()
        );

        for (final Widget<?> widget : widgets) {
            final WidgetSandbox<?> sandbox = WidgetSandbox.open(widget);
            sandbox.clearUpdates();

            ((HasText) widget).getTextModel().setData("changed");

            final JsonObject update = singleUpdate(sandbox, "set text", widget);
            assertEquals("changed", update.get("text").getStringValue());
        }
    }

    /**
     * Verifies that links expose a reactive destination model with a safe local default.
     */
    @Test
    public void linksReactToTheirHrefModels() {
        final Link link = new Link("Documentation");
        assertEquals("#", link.getHref());
        final WidgetSandbox<Link> sandbox = WidgetSandbox.open(link);
        sandbox.clearUpdates();

        link.getHrefModel().setData("/documentation");

        final JsonObject update = singleUpdate(sandbox, "set href", link);
        assertEquals("/documentation", update.get("href").getStringValue());
    }

    /**
     * Verifies the imageWidgetsReactToTheirSourceModels behavior.
     */
    @Test
    public void imageWidgetsReactToTheirSourceModels() {
        final ImageWidget image = new ImageWidget("before.png");
        final WidgetSandbox<ImageWidget> imageSandbox = WidgetSandbox.open(image);
        imageSandbox.clearUpdates();

        image.getSourceModel().setData(ImageSource.fromHyperlink("after.png"));

        assertEquals(
            "after.png",
            singleUpdate(imageSandbox, "set source", image).get("source").getStringValue()
        );

        final ActiveImage activeImage = new ActiveImage("before.png");
        final WidgetSandbox<ActiveImage> activeSandbox = WidgetSandbox.open(activeImage);
        activeSandbox.clearUpdates();

        activeImage.getSourceModel(State.HOVERED).setData(
            ImageSource.fromHyperlink("hovered.png")
        );

        final JsonObject update = singleUpdate(activeSandbox, "set source", activeImage);
        assertEquals("hovered", update.get("state").getStringValue());
        assertEquals("hovered.png", update.get("source").getStringValue());
    }

    /**
     * Verifies the controlsReactToBehaviorModels behavior.
     */
    @Test
    public void controlsReactToBehaviorModels() {
        final Button button = new Button();
        final WidgetSandbox<Button> buttonSandbox = WidgetSandbox.open(button);
        final Model<Boolean> disabled = button.getDisabledStateModel();
        buttonSandbox.clearUpdates();
        disabled.setData(true);
        assertEquals(
            true,
            singleUpdate(buttonSandbox, "set disabled", button)
                .get("disabled").getBooleanValue()
        );

        final CheckBox checkBox = new CheckBox();
        final WidgetSandbox<CheckBox> checkBoxSandbox = WidgetSandbox.open(checkBox);
        checkBoxSandbox.clearUpdates();
        checkBox.getCheckedStateModel().setData(true);
        assertEquals(
            true,
            singleUpdate(checkBoxSandbox, "set checked", checkBox)
                .get("checked").getBooleanValue()
        );

        final RadioButton radioButton = new RadioButton();
        final WidgetSandbox<RadioButton> radioSandbox = WidgetSandbox.open(radioButton);
        radioSandbox.clearUpdates();
        radioButton.getCheckedStateModel().setData(true);
        assertEquals(
            true,
            singleUpdate(radioSandbox, "set checked", radioButton)
                .get("checked").getBooleanValue()
        );

        final DropDownList list = new DropDownList("first", "second");
        final WidgetSandbox<DropDownList> listSandbox = WidgetSandbox.open(list);
        listSandbox.clearUpdates();
        list.getSelectedIndexModel().setData(1);
        assertEquals(
            1,
            singleUpdate(listSandbox, "set selected index", list)
                .get("selected index").getIntValue()
        );

        final FileLoader loader = new FileLoader();
        final WidgetSandbox<FileLoader> loaderSandbox = WidgetSandbox.open(loader);
        final Model<String> acceptedFiles = loader.getAcceptedFilesModel();
        final Model<Boolean> multipleInput = loader.getMultipleInputModel();
        loaderSandbox.clearUpdates();
        acceptedFiles.setData("image/*");
        multipleInput.setData(true);

        final List<JsonObject> updates = loaderSandbox.drainUpdates();
        assertEquals(1, WidgetSandbox.findUpdates(updates, "set accepted files", loader).size());
        assertEquals(1, WidgetSandbox.findUpdates(updates, "set multiple input", loader).size());
    }

    /**
     * Verifies that inputFieldReactsToHorizontalAlignment behavior.
     */
    @Test
    public void inputFieldReactsToHorizontalAlignment() {
        final InputField input = new InputField();
        final WidgetSandbox<InputField> sandbox = WidgetSandbox.open(input);
        sandbox.clearUpdates();

        input.setCenterAlignment();

        final JsonObject update = singleUpdate(sandbox, "set horz alignment", input);
        assertEquals("center", update.get("horz alignment").getStringValue());
    }

    /**
     * Verifies the containersReactToTheirModels behavior.
     */
    @Test
    public void containersReactToTheirModels() {
        assertModelUpdate(
            new RootWidget(), "set bg color",
            widget -> ((RootWidget) widget).getBgColorModel(State.NORMAL).setData(Color.RED)
        );
        assertModelUpdate(
            new Panel(), "set bg color",
            widget -> ((Panel) widget).getBgColorModel(State.NORMAL).setData(Color.RED)
        );
        assertModelUpdate(
            new InlineBlock(), "set bg color",
            widget -> ((InlineBlock) widget).getBgColorModel(State.NORMAL).setData(Color.RED)
        );
        assertModelUpdate(
            new Row(), "set bg color",
            widget -> ((Row) widget).getBgColorModel(State.NORMAL).setData(Color.RED)
        );
        assertModelUpdate(
            new Cell(), "set bg color",
            widget -> ((Cell) widget).getBgColorModel(State.NORMAL).setData(Color.RED)
        );
        assertModelUpdate(
            new Section(), "set hidden",
            widget -> ((Section) widget).getHiddenStateModel().setData(true)
        );
        assertModelUpdate(
            new Table(), "set cell spacing",
            widget -> ((Table) widget).getCellSpacingModel().setData(
                new com.kniazkov.widgets.common.AbsoluteSize(4)
            )
        );
        final MarginDecorator decorator = new MarginDecorator(new TextWidget());
        decorator.getMarginModel();
        assertModelUpdate(
            decorator, "set margin",
            widget -> ((MarginDecorator) widget).getMarginModel().setData(
                new com.kniazkov.widgets.common.Offset(4)
            )
        );
    }

    /**
     * Operation that mutates a widget model.
     */
    private interface ModelChange {
        /**
         * Applies the change.
         *
         * @param widget target widget
         */
        void apply(Widget<?> widget);
    }

    /**
     * Verifies that one model mutation creates one update for the subject.
     *
     * @param widget widget under test
     * @param action expected action
     * @param change model mutation
     */
    private static void assertModelUpdate(final Widget<?> widget, final String action,
            final ModelChange change) {
        final WidgetSandbox<?> sandbox = WidgetSandbox.open(widget);
        sandbox.clearUpdates();

        change.apply(widget);

        assertEquals(1, WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), action, widget
        ).size());
    }

    /**
     * Returns the only matching update.
     *
     * @param sandbox widget sandbox
     * @param action expected action
     * @param widget target widget
     * @return matching update
     */
    private static JsonObject singleUpdate(final WidgetSandbox<?> sandbox, final String action,
            final Widget<?> widget) {
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), action, widget
        );
        assertEquals("update count for " + widget.getType() + ": " + action, 1, updates.size());
        return updates.get(0);
    }
}
