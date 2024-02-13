package hexlet.code.controllers;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UrlController {
    public static void addUrl(Context ctx) throws SQLException {
        String name = ctx.formParam("url");
        URL spec;
        try {
            spec = new URL(name.trim());
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }
        String protocol = spec.getProtocol() + "://";
        String host = spec.getHost();
        String port = spec.getPort() == -1 ? "" : ":" + spec.getPort();
        name = protocol + host + port;

        Url url = UrlRepository.findByName(name).orElse(null);
        if (url == null) {
            url = new Url(name);
            UrlRepository.save(url);
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
        } else {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "success");
        }
        ctx.redirect("/urls");
    }

    public static void listUrls(Context ctx) throws SQLException {
        int currentPage = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int per = 10;
        int begin = (currentPage - 1) * per;
        int end = begin + per;

        List<Url> pageUrls = UrlRepository.getEntities();
        List<Url> sliceOfUrls;

        if (begin >= pageUrls.size()) {
            sliceOfUrls = new ArrayList<>();
        } else if (end > pageUrls.size()) {
            sliceOfUrls = pageUrls.subList(begin, pageUrls.size());
        } else {
            sliceOfUrls = pageUrls.subList(begin, end);
        }

        Map<Long, UrlCheck> urlChecks = UrlCheckRepository.findLatestChecks();

        int lastPage = ((pageUrls.size() - 1) / per) + 2;

        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        ctx.attribute("currentPage", currentPage);
        ctx.attribute("pages", pages);
        ctx.attribute("urls", sliceOfUrls);
        ctx.attribute("pageUrls", pageUrls);
        ctx.attribute("urlChecks", urlChecks);
        ctx.render("urls/index.html");
    }

    public static void showUrl(Context ctx) throws SQLException {
        Long urlId = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        Url url = UrlRepository.findById(urlId)
                .orElseThrow(() -> new NotFoundResponse("Url with id = " + urlId + " not found"));
        List<UrlCheck> urlChecks = UrlCheckRepository.findByUrlId(urlId);
        ctx.attribute("url", url);
        ctx.attribute("urlChecks", urlChecks);
        ctx.render("urls/show.html");
    }

    public static void checkUrl(Context ctx) throws SQLException {
        Long urlId = ctx.pathParamAsClass("id", long.class).getOrDefault(null);
        Url url = UrlRepository.findById(urlId)
                .orElseThrow(() -> new NotFoundResponse("Url with id = " + urlId + " not found"));
        try {
            HttpResponse<String> response = Unirest.get(url.getName()).asString();
            Document doc = Jsoup.parse(response.getBody());
            String title = doc.title();
            Element h1Element = doc.selectFirst("h1");
            String h1 = Objects.isNull(h1Element) ? "" : h1Element.text();
            Element descriptionElement = doc.selectFirst("meta[name=description]");
            String description = Objects.isNull(descriptionElement) ? "" : descriptionElement.attr("content");
            int statusCode = response.getStatus();

            UrlCheck newUrlCheck = new UrlCheck(statusCode, title, h1, description, urlId);
            UrlCheckRepository.save(newUrlCheck);

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Некорректный адрес");
            ctx.sessionAttribute("flash-type", "danger");
        } catch (Exception e) {
            ctx.sessionAttribute("flash", e.getMessage());
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls/" + urlId);
    }
}
