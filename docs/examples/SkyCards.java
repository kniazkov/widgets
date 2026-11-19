package example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.HeadingLevel;
import com.kniazkov.widgets.view.ImageWidget;
import com.kniazkov.widgets.view.Panel;
import com.kniazkov.widgets.view.PanelStyle;
import com.kniazkov.widgets.view.ResponsiveGrid;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

public class SkyCards {
    private static final class SkyCard extends Panel {
        SkyCard(PanelStyle style, String name, String photoUrl, String alt, String description) {
            super(style);
            ImageWidget photo = new ImageWidget(photoUrl);
            photo.setAlternativeText(alt);
            photo.setWidth(140);
            photo.setHeight(140);
            photo.setBorderRadius(70);
            Section portrait = new Section(photo);
            portrait.setCenterAlignment();
            portrait.setBottomMargin(16);

            TextWidget caption = new TextWidget("Mark as seen");
            Button button = new Button(caption);
            button.onClick(event -> {
                caption.setText("Seen");
                button.disable();
            });
            Section actions = new Section(button);
            actions.setTopMargin(12);

            add(portrait);
            add(new Section(heading(name, HeadingLevel.H2, "24px")));
            add(new Section(new TextWidget(description)));
            add(actions);
        }
    }

    private static TextWidget heading(String text, HeadingLevel level, String size) {
        TextWidget title = new TextWidget(text);
        title.setHeadingLevel(level);
        title.setFontSize(size);
        title.setFontWeight(FontWeight.BOLD);
        title.setColor(Color.fromString("#19344a"));
        return title;
    }

    public static void main(String[] args) {
        Page page = (root, context) -> {
            PanelStyle cardStyle = Panel.getDefaultStyle().derive();
            cardStyle.setBgColor(Color.WHITE);
            cardStyle.setBorderRadius(16);
            cardStyle.setPadding(20);

            ResponsiveGrid grid = new ResponsiveGrid();
            grid.setColumnWidths(280, 0);
            grid.setGap(16);
            grid.add(new SkyCard(cardStyle, "Moon", "/moon.jpg",
                "The Moon with dark plains and a bright crater",
                "Dark lava plains cover parts of the lunar surface."));
            grid.add(new SkyCard(cardStyle, "Mars", "/mars.jpg",
                "Rust-colored Mars with a bright polar cap",
                "Look for the bright polar cap near the top."));

            Section title = new Section(heading("Observing list", HeadingLevel.H1, "32px"));
            title.setBottomMargin(16);
            Panel content = new Panel(title, grid);
            content.setPadding(20);
            root.setBgColor(Color.fromString("#eaf0f4"));
            root.add(content);
        };
        Server.start(new Application(page), new Options.Builder().setPort(8080).build());
    }
}
