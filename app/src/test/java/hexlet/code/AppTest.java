package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.testtools.JavalinTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import static org.assertj.core.api.Assertions.assertThat;

import io.javalin.Javalin;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

class AppTest {
    private static final String TITLE_PAGE = "Анализатор страниц";
    private static final String TEST_NAME_1 = "https://ru.hexlet.io";
    private static final String TEST_NAME_2 = "https://google.com";
    private static MockWebServer mockWebServer;


    private static Javalin app;
    private static Url url;
    private static String baseUrl = "http://localhost:";

    @BeforeAll
    public static void beforeAll() throws IOException {
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
    public void beforeEach() throws SQLException, IOException {
        app = App.getApp();

        UrlCheckRepository.truncate();
        UrlRepository.truncate();
        url = new Url(TEST_NAME_1);
        UrlRepository.save(url);
    }

    @Test
    void testMainPage() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(200);
            String responseBody = response.body().string();
            assertThat(responseBody).contains(TITLE_PAGE);
            assertThat(responseBody).contains("Анализатор страниц");
        });
    }

    @Test
    void testListUrls() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains(TEST_NAME_1);
        });
    }

    @Test
    void testShowUrl() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls/" + url.getId());
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains(TEST_NAME_1);
        });
    }

    @Test
    void testAddCorrectUrl() {
        JavalinTest.test(app, (server, client) -> {
            String hostUrl = baseUrl + app.port() + "/urls";

            assertThat(UrlRepository.findByName(TEST_NAME_2).isEmpty()).isTrue();

            HttpResponse<String> response = Unirest.get(hostUrl).asString();
            assertThat(response.getBody()).contains("ID", "Имя", "Последняя проверка", "Код ответа");
            assertThat(response.getBody()).contains(TEST_NAME_1);
            assertThat(response.getBody()).doesNotContain(TEST_NAME_2);

            HttpResponse postResponse = Unirest.post(hostUrl)
                    .field("url", TEST_NAME_2)
                    .asEmpty();
            assertThat(postResponse.getStatus()).isEqualTo(302);
            assertThat(UrlRepository.findByName(TEST_NAME_2).isPresent()).isTrue();

            response = Unirest.get(hostUrl).asString();
            assertThat(response.getBody()).contains(TEST_NAME_1, TEST_NAME_2);
            assertThat(response.getBody()).contains("Страница успешно добавлена");

            int entitiesListSize = UrlRepository.getEntities().size();

            Unirest.post(hostUrl)
                    .field("url", TEST_NAME_2)
                    .asEmpty();
            assertThat(UrlRepository.getEntities().size()).isEqualTo(entitiesListSize);

            response = Unirest.get(hostUrl).asString();
            assertThat(response.getBody()).contains("Страница уже существует");
        });
    }

    @Test
    void testIncorrectUrl() {
        JavalinTest.test(app, (server, client) -> {
            String hostUrl = baseUrl + app.port() + "/urls";
            String url = "google.com";
            int entitiesListSize = UrlRepository.getEntities().size();

            HttpResponse postResponse = Unirest.post(hostUrl)
                    .field("url", url)
                    .asEmpty();

            assertThat(postResponse.getStatus()).isEqualTo(302);
            assertThat(UrlRepository.getEntities().size()).isEqualTo(entitiesListSize);

            HttpResponse<String> response = Unirest.get(hostUrl).asString();
            assertThat(response.getBody()).contains("Некорректный URL", TITLE_PAGE);
        });
    }

    @Test
    void testCheckUrl() {
        JavalinTest.test(app, (server, client) -> {
            String hostUrl = baseUrl + app.port() + "/urls";
            String addressUrl = mockWebServer.url("/").toString();

            HttpResponse postResponse = Unirest.post(hostUrl)
                    .field("url", addressUrl)
                    .asEmpty();

            assertThat(postResponse.getStatus()).isEqualTo(302);

            Url url = UrlRepository.findByName(addressUrl.substring(0, addressUrl.length() - 1)).orElse(null);
            long id = url.getId();

            List<UrlCheck> urlChecks = UrlCheckRepository.findByUrlId(id);
            assertThat(urlChecks.isEmpty()).isTrue();

            postResponse = Unirest.post(hostUrl + "/" + id + "/checks")
                    .asEmpty();
            assertThat(postResponse.getStatus()).isEqualTo(302);

            urlChecks = UrlCheckRepository.findByUrlId(id);
            assertThat(urlChecks.isEmpty()).isFalse();

            UrlCheck urlCheck = UrlCheckRepository.findLatestChecks().get(id);
            assertThat(urlCheck.getDescription()).isEqualTo("Description of page is here.");
            assertThat(urlCheck.getH1()).isEqualTo("It's just for test");
            assertThat(urlCheck.getTitle()).isEqualTo("Here is title this page!");
            assertThat(urlCheck.getStatusCode()).isEqualTo(200);
        });
    }

    private static Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName)
                .toAbsolutePath().normalize();
    }

    private static String readFixture(String fileName) throws IOException {
        Path filePath = getFixturePath(fileName);
        return Files.readString(filePath).trim();
    }

    private static String readSqlFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = AppTest.class.getResourceAsStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
