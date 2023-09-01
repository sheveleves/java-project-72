package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import static org.assertj.core.api.Assertions.assertThat;

import io.javalin.Javalin;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class AppTest {
    private static final String TITLE_PAGE = "Анализатор страниц";
    private static final String TEST_NAME_1 = "https://github.com";
    private static final String TEST_NAME_2 = "https://ru.hexlet.io";
    private static MockWebServer mockWebServer;


    private static Javalin app;
    private static String baseUrl;
    private static Database database;
    private static Transaction transaction;

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();

        mockWebServer = new MockWebServer();
        MockResponse mockResponse = new MockResponse().setBody(readFixture("testPage.html"));
        mockWebServer.enqueue(mockResponse);
        mockWebServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        mockWebServer.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        database.script().run("/truncate.sql");
        database.script().run("/seed.sql");
    }

    @Test
    void testMainPage() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains(TITLE_PAGE);
    }

    @Test
    void testShowUrls() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("ID", "Имя", "Последняя проверка", "Код ответа");
        assertThat(response.getBody()).contains(TEST_NAME_1, TEST_NAME_2);
        assertThat(response.getBody()).contains("26/07/2023", "17:54", "200");
    }

    @Test
    void testShowUrl() {
        Url url = new QUrl()
                .name.equalTo(TEST_NAME_1)
                .findOne();
        UrlCheck urlCheck = new QUrlCheck()
                .url.equalTo(url)
                .findOne();

        HttpResponse<String> response = Unirest.get(baseUrl + "/urls/" + url.getId()).asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains(TEST_NAME_1);
        assertThat(response.getBody()).doesNotContain(TEST_NAME_2);
        assertThat(response.getBody()).contains(urlCheck.getDescription(), urlCheck.getTitle(), urlCheck.getH1());
    }

    @Test
    void testAddCorrectUrl() {
        String url = "https://google.com";
        List<Url> list = new QUrl()
                .findList();
        assertThat(list.size()).isEqualTo(2);
        Url urlEntity = new QUrl()
                .name.equalTo(url)
                .findOne();
        Assertions.assertNull(urlEntity);
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        assertThat(response.getBody()).contains("ID", "Имя", "Последняя проверка", "Код ответа");
        assertThat(response.getBody()).contains(TEST_NAME_1, TEST_NAME_2);
        assertThat(response.getBody()).doesNotContain(url);

        HttpResponse postResponse = Unirest.post(baseUrl + "/urls")
                .field("url", url)
                .asEmpty();

        assertThat(postResponse.getStatus()).isEqualTo(302);

        list = new QUrl()
                .findList();
        assertThat(list.size()).isEqualTo(3);

        response = Unirest.get(baseUrl + "/urls").asString();
        assertThat(response.getBody()).contains(TEST_NAME_1, TEST_NAME_2, url);
        assertThat(response.getBody()).contains("Страница успешно добавлена");

        urlEntity = new QUrl()
                .name.equalTo(url)
                .findOne();
        Assertions.assertNotNull(urlEntity);

        postResponse = Unirest.post(baseUrl + "/urls")
                .field("url", url)
                .asEmpty();

        list = new QUrl()
                .findList();
        assertThat(list.size()).isEqualTo(3);

        response = Unirest.get(baseUrl + "/urls").asString();
        assertThat(response.getBody()).contains("Страница уже существует");
    }

    @Test
    void testIncorrectUrl() {
        String url = "google.com";
        HttpResponse postResponse = Unirest.post(baseUrl + "/urls")
                .field("url", url)
                .asEmpty();

        assertThat(postResponse.getStatus()).isEqualTo(302);
        List<Url> list = new QUrl()
                .findList();
        assertThat(list.size()).isEqualTo(2);

        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getBody()).contains("Некорректный URL", TITLE_PAGE);
    }

    @Test
    void testCheckUrl() {
        String addressUrl = mockWebServer.url("/").toString();

        HttpResponse postResponse = Unirest.post(baseUrl + "/urls")
                .field("url", addressUrl)
                .asEmpty();
        assertThat(postResponse.getStatus()).isEqualTo(302);

        Url url = new QUrl()
                .name.iequalTo(addressUrl.substring(0, addressUrl.length() - 1))
                .findOne();
        long id = url.getId();
        UrlCheck urlCheck = new QUrlCheck()
                .url.equalTo(url)
                .findOne();
        Assertions.assertNull(urlCheck);

        postResponse = Unirest.post(baseUrl + "/urls/" + id + "/checks")
                .asEmpty();

        assertThat(postResponse.getStatus()).isEqualTo(302);
        urlCheck = new QUrlCheck()
                .url.equalTo(url)
                .findOne();
        Assertions.assertNotNull(urlCheck);
        assertThat(urlCheck.getDescription()).isEqualTo("Description of page is here.");
        assertThat(urlCheck.getH1()).isEqualTo("It's just for test");
        assertThat(urlCheck.getTitle()).isEqualTo("Here is title this page!");
        assertThat(urlCheck.getStatusCode()).isEqualTo(200);
    }

    private static Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName)
                .toAbsolutePath().normalize();
    }

    private static String readFixture(String fileName) throws IOException {
        Path filePath = getFixturePath(fileName);
        return Files.readString(filePath).trim();
    }
}
