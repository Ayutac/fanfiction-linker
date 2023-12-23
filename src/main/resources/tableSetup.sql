BEGIN;
CREATE TABLE rating (
  id    SERIAL,
  name  VARCHAR(63),
  PRIMARY KEY(id)
);
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
CREATE TABLE tag (
  id              SERIAL,
  name            VARCHAR(255)  UNIQUE NOT NULL,
  description     VARCHAR(511)  NOT NULL          DEFAULT '',
  is_character    BOOLEAN       NOT NULL          DEFAULT FALSE,
  is_relationship BOOLEAN       NOT NULL          DEFAULT FALSE,
  fandom_id       INT           REFERENCES fandom(id), -- allowed to be null
  link            TEXT          UNIQUE,
  PRIMARY KEY(id)
);
CREATE TABLE tag_alias (
  tag_id  INT           NOT NULL  REFERENCES tag(id),
  alias   VARCHAR(255)  NOT NULL,
  PRIMARY KEY(tag_id, alias)
);
CREATE TABLE related (
  character_id    INT NOT NULL  REFERENCES tag(id),
  relationship_id INT NOT NULL  REFERENCES tag(id),
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
  chapters            INT           NOT NULL  DEFAULT 1,
  words               INT           NOT NULL,
  lang_id             INT           NOT NULL  REFERENCES lang(id)   DEFAULT 1,
  rating_id           INT           NOT NULL  REFERENCES rating(id) DEFAULT 1,
  warning_none_given  BOOLEAN       NOT NULL  DEFAULT TRUE,
  warning_none_apply  BOOLEAN       NOT NULL  DEFAULT FALSE,
  warning_violence    BOOLEAN       NOT NULL  DEFAULT FALSE,
  warning_rape        BOOLEAN       NOT NULL  DEFAULT FALSE,
  warning_death       BOOLEAN       NOT NULL  DEFAULT FALSE,
  warning_underage    BOOLEAN       NOT NULL  DEFAULT FALSE,
  cat_ff              BOOLEAN       NOT NULL  DEFAULT FALSE,
  cat_fm              BOOLEAN       NOT NULL  DEFAULT FALSE,
  cat_mm              BOOLEAN       NOT NULL  DEFAULT FALSE,
  cat_gen             BOOLEAN       NOT NULL  DEFAULT FALSE,
  cat_multi           BOOLEAN       NOT NULL  DEFAULT FALSE,
  cat_other           BOOLEAN       NOT NULL  DEFAULT FALSE,
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