INSERT INTO url (name, create_at) VALUES
('https://github.com', '2023-07-26 17:54:02'),
('https://ru.hexlet.io', '2023-07-26 17:54:42');

INSERT INTO url_check (status_code, title, h1, description, created_at, url_id) VALUES
(200, 'github title', 'github h1', 'github description', '2023-07-26 17:54:02', 1),
(200, 'hexlet title', 'hexlet h1', 'hexlet description', '2023-07-26 17:54:42', 2);