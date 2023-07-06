package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.Instant;

@Entity
public final class Url extends Model {
    @Id
    private long id;
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

    public void setName(String nameUrl) {
        this.name = nameUrl;
    }

    @Override
    public String toString() {
        return "Url{" + "id=" + id
                + ", name='" + name + '\''
                + ", createAt=" + createAt + '}';
    }
}
