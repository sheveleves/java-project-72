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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pageUrls = new QUrl()
                .setFirstRow(page * rowsPerPage)
                .orderBy()
                .id.asc()
                .setMaxRows(rowsPerPage)
                .findPagedList();

        int currentPage = pageUrls.getPageIndex() + 1;
        int lastPage = pageUrls.getTotalPageCount() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        List<Url> urls = pageUrls.getList();
        ctx.attribute("currentPage", currentPage);
        ctx.attribute("pages", pages);
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

