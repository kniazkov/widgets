package example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.base.TabContext;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.view.FormField;
import com.kniazkov.widgets.view.HeadingLevel;
import com.kniazkov.widgets.view.Link;
import com.kniazkov.widgets.view.Panel;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class ObservingNotes {
    private final Map<TabContext, Model<String>> targets =
        Collections.synchronizedMap(new WeakHashMap<>());
    private final Model<String> notice = new StringModel().asSynchronized();

    public static void main(String[] args) {
        ObservingNotes notes = new ObservingNotes();
        Page planner = notes.page("Planner", "Open log", "/log");
        Page log = notes.page("Observing log", "Open planner", "/");
        Application app = new Application(planner);
        app.addPage("log", log);
        Server.start(app, new Options.Builder().setPort(8080).build());
    }

    private Page page(String title, String linkText, String destination) {
        return (root, context) -> {
            StringModel draft = new StringModel();
            Model<String> target = targets.computeIfAbsent(context.tabContext,
                tab -> new StringModel().asSynchronized());

            TextWidget heading = new TextWidget(title);
            heading.setHeadingLevel(HeadingLevel.H1);
            heading.setFontSize("26px");
            heading.setFontWeight(FontWeight.BOLD);
            Panel content = new Panel(
                new Section(heading),
                field("draft", "Page draft", draft),
                field("target", "Tab target", target),
                field("notice", "Shared notice", notice),
                new Section(new Link(linkText, destination))
            );
            content.setPadding(16);
            root.add(content);
        };
    }

    private static FormField field(String name, String label, Model<String> model) {
        FormField field = new FormField(name, label);
        field.getEditor().setTextModel(model);
        return field;
    }
}
