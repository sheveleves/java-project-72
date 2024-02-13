package hexlet.code.model;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
public final class UrlCheck {
    private long id;
    private Timestamp createdAt;
    private int statusCode;
    private String title;
    private String h1;
    private String description;
    private long urlId;

    public UrlCheck() {
    }

    public UrlCheck(int statusCode, String title, String h1, String description, long urlId) {
        this.statusCode = statusCode;
        this.title = title;
        this.h1 = h1;
        this.description = description;
        this.urlId = urlId;
    }

    public Instant getCreatedAtToInstant() {
        return this.createdAt.toInstant();
    }
}
