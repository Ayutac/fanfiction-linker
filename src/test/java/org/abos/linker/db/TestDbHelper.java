package org.abos.linker.db;

import org.abos.linker.core.Author;
import org.abos.linker.core.Fandom;
import org.abos.linker.core.Fanfiction;
import org.abos.linker.core.FanfictionBuilder;
import org.abos.linker.core.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Test class for {@link DbHelper}.
 */
public final class TestDbHelper {

    private final Author noLinkAuthor = new Author("NoLinkAuthor", List.of());

    private final Tag simplestTag = new Tag("noDescNoLinkTag", null, false, false, null, null);

    private final Fandom noLinkFandom = new Fandom("NoLinkFandom", null);

    private DbHelper dbHelper;

    @BeforeAll
    public static void setupDbConfig() {
        System.setProperty(DbHelper.PROPERTY_URL, "localhost:5432/linker_test");
        System.setProperty(DbHelper.PROPERTY_SU_NAME, "postgres");
        System.setProperty(DbHelper.PROPERTY_SU_PW, "postgres");
    }

    @BeforeEach
    public void setupDb() throws SQLException, IOException {
        dbHelper = new DbHelper();
        // for cleanup after aborted tests
        try {
            dbHelper.tearDownTables();
        }
        catch (SQLException ex) {
            /* Ignore. */
        }
        dbHelper.setupTables();
    }

    @AfterEach
    public void tearDownDb() throws SQLException, IOException {
        dbHelper.tearDownTables();
        dbHelper = null;
    }

    @Test
    public void testUpdateFanfiction() throws SQLException {
        final BlockingQueue<Fanfiction> queue = new LinkedBlockingQueue<>();
        final FanfictionBuilder builder = new FanfictionBuilder("test", 1, 10, Instant.EPOCH, "");
        builder.author(noLinkAuthor).tag(simplestTag).crossover(noLinkFandom);
        queue.add(builder.build());
        queue.add(Fanfiction.DUMMY);
        dbHelper.updateFanfictions(queue);
    }

}
