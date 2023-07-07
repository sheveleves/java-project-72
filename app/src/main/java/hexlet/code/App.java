package hexlet.code;


import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.javalin.Javalin;
import io.javalin.http.Handler;

import java.util.List;


public class App {
    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "8000");
        return Integer.valueOf(port);
    }
    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            config.plugins.enableDevLogging(); })
                .get("/", ctx -> ctx.result("Hello world"))
                .get("/url/", see)
                .post("/url/", create);
        return app;

    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }
    //for check up and debug temporarily
    private static Handler create = ctx -> {
        String urlParam = ctx.formParam("urlParam");
        Url url = new Url(urlParam);
        url.save();
    };

    private static Handler see = ctx -> {
        List<Url> urls = new QUrl()
//                .id.equalTo(1L)
                .findList();
        ctx.result(urls.toString());
    };
}
