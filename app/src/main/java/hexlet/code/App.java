package hexlet.code;


import io.javalin.Javalin;

public class App {
    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "8000");
        return Integer.valueOf(port);
    }
    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            config.plugins.enableDevLogging(); })
                .get("/", ctx -> ctx.result("Hello world"));
        return app;

    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }
}
