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

    public void setupTables() throws IOException, SQLException {
        final URL url = DbHelper.class.getClassLoader().getResource(TABLE_SETUP_FILE_NAME);
        final String sql = Files.readString(new File(url.getFile()).toPath());
        try (final Connection connection = new PgConnection(specs, suInfo, specs[0].getLocalSocketAddress())) {
            try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.execute();
            }
        }
    }

    public void addCharacters(final BlockingQueue<Character> queue) throws SQLException {
        final Map<String, Integer> fandomIds = new HashMap<>();
        try (final Connection connection = new PgConnection(specs, suInfo, specs[0].getLocalSocketAddress())) {
            final String insertSql = "INSERT INTO character (name, description, fandom_id, link) VALUES (?,?,?,?)";
            final String readSql = "SELECT id FROM fandom WHERE name=?";
            try (final PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                Character current;
                while (!Character.DUMMY.equals(queue.peek())) {
                    try {
                        current = queue.poll(1, TimeUnit.SECONDS);
                        if (current == null) {
                            continue;
                        }
                        insertStmt.setString(1, current.name());
                        insertStmt.setString(2, current.description());
                        if (current.fandom() == null) {
                            insertStmt.setNull(3, JDBCType.INTEGER.getVendorTypeNumber());
                        }
                        else {
                            Integer id = fandomIds.get(current.fandom());
                            if (id != null) {
                                insertStmt.setInt(3, id);
                            }
                            else {
                                try (final PreparedStatement readStmt = connection.prepareStatement(readSql)) {
                                    readStmt.setString(1, current.fandom());
                                    try (final ResultSet rs = readStmt.executeQuery()) {
                                        if (!rs.next()) {
                                            throw new IllegalStateException("Unknown fandom " + current.fandom() + " encountered!");
                                        }
                                        fandomIds.put(current.fandom(), rs.getInt(1));
                                    } // -> try with ResultSet
                                } // -> try with PreparedStatement
                            } // -> if cached fandom id == null
                        } // -> if fandom != null
                        insertStmt.setString(4, current.link());
                        insertStmt.execute();
                    } catch (InterruptedException e) {
                        /* Ignore. */
                    }
                } // -> while queue not ended
            } // -> try with PreparedStatement
        } // -> try with Connection
    }

    public static void main(String[] args) throws SQLException, IOException {
        final DbHelper dbHelper = new DbHelper();
        BlockingQueue<Character> queue = new WikiScraper().scrapeCharacters();
        dbHelper.setupTables();
        dbHelper.addCharacters(queue);
    }

}
