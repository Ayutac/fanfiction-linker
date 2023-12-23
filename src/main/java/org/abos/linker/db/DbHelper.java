package org.abos.linker.db;

import org.abos.linker.core.Character;
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

    public static final String URL_PROPERTY = "postgresql_url";

    public static final String SU_NAME = "postgresql_su_name";

    public static final String SU_PW = "postgresql_su_pw";

    public static final String CHARACTER_TABLE = "character";

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

    public void setupTables() throws IOException, SQLException {
        final URL url = DbHelper.class.getClassLoader().getResource(TABLE_SETUP_FILE_NAME);
        final String sql = Files.readString(new File(url.getFile()).toPath());
        try (final Connection connection = getConnection()) {
            try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.execute();
            }
        }
    }

    /**
     * Adds all characters in the queue to the DB. Will only partially succeed if a duplicate entry is detected.
     * @param queue the {@link BlockingQueue} with the characters that ends with {@link Character#DUMMY}
     * @throws IllegalStateException If any specified fandom is not in the DB.
     * @throws SQLException If an SQL exception occurs, especially if a duplicate entry was attempted to be inserted.
     */
    public void addCharacters(final BlockingQueue<Character> queue) throws IllegalStateException, SQLException {
        final Map<String, Integer> fandomIds = new HashMap<>();
        try (final Connection connection = getConnection()) {
            final String insertSql = "INSERT INTO character (name, description, fandom_id, link) VALUES (?,?,?,?)";
            try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                Character current;
                while (true) {
                    try {
                        current = queue.poll(1, TimeUnit.SECONDS);
                        if (current == null) {
                            continue;
                        }
                        if (current.equals(Character.DUMMY)) {
                            break;
                        }
                        insertStmt.setString(1, current.name());
                        insertStmt.setString(2, current.description());
                        if (current.fandom() == null) {
                            insertStmt.setNull(3, JDBCType.INTEGER.getVendorTypeNumber());
                        }
                        else {
                            Integer id = fandomIds.get(current.fandom());
                            if (id == null) {
                                id = getIdByName(connection, "fandom", current.fandom());
                                fandomIds.put(current.fandom(), id);
                            }
                            insertStmt.setInt(3, id);
                        }
                        insertStmt.setString(4, current.link());
                        insertStmt.execute();
                    } catch (InterruptedException e) {
                        /* Ignore. */
                    }
                } // -> while true
            } // -> try with PreparedStatement
        } // -> try with Connection
    }

    private void internalAddCharacterAlias(final Connection connection, final int characterId, final String alias) throws SQLException {
        final String insertSql = "INSERT INTO character_alias (character_id, alias) VALUES (?,?)";
        try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setInt(1, characterId);
            insertStmt.setString(2, alias);
        }
    }

    /**
     * Adds an alias for a character name to the DB.
     * @param name the name of the character
     * @param alias a new(!) alias for the character
     * @throws IllegalStateException If the character name was not to be found in the DB.
     * @throws SQLException If an SQL exception occurs, especially if a duplicate entry was attempted to be inserted.
     */
    public void addCharacterAlias(final String name, final String alias) throws SQLException {
        Objects.requireNonNull(name);
        Objects.requireNonNull(alias);
        try (final Connection connection = getConnection()) {
            final int id = getIdByName(connection, CHARACTER_TABLE, name);
            internalAddCharacterAlias(connection, id, alias);
        }
    }

    /**
     * Removes the given alias from the character table and makes it an alias of the specified name.
     * @param name the name to make an alias of
     * @param alias the alias to be removed from the character table
     */
    public void changeCharacterToAlias(final String name, final String alias) throws IllegalStateException, SQLException{
        Objects.requireNonNull(name);
        Objects.requireNonNull(alias);
        final String preformattedUpdateSQL = "UPDATE %s SET character_id=? WHERE character_id=?";
        try (final Connection connection = getConnection()) {
            final int characterId = getIdByName(connection, CHARACTER_TABLE, name);
            final int aliasId = getIdByName(connection, CHARACTER_TABLE, alias);
            try (final PreparedStatement aliasStmt = connection.prepareStatement(String.format(preformattedUpdateSQL, "character_alias"))) {
                aliasStmt.setInt(1, characterId);
                aliasStmt.setInt(2, aliasId);
            }
            try (final PreparedStatement aliasStmt = connection.prepareStatement(String.format(preformattedUpdateSQL, "related"))) {
                aliasStmt.setInt(1, characterId);
                aliasStmt.setInt(2, aliasId);
            }
            internalAddCharacterAlias(connection, characterId, alias);
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        final DbHelper dbHelper = new DbHelper();
        BlockingQueue<Character> queue = new WikiScraper().scrapeCharacters();
        try {
            dbHelper.setupTables();
        }
        catch (SQLException ex) {
            /* Tables were already set up, ignore. */
        }
        dbHelper.addCharacters(queue);
    }

}
