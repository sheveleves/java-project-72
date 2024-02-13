package hexlet.code.model;

import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
public final class Url {
    private long id;
    private String name;
    private Timestamp createdAt;

    public Url(String nameUrl) {
        this.name = nameUrl;
    }

    public Instant getCreatedAtToInstant() {
        return this.createdAt.toInstant();
    }
}
