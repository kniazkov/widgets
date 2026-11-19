package example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.model.DefaultModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.FieldError;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Label;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.regex.Pattern;

public class ExtensionForm {
    private static final int DIGITS = 4;

    private static final class ExtensionModel extends DefaultModel<String> {
        private static final Pattern FORMAT = Pattern.compile("[0-9]{" + DIGITS + "}");

        ExtensionModel(String value) {
            super(value);
        }

        @Override
        public boolean isValid() {
            return FORMAT.matcher(getData()).matches();
        }

        @Override
        protected String getDefaultData() {
            return "";
        }

        @Override
        public Model<String> deriveWithData(String value) {
            return new ExtensionModel(value);
        }
    }

    public static void main(String[] args) {
        Page page = (root, context) -> {
            ExtensionModel draft = new ExtensionModel("");
            StringModel saved = new StringModel();

            InputField input = new InputField();
            input.setTextModel(draft);
            Label label = new Label("Phone extension");
            label.setTarget(input);

            var valid = draft.getValidFlagModel();
            FieldError error = new FieldError("Enter exactly " + DIGITS + " digits (0–9).");
            input.setAccessibleError(error);
            error.setHiddenStateModel(valid);

            Button button = new Button("Save");
            button.setDisabledStateModel(valid.invert());
            button.onClick(event -> save(draft, saved));
            input.onEnter(event -> save(draft, saved));

            TextWidget value = new TextWidget();
            value.setTextModel(saved);
            root.add(new Section(label, input));
            root.add(error);
            root.add(new Section(button));
            root.add(new Section(new TextWidget("Saved extension: "), value));
        };
        Server.start(new Application(page), new Options.Builder().setPort(8080).build());
    }

    private static void save(ExtensionModel draft, StringModel saved) {
        if (draft.isValid()) {
            saved.setData(draft.getData());
        }
    }
}
