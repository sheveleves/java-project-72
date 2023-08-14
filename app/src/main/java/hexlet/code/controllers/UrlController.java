package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.net.URL;
import java.util.List;

public class UrlController {


    public static Handler addUrl = ctx -> {
        String name = ctx.formParam("url");
        URL spec;
        try {
            spec = new URL(name);
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }

        name = String.format("%s://%s%s", spec.getProtocol(), spec.getHost(),
                spec.getPort() == -1 ? "" : ":" + spec.getPort());

        Url url = new QUrl().name.equalTo(name).findOne();


        if (url == null) {
            url = new Url(name);
            url.save();
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
        } else {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "success");
        }

        ctx.redirect("/urls");

    };

    public static Handler showUrls = ctx -> {
        PagedList<Url> pageUrls = new QUrl()
                .orderBy()
                .id.asc()
                .setMaxRows(100)
                .findPagedList();

        List<Url> urls = pageUrls.getList();

        ctx.attribute("urls", urls);
        ctx.render("urls/index.html");
    };

    public static Handler showUrl = ctx -> {
        Long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();
        ctx.attribute("url", url);
        List<UrlCheck> urlChecks = url.getUrlChecks();
        ctx.attribute("urlChecks", urlChecks);

        ctx.render("urls/show.html");
    };

    public static Handler checkUrl = ctx -> {
        Long id = ctx.pathParamAsClass("id", long.class).getOrDefault(null);
        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();
        HttpResponse<String> urlResponse = Unirest.get(url.getName()).asString();
        int statusCode = urlResponse.getStatus();
        UrlCheck urlCheck = new UrlCheck(statusCode, "title" + url.getName(), "h1" + url.getName(), "description"
                + url.getName(), url);
        List<UrlCheck> urlChecks = url.getUrlChecks();
        urlChecks.add(urlCheck);
        urlCheck.save();
        ctx.redirect("/urls/" + id);

    };
}

