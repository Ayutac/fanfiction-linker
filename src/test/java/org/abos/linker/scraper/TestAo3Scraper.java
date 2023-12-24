package org.abos.linker.scraper;

import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Test class for {@link Ao3Scraper}
 */
public class TestAo3Scraper {

    @Test
    public void testScrapeFanfictions() throws IOException {
        ScraperTestUtil.doTestBlockingQueue(Ao3Scraper.TIME_OUT, new Ao3Scraper().scrapeFanfictions());
    }
}
