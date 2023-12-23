package org.abos.linker.db;

import org.abos.linker.core.Tag;
import org.abos.linker.scraper.WikiScraper;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Class to work with the database.
 */
public final class DbHelper {

    public static final String TABLE_SETUP_FILE_NAME = "tableSetup.sql";

    public static final String TABLE_TEARDOWN_FILE_NAME = "tableTearDown.sql";

    public static final String URL_PROPERTY = "postgresql_url";

    public static final String SU_NAME = "postgresql_su_name";

    public static final String SU_PW = "postgresql_su_pw";

    public static final String TAG_TABLE = "tag";

    private final HostSpec[] specs = new HostSpec[1];

    private final Properties suInfo = new Properties();

    public DbHelper() throws IllegalStateException {
        final String url = System.getProperty(URL_PROPERTY);
        if (url == null) {
            throw new IllegalStateException("No url property given! Use: " + URL_PROPERTY);
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
        suInfo.put(PGProperty.USER.getName(), System.getProperty(SU_NAME));
        suInfo.put(PGProperty.PASSWORD.getName(), System.getProperty(SU_PW));
    }

    private Connection getConnection() throws SQLException {
        return new PgConnection(specs, suInfo, specs[0].getLocalSocketAddress());
    }

    private int getIdByName(final Connection connection, final String table, final String name) throws SQLException {
        Objects.requireNonNull(table);
        Objects.requireNonNull(name);
        final String readSql = "SELECT id FROM " + table + " WHERE name=?";
        try (final PreparedStatement readStmt = connection.prepareStatement(readSql)) {
            readStmt.setString(1, name);
            try (final ResultSet rs = readStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Unknown name " + name + " in table " + table + " encountered!");
                }
                return rs.getInt(1);
            }
        }
    }

    private void innerExecuteScript(final Connection connection, final String sql) throws SQLException {
        try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    private void executeScript(final String resourceLocation) throws SQLException, IOException {
        final URL url = DbHelper.class.getClassLoader().getResource(resourceLocation);
        final String sql = Files.readString(new File(url.getFile()).toPath());
        try (final Connection connection = getConnection()) {
            innerExecuteScript(connection, sql);
        }
    }

    public void setupTables() throws IOException, SQLException {
        executeScript(TABLE_SETUP_FILE_NAME);
    }

    public void tearDownTables() throws IOException, SQLException {
        executeScript(TABLE_TEARDOWN_FILE_NAME);
    }

    /**
     * Adds all tags in the queue to the DB. Will only partially succeed if a duplicate entry is detected.
     * @param queue the {@link BlockingQueue} with the tags that ends with {@link Tag#DUMMY}
     * @throws IllegalStateException If any specified fandom is not in the DB.
     * @throws SQLException If an SQL exception occurs, especially if a duplicate entry was attempted to be inserted.
     */
    public void addTags(final BlockingQueue<Tag> queue) throws IllegalStateException, SQLException {
        final Map<String, Integer> fandomIds = new HashMap<>();
        try (final Connection connection = getConnection()) {
            final String insertSql = "INSERT INTO tag (name, description, is_character, is_relationship, fandom_id, link) VALUES (?,?,?,?,?,?)";
            try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                Tag current;
                while (true) {
                    try {
                        current = queue.poll(1, TimeUnit.SECONDS);
                        if (current == null) {
                            continue;
                        }
                        if (current.equals(Tag.DUMMY)) {
                            break;
                        }
                        insertStmt.setString(1, current.name());
                        insertStmt.setString(2, current.description());
                        insertStmt.setBoolean(3, current.isCharacter());
                        insertStmt.setBoolean(4, current.isRelationship());
                        if (current.fandom() == null) {
                            insertStmt.setNull(5, JDBCType.INTEGER.getVendorTypeNumber());
                        }
                        else {
                            Integer id = fandomIds.get(current.fandom());
                            if (id == null) {
                                id = getIdByName(connection, "fandom", current.fandom());
                                fandomIds.put(current.fandom(), id);
                            }
                            insertStmt.setInt(5, id);
                        }
                        insertStmt.setString(6, current.link());
                        insertStmt.execute();
                    } catch (InterruptedException e) {
                        /* Ignore. */
                    }
                } // -> while true
            } // -> try with PreparedStatement
        } // -> try with Connection
    }

    private void internalAddTagAlias(final Connection connection, final int tagId, final String alias) throws SQLException {
        final String insertSql = "INSERT INTO tag_alias (tag_id, alias) VALUES (?,?)";
        try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setInt(1, tagId);
            insertStmt.setString(2, alias);
        }
    }

    /**
     * Adds an alias for a tag name to the DB.
     * @param name the name of the tag
     * @param alias a new(!) alias for the tag
     * @throws IllegalStateException If the tag name was not to be found in the DB.
     * @throws SQLException If an SQL exception occurs, especially if a duplicate entry was attempted to be inserted.
     */
    public void addTagAlias(final String name, final String alias) throws SQLException {
        Objects.requireNonNull(name);
        Objects.requireNonNull(alias);
        try (final Connection connection = getConnection()) {
            final int id = getIdByName(connection, TAG_TABLE, name);
            internalAddTagAlias(connection, id, alias);
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
        final String preformattedUpdateSQL = "UPDATE %s SET tag_id=? WHERE tag_id=?";
        try (final Connection connection = getConnection()) {
            final int tagId = getIdByName(connection, TAG_TABLE, name);
            final int aliasId = getIdByName(connection, TAG_TABLE, alias);
            // replace the IDs where needed
            try (final PreparedStatement aliasStmt = connection.prepareStatement(String.format(preformattedUpdateSQL, "tag_alias"))) {
                aliasStmt.setInt(1, tagId);
                aliasStmt.setInt(2, aliasId);
            }
            try (final PreparedStatement aliasStmt = connection.prepareStatement(String.format(preformattedUpdateSQL, "related"))) {
                aliasStmt.setInt(1, tagId);
                aliasStmt.setInt(2, aliasId);
            }
            // add the alias
            internalAddTagAlias(connection, tagId, alias);
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        final DbHelper dbHelper = new DbHelper();
        BlockingQueue<Tag> queue = new WikiScraper().scrapeCharacterTags();
        try {
            dbHelper.tearDownTables();
        }
        catch (SQLException ex) {
            /* Tables were already deleted, ignore. */
        }
        dbHelper.setupTables();
        dbHelper.addTags(queue);
    }

}
