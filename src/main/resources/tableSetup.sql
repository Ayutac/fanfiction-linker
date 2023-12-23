BEGIN;
CREATE TABLE tag (
  id          SERIAL,
  name        VARCHAR(255)  UNIQUE NOT NULL,
  description VARCHAR(511)  NOT NULL          DEFAULT '',
  PRIMARY KEY(id)
);
CREATE TABLE rating (
  PRIMARY KEY(id)
) INHERITS (tag);
INSERT INTO rating (name)
VALUES
  ('Not rated'),
  ('General Audiences'),
  ('Teen and Up Audiences'),
  ('Mature'),
  ('Explicit');
CREATE TABLE fandom (
  id            SERIAL,
  name          VARCHAR(255)  UNIQUE NOT NULL,
  link          TEXT          UNIQUE NOT NULL,
  PRIMARY KEY(id)
);
CREATE TABLE character (
  fandom_id INT   REFERENCES fandom(id), -- allowed to be null
  link      TEXT  UNIQUE,
  PRIMARY KEY(id)
) INHERITS (tag);
CREATE TABLE character_alias (
  character_id  INT           NOT NULL  REFERENCES character(id),
  alias         VARCHAR(255)  NOT NULL,
  PRIMARY KEY(character_id, alias)
);
CREATE TABLE relationship (
  PRIMARY KEY(id)
) INHERITS (tag);
CREATE TABLE related (
  character_id    INT NOT NULL  REFERENCES character(id),
  relationship_id INT NOT NULL  REFERENCES relationship(id),
  PRIMARY KEY(character_id, relationship_id)
);
CREATE TABLE author (
  id            SERIAL,
  name          VARCHAR(255)  NOT NULL,
  PRIMARY KEY(id)
);
INSERT INTO author (name)
VALUES ('Anonymous');
CREATE TABLE profile (
  author_id     INT   NOT NULL  REFERENCES author(id),
  link          TEXT  NOT NULL,
  PRIMARY KEY(author_id, link)
);
CREATE TABLE lang ( -- recognized languages
  id    SERIAL,
  name  VARCHAR(63) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);
INSERT INTO lang (name)
VALUES ('English');
CREATE TABLE fanfiction (
  id                  SERIAL,
  title               VARCHAR(255)  NOT NULL,
  chapters            INT           NOT NULL,
  words               INT           NOT NULL,
  lang_id             INT           NOT NULL  REFERENCES lang(id)   DEFAULT 1,
  rating_id           INT           NOT NULL  REFERENCES rating(id) DEFAULT 1,
  warning_none_given  BOOLEAN       NOT NULL,
  warning_none_apply  BOOLEAN       NOT NULL,
  warning_violence    BOOLEAN       NOT NULL,
  warning_rape        BOOLEAN       NOT NULL,
  warning_death       BOOLEAN       NOT NULL,
  warning_underage    BOOLEAN       NOT NULL,
  cat_ff              BOOLEAN       NOT NULL,
  cat_fm              BOOLEAN       NOT NULL,
  cat_mm              BOOLEAN       NOT NULL,
  cat_gen             BOOLEAN       NOT NULL,
  cat_multi           BOOLEAN       NOT NULL,
  cat_other           BOOLEAN       NOT NULL,
  completed           BOOLEAN       NOT NULL  DEFAULT FALSE,
  last_updated        BIGINT        NOT NULL, -- millis since epoch
  last_checked        BIGINT        NOT NULL  DEFAULT FLOOR(EXTRACT(EPOCH from NOW())*1000), -- millis since epoch
  link                TEXT          NOT NULL,
  PRIMARY KEY(id)
);
CREATE TABLE authored (
  fanfiction_id INT NOT NULL  REFERENCES fanfiction(id),
  author_id     INT NOT NULL  REFERENCES author(id)       DEFAULT 1,
  PRIMARY KEY(fanfiction_id, author_id)
);
CREATE TABLE tagged (
  fanfiction_id INT NOT NULL  REFERENCES fanfiction(id),
  tag_id        INT NOT NULL  REFERENCES tag(id),
  PRIMARY KEY(fanfiction_id, tag_id)
);
CREATE TABLE crossed_over (
  fanfiction_id INT NOT NULL  REFERENCES fanfiction(id),
  fandom_id     INT NOT NULL  REFERENCES fandom(id),
  PRIMARY KEY(fanfiction_id, fandom_id)
);
COMMIT;