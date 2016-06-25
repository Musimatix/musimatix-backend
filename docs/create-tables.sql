CREATE TABLE songs (
    id INT UNSIGNED AUTO_INCREMENT,
    title_rus VARCHAR(255),
    title_eng VARCHAR(255),
    plain MEDIUMTEXT,
    year INT,
    vector MEDIUMBLOB,
    old_id INT UNSIGNED,
    PRIMARY KEY(id)
)
CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE rows (
    id INT UNSIGNED AUTO_INCREMENT,
    song_id INT UNSIGNED NOT NULL,
    idx INT UNSIGNED NOT NULL,
    plain MEDIUMTEXT NOT NULL,
    accents MEDIUMBLOB,
    vector MEDIUMBLOB,
    PRIMARY KEY(id),
    FOREIGN KEY (song_id) REFERENCES songs(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
)
CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE authors (
    id INT UNSIGNED AUTO_INCREMENT,
    name_rus VARCHAR(255),
    name_eng VARCHAR(255),
    old_id INT UNSIGNED,
    PRIMARY KEY(id)
)
CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE song_author (
    song_id INT UNSIGNED NOT NULL,
    author_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (song_id) REFERENCES songs(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    FOREIGN KEY (author_id) REFERENCES authors(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

CREATE TABLE tag_groups (
    id INT UNSIGNED AUTO_INCREMENT,
    name_rus VARCHAR(255),
    name_eng VARCHAR(255),
    PRIMARY KEY(id)
)
CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE tags (
    id INT UNSIGNED AUTO_INCREMENT,
	group_id INT UNSIGNED NOT NULL,
    name_rus VARCHAR(255),
    name_eng VARCHAR(255),
    PRIMARY KEY(id),
    FOREIGN KEY (group_id) REFERENCES tag_groups(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
)
CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE tagged (
    song_id INT UNSIGNED NOT NULL,
    tag_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (song_id) REFERENCES songs(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    FOREIGN KEY (tag_id) REFERENCES tags(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

CREATE TABLE fragments (
    id INT UNSIGNED AUTO_INCREMENT,
    song_id INT UNSIGNED NOT NULL,
    start_idx INT UNSIGNED NOT NULL,
    end_idx INT UNSIGNED NOT NULL,
    ftype INT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (song_id) REFERENCES songs(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

CREATE TABLE users (
    id INT UNSIGNED AUTO_INCREMENT,
    email VARCHAR(255),
    name VARCHAR(255),
    password VARCHAR(255)
    PRIMARY KEY(id)
)
CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE visits (
    user_id INT UNSIGNED NOT NULL,
    visit_time DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
 );

CREATE TABLE feedback (
    user_id INT UNSIGNED NOT NULL,
    fb_time DATETIME NOT NULL,
    text MEDIUMTEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
 );

INSERT INTO tag_groups (name_rus, name_eng) VALUES ('Язык', 'Language');
INSERT INTO tags (group_id, name_rus, name_eng) VALUES (1, 'Англ.', 'Eng');
INSERT INTO tags (group_id, name_rus, name_eng) VALUES (1, 'Рус.', 'Rus');
SELECT * FROM tags;
