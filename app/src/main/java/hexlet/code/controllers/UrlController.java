package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.List;
import java.util.Objects;

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

        Document parse = Jsoup.parse(urlResponse.getBody());
        String title = parse.title();
        Element h1Tag = parse.select("h1").first();
        String h1 = Objects.isNull(h1Tag) ? "" : h1Tag.text();
        Element nameDescriptionTag = parse.select("meta[name=description]").first();
        String description = Objects.isNull(nameDescriptionTag) ? "" : nameDescriptionTag.attr("content");
        int statusCode = urlResponse.getStatus();

        UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
        urlCheck.save();
        ctx.redirect("/urls/" + id);
    };
}

