package org.abos.linker.db;

import org.abos.common.LogUtil;
import org.abos.common.Named;
import org.abos.linker.core.Author;
import org.abos.linker.core.Fandom;
import org.abos.linker.core.Fanfiction;
import org.abos.linker.core.Tag;
import org.abos.linker.scraper.Ao3Scraper;
import org.abos.linker.scraper.WikiScraper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.postgresql.PGProperty;
import org.postgresql.jdbc.PgConnection;
import org.postgresql.util.HostSpec;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Class to work with the database.
 */
public final class DbHelper {

    public static final String TABLE_SETUP_FILE_NAME = "tableSetup.sql";

    public static final String TABLE_TEARDOWN_FILE_NAME = "tableTearDown.sql";

    public static final String PROPERTY_URL = "postgresql_url";

    public static final String PROPERTY_SU_NAME = "postgresql_su_name";

    public static final String PROPERTY_SU_PW = "postgresql_su_pw";

    public static final String TABLE_RATING = "rating";

    public static final String TABLE_FANDOM = "fandom";

    public static final String TABLE_AUTHOR = "author";

    public static final String TABLE_PROFILE = "profile";

    public static final String TABLE_LANGUAGE = "lang";

    public static final String TABLE_TAG = "tag";

    public static final String TABLE_FANFICTION = "fanfiction";

    public static final String TABLE_AUTHORED = "authored";

    public static final String INSERT_INTO_TAG_SQL = "INSERT INTO tag (name, description, is_character, is_relationship, fandom_id, link) VALUES (?,?,?,?,?,?)";

    private static final Logger LOGGER = LogManager.getLogger(DbHelper.class);

    private static final String LOG_SQL_MSG = "SQL about to be executed: {}";

    private final HostSpec[] specs = new HostSpec[1];

    private final Properties suInfo = new Properties();

    public DbHelper() throws IllegalStateException {
        final String url = System.getProperty(PROPERTY_URL);
        if (url == null) {
            throw new IllegalStateException("No url property given! Use: " + PROPERTY_URL);
        }
        final int portIndex = url.indexOf(':');
        final int port;
        final String host;
        final String dbName;
        if (portIndex != -1) {
            final String urlSnippet = url.substring(portIndex + 1);
            final int slashIndex = urlSnippet.indexOf('/');
            host = url.substring(0, portIndex);
            port = slashIndex == -1 ? Integer.parseInt(urlSnippet) : Integer.parseInt(urlSnippet.substring(0, slashIndex));
            dbName = urlSnippet.substring(slashIndex+1);
        }
        else {
            host = url;
            port = 5432;
            dbName = "linker";
        }
        specs[0] = new HostSpec(host, port);
        suInfo.put(PGProperty.PG_DBNAME.getName(), dbName);
        suInfo.put(PGProperty.USER.getName(), System.getProperty(PROPERTY_SU_NAME));
        suInfo.put(PGProperty.PASSWORD.getName(), System.getProperty(PROPERTY_SU_PW));
    }

    private Connection getConnection() throws SQLException {
        return new PgConnection(specs, suInfo, specs[0].getLocalSocketAddress());
    }

