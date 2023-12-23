BEGIN;
CREATE TABLE tag (
  id    SERIAL,
  name  VARCHAR(255)  UNIQUE NOT NULL,
  nsfw  BOOLEAN       NOT NULL,
  PRIMARY KEY(id)
);
CREATE TABLE author (
  id            SERIAL,
  name          VARCHAR(255)  NOT NULL,
  PRIMARY KEY(id)
);
CREATE TABLE profile (
  author_id     INT   NOT NULL  REFERENCES author(id),
  link          TEXT  NOT NULL,
  PRIMARY KEY(author_id, link)
);
CREATE TABLE fanfiction (
  id            SERIAL,
  title         VARCHAR(255)  NOT NULL,
  chapters      INT           NOT NULL,
  words         INT           NOT NULL,
  last_updated  BIGINT        NOT NULL, -- millis since epoch
  last_checked  BIGINT        NOT NULL, -- millis since epoch
  link          TEXT          NOT NULL,
  PRIMARY KEY(id)
);
CREATE TABLE authored (
  fanfiction_id INT NOT NULL  REFERENCES fanfiction(id),
  author_id     INT NOT NULL  REFERENCES author(id),
  PRIMARY KEY(fanfiction_id, author_id)
);
CREATE TABLE tagged (
  fanfiction_id INT NOT NULL  REFERENCES fanfiction(id),
  tag_id        INT NOT NULL  REFERENCES tag(id),
  PRIMARY KEY(fanfiction_id, tag_id)
);
COMMIT;