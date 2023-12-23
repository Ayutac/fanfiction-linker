package org.abos.linker.db;

import org.postgresql.PGProperty;
import org.postgresql.jdbc.PgConnection;
import org.postgresql.util.HostSpec;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

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

    public DbHelper() {
        final String url = System.getProperty(URL_PROPERTY);
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

    public static void main(String[] args) throws SQLException, IOException {
        new DbHelper().setupTables();
    }

}