    /**
     * Replaces {@code "} and {@code '} in the specified string to prevent SQL injection.
     * @param input the string to be sanitized
     * @return The string in a sanitized version. {@code null} returns {@code null}.
     */
    public static String sanitizeString(final String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\"", "\\\"")
                .replace("'", "\\'");
    }

    /**
     * Use this instead of {@link PreparedStatement#setString(int, String)} to avoid SQL injection.
     * @param stmt the prepared statement to set the string into
     * @param index the parameter index
     * @param s the string to set
     * @throws SQLException If an SQL error occurs.
     */
    private static void setString(final PreparedStatement stmt, final int index, final String s) throws SQLException {
        stmt.setString(index, sanitizeString(s));
    }

    private Integer getIdBy(final Connection connection, final String table, final String type, final String what) throws SQLException {
        Objects.requireNonNull(table);
        Objects.requireNonNull(type);
        Objects.requireNonNull(what);
        final String selectSql = String.format("SELECT id FROM %s WHERE %s=?", table, type);
        LOGGER.debug(LOG_SQL_MSG, selectSql);
        try (final PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            setString(selectStmt, 1, what);
            try (final ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getInt(1);
            }
        }
    }

    private Integer getIdByName(final Connection connection, final String table, final String name) throws SQLException {
        return getIdBy(connection, table, "name", name);
    }

    /**
     * Polls from the {@link BlockingQueue} with a certain timeout.
     * @param queue the queue to poll from, not {@code null}
     * @param <T> the type of the queue
     * @return the polled element
     * @throws NullPointerException If {@code queue} refers to {@code null}.
     * @throws InterruptedException If this method gets interrupted.
     */
    private <T> T pollQueue(final BlockingQueue<T> queue) throws InterruptedException {
        return queue.poll(1, TimeUnit.SECONDS);
    }

    private void innerExecuteScript(final Connection connection, final String sql) throws SQLException {
        LOGGER.debug(LOG_SQL_MSG, sql);
        try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    // TODO this is unsafe if an attacker can modify the JAR, ask friend
    private void executeScript(final String resourceLocation) throws SQLException, IOException {
        final URL url = DbHelper.class.getClassLoader().getResource(resourceLocation);
        final String sql = Files.readString(new File(url.getFile()).toPath());
        try (final Connection connection = getConnection()) {
            innerExecuteScript(connection, sql);
        }
    }

    public void setupTables() throws IOException, SQLException {
        LOGGER.info("Setting up tables...");
        final Instant start = Instant.now();
        executeScript(TABLE_SETUP_FILE_NAME);
        final Duration time = Duration.between(start, Instant.now());
        LOGGER.info(LogUtil.LOG_TIME_MSG, "Setting up tables", time.toMinutes(), time.toSecondsPart());
    }

    public void tearDownTables() throws IOException, SQLException {
        LOGGER.info("Tearing down tables...");
        final Instant start = Instant.now();
        executeScript(TABLE_TEARDOWN_FILE_NAME);
        final Duration time = Duration.between(start, Instant.now());
        LOGGER.info(LogUtil.LOG_TIME_MSG, "Tearing down tables", time.toMinutes(), time.toSecondsPart());
    }

    private void internalInsertLanguage(final Connection connection, final String language) throws SQLException {
        String insertSql = String.format("INSERT INTO %s (name) VALUES (?)", TABLE_LANGUAGE);
        LOGGER.debug(LOG_SQL_MSG, insertSql);
        try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            setString(insertStmt, 1, Objects.requireNonNull(language));
            insertStmt.execute();
        }
    }

    private void fillInsertTagStmt(final PreparedStatement stmt, final Tag tag, final Integer fandomId) throws SQLException {
        setString(stmt, 1, tag.name());
        setString(stmt, 2, tag.description());
        stmt.setBoolean(3, tag.isCharacter());
        stmt.setBoolean(4, tag.isRelationship());
        if (fandomId == null) {
            stmt.setNull(5, JDBCType.INTEGER.getVendorTypeNumber());
        }
        else {
            stmt.setInt(5, fandomId);
        }
        setString(stmt, 6, tag.link());
    }

    private void internalInsertFandom(final Connection connection, final Fandom fandom) throws SQLException {
        final String insertSql;
        final boolean hasLink = fandom.link() != null;
        if (hasLink) {
            insertSql = String.format("INSERT INTO %s (name, link) VALUES (?, ?)", TABLE_FANDOM);
        }
        else {
            insertSql = String.format("INSERT INTO %s (name) VALUES (?)", TABLE_FANDOM);
        }
        LOGGER.debug(LOG_SQL_MSG, insertSql);
        try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            setString(insertStmt, 1, fandom.name());
            if (hasLink) {
                setString(insertStmt, 2, fandom.link());
            }
            insertStmt.execute();
        }
    }

    private int internalUpdateFandom(final Connection connection, final Fandom fandom) throws SQLException {
        Integer fandomId = getIdByName(connection, TABLE_FANDOM, fandom.name());
        // create if absent
        if (fandomId == null) {
            internalInsertFandom(connection, new Fandom(fandom.name(), null));
            fandomId = getIdByName(connection, TABLE_FANDOM, fandom.name());
            if (fandomId == null) {
                throw new IllegalStateException("Freshly created fandom " + fandom.name() + " has vanished!");
            }
            return fandomId;
        }
        // update link if present
        if (fandom.link() != null) {
            final String updateSql = String.format("UPDATE %s SET link=? WHERE id=?", TABLE_FANDOM);
            LOGGER.debug(LOG_SQL_MSG, updateSql);
            try (final PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                setString(updateStmt, 1, fandom.link());
                updateStmt.setInt(2, fandomId);
                updateStmt.execute();
            }
        }
        return fandomId;
    }

    private void internalUpdateTag(final Connection connection, final Tag tag) throws SQLException {
        final Integer tagId = getIdByName(connection, TABLE_TAG, tag.name());
        // get fandom ID after maybe adding fandom
        final boolean hasFandom = tag.fandom() != null;
        Integer fandomId = null;
        if (hasFandom) {
            fandomId = internalUpdateFandom(connection, new Fandom(tag.fandom(), null));
        }
        // if tag is not existent, create it
        if (tagId == null) {
            LOGGER.debug(LOG_SQL_MSG, INSERT_INTO_TAG_SQL);
            try (final PreparedStatement insertStmt = connection.prepareStatement(INSERT_INTO_TAG_SQL)) {
                fillInsertTagStmt(insertStmt, tag, fandomId);
                insertStmt.execute();
            }
        }
        // otherwise update it
        else {
            final StringBuilder updateSqlBuilder = new StringBuilder("UPDATE ");
            updateSqlBuilder.append(TABLE_TAG);
            updateSqlBuilder.append(" SET name=?");
            if (tag.description() != null) {
                updateSqlBuilder.append(", description=?");
            }
            updateSqlBuilder.append(", is_character=?, is_relationship=?");
            if (hasFandom) {
                updateSqlBuilder.append(", fandom_id=?");
            }
            if (tag.link() != null) {
                updateSqlBuilder.append(", link=?");
            }
            updateSqlBuilder.append("WHERE id=?");
            final String updateSql = updateSqlBuilder.toString();
            LOGGER.debug(LOG_SQL_MSG, updateSql);
            try (final PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                int index = 0;
                setString(updateStmt, ++index, tag.name());
                if (tag.description() != null) {
                    setString(updateStmt, ++index, tag.description());
                }
                updateStmt.setBoolean(++index, tag.isCharacter());
                updateStmt.setBoolean(++index, tag.isRelationship());
                if (hasFandom) {
                    updateStmt.setInt(++index, fandomId);
                }
                if (tag.link() != null) {
                    setString(updateStmt, ++index, tag.link());
                }
                updateStmt.setInt(++index, tagId);
                updateStmt.execute();
            }
        }
    }

    public void updateTag(final Tag tag) throws SQLException {
        try (final Connection connection = getConnection()) {
            internalUpdateTag(connection, tag);
        }
    }

    /**
     * Adds all tags in the queue to the DB. Will only partially succeed if a duplicate entry is detected.
     * @param queue the {@link BlockingQueue} with the tags that ends with {@link Tag#DUMMY}
     * @throws IllegalStateException If any specified fandom is not in the DB.
     * @throws SQLException If an SQL exception occurs, especially if a duplicate entry was attempted to be inserted.
     */
    public void addTags(final BlockingQueue<Tag> queue) throws IllegalStateException, SQLException {
        LOGGER.info("Adding tags...");
        final Instant start = Instant.now();
        final Map<String, Integer> fandomIds = new HashMap<>();
        try (final Connection connection = getConnection()) {
            LOGGER.debug(LOG_SQL_MSG, INSERT_INTO_TAG_SQL);
            try (final PreparedStatement insertStmt = connection.prepareStatement(INSERT_INTO_TAG_SQL)) {
                Tag current;
                while (true) {
                    try {
                        current = pollQueue(queue);
                        if (current == null) {
                            continue;
                        }
                        if (current.equals(Tag.DUMMY)) {
                            break;
                        }
                        Integer fandomId = null;
                        if (current.fandom() != null) {
                            fandomId = fandomIds.get(current.fandom());
                            if (fandomId == null) {
                                fandomId = getIdByName(connection, TABLE_FANDOM, current.fandom());
                                if (fandomId == null) {
                                    throw new IllegalStateException("Unknown fandom " + current.fandom() + " encountered!");
                                }
                                fandomIds.put(current.fandom(), fandomId);
                            }
                        }
                        fillInsertTagStmt(insertStmt, current, fandomId);
                        insertStmt.execute();
                    }
                    catch (InterruptedException e) {
                        /* Ignore. */
                    }
                } // -> while true
            } // -> try with PreparedStatement
        } // -> try with Connection
        final Duration time = Duration.between(start, Instant.now());
        LOGGER.info(LogUtil.LOG_TIME_MSG, "Adding tags", time.toMinutes(), time.toSecondsPart());
    }

    private void internalInsertTagAlias(final Connection connection, final int tagId, final String alias) throws SQLException {
        final String insertSql = "INSERT INTO tag_alias (tag_id, alias) VALUES (?,?)";
        LOGGER.debug(insertSql);
        try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setInt(1, tagId);
            setString(insertStmt, 2, alias);
        }
    }

    /**
     * Adds an alias for a tag name to the DB.
     * @param name the name of the tag
     * @param alias a new(!) alias for the tag
     * @throws IllegalStateException If the tag name was not to be found in the DB.
     * @throws SQLException If an SQL exception occurs, especially if a duplicate entry was attempted to be inserted.
     */
    public void addTagAlias(final String name, final String alias) throws IllegalStateException, SQLException {
        Objects.requireNonNull(name);
        Objects.requireNonNull(alias);
        try (final Connection connection = getConnection()) {
            final Integer id = getIdByName(connection, TABLE_TAG, name);
            if (id == null) {
                throw new IllegalStateException("Unknown tag name " + name + " encountered!");
            }
            internalInsertTagAlias(connection, id, alias);
        }
    }

    /**
     * Removes the given alias from the tag table and makes it an alias of the specified name.
     * @param name the name to make an alias of
     * @param alias the alias to be removed from the tag table
     */
    public void changeTagToAlias(final String name, final String alias) throws IllegalStateException, SQLException{
        Objects.requireNonNull(name);
        Objects.requireNonNull(alias);
        final String preformattedErrMsg = "Unknown tag %s encountered!";
        final String preformattedUpdateSQL = "UPDATE %s SET tag_id=? WHERE tag_id=?";
        try (final Connection connection = getConnection()) {
            final Integer tagId = getIdByName(connection, TABLE_TAG, name);
            if (tagId == null) {
                throw new IllegalStateException(String.format(preformattedErrMsg, name));
            }
            final Integer aliasId = getIdByName(connection, TABLE_TAG, alias);
            if (aliasId == null) {
                throw new IllegalStateException(String.format(preformattedErrMsg, alias));
            }
            // replace the IDs where needed
            final String updateTagAliasSql = String.format(preformattedUpdateSQL, "tag_alias");
            LOGGER.debug(LOG_SQL_MSG, updateTagAliasSql);
            try (final PreparedStatement aliasStmt = connection.prepareStatement(updateTagAliasSql)) {
                aliasStmt.setInt(1, tagId);
                aliasStmt.setInt(2, aliasId);
            }
            final String updateRelatedSql = String.format(preformattedUpdateSQL, "related");
            LOGGER.debug(LOG_SQL_MSG, updateRelatedSql);
            try (final PreparedStatement aliasStmt = connection.prepareStatement(updateRelatedSql)) {
                aliasStmt.setInt(1, tagId);
                aliasStmt.setInt(2, aliasId);
            }
            // add the alias
            internalInsertTagAlias(connection, tagId, alias);
        }
    }

    private String buildInsertLinks(final List<String> links, final int authorId) {
        final StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(TABLE_PROFILE);
        sql.append("(author_id,link) VALUES ");
        sql.append(links.stream().map(link -> "(" + authorId + ",'" + sanitizeString(link) + "')").collect(Collectors.joining(",")));
        sql.append(";");
        return sql.toString();
    }

    private void internalUpdateAuthorLinks(final Connection connection, final List<String> newLinks, final int authorId, final boolean replaceLinks) throws SQLException {
        final StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("BEGIN;");
        sqlBuilder.append(System.lineSeparator());
        if (replaceLinks) {
            sqlBuilder.append("DELETE FROM ");
            sqlBuilder.append(TABLE_PROFILE);
            sqlBuilder.append(" WHERE author_id=");
            sqlBuilder.append(authorId);
            sqlBuilder.append(";");
            sqlBuilder.append(System.lineSeparator());
            if (!newLinks.isEmpty()) {
                sqlBuilder.append(buildInsertLinks(newLinks, authorId));
                sqlBuilder.append(System.lineSeparator());
            }
        }
        else {
            if (newLinks.isEmpty()) {
                return;
            }
            final List<String> existingLinks = new LinkedList<>();
            final String selectSql = "SELECT (link) FROM " + TABLE_PROFILE + " WHERE author_id=" + authorId;
            LOGGER.debug(LOG_SQL_MSG, selectSql);
            try (final PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                try (final ResultSet rs = selectStmt.executeQuery()) {
                    while (rs.next()) {
                        existingLinks.add(rs.getString(1));
                    }
                }
            }
            final List<String> remainingLinks = new ArrayList<>(newLinks);
            remainingLinks.removeAll(existingLinks);
            if (!remainingLinks.isEmpty()) {
                sqlBuilder.append(buildInsertLinks(remainingLinks, authorId));
                sqlBuilder.append(System.lineSeparator());
            }
        }
        sqlBuilder.append("COMMIT;");
        final String sql = sqlBuilder.toString();
        LOGGER.debug(LOG_SQL_MSG, sql);
        try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    private void internalInsertAuthor(final Connection connection, final Author author, final boolean replaceLinks) throws SQLException {
        final String insertSql = String.format("INSERT INTO %s (name) VALUES (?)", TABLE_AUTHOR);
        LOGGER.debug(LOG_SQL_MSG, insertSql);
        try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            setString(insertStmt, 1, author.name());
            insertStmt.execute();
        }
        final Integer authorId = getIdByName(connection, TABLE_AUTHOR, author.name());
        if (authorId == null) {
            throw new IllegalStateException("Freshly inserted author " + author.name() + " vanished!");
        }
        internalUpdateAuthorLinks(connection, author.links(), authorId, replaceLinks);
    }

    private void internalUpdateAuthor(final Connection connection, final Author author, final boolean replaceLinks) throws SQLException {
        final Integer authorId = getIdByName(connection, TABLE_AUTHOR, author.name());
        if (authorId == null) {
            internalInsertAuthor(connection, author, replaceLinks);
        }
        else {
            internalUpdateAuthorLinks(connection, author.links(), authorId, replaceLinks);
        }
    }

    public void updateAuthor(final Author author, final boolean replaceLinks) throws SQLException {
        try (final Connection connection = getConnection()) {
            internalUpdateAuthor(connection, author, replaceLinks);
        }
    }

    // TODO JavaDoc This method expects all refs in the list to be in the DB already

    /**
     *
     * @param connection
     * @param refs
     * @param fanfictionId
     * @param refViewName the name of the view with the references
     * @param refTableName the name of the table with the references
     * @param tableName the name of the table where the references come from
     * @param refIdName
     * @param addAnon
     * @throws SQLException
     */
    private void internalUpdateFanfictionRefs(final Connection connection, final List<? extends Named> refs, final Integer fanfictionId, final String refViewName, final String refTableName, final String tableName, final String refIdName, final boolean addAnon) throws SQLException {
        final List<String> present = new LinkedList<>();
        final String selectSql = String.format("SELECT name FROM %s WHERE fanfiction_id=%d", refViewName, fanfictionId);
        LOGGER.debug(LOG_SQL_MSG, selectSql);
        try (final PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            try (final ResultSet rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    present.add(rs.getString(1));
                }
            }
        }
        final String insertSql = String.format("INSERT INTO %s (fanfiction_id, %s) VALUES (?,?)", refTableName, refIdName);
        LOGGER.debug(LOG_SQL_MSG, insertSql);
        try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            if (addAnon && refs.isEmpty()) {
                if (!present.contains("Anonymous")) {
                    insertStmt.setInt(1, fanfictionId);
                    insertStmt.setInt(2, 1); // 1 is first author which is Anon
                    insertStmt.execute();
                }
                return;
            }
            final List<? extends Named> remaining = refs.stream()
                    .filter(ref -> !present.contains(ref.getName()))
                    .toList();
            for (Named ref : remaining) {
                insertStmt.setInt(1, fanfictionId);
                final Integer refId = getIdByName(connection, tableName, ref.getName());
                if (refId == null) {
                    throw new IllegalStateException("Unknown ref " + ref + " encountered!");
                }
                insertStmt.setInt(2, refId);
                insertStmt.execute();
            }
        }
    }

    // TODO JavaDoc This method expects all authors in the list to be in the DB already
    private void internalUpdateAuthored(final Connection connection, final List<Author> authors, final Integer fanfictionId) throws SQLException {
        internalUpdateFanfictionRefs(connection, authors, fanfictionId, "authored_resolved", TABLE_AUTHORED, TABLE_AUTHOR, "author_id", true);
    }

    // TODO JavaDoc This method expects all tags in the list to be in the DB already
    private void internalUpdateTagged(final Connection connection, final List<Tag> tags, final Integer fanfictionId) throws SQLException {
        internalUpdateFanfictionRefs(connection, tags, fanfictionId, "tagged_resolved", "tagged", TABLE_TAG, "tag_id", false);
    }

    // TODO JavaDoc This method expects all fandoms in the list to be in the DB already
    private void internalUpdateCrossedOver(final Connection connection, final List<Fandom> fandoms, final Integer fanfictionId) throws SQLException {
        internalUpdateFanfictionRefs(connection, fandoms, fanfictionId, "crossed_over_resolved", "crossed_over", TABLE_FANDOM, "fandom_id", false);
    }

    private void internalInsertFanfiction(final Connection connection, final Fanfiction fanfiction) throws SQLException {
        if (fanfiction.lastUpdated() == null) {
            throw new NullPointerException("At this point a non-null last update must be in the fanfiction instance!");
        }
        // prepare command
        final StringBuilder insertSqlBuilder = new StringBuilder("INSERT INTO ");
        insertSqlBuilder.append(TABLE_FANFICTION);
        insertSqlBuilder.append(" (title, chapters, words, ");
        int extraCounter = 0;
        if (fanfiction.language() != null) {
            insertSqlBuilder.append("lang_id, ");
            extraCounter++;
        }
        if (fanfiction.rating() != null) {
            insertSqlBuilder.append("rating_id, ");
            extraCounter++;
        }
        insertSqlBuilder.append("warning_none_given, warning_none_apply, warning_violence, warning_rape, warning_death, warning_underage, ");
        insertSqlBuilder.append("cat_ff, cat_fm, cat_mm, cat_gen, cat_multi, cat_other, ");
        insertSqlBuilder.append("last_updated, ");
        if (fanfiction.lastChecked() != null) {
            insertSqlBuilder.append("last_checked, ");
            extraCounter++;
        }
        insertSqlBuilder.append("link) VALUES (");
        insertSqlBuilder.append("?,".repeat(16 + extraCounter));
        insertSqlBuilder.append("?)");
        // prepare optional IDs
        Integer languageId = null;
        if (fanfiction.language() != null) {
            languageId = getIdByName(connection, TABLE_LANGUAGE, fanfiction.language());
            if (languageId == null) {
                internalInsertLanguage(connection, fanfiction.language());
                languageId = getIdByName(connection, TABLE_LANGUAGE, fanfiction.language());
                if (languageId == null) {
                    throw new IllegalStateException("New language " + fanfiction.language() + " vanished!");
                }
            }
        }
        Integer ratingId = null;
        if (fanfiction.rating() != null) {
            ratingId = getIdByName(connection, TABLE_RATING, fanfiction.rating());
            if (ratingId == null) {
                throw new IllegalStateException("Unknown rating " + fanfiction.rating() + " encountered!");
            }
        }
        // fill out command and execute
        int index = 0;
        final String insertSql = insertSqlBuilder.toString();
        LOGGER.debug(LOG_SQL_MSG, insertSql);
        try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            setString(insertStmt, ++index, fanfiction.title());
            insertStmt.setInt(++index, fanfiction.chapters());
            insertStmt.setInt(++index, fanfiction.words());
            if (fanfiction.language() != null) {
                insertStmt.setInt(++index, languageId);
            }
            if (fanfiction.rating() != null) {
                insertStmt.setInt(++index, ratingId);
            }
            insertStmt.setBoolean(++index, fanfiction.warningNoneGiven());
            insertStmt.setBoolean(++index, fanfiction.warningNoneApply());
            insertStmt.setBoolean(++index, fanfiction.warningViolence());
            insertStmt.setBoolean(++index, fanfiction.warningRape());
            insertStmt.setBoolean(++index, fanfiction.warningDeath());
            insertStmt.setBoolean(++index, fanfiction.warningUnderage());
            insertStmt.setBoolean(++index, fanfiction.catFf());
            insertStmt.setBoolean(++index, fanfiction.catFm());
            insertStmt.setBoolean(++index, fanfiction.catMm());
            insertStmt.setBoolean(++index, fanfiction.catGen());
            insertStmt.setBoolean(++index, fanfiction.catMulti());
            insertStmt.setBoolean(++index, fanfiction.catOther());
            insertStmt.setLong(++index, fanfiction.lastUpdated().toEpochMilli());
            if (fanfiction.lastChecked() != null) {
                insertStmt.setLong(++index, fanfiction.lastChecked().toEpochMilli());
            }
            setString(insertStmt, ++index, fanfiction.link());
            insertStmt.execute();
        }
        final Integer fanfictionId = getIdBy(connection, TABLE_FANFICTION, "title", fanfiction.title());
        if (fanfictionId == null) {
            throw new IllegalStateException("Freshly created fanfiction " + fanfiction.title() + " vanished!");
        }
        for (Author author : fanfiction.authors()) {
            internalUpdateAuthor(connection, author, false);
        }
        internalUpdateAuthored(connection, fanfiction.authors(), fanfictionId);
        for (Tag tag : fanfiction.tags()) {
            internalUpdateTag(connection, tag);
        }
        internalUpdateTagged(connection, fanfiction.tags(), fanfictionId);
        for (Fandom fandom : fanfiction.crossovers()) {
            internalUpdateFandom(connection, fandom);
        }
        internalUpdateCrossedOver(connection, fanfiction.crossovers(), fanfictionId);
    }

    private void internalUpdateFanfiction(final Connection connection, final Fanfiction current, final int fanfictionId) {
        // TODO
    }

    public void updateFanfictions(final BlockingQueue<Fanfiction> queue) throws SQLException {
        LOGGER.info("Updating fanfictions...");
        final Instant start = Instant.now();
        try (final Connection connection = getConnection()) {
            Fanfiction current;
            while (true) {
                try {
                    current = pollQueue(queue);
                    if (current == null) {
                        continue;
                    }
                    if (current.equals(Fanfiction.DUMMY)) {
                        break;
                    }
                    Integer fanfictionId = null;
                    if (current.link() != null) {
                        fanfictionId = getIdBy(connection, TABLE_FANFICTION, "link", current.link());
                    }
                    if (fanfictionId == null) {
                        fanfictionId = getIdBy(connection, TABLE_FANFICTION, "title", current.title());
                    }
                    if (fanfictionId == null) {
                        internalInsertFanfiction(connection, current);
                    }
                    else {
                        internalUpdateFanfiction(connection, current, fanfictionId);
                    }
                }
                catch (InterruptedException ex) {
                    /* Ignore. */
                }
            } // -> while true
        } // -> try with Connection
        final Duration time = Duration.between(start, Instant.now());
        LOGGER.info(LogUtil.LOG_TIME_MSG, "Updating fanfictions", time.toMinutes(), time.toSecondsPart());
    }

    public static void main(String[] args) throws SQLException, IOException {
        if ("true".equals(System.getProperty("developer_mode"))) {
            Configurator.setRootLevel(Level.DEBUG);
        }
        final DbHelper dbHelper = new DbHelper();
        BlockingQueue<Tag> tagQueue = new WikiScraper().scrapeCharacterTags();
        BlockingQueue<Fanfiction> tagFiction = new Ao3Scraper().scrapeFanfictions();
        try {
            dbHelper.tearDownTables();
        }
        catch (SQLException ex) {
            /* Tables were already deleted, ignore. */
            ex.printStackTrace();
        }
        dbHelper.setupTables();
        dbHelper.addTags(tagQueue);
        dbHelper.updateFanfictions(tagFiction);
    }

}
