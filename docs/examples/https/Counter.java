package example;

import com.kniazkov.webserver.SslOptions;
import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Counter {
    public static void main(String[] args) throws UnknownHostException {
        Options options = listener(args);
        Page page = (root, context) -> {
            IntegerModel count = new IntegerModel();
            TextWidget value = new TextWidget();
            value.setTextModel(new IntegerToStringModel(count));

            Button button = new Button("Click me");
            button.onClick(event -> count.setData(count.getData() + 1));
            root.add(new Section(button, new TextWidget("Click counter: "), value));
        };
        Server.start(new Application(page), options);
    }

    private static Options listener(String[] args) throws UnknownHostException {
        Options.Builder options = new Options.Builder()
            .setBindAddress(InetAddress.getByName("127.0.0.1"));
        if (args.length == 3 && "direct".equals(args[0])) {
            SslOptions tls = new SslOptions.Builder()
                .setCertificateChainFile(args[1])
                .setPrivateKeyFile(args[2])
                .build();
            return options.setPort(8443).setSslOptions(tls).build();
        }
        if (args.length == 2 && "proxy".equals(args[0])) {
            return options.setPort(8080).setPublicOrigin(args[1]).build();
        }
        throw new IllegalArgumentException("Use direct <chain.pem> <key.pem> or proxy <public-origin>");
    }
}
