package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import java.time.Instant;

@Entity
public final class Url extends Model {
    @Id
    private long id;
    @Lob
    private String name;
    @WhenCreated
    private Instant createAt;

    public Url() {
    }

    public Url(String nameUrl) {
        this.name = nameUrl;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreateAt() {
        return createAt;
    }
}