DROP TABLE IF EXISTS urls cascade;
DROP TABLE IF EXISTS url_checks;

CREATE TABLE urls
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP NOT NULL
);

CREATE TABLE url_checks
(
    id          SERIAL PRIMARY KEY,
    url_id      INT NOT NULL,
    status_code  INT NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    title       VARCHAR(500),
    h1          VARCHAR(500),
    description VARCHAR(500),
    FOREIGN KEY(url_id) REFERENCES urls(id)
);
