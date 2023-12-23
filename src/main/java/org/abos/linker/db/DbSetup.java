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
 * Class to set up the database.
 */
public final class DbSetup {

    public static final String SETUP_FILE_NAME = "dbSetup.sql";

    public static void main(String[] args) throws SQLException, IOException {
        final URL url = DbSetup.class.getClassLoader().getResource(SETUP_FILE_NAME);
        final String sql = Files.readString(new File(url.getFile()).toPath());
        final HostSpec spec = new HostSpec("localhost", 5432);
        final Properties info = new Properties();
        info.put(PGProperty.PG_DBNAME.getName(), "linker");
        info.put(PGProperty.USER.getName(), "postgres");
        info.put(PGProperty.PASSWORD.getName(), "postgres");
        try (final Connection connection = new PgConnection(new HostSpec[]{spec}, info, spec.getLocalSocketAddress())) {
            try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.execute();
            }
        }
    }

}
